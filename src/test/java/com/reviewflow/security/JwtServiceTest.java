package com.reviewflow.security;

import com.reviewflow.model.entity.User;
import com.reviewflow.model.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "a".repeat(32) + "b".repeat(32);

    private JwtService jwtService;
    private ReviewFlowUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.INSTRUCTOR)
                .isActive(true)
                .build();
        userDetails = new ReviewFlowUserDetails(user);
    }

    @Test
    void generateAccessToken_containsEmailAndRole() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("INSTRUCTOR");
    }

    @Test
    void generateRefreshToken_returnsNonBlankToken() {
        String token = jwtService.generateRefreshToken();
        assertThat(token).isNotBlank();
    }

    @Test
    void isTokenValid_returnsTrue_forValidToken() {
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forWrongUser() {
        String token = jwtService.generateAccessToken(userDetails);
        User other = User.builder().id(2L).email("other@example.com").passwordHash("x").role(UserRole.STUDENT).isActive(true).build();
        assertThat(jwtService.isTokenValid(token, new ReviewFlowUserDetails(other))).isFalse();
    }

    @Test
    void isTokenExpired_returnsTrue_afterExpiry() {
        JwtService shortLived = new JwtService(SECRET, -1000L, 60000L);
        String token = shortLived.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    void isTokenExpired_returnsFalse_forFreshToken() {
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forNullOrBlankToken() {
        assertThat(jwtService.isTokenValid(null, userDetails)).isFalse();
        assertThat(jwtService.isTokenValid("", userDetails)).isFalse();
        assertThat(jwtService.isTokenValid("   ", userDetails)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        assertThat(jwtService.isTokenValid("invalid.token.here", userDetails)).isFalse();
    }
}
