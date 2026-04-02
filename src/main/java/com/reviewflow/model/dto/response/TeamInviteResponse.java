package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Value
@Builder
public class TeamInviteResponse {
    String teamMemberId;
    String teamId;
    String teamName;
    String assignmentId;
    String assignmentTitle;
    String courseCode;
    String invitedByName;
    Instant createdAt;
}
