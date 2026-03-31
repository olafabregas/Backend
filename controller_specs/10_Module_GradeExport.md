# ReviewFlow — Module 10: Grade Export

> Controller: `GradeExportController.java`
> Base path: `/api/v1/courses/{courseId}/assignments/{assignmentId}`

---

## 10.1 Module Overview

### Purpose

Enable instructors to export assignment grades as CSV files for submission to institutional grading systems, program coordinators, or local archival. Grade exports adapt to the assignment's submission type—TEAM assignments produce team-level rows, INDIVIDUAL assignments produce student-level rows.

### Scope

- Export published evaluations only (draft evaluations excluded)
- Include unevaluated submissions with blank scores (for visibility of who has not been graded)
- Support both team and individual assignment types
- Enforce instructor/admin access control per course
- Generate deterministic, reproducible CSV output

### Integration Points

- **Evaluations Module**: Only published evaluations (`is_draft = false`) are included
- **Submissions Module**: Uses latest submission version per team/student
- **Teams Module**: Team-level row grouping for TEAM assignment types
- **Audit Logging**: Logs `GRADE_EXPORT_DOWNLOADED` event per export
- **Global Error Codes**: Uses standard error codes from [Global Rules](00_Global_Rules_and_Reference.md)

---

## 10.2 Authentication & Authorization

### Required Roles

| Role       | Can Export                      |
| ---------- | ------------------------------- |
| STUDENT    | ✗                               |
| INSTRUCTOR | ✓ — only for courses they teach |
| ADMIN      | ✓ — any course                  |

### Access Control Requirements

- [ ] Endpoint is protected — requires valid access token
- [ ] INSTRUCTOR must be assigned to the course
- [ ] ADMIN has unrestricted access
- [ ] Returns `403 Forbidden` if user lacks permission for this course
- [ ] STUDENT attempting access returns `403` (never `404` to hide endpoint existence)

---

## 10.3 API Endpoints

### 10.3.1 GET /grades/export

**Exports grades for an assignment as a CSV file**

```
GET /api/v1/courses/{courseId}/assignments/{assignmentId}/grades/export
```

#### Path Parameters

| Parameter      | Type   | Required | Description                                     |
| -------------- | ------ | -------- | ----------------------------------------------- |
| `courseId`     | HashId | ✓        | Hashid-encoded course ID (e.g., `aB12cD3E`)     |
| `assignmentId` | HashId | ✓        | Hashid-encoded assignment ID (e.g., `x9yZ2kL5`) |

#### Query Parameters

Currently no optional query parameters. Format is fixed as `text/csv`.

**Future extension** (deferred per PRD-06): `?includeCriteria=true` would add per-criterion score columns.

---

## 10.4 Request & Response Examples

### Request

```http
GET /api/v1/courses/aB12cD3E/assignments/x9yZ2kL5/grades/export HTTP/1.1
Host: localhost:8081
Cookie: reviewflow_access=<valid_jwt>
```

### Response: Success (200 OK)

```
HTTP/1.1 200 OK
Content-Type: text/csv; charset=utf-8
Content-Disposition: attachment; filename="CS401_Project-Phase-1-System-Design_2026-03-25.csv"
Content-Length: 2847
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000

Course Code,Assignment Title,Team Name,Students,Total Score,Max Score,Percentage,Is Late,Submitted At,Evaluated At
CS401,Project Phase 1 — System Design,Team Alpha,"Jane Smith, Marcus Chen, Priya Patel, Sam Lee",92,100,92.0%,No,2026-03-22T10:00:00Z,2026-03-23T14:00:00Z
CS401,Project Phase 1 — System Design,Team Beta,"Alex Kumar, Rachel Park, Liam Nguyen, Tariq Ali",78,100,78.0%,No,2026-03-21T16:30:00Z,2026-03-23T15:00:00Z
```

### Response: Error (400 Bad Request) — Assignment not in course

```json
{
  "success": false,
  "error": {
    "code": "ASSIGNMENT_NOT_IN_COURSE",
    "message": "This assignment does not belong to this course"
  },
  "timestamp": "2026-03-30T14:22:15.123Z"
}
```

