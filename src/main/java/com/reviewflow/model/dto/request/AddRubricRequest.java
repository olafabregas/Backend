package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddRubricRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Integer maxScore;
    private Integer displayOrder = 0;
}
