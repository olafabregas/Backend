package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CreateEvaluationRequest {
    @Schema(description = "ID of the submission to evaluate", example = "abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String submissionId;
}
