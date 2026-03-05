package com.reviewflow.controller;

import com.reviewflow.model.dto.request.CreateEvaluationRequest;
import com.reviewflow.model.dto.request.PatchCommentRequest;
import com.reviewflow.model.dto.request.PatchScoreRequest;
import com.reviewflow.model.dto.request.UpdateScoresRequest;
import com.reviewflow.model.dto.response.ApiResponse;
import com.reviewflow.model.dto.response.EvaluationResponse;
import com.reviewflow.model.entity.Evaluation;
import com.reviewflow.model.entity.RubricScore;
import com.reviewflow.repository.RubricScoreRepository;
import com.reviewflow.security.ReviewFlowUserDetails;
import com.reviewflow.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final RubricScoreRepository rubricScoreRepository;

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> create(
            @Valid @RequestBody CreateEvaluationRequest request,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.createEvaluation(request.getSubmissionId(), user.getUserId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok(toResponse(ev)));
    }

    @GetMapping("/{id}")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> get(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.getEvaluationWithAccessControl(id, user.getUserId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(ev)));
    }

    @PutMapping("/{id}/scores")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> setScores(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScoresRequest request,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.setScores(id, request.getScores(), user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(ev)));
    }

    @PatchMapping("/{id}/scores/{criterionId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> patchScore(
            @PathVariable Long id,
            @PathVariable Long criterionId,
            @Valid @RequestBody PatchScoreRequest request,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.patchScore(id, criterionId, request.getScore(), request.getComment(), user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(ev)));
    }

    @PatchMapping("/{id}/comment")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> setComment(
            @PathVariable Long id,
            @RequestBody PatchCommentRequest request,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.setComment(id, request.getOverallComment(), user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(ev)));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.publishEvaluation(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(ev)));
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<EvaluationResponse>> reopen(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.reopenEvaluation(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(ev)));
    }

    @PostMapping("/{id}/pdf")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<ApiResponse<Map<String, String>>> generatePdf(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Evaluation ev = evaluationService.generatePdf(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("pdfPath", ev.getPdfPath())));
    }

    @GetMapping("/{id}/pdf")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<Resource> downloadPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        Resource resource = evaluationService.downloadPdf(id, user.getUserId(), user.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evaluation_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private EvaluationResponse toResponse(Evaluation ev) {
        List<RubricScore> scores = rubricScoreRepository.findByEvaluation_Id(ev.getId());
        BigDecimal maxPossible = scores.stream()
                .map(s -> s.getCriterion() != null && s.getCriterion().getMaxScore() != null
                        ? BigDecimal.valueOf(s.getCriterion().getMaxScore())
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return getEvaluationResponse(ev, scores, maxPossible);
    }

    static EvaluationResponse getEvaluationResponse(Evaluation ev, List<RubricScore> scores, BigDecimal maxPossible) {
        List<EvaluationResponse.RubricScoreResponse> scoreResponses = scores.stream()
                .map(s -> {
                    BigDecimal maxScore = s.getCriterion() != null && s.getCriterion().getMaxScore() != null
                            ? BigDecimal.valueOf(s.getCriterion().getMaxScore())
                            : BigDecimal.ZERO;
                    return EvaluationResponse.RubricScoreResponse.builder()
                            .id(s.getId())
                            .criterionId(s.getCriterion() != null ? s.getCriterion().getId() : null)
                            .criterionName(s.getCriterion() != null ? s.getCriterion().getName() : null)
                            .maxScore(maxScore)
                            .score(s.getScore())
                            .comment(s.getComment())
                            .build();
                })
                .collect(Collectors.toList());
        return EvaluationResponse.builder()
                .id(ev.getId())
                .submissionId(ev.getSubmission() != null ? ev.getSubmission().getId() : null)
                .instructorId(ev.getInstructor() != null ? ev.getInstructor().getId() : null)
                .instructorName(ev.getInstructor() != null
                        ? ev.getInstructor().getFirstName() + " " + ev.getInstructor().getLastName() : null)
                .overallComment(ev.getOverallComment())
                .totalScore(ev.getTotalScore())
                .maxPossibleScore(maxPossible)
                .isDraft(ev.getIsDraft())
                .publishedAt(ev.getPublishedAt())
                .createdAt(ev.getCreatedAt())
                .hasPdf(ev.getPdfPath() != null)
                .rubricScores(scoreResponses)
                .build();
    }
}
