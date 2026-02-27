package com.reviewflow.security;

import com.reviewflow.model.entity.User;
import com.reviewflow.model.entity.UserRole;
import com.reviewflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_returnsReviewFlowUserDetails_whenUserExists() {
        User user = User.builder()
                .id(1L)
                .email("instructor@example.com")
                .passwordHash("hashed")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.INSTRUCTOR)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("instructor@example.com")).thenReturn(Optional.of(user));

        var details = userDetailsService.loadUserByUsername("instructor@example.com");

        assertThat(details).isInstanceOf(ReviewFlowUserDetails.class);
        assertThat(details.getUsername()).isEqualTo("instructor@example.com");
        assertThat(details.getUserId()).isEqualTo(1L);
        assertThat(details.getRole()).isEqualTo(UserRole.INSTRUCTOR);
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenUserMissing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }
}
