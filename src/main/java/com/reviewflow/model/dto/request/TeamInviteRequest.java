package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamInviteRequest {
    @NotBlank(message = "inviteeEmail is required")
    @Email(message = "inviteeEmail must be a valid email")
    private String inviteeEmail;
}
