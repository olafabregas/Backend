package com.reviewflow.controller;

import com.reviewflow.security.ReviewFlowUserDetails;
import com.reviewflow.service.GradeExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GradeExportController {

    private final GradeExportService gradeExportService;

    @GetMapping("/courses/{courseId}/evaluations/export")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<byte[]> export(
            @PathVariable String courseId,
            @RequestParam String assignmentId,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {
        GradeExportService.ExportResult result = gradeExportService.export(courseId, assignmentId, user.getUserId());

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(result.bytes());
    }
}
