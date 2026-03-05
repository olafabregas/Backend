package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class EvaluationResponse {
    Long id;
    Long submissionId;
    Long instructorId;
    String instructorName;
    String overallComment;
    BigDecimal totalScore;
    BigDecimal maxPossibleScore;
    Boolean isDraft;
    Instant publishedAt;
    Instant createdAt;
    Boolean hasPdf;
    List<RubricScoreResponse> rubricScores;

    @Value
    @Builder
    public static class RubricScoreResponse {
        Long id;
        Long criterionId;
        String criterionName;
        BigDecimal maxScore;
        BigDecimal score;
        String comment;
    }
}
