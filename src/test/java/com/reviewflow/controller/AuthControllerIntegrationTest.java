package com.reviewflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewflow.model.dto.request.LoginRequest;
import com.reviewflow.model.entity.User;
import com.reviewflow.model.entity.UserRole;
import com.reviewflow.repository.RefreshTokenRepository;
import com.reviewflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collection;
import java.util.stream.Collectors;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("reviewflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        activeUser = User.builder()
                .email("active@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Active")
                .lastName("User")
                .role(UserRole.STUDENT)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(activeUser);

        inactiveUser = User.builder()
                .email("inactive@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Inactive")
                .lastName("User")
                .role(UserRole.STUDENT)
                .isActive(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(inactiveUser);
    }

    @Test
    void login_success_returns200AndSetsCookies() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("active@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(activeUser.getId()))
                .andExpect(jsonPath("$.data.email").value("active@example.com"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("active@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_inactiveUser_returns403() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_validToken_returns200AndNewCookies() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("active@example.com");
        request.setPassword("password123");
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String cookieHeader = toCookieHeader(result.getResponse().getHeaders("Set-Cookie"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Cookie", cookieHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Token refreshed"));
    }

    private static String toCookieHeader(Collection<String> setCookieHeaders) {
        return setCookieHeaders.stream()
                .map(h -> h.contains(";") ? h.substring(0, h.indexOf(';')).trim() : h)
                .collect(Collectors.joining("; "));
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookiesAndRevokesToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("active@example.com");
        request.setPassword("password123");
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String cookieHeader = toCookieHeader(loginResult.getResponse().getHeaders("Set-Cookie"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Cookie", cookieHeader))
                .andExpect(status().isOk());

        // After logout, refresh with old cookie should fail (token revoked)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Cookie", cookieHeader))
                .andExpect(status().isUnauthorized());
    }
}
