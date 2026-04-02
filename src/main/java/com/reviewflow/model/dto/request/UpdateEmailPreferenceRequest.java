package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateEmailPreferenceRequest {

    @Schema(description = "Whether email notifications are enabled for this user", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Boolean emailNotificationsEnabled;
}
