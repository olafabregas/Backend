package com.reviewflow.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateScoresRequest {

    @NotEmpty
    private List<ScoreEntry> scores;

    @Data
    public static class ScoreEntry {
        private Long criterionId;
        private BigDecimal score;
        private String comment;
    }
}
