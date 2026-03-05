package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamRespondRequest {
    @NotNull
    private Boolean accept;
}
