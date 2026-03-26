package com.reviewflow.event;

import java.util.List;

import com.reviewflow.model.enums.SubmissionType;

public record SubmissionUploadedEvent(
        List<Long> recipientUserIds, // all ACCEPTED members EXCEPT the uploader
        String uploaderName,
        Long teamId,
        Long studentId,
        String teamName,
        Long assignmentId,
        String assignmentTitle,
        int versionNumber,
        SubmissionType submissionType,
        Long submissionId
        ) {

}
