package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PatchScoreRequest {
    @Schema(description = "Score for the rubric criterion", example = "8.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private BigDecimal score;
    private String comment;
}
