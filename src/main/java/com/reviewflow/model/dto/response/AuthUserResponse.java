package com.reviewflow.model.dto.response;

import com.reviewflow.model.entity.UserRole;
import lombok.Builder;
import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;

@Value
@Builder
public class AuthUserResponse {

    String userId;
    String firstName;
    String lastName;
    String email;
    String avatarUrl;
    Boolean emailNotificationsEnabled;
    Boolean isActive;
    UserRole role;
}
