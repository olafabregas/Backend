package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SubmissionResponse {
    Long id;
    Long teamId;
    String teamName;
    Long assignmentId;
    String assignmentTitle;
    String courseCode;
    Integer versionNumber;
    String fileName;
    Long fileSizeBytes;
    Boolean isLate;
    Instant uploadedAt;
    String changeNote;
    Long uploadedById;
    String uploadedByName;
}
