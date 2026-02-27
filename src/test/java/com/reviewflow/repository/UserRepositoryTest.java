package com.reviewflow.repository;

import com.reviewflow.model.entity.User;
import com.reviewflow.model.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_returnsUser_whenExists() {
        User user = User.builder()
                .email("jane@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.STUDENT)
                .isActive(true)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("jane@example.com");
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void findByEmail_returnsEmpty_whenNotExists() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }
}
