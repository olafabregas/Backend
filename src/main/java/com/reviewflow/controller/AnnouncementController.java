package com.reviewflow.controller;

import com.reviewflow.model.dto.request.CreateAnnouncementRequest;
import com.reviewflow.model.dto.response.AnnouncementResponse;
import com.reviewflow.model.dto.response.ApiResponse;
import com.reviewflow.model.dto.response.PaginatedAnnouncementResponse;
import com.reviewflow.model.entity.Announcement;
import com.reviewflow.security.ReviewFlowUserDetails;
import com.reviewflow.service.AnnouncementService;
import com.reviewflow.service.HashidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * AnnouncementController — handles all announcement endpoints per PRD-04. Thin
 * translators: all business logic delegated to AnnouncementService.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final HashidService hashidService;

    /**
     * POST /courses/{id}/announcements Create a course announcement (draft).
     * Only instructors of the course can create.
     *
     * @param courseId encoded course hashid
     * @param request title and body (recipientType ignored for course
     * announcements)
     * @param user authenticated user (must be instructor of course)
     * @return 201 Created with AnnouncementResponse
     */
    @PostMapping("/courses/{id}/announcements")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createCourseAnnouncement(
            @PathVariable("id") String courseId,
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {

        Announcement announcement = announcementService.createCourseAnnouncement(
                courseId,
                user.getUserId(),
                request.getTitle(),
                request.getBody()
        );

        AnnouncementResponse response = AnnouncementResponse.builder()
                .id(hashidService.encode(announcement.getId()))
                .courseId(announcement.getCourse() != null ? hashidService.encode(announcement.getCourse().getId()) : null)
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .target(announcement.getTarget())
                .recipientType(announcement.getRecipientType())
                .isDraft(!announcement.getIsPublished())
                .publishedAt(announcement.getPublishedAt())
                .createdAt(announcement.getCreatedAt())
                .createdByName(getDisplayName(user))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * POST /admin/announcements Create a platform-wide announcement (draft).
     * Only admins can create.
     *
     * @param request title, body, recipientType (required for platform
     * announcements)
     * @param user authenticated user (must be ADMIN)
     * @return 201 Created with AnnouncementResponse
     */
    @PostMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createPlatformAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {

        Announcement announcement = announcementService.createPlatformAnnouncement(
                user.getUserId(),
                request.getTitle(),
                request.getBody(),
                request.getRecipientType()
        );

        AnnouncementResponse response = AnnouncementResponse.builder()
                .id(hashidService.encode(announcement.getId()))
                .courseId(null) // Platform announcements have no course
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .target(announcement.getTarget())
                .recipientType(announcement.getRecipientType())
                .isDraft(!announcement.getIsPublished())
                .publishedAt(announcement.getPublishedAt())
                .createdAt(announcement.getCreatedAt())
                .createdByName(getDisplayName(user))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * PATCH /announcements/{id}/publish Publish a draft announcement. Only the
     * creator or an admin can publish.
     *
     * @param id encoded announcement hashid
     * @param user authenticated user (must be creator or admin)
     * @return 200 OK with updated AnnouncementResponse (isDraft=false,
     * publishedAt set)
     */
    @PatchMapping("/announcements/{id}/publish")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> publish(
            @PathVariable String id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {

        Long announcementId = hashidService.decodeOrThrow(id);
        Announcement announcement = announcementService.publish(announcementId, user.getUserId());

        AnnouncementResponse response = AnnouncementResponse.builder()
                .id(hashidService.encode(announcement.getId()))
                .courseId(announcement.getCourse() != null ? hashidService.encode(announcement.getCourse().getId()) : null)
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .target(announcement.getTarget())
                .recipientType(announcement.getRecipientType())
                .isDraft(!announcement.getIsPublished())
                .publishedAt(announcement.getPublishedAt())
                .createdAt(announcement.getCreatedAt())
                .createdByName(getDisplayName(user))
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * DELETE /announcements/{id} Delete an announcement (hard delete). Only the
     * creator or an admin can delete.
     *
     * @param id encoded announcement hashid
     * @param user authenticated user (must be creator or admin)
     * @return 200 OK (no body)
     */
    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {

        Long announcementId = hashidService.decodeOrThrow(id);
        announcementService.delete(announcementId, user.getUserId());

        return ResponseEntity.ok().build();
    }

    /**
     * GET /courses/{id}/announcements?page=0&size=20 List published
     * announcements for a course (paginated, by publishedAt DESC). Only
     * enrolled students or course instructors can view.
     *
     * @param courseId encoded course hashid
     * @param pageable pagination parameters
     * @param user authenticated user
     * @return 200 OK with PaginatedAnnouncementResponse
     */
    @GetMapping("/courses/{id}/announcements")
    public ResponseEntity<ApiResponse<PaginatedAnnouncementResponse>> listCourseAnnouncements(
            @PathVariable("id") String courseId,
            Pageable pageable,
            @AuthenticationPrincipal ReviewFlowUserDetails user) {

        Long courseIdLong = hashidService.decodeOrThrow(courseId);
        Page<Announcement> page = announcementService.getByCourse(courseIdLong, user.getUserId(), pageable);

        PaginatedAnnouncementResponse response = PaginatedAnnouncementResponse.builder()
                .content(page.getContent().stream()
                        .map(a -> PaginatedAnnouncementResponse.AnnouncementListItem.builder()
                        .id(hashidService.encode(a.getId()))
                        .title(a.getTitle())
                        .body(a.getBody())
                        .publishedAt(a.getPublishedAt())
                        .createdByName(getCreatedByName(a))
                        .build())
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Helper to extract user display name from authenticated principal.
     */
    private String getDisplayName(ReviewFlowUserDetails user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (user.getFirstName() != null) {
            return user.getFirstName();
        }
        return user.getUsername();  // username is typically email
    }

    /**
     * Helper to extract creator name from announcement entity.
     */
    private String getCreatedByName(Announcement announcement) {
        if (announcement.getCreatedBy().getFirstName() != null && announcement.getCreatedBy().getLastName() != null) {
            return announcement.getCreatedBy().getFirstName() + " " + announcement.getCreatedBy().getLastName();
        }
        if (announcement.getCreatedBy().getFirstName() != null) {
            return announcement.getCreatedBy().getFirstName();
        }
        return announcement.getCreatedBy().getEmail();
    }
}
