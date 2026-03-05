package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PatchScoreRequest {
    @NotNull
    private BigDecimal score;
    private String comment;
}
