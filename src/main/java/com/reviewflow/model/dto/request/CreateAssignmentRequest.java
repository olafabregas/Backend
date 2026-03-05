package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateAssignmentRequest {

    @NotBlank
    private String title;
    
    private String description;
    
    @NotNull
    private Instant dueAt;
    
    @NotNull
    private Integer maxTeamSize;
    
    private Instant teamLockAt;
    
    private Boolean isPublished;
}