### Response: Error (403 Forbidden) — Access denied

```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_PERMISSION",
    "message": "You do not have permission to export grades for this course"
  },
  "timestamp": "2026-03-30T14:22:15.123Z"
}
```

### Response: Error (404 Not Found) — No submissions

```json
{
  "success": false,
  "error": {
    "code": "NO_SUBMISSIONS_FOUND",
    "message": "No submissions exist for this assignment"
  },
  "timestamp": "2026-03-30T14:22:15.123Z"
}
```

---

## 10.5 CSV Format Specification

### Column Definitions

| Column           | TEAM | INDIVIDUAL | Type     | Notes                                                                   |
| ---------------- | ---- | ---------- | -------- | ----------------------------------------------------------------------- |
| Course Code      | ✓    | ✓          | String   | From `courses.code`                                                     |
| Assignment Title | ✓    | ✓          | String   | From `assignments.title`                                                |
| Team Name        | ✓    | ✗          | String   | From `teams.name`                                                       |
| Students         | ✓    | ✗          | String   | Comma-separated full names of ACCEPTED members                          |
| Student Name     | ✗    | ✓          | String   | Format: `LastName FirstName`                                            |
| Student Email    | ✗    | ✓          | String   | From `users.email`                                                      |
| Total Score      | ✓    | ✓          | Decimal  | Blank if no published evaluation; numeric otherwise                     |
| Max Score        | ✓    | ✓          | Integer  | Sum of all rubric criteria `maxScore` values                            |
| Percentage       | ✓    | ✓          | Decimal  | `(totalScore / maxScore) × 100`, 1 decimal place. Blank if unevaluated. |
| Is Late          | ✓    | ✓          | String   | "Yes" or "No" from `submissions.is_late`                                |
| Submitted At     | ✓    | ✓          | ISO 8601 | Latest submission version's `uploaded_at` timestamp                     |
| Evaluated At     | ✓    | ✓          | ISO 8601 | `evaluations.published_at`. Blank if never published.                   |

### TEAM Assignment Example

```csv
Course Code,Assignment Title,Team Name,Students,Total Score,Max Score,Percentage,Is Late,Submitted At,Evaluated At
CS401,Project Phase 1 — System Design,Team Alpha,"Jane Smith, Marcus Chen, Priya Patel, Sam Lee",92,100,92.0%,No,2026-03-22T10:00:00Z,2026-03-23T14:00:00Z
CS401,Project Phase 1 — System Design,Team Beta,"Alex Kumar, Rachel Park, Liam Nguyen, Tariq Ali",78,100,78.0%,No,2026-03-21T16:30:00Z,2026-03-23T15:00:00Z
CS401,Project Phase 1 — System Design,Team Gamma,"Chloe Wang, Ben Okafor, Sofia Rodriguez, Noah Brown",,100,,No,2026-03-22T11:00:00Z,
```

**Row 3 (Team Gamma)**: Unevaluated submission. Total Score, Percentage, and Evaluated At are blank. Score can still be submitted later.

### INDIVIDUAL Assignment Example

```csv
Course Code,Assignment Title,Student Name,Student Email,Total Score,Max Score,Percentage,Is Late,Submitted At,Evaluated At
CS401,Midterm Essay,Smith Jane,jane.smith@university.edu,88,100,88.0%,No,2026-03-22T09:00:00Z,2026-03-23T10:00:00Z
CS401,Midterm Essay,Chen Marcus,marcus.chen@university.edu,,100,,No,2026-03-22T14:00:00Z,
CS401,Midterm Essay,Kumar Alex,alex.kumar@university.edu,,100,,Yes,2026-03-26T01:00:00Z,
```

**Row 2**: Submitted but not yet evaluated — blank score fields.  
**Row 3**: Submitted late, not yet evaluated.

### File Naming Convention

```
{courseCode}_{sanitisedTitle}_{YYYY-MM-DD}.csv
```

**Examples:**

