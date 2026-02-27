package com.reviewflow.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundException_returns404WithEnvelope() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 99L);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getError().getMessage()).contains("User").contains("99");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleAccessDeniedException_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleException_returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("Unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
    }
}
