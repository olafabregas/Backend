package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class AssignmentResponse {

    Long id;
    Long courseId;
    String courseCode;
    String courseName;
    String title;
    String description;
    Instant dueAt;
    Integer maxTeamSize;
    Boolean isPublished;
    Instant teamLockAt;
    List<RubricCriterionResponse> rubricCriteria;
    
    // Student-specific fields (null for instructor/admin views)
    String teamStatus; // HAS_TEAM | NO_TEAM | LOCKED
    String submissionStatus; // NOT_SUBMITTED | SUBMITTED | LATE
    Boolean isLate;

    @Value
    @Builder
    public static class RubricCriterionResponse {
        Long id;
        String name;
        String description;
        Integer maxScore;
        Integer displayOrder;
    }
}
