package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RenameTeamRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
}