- `CS401_Project-Phase-1-System-Design_2026-03-25.csv`
- `CS402_Database-Design-Project_2026-03-25.csv`

**Sanitisation rules:**

- Spaces → hyphens
- Strip special characters (keep only alphanumeric and hyphens)
- Max 60 characters for title portion
- Always lowercase

### Data Sanitization & Safety

**Injection Prevention:**

- All text fields escaped per RFC 4180: quotes (`"`) doubled (`""`)
- Newlines within fields preserved (not stripped)
- Raw CSV library (OpenCSV) handles escaping automatically

**Formula Injection Prevention:**

- Numbers never prefixed with `=`, `+`, `-`, `@` — all formulas blocked
- Score fields are JSON numbers when serialized, CSV-escaped when written

**Encoding:**

- UTF-8 with BOM (Byte Order Mark): `0xEF 0xBB 0xBF`
- Ensures Excel opens file correctly on all platforms

---

## 10.6 Performance Considerations

### Query Optimization

**Single database query with LEFT JOINs** — no N+1 problem:

```sql
-- TEAM assignment example
SELECT
  c.code AS courseCode,
  a.title AS assignmentTitle,
  t.name AS teamName,
  GROUP_CONCAT(u.first_name, ' ', u.last_name) AS studentNames,
  e.total_score,
  c.max_score,
  s.is_late,
  s.uploaded_at,
  e.published_at
FROM submissions s
LEFT JOIN evaluations e ON s.id = e.submission_id AND e.is_draft = false
JOIN teams t ON s.team_id = t.id
LEFT JOIN team_members tm ON t.id = tm.team_id AND tm.status = 'ACCEPTED'
JOIN users u ON tm.user_id = u.id
JOIN assignments a ON s.assignment_id = a.id
JOIN courses c ON a.course_id = c.id
WHERE a.id = ? AND a.course_id = ?
ORDER BY t.name ASC;
```

**Result:** Single round-trip to database. All data retrieved in one statement.

### Memory & Generation

- **In-memory generation**: CSV built in process heap using OpenCSV library
- **Typical scale**: <5MB for 100 submissions (per Decision 14)
- **No streaming**: Entire file generated before download begins
- **Atomic operation**: No race conditions between build and delivery

### Response Time Targets

| Scenario                          | Target                                |
| --------------------------------- | ------------------------------------- |
| 10 submissions (small assignment) | <500ms                                |
| 100 submissions                   | <2 seconds                            |
| >5000 submissions                 | Rejected with `413 Payload Too Large` |

### Monitoring & Limits

- [ ] Endpoint rejects exports for assignments with >5000 submissions
- [ ] Heap pressure alerts trigger if export uses >50% of available heap
- [ ] CloudWatch logs include generation time: `gradeExportGenerationMs`
- [ ] Audit log records every export with timing data

---

## 10.7 Error Codes & Responses

