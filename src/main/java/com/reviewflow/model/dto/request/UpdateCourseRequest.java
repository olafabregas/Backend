package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCourseRequest {
    private String code;
    @NotBlank
    private String name;
    private String term;
    private String description;
}
