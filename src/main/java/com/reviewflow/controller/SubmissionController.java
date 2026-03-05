package com.reviewflow.controller;

import com.reviewflow.model.dto.response.ApiResponse;
import com.reviewflow.model.dto.response.SubmissionResponse;
import com.reviewflow.model.entity.Submission;
import com.reviewflow.security.ReviewFlowUserDetails;
import com.reviewflow.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionResponse>> upload(
            @RequestParam Long teamId,
            @RequestParam Long assignmentId,
            @RequestParam(required = false) String changeNote,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Submission sub = submissionService.upload(teamId, assignmentId, changeNote, file, user.getUserId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok(toResponse(sub)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponse>> get(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Submission sub = submissionService.getSubmission(id, user.getUserId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(sub)));
    }

    @GetMapping("/teams/{teamId}/assignments/{assignmentId}/history")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> history(
            @PathVariable Long teamId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        List<SubmissionResponse> data = submissionService
                .getVersionHistory(teamId, assignmentId, user.getUserId(), user.getRole())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Submission sub = submissionService.getSubmission(id, user.getUserId(), user.getRole());
        Resource resource = submissionService.downloadSubmission(id, user.getUserId(), user.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sub.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private SubmissionResponse toResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .teamId(s.getTeam() != null ? s.getTeam().getId() : null)
                .teamName(s.getTeam() != null ? s.getTeam().getName() : null)
                .assignmentId(s.getAssignment() != null ? s.getAssignment().getId() : null)
                .assignmentTitle(s.getAssignment() != null ? s.getAssignment().getTitle() : null)
                .courseCode(s.getAssignment() != null && s.getAssignment().getCourse() != null
                        ? s.getAssignment().getCourse().getCode() : null)
                .versionNumber(s.getVersionNumber())
                .fileName(s.getFileName())
                .fileSizeBytes(s.getFileSizeBytes())
                .isLate(s.getIsLate())
                .uploadedAt(s.getUploadedAt())
                .changeNote(s.getChangeNote())
                .uploadedById(s.getUploadedBy() != null ? s.getUploadedBy().getId() : null)
                .uploadedByName(s.getUploadedBy() != null
                        ? s.getUploadedBy().getFirstName() + " " + s.getUploadedBy().getLastName() : null)
                .build();
    }
}
