package com.reviewflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Test
    void encode_producesDifferentHashEachTime() {
        String raw = "mySecretPassword";
        String hash1 = passwordEncoder.encode(raw);
        String hash2 = passwordEncoder.encode(raw);
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).startsWith("$2");
    }

    @Test
    void matches_returnsTrue_forCorrectPassword() {
        String raw = "mySecretPassword";
        String hash = passwordEncoder.encode(raw);
        assertThat(passwordEncoder.matches(raw, hash)).isTrue();
    }

    @Test
    void matches_returnsFalse_forWrongPassword() {
        String hash = passwordEncoder.encode("correct");
        assertThat(passwordEncoder.matches("wrong", hash)).isFalse();
    }
}