All errors conform to [Global Rules Response Envelope](00_Global_Rules_and_Reference.md#error-handling).

### Error Codes

| Code                       | HTTP | Cause                                                        | User Message                                                        |
| -------------------------- | ---- | ------------------------------------------------------------ | ------------------------------------------------------------------- |
| `COURSE_NOT_FOUND`         | 404  | `courseId` doesn't decode or decode result doesn't exist     | Course not found                                                    |
| `ASSIGNMENT_NOT_FOUND`     | 404  | `assignmentId` doesn't decode or decode result doesn't exist | Assignment not found                                                |
| `ASSIGNMENT_NOT_IN_COURSE` | 400  | Assignment belongs to different course                       | This assignment does not belong to this course                      |
| `NO_SUBMISSIONS_FOUND`     | 404  | Assignment has zero submissions                              | No submissions exist for this assignment                            |
| `INSUFFICIENT_PERMISSION`  | 403  | INSTRUCTOR not teaching course or STUDENT attempting access  | You do not have permission to export grades for this course         |
| `TOO_MANY_SUBMISSIONS`     | 413  | Assignment has >5000 submissions                             | Export too large (>5000 submissions). Contact admin for assistance. |
| `EXPORT_GENERATION_FAILED` | 500  | CSV generation or file I/O error                             | Failed to generate export. Please try again.                        |
| `UNAUTHORIZED`             | 401  | No valid access token                                        | Access token expired or missing                                     |

### Typical Response Flow

```
Request → Role Check (403 if STUDENT)
        → Course Access Check (403 if INSTRUCTOR not teaching)
        → Course Exists (404 if not)
        → Assignment Exists (404 if not)
        → Assignment In Course (400 if not)
        → Count Submissions (404 if none)
        → Check Size Limit (413 if >5000)
        → Generate CSV
        → Stream to Client (200 OK + CSV bytes)
        → Audit Log Entry
```

---

## 10.8 Must Have Checklist

### Endpoint Requirements

- [ ] Endpoint exists: `GET /api/v1/courses/{courseId}/assignments/{assignmentId}/grades/export`
- [ ] Accepts `GET` method only (no POST/PUT/DELETE)
- [ ] Both path parameters are HashIds (decoded before use)
- [ ] Requires valid access token cookie (protected endpoint)
- [ ] Returns `Content-Type: text/csv; charset=utf-8`
- [ ] Returns `Content-Disposition: attachment; filename="{courseCode}_{sanitisedTitle}_{date}.csv"`
- [ ] Includes `X-Trace-Id` response header

### Authorization

- [ ] STUDENT attempting export → `403 INSUFFICIENT_PERMISSION`
- [ ] INSTRUCTOR from different course → `403 INSUFFICIENT_PERMISSION`
- [ ] INSTRUCTOR of the course → `200` (allowed)
- [ ] ADMIN → `200` (allowed)
- [ ] No access token → `401 UNAUTHORIZED`

### Data Assembly

- [ ] Query uses single LEFT JOIN statement (no N+1)
- [ ] Includes all submissions (even unevaluated ones)
- [ ] Only published evaluations included (`is_draft = false`)
- [ ] Draft evaluations produce blank score rows
- [ ] Latest submission version per team/student only
- [ ] TEAM assignments group by team
- [ ] INDIVIDUAL assignments group by student

### CSV Output

- [ ] Headers match specified column names exactly
- [ ] All text fields properly quoted and escaped (RFC 4180)
- [ ] Numbers not prefixed with `=`, `+`, `-`, `@`
- [ ] UTF-8 encoding with BOM
- [ ] Sorted: teams/students in alphabetical order
- [ ] File named per convention: `{courseCode}_{sanitisedTitle}_{YYYY-MM-DD}.csv`
- [ ] Percentage formatted to 1 decimal place with `%` suffix

### Error Handling

- [ ] Course doesn't exist → `404 COURSE_NOT_FOUND`
- [ ] Assignment doesn't exist → `404 ASSIGNMENT_NOT_FOUND`
- [ ] Assignment not in course → `400 ASSIGNMENT_NOT_IN_COURSE`
- [ ] No submissions → `404 NO_SUBMISSIONS_FOUND`
- [ ] Too many submissions (>5000) → `413 TOO_MANY_SUBMISSIONS`
- [ ] Generation fails → `500 EXPORT_GENERATION_FAILED`
- [ ] All errors use standard envelope: `{ success: false, error: { code, message }, timestamp }`

### Audit & Logging

- [ ] Log `GRADE_EXPORT_DOWNLOADED` event to `audit_log` table
- [ ] Include `courseId`, `assignmentId`, `actorId` in audit log
- [ ] Record generation time in milliseconds
- [ ] Record submission count in audit log

---

## 10.9 Edge Cases

| Scenario                                                               | Expected Behavior                                           |
| ---------------------------------------------------------------------- | ----------------------------------------------------------- |
| Assignment with 0 submissions                                          | Return `404 NO_SUBMISSIONS_FOUND`                           |
| All submissions unevaluated (blank scores)                             | CSV includes all rows with empty score columns              |
| Some submissions evaluated, some not                                   | Mixed rows: some with scores, some blank                    |
| Team with only 1 member                                                | "Students" field lists that one member                      |
| Student in team but status ≠ ACCEPTED                                  | Excluded from CSV (only ACCEPTED members counted)           |
| Multiple submission versions per team                                  | Only latest version included (by `uploaded_at`)             |
| Reexport same assignment twice                                         | Both downloads are identical CSV (deterministic)            |
| Instructor exports for course not assigned to                          | `403 INSUFFICIENT_PERMISSION` (not `404`—don't hide course) |
| Student attempts export                                                | `403 INSUFFICIENT_PERMISSION` (forbid student access)       |
| Course deleted after creating assignment                               | Course code still valid in CSV (archived data preserved)    |
| Assignment title with special chars (e.g., `Phase 1 — Design & Build`) | Sanitised to `phase-1-design-build` in filename             |
| Assignment with 5000 submissions exactly                               | `200` (allowed, at limit)                                   |
| Assignment with 5001 submissions                                       | `413 TOO_MANY_SUBMISSIONS`                                  |

---

## 10.10 Implementation Notes

### Query Branching by Submission Type

The service must inspect `assignment.submission_type` and execute different queries:

**If TEAM:**

```java
// Fetch team list with member names, scores, submission dates
// GROUP BY team_id to get one row per team
// JOIN teams, team_members, users to collect member names
```

**If INDIVIDUAL:**

```java
// Fetch student list with scores, submission dates
// No team grouping needed
// ORDER BY user.last_name, user.first_name
```

### Injection Defense Strategies

1. **SQL Injection**: Use parameterized queries (PreparedStatement) for course/assignment IDs
2. **CSV Injection**: Use OpenCSV `CSVWriter` with proper quote escaping
3. **XSS**: Not applicable (CSV is binary/text, not HTML)
4. **Path Traversal**: Filename sanitisation (no `/` or `..` allowed)

### Role Check Implementation

```java
if (currentUser.getRole() == STUDENT) {
    return 403 INSUFFICIENT_PERMISSION;
}
if (currentUser.getRole() == INSTRUCTOR) {
    if (!courseService.isInstructor(courseId, currentUser.getId())) {
        return 403 INSUFFICIENT_PERMISSION;
    }
}
// ADMIN: no further check needed
```

### CSV Generation (OpenCSV)

```java
// Pseudocode
StringWriter sw = new StringWriter();
CSVWriter writer = new CSVWriter(sw);

// Write header
writer.writeNext(new String[] { "Course Code", "Assignment Title", ... });

// Write data rows
for (SubmissionRow row : submissions) {
    writer.writeNext(new String[] { row.courseCode, row.title, ... });
}

byte[] csvBytes = sw.toString().getBytes(StandardCharsets.UTF_8);
return ResponseEntity.ok()
    .header("Content-Type", "text/csv; charset=utf-8")
    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
    .body(csvBytes);
```

### Future Streaming Path

Per Decision 14, current implementation uses in-memory generation. If scale grows beyond 5000 submissions:

1. Swap to async job queue (Spring `@Async` or dedicated task service)
2. Generate CSV to S3 (from PRD-S3)
3. Return job status immediately, provide S3 download link when ready
4. Document this upgrade path in PR description

---

## 10.11 Related PRDs

- **[PRD-06: Grade Export](../Features/PRD_06_grade_export.md)** — User stories, CSV column specs, file naming, flow diagrams
- **[PRD-01: Submission Types](../Features/PRD_01_submission_type.md)** — TEAM vs INDIVIDUAL submission logic affects CSV shape
- **[PRD-08: Logging & Monitoring](../Features/PRD_08_logging_monitoring.md)** — Audit log schema and CloudWatch metrics

---

## 10.12 Related Decisions

- **[Decision 14: CSV in-memory generation at current scale](../DECISIONS.md#14-csv-in-memory-generation-at-current-scale)** — Current implementation rationale, tradeoffs, upgrade path when scale demands async + S3
- **[Decision 1: Hashids for URL opaqueness](../DECISIONS.md#1-hashids-for-url-opaqueness)** — Why `courseId` and `assignmentId` are HashIds, not sequential integers
