-- V18__create_announcements.sql
-- PRD-04: Create announcements table with course/platform targeting
-- Author: Agent 1 (Database)
-- Date: 2026-03-26

-- Create ENUM type for announcement targets
CREATE TABLE announcements (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    course_id      BIGINT         NULL,
    created_by     BIGINT         NOT NULL,
    title          VARCHAR(255)   NOT NULL,
    body           TEXT           NOT NULL,
    target         ENUM('COURSE','PLATFORM') NOT NULL DEFAULT 'COURSE',
    recipient_type ENUM('ALL_STUDENTS','ALL_INSTRUCTORS','ALL_USERS') NULL,
    is_published   BOOLEAN        NOT NULL DEFAULT false,
    published_at   DATETIME       NULL,
    created_at     DATETIME       NOT NULL DEFAULT NOW(),
    updated_at     DATETIME       NOT NULL DEFAULT NOW()
                                  ON UPDATE NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_announcement_course
        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_creator
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_announcement_target
        CHECK (
            (target = 'COURSE' AND course_id IS NOT NULL AND recipient_type IS NULL) OR
            (target = 'PLATFORM' AND course_id IS NULL AND recipient_type IS NOT NULL)
        )
);

-- Index for efficient course + published filtering with pagination
CREATE INDEX idx_announcements_course_published
    ON announcements(course_id, is_published, published_at DESC);

-- Add ANNOUNCEMENT to notification type enum
ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'TEAM_INVITE',
        'TEAM_LOCKED',
        'NEW_SUBMISSION',
        'FEEDBACK_PUBLISHED',
        'SYSTEM',
        'ANNOUNCEMENT'
    ) NOT NULL;
