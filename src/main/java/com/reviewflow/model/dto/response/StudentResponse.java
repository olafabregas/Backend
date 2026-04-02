package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Value
@Builder
public class StudentResponse {
    String userId;
    String email;
    String firstName;
    String lastName;
    Instant enrolledAt;
}
