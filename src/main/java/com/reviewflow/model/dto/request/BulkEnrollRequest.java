package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class BulkEnrollRequest {
    @Schema(description = "List of email addresses to enroll in the course", example = "[\"student1@university.edu\", \"student2@university.edu\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<String> emails;
}
