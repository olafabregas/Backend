package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotBlank
    private String term;
    
    private String description;
}
