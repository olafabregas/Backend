package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TeamInviteResponse {
    Long teamMemberId;
    Long teamId;
    String teamName;
    Long assignmentId;
    String assignmentTitle;
    String courseCode;
    String invitedByName;
    Instant createdAt;
}
