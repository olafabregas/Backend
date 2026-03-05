package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class TeamResponse {

    Long id;
    Long assignmentId;
    String assignmentTitle;
    String name;
    Boolean isLocked;
    Long createdById;
    Integer memberCount;
    List<TeamMemberResponse> members;

    @Value
    @Builder
    public static class TeamMemberResponse {
        Long teamMemberId;
        Long userId;
        String firstName;
        String lastName;
        String email;
        String status;
        Long invitedById;
        Instant joinedAt;
    }
}
