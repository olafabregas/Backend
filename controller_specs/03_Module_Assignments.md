# ReviewFlow — Module 3: Assignments

> Controller: `AssignmentController.java`
> Base path: `/api/v1`
>
> **Related PRDs:** [PRD-01 Submission Type](../Features/PRD_01_submission_type.md) | [PRD-05 Extensions](../Features/PRD_05_assignment_extensions.md) | [PRD-06 Grade Export](../Features/PRD_06_grade_export.md)
> **Related Modules:** [Module 5: Submissions](./05_Module_Submissions.md) | [Module 10: Grade Export](./10_Module_GradeExport.md)
> **Related Decisions:** [Decision 1: Submission Type](../DECISIONS.md#10-submission-type-individual-vs-team)

---

## 3.1 GET /assignments ⭐ NEW

### Must Have

- [ ] Endpoint exists — global assignment feed with NO courseId required
- [ ] STUDENT: returns published assignments for ALL enrolled courses
- [ ] INSTRUCTOR: returns all assignments (including drafts) for ALL their courses
- [ ] ADMIN: returns all assignments in the system
- [ ] Each item includes: `id, title, courseCode, courseName, dueAt, isPublished, maxTeamSize, teamStatus, submissionStatus, isLate`
- [ ] `teamStatus` per student: `HAS_TEAM | NO_TEAM | LOCKED`
- [ ] `submissionStatus` per student: `NOT_SUBMITTED | SUBMITTED | LATE`
- [ ] Default sort: `dueAt ASC` (most urgent first)
- [ ] Supports `?status=UPCOMING|PAST_DUE|ALL` filter
- [ ] Supports `?courseId=X` to optionally scope to one course

### Responses

- [ ] `200 OK` — paginated assignment list
- [ ] `401 Unauthorized` — no session

### Edge Cases

- [ ] Student enrolled in 3 courses with 2 assignments each → returns 6 assignments
- [ ] Student sees only PUBLISHED assignments — never drafts
- [ ] Instructor sees both published and draft assignments
- [ ] Student with no enrollments → `200` with empty array

---

## 3.2 GET /courses/{id}/assignments

### Must Have

- [ ] Returns assignments for a specific course
- [ ] STUDENT: only published assignments
- [ ] INSTRUCTOR: all assignments including drafts
- [ ] Each item includes rubric criteria array
- [ ] Student must be enrolled in the course

### Responses

- [ ] `200 OK` — list of assignments
- [ ] `403 Forbidden` — student not enrolled in course, instructor not assigned
- [ ] `404 Not Found` — course doesn't exist

### Edge Cases

- [ ] Student requests unpublished assignment → `404` (NOT `403` — never reveal it exists)
- [ ] No assignments in course → `200` with empty array

---

## 3.3 POST /courses/{id}/assignments

### Must Have

- [ ] INSTRUCTOR only (assigned to this course)
- [ ] Required: `title`, `description`, `dueAt`, `maxTeamSize`
- [ ] Optional: `teamLockAt`, `isPublished` (default `false`)
- [ ] `dueAt` must be a future date
- [ ] `teamLockAt` must be before `dueAt`
- [ ] `maxTeamSize` must be between 1 and 10
- [ ] Saved as draft if `isPublished = false`

### Responses

- [ ] `201 Created` — assignment created, returns full object with `id`
- [ ] `400 Bad Request` — missing required fields
- [ ] `400 Bad Request` — `dueAt` is in the past → `{ code: "INVALID_DUE_DATE" }`
- [ ] `400 Bad Request` — `teamLockAt` is after `dueAt` → `{ code: "INVALID_LOCK_DATE" }`
- [ ] `400 Bad Request` — `maxTeamSize` out of range (1–10)
- [ ] `403 Forbidden` — user is not INSTRUCTOR assigned to this course
- [ ] `404 Not Found` — course doesn't exist

### Edge Cases

- [ ] Create with `isPublished: false` → saved as draft, students can't see it
- [ ] Create with `isPublished: true` → immediately visible to enrolled students
- [ ] `dueAt` = today's date/time (in the past by seconds) → `400`
- [ ] `teamLockAt` = same as `dueAt` → `400`
- [ ] `maxTeamSize = 0` → `400`
- [ ] `maxTeamSize = 11` → `400`
- [ ] Instructor from DIFFERENT course tries to create → `403`

---

## 3.4 GET /assignments/{id}

### Must Have

- [ ] Returns full assignment detail
- [ ] Includes `rubricCriteria` array
- [ ] STUDENT: only if enrolled AND assignment is published
- [ ] INSTRUCTOR: only if assigned to the course
- [ ] ADMIN: always

### Responses

- [ ] `200 OK` — assignment with rubric
- [ ] `403 Forbidden` — student not enrolled
- [ ] `404 Not Found` — assignment doesn't exist OR is unpublished (for student) → always `404` never `403` for unpublished

---

## 3.5 PUT /assignments/{id}

### Must Have

- [ ] INSTRUCTOR only (must be assigned to the course)
- [ ] Allows updating: `title`, `description`, `dueAt`, `teamLockAt`, `maxTeamSize`
- [ ] Same validation rules as create
- [ ] Returns updated assignment

### Responses

- [ ] `200 OK` — updated
- [ ] `400 Bad Request` — validation failures
- [ ] `403 Forbidden` — not the instructor for this course
- [ ] `404 Not Found` — assignment doesn't exist

---

## 3.6 DELETE /assignments/{id}

### Must Have

- [ ] INSTRUCTOR only
- [ ] Only allowed if `is_published = false` AND no submissions exist
- [ ] Cascades to delete all `rubric_criteria` for this assignment
- [ ] Does NOT cascade to teams (teams are orphaned but that's acceptable)

### Responses

- [ ] `200 OK` — deleted, returns `{ message: "Assignment deleted" }`
- [ ] `400 Bad Request` — assignment is published → `{ code: "ASSIGNMENT_PUBLISHED", message: "Unpublish the assignment before deleting" }`
- [ ] `400 Bad Request` — submissions exist → `{ code: "HAS_SUBMISSIONS", message: "Cannot delete an assignment with existing submissions" }`
- [ ] `403 Forbidden` — not the instructor for this course
- [ ] `404 Not Found` — doesn't exist

---

## 3.7 PATCH /assignments/{id}/publish

### Must Have

- [ ] INSTRUCTOR only
- [ ] Toggles publish state — publishes if draft, unpublishes if published
- [ ] Cannot unpublish if submissions exist → `400`
- [ ] Returns updated assignment with new `isPublished` value

### Responses

- [ ] `200 OK` — toggled, returns assignment with `isPublished`
- [ ] `400 Bad Request` — trying to unpublish assignment that has submissions → `{ code: "HAS_SUBMISSIONS" }`
- [ ] `403 Forbidden` — not the instructor
- [ ] `404 Not Found`

---

## 3.8 POST /assignments/{id}/rubric

### Must Have

- [ ] INSTRUCTOR only
- [ ] Required: `name`, `maxScore`, `displayOrder`
- [ ] Optional: `description`
- [ ] `maxScore` must be a positive integer
- [ ] Returns created criterion with `id`

### Responses

- [ ] `201 Created` — criterion created
- [ ] `400 Bad Request` — missing fields or `maxScore ≤ 0`
- [ ] `403 Forbidden` — not the instructor
- [ ] `404 Not Found` — assignment doesn't exist

---

## 3.9 PUT /assignments/{id}/rubric/{criterionId}

### Must Have

- [ ] INSTRUCTOR only
- [ ] Allows updating: `name`, `description`, `maxScore`, `displayOrder`
- [ ] Returns updated criterion

### Responses

- [ ] `200 OK` — updated
- [ ] `400 Bad Request` — `maxScore ≤ 0`
- [ ] `403 Forbidden`
- [ ] `404 Not Found` — assignment or criterion doesn't exist

---

## 3.10 DELETE /assignments/{id}/rubric/{criterionId}

### Must Have

- [ ] INSTRUCTOR only
- [ ] Deletes the criterion
- [ ] Cannot delete if evaluation scores exist for this criterion → `400`

### Responses

- [ ] `200 OK` — deleted, returns `{ message: "Criterion deleted" }`
- [ ] `400 Bad Request` — scores exist for this criterion → `{ code: "HAS_SCORES" }`
- [ ] `403 Forbidden`
- [ ] `404 Not Found`

---

## 3.11 GET /assignments/{id}/gradebook

### Must Have

- [ ] INSTRUCTOR only (assigned to course)
- [ ] Returns all teams for the assignment with their submission and evaluation status
- [ ] Each row: `teamId, teamName, memberNames, latestVersion, submittedAt, isLate, totalScore, evaluationStatus: NOT_STARTED | DRAFT | PUBLISHED`
- [ ] Supports `?sort=teamName|score|submittedAt`

### Responses

- [ ] `200 OK` — gradebook rows
- [ ] `403 Forbidden` — not instructor for this course
- [ ] `404 Not Found` — assignment doesn't exist

---

## 3.12 GET /assignments/{id}/submissions

### Must Have

- [ ] INSTRUCTOR only
- [ ] Returns latest submission per team (not all versions)
- [ ] Each item: `teamId, teamName, versionNumber, submittedAt, isLate, fileName, fileSizeBytes`

### Responses

- [ ] `200 OK` — list of latest submissions per team
- [ ] `403 Forbidden` — not instructor
- [ ] `404 Not Found`
