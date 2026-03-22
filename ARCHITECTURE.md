# ReviewFlow — Architecture & System Flows

This document covers all system flows, diagrams, and structural details.  
For the project overview, setup, and API reference see [README.md](./README.md).  
For design decisions and tradeoff reasoning see [DECISIONS.md](./DECISIONS.md).

---

## Contents

1. [Authentication Flow](#1-authentication-flow)
2. [File Security Pipeline](#2-file-security-pipeline)
3. [Team Formation & Invite Flow](#3-team-formation--invite-flow)
4. [Submission Pipeline](#4-submission-pipeline)
5. [Evaluation Pipeline](#5-evaluation-pipeline)
6. [Notification Async Flow](#6-notification-async-flow)
7. [Caching Strategy](#7-caching-strategy)
8. [Security Model](#8-security-model)
9. [Data Model](#9-data-model)
10. [Failure Scenarios](#10-failure-scenarios)
11. [Known Limitations](#11-known-limitations)
12. [Architecture Evolution Path](#12-architecture-evolution-path)

---

## 1. Authentication Flow

<!-- Paste Eraser diagram image URL below -->
> 📌 *Diagram — Authentication Flow*

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Browser)
    participant F as JWT Filter
    participant A as AuthService
    participant DB as MySQL

    Note over C,DB: Login
    C->>A: POST /auth/login { email, password }
    A->>DB: SELECT user, verify bcrypt hash
    DB-->>A: User { id, role, isActive }
    A->>A: Generate fingerprint hash (User-Agent + IP)
    A->>A: Sign JWT — embed fingerprint claim
    A->>DB: INSERT refresh_token (rotated hash)
    A->>DB: INSERT audit_log USER_LOGIN
    A-->>C: 200 OK — Set-Cookie: reviewflow_access (HttpOnly, Secure, SameSite=Strict)

    Note over C,DB: Authenticated Request
    C->>F: Any request — Cookie: reviewflow_access
    F->>F: Verify JWT signature
    F->>F: Recompute fingerprint from request headers
    F->>F: Compare against fingerprint claim in token
    alt Fingerprint mismatch
        F-->>C: 401 INVALID_FINGERPRINT
    else Valid
        F->>A: Set SecurityContext (userId, role)
        A-->>C: 200 OK
    end

    Note over C,DB: Token Refresh
    C->>A: POST /auth/refresh — Cookie: reviewflow_refresh
    A->>DB: SELECT refresh_token WHERE hash matches
    alt Expired or revoked
        A-->>C: 401 TOKEN_EXPIRED
    else Valid
        A->>DB: UPDATE SET isRevoked=true (rotation)
        A->>DB: INSERT new refresh_token
        A-->>C: 200 OK — new cookies set
    end

    Note over C,DB: Logout
    C->>A: POST /auth/logout
    A->>DB: UPDATE refresh_token SET isRevoked=true
    A-->>C: 200 OK — Clear-Cookie both tokens
```

**How it works:**
- Login verifies the bcrypt password hash, generates a device fingerprint, and sets two HTTP-only cookies — access token and refresh token
- Every authenticated request is fingerprint-validated — a token stolen from another device is rejected at the filter level
- Refresh tokens are single-use — each refresh rotates to a new token and revokes the old one
- Logout revokes the refresh token in the database, invalidating the session server-side

---

## 2. File Security Pipeline

<!-- Paste Eraser diagram image URL below -->
> 📌 *Diagram — File Security Pipeline*

```mermaid
flowchart TD
    A[Multipart file received] --> B

    B{Stage 1\nExtension whitelist}
    B -->|.exe .sh .bat .js etc.| E1[400 BLOCKED_FILE_TYPE]
    B -->|.zip .pdf .docx etc.| C

    C{Stage 2\nMIME type verification}
    C -->|MIME ≠ declared extension| E2[400 MIME_MISMATCH]
    C -->|MIME matches| D

    D{Stage 3\nFile size limit · 100MB}
    D -->|Exceeds limit| E3[400 FILE_TOO_LARGE]
    D -->|Within limit| E

    E{Stage 4\nClamAV virus scan\nasync · non-blocking}
    E -->|Malware detected| E4[400 MALWARE_DETECTED]
    E -->|CLEAN| F[All stages passed]

    F --> G[Upload to AWS S3]
    G --> H[INSERT submission record]

    style E1 fill:#fee2e2,stroke:#ef4444,color:#7f1d1d
    style E2 fill:#fee2e2,stroke:#ef4444,color:#7f1d1d
    style E3 fill:#fee2e2,stroke:#ef4444,color:#7f1d1d
    style E4 fill:#fee2e2,stroke:#ef4444,color:#7f1d1d
    style F fill:#dcfce7,stroke:#16a34a,color:#14532d
```

**How it works:**
- Stages run in cheapest-first order — extension check is instant, ClamAV scan is last
- A file that fails any stage is rejected immediately; subsequent stages are not run
- ClamAV runs asynchronously so the validation thread is never blocked waiting for the scan result
- Fail-open in local/dev (ClamAV not required), fail-closed in production (uploads rejected if ClamAV is unavailable)

---

## 3. Team Formation & Invite Flow

<!-- Paste Eraser diagram image URL below -->
> 📌 *Diagram — Team Formation & Invite Flow*

```mermaid
sequenceDiagram
    autonumber
    participant S1 as Student A (Creator)
    participant S2 as Student B (Invitee)
    participant TS as TeamService
    participant DB as MySQL
    participant WS as WebSocket

    Note over S1,WS: Team Creation
    S1->>TS: POST /assignments/{id}/teams { name }
    TS->>DB: Verify S1 enrolled + not already in a team
    TS->>DB: INSERT team + team_member (status=ACCEPTED)
    TS-->>S1: 201 Created { teamId }

    Note over S1,WS: Invite
    S1->>TS: POST /teams/{id}/invite { email }
    TS->>DB: Verify B enrolled + no team + not locked + under max size
    TS->>DB: INSERT team_member (status=PENDING)
    TS->>WS: Notify S2 — TEAM_INVITE push
    WS-->>S2: Real-time invite notification
    TS-->>S1: 200 OK

    Note over S1,WS: Response
    S2->>TS: PATCH /team-members/{id}/respond { accept: true }
    TS->>DB: Verify PENDING + team not locked
    alt Accepted
        TS->>DB: UPDATE status=ACCEPTED
        TS->>WS: Notify S1 — S2 joined
        TS-->>S2: 200 OK
    else Declined
        TS->>DB: UPDATE status=DECLINED
        TS-->>S2: 200 OK
    end

    Note over S1,WS: Team Lock
    TS->>DB: UPDATE team SET is_locked=true
    TS->>WS: TEAM_LOCKED push to all members

    Note over S1,WS: Post-lock invite blocked
    S1->>TS: POST /teams/{id}/invite
    TS-->>S1: 409 TEAM_LOCKED
```

**How it works:**
- Team member status follows a defined lifecycle: `PENDING → ACCEPTED | DECLINED`
- All guard checks run before the `INSERT` — enrollment, existing team, team size, lock status
- Once locked, a team cannot accept new members regardless of size
- Lock can be triggered manually by an instructor or automatically by the scheduler at `team_lock_at`

---

## 4. Submission Pipeline

<!-- Paste Eraser diagram image URL below -->
> 📌 *Diagram — Submission Pipeline*

```mermaid
sequenceDiagram
    autonumber
    participant C as Student
    participant SS as SubmissionService
    participant FSV as FileSecurityValidator
    participant S3 as AWS S3
    participant DB as MySQL
    participant NL as NotificationListener
    participant WS as WebSocket

    C->>SS: POST /submissions (multipart ZIP)
    SS->>DB: Verify team membership + deadline
    SS->>FSV: validate(file) — 4-stage pipeline
    FSV-->>SS: CLEAN ✓
    SS->>DB: SELECT MAX(version_number) WHERE teamId + assignmentId
    DB-->>SS: version = 1
    SS->>S3: putObject(submissions/{team}/{assignment}/v2/file.zip)
    S3-->>SS: storageKey confirmed
    SS->>DB: INSERT submission (version=2, isLate, uploadedBy)
    SS->>SS: HashidService.encode(id) → hashId
    SS-->>C: 201 Created { id: hashId, versionNumber: 2 }

    Note over NL,WS: Async — does not block the 201 response
    SS->>NL: publishEvent(SubmissionUploadedEvent)
    NL->>DB: INSERT notification per team member
    NL->>DB: EVICT unreadCount cache per member
    NL->>DB: INSERT audit_log SUBMISSION_UPLOADED
    NL->>WS: push to each team member
    WS-->>C: Real-time notification
```

**How it works:**
- Submissions are versioned — no overwrite, full history preserved per team per assignment
- `isLate` is computed at write time by comparing `uploaded_at` against `assignment.due_at`
- The 201 response returns before async notification processing completes — upload is never delayed by notification delivery
- All external IDs in the response are Hashid-encoded — the raw database integer is never exposed

---

## 5. Evaluation Pipeline

<!-- Paste Eraser diagram image URL below -->
> 📌 *Diagram — Evaluation Pipeline*

```mermaid
sequenceDiagram
    autonumber
    participant I as Instructor
    participant S as Student
    participant ES as EvaluationService
    participant DB as MySQL
    participant S3 as AWS S3
    participant WS as WebSocket

    Note over I,WS: Create Draft
    I->>ES: POST /evaluations { submissionId, assignmentId }
    ES->>DB: Verify instructor owns course
    ES->>DB: INSERT evaluation (is_draft=true, total_score=0)
    ES-->>I: 201 Created — invisible to student

    Note over S,DB: Student blocked from draft
    S->>ES: GET /evaluations/{id}
    ES->>DB: SELECT WHERE id AND is_draft=false → null
    ES-->>S: 404 Not Found

    Note over I,WS: Score
    I->>ES: PUT /evaluations/{id}/scores
    ES->>ES: Validate each score ≤ criterion.max_score
    ES->>DB: UPSERT rubric_scores
    ES->>DB: UPDATE total_score = SUM(scores)
    ES-->>I: 200 OK { totalScore: 84 }

    Note over I,WS: Publish
    I->>ES: PATCH /evaluations/{id}/publish
    ES->>DB: UPDATE is_draft=false, published_at=NOW()
    ES->>WS: FEEDBACK_PUBLISHED push to team members
    WS-->>S: Grade released notification

    Note over S,DB: Student views published evaluation
    S->>ES: GET /evaluations/{id}
    ES->>DB: SELECT WHERE is_draft=false
    DB-->>ES: evaluation + rubric_scores[]
    ES-->>S: 200 OK — full scores and feedback visible

    Note over I,S3: Generate PDF
    I->>ES: POST /evaluations/{id}/pdf
    ES->>DB: Fetch evaluation + scores + team + assignment
    ES->>ES: Render via OpenPDF
    ES->>S3: putObject(pdfs/{evaluationId}/report.pdf)
    ES->>DB: INSERT audit_log PDF_GENERATED
    ES-->>I: 200 OK { downloadUrl: presignedUrl }
```

**How it works:**
- Draft evaluations return `404` to students — not `403`. Students cannot know an evaluation exists until it is published
- Score validation enforces rubric maximums at write time — invalid scores are rejected before saving
- Publishing is a one-way transition — once published, an evaluation is permanently visible to the team
- PDF is generated on demand, stored in S3, and returned as a pre-signed download URL

---

## 6. Notification Async Flow

<!-- Paste Eraser diagram image URL below -->
> 📌 *Diagram — Notification Async Flow*

```mermaid
sequenceDiagram
    autonumber
    participant SL as Any Service
    participant EP as Event Publisher
    participant NL as NotificationListener (@Async)
    participant DB as MySQL
    participant CC as Caffeine Cache
    participant WS as WebSocket
    participant C as Client

    SL->>EP: publishEvent(DomainEvent)
    Note right of EP: Returns immediately — non-blocking
    EP-->>SL: void

    Note over EP,NL: Separate async thread pool
    EP->>NL: handle(event)
    NL->>NL: Determine recipients
    NL->>DB: INSERT notifications per recipient
    NL->>NL: HashidService.encode → action URL
    NL->>CC: evict unreadCount per recipient
    NL->>WS: convertAndSendToUser /queue/notifications

    alt Client connected
        WS-->>C: Real-time push received
    else Client offline
        Note over DB,C: Push dropped — notification safe in DB
    end

    Note over C,DB: Reconnect recovery
    C->>DB: GET /notifications/unread-count
    Note right of CC: Cache miss → DB query → re-cached
    DB-->>C: { count: 3 }
```

**How it works:**
- `publishEvent()` returns immediately — the calling service thread is never blocked by notification logic
- DB write happens before WebSocket push — a notification is never lost because of a delivery failure
- WebSocket delivery is best-effort — if the client is offline, the push is dropped silently
- Clients recover full unread state on reconnect by polling `GET /notifications/unread-count`, which re-hydrates the cache from the database

---

## 7. Caching Strategy

Four Caffeine caches with deliberately chosen TTLs. The abstraction is Spring Cache — replacing Caffeine with Redis requires changing only `CacheConfig`, no annotation changes across any service.

| Cache | TTL | Cache key | Eviction triggers |
|---|---|---|---|
| `adminStats` | 60 s | `'global'` | Any user, course, or submission change |
| `unreadCount` | 30 s | `userId` | Notification create, read, or delete |
| `userCourses` | 5 min | `userId` | Enroll, unenroll, course archive |
| `assignmentDetail` | 10 min | `assignmentId` | Assignment update, rubric change |

Boundaries were chosen deliberately — only data that is read frequently, changes infrequently, and is safe to serve slightly stale qualifies. Write endpoints never cache; they only evict.

---

## 8. Security Model

| Concern | Implementation |
|---|---|
| Authentication | JWT in HTTP-only cookies — XSS cannot read the token |
| Token binding | Fingerprint hash (User-Agent + IP) embedded in JWT — stolen tokens from another device are rejected |
| Token lifetime | Short-lived access token + rotating refresh token — each refresh token is single-use |
| Authorization | STUDENT / INSTRUCTOR / ADMIN enforced at controller and service layers |
| File safety | 4-stage `FileSecurityValidator` — extension, MIME, size, ClamAV |
| ID safety | Hashids on all external IDs — sequential integers never exposed |
| Rate limiting | Per-IP sliding window on auth endpoints |
| Audit trail | Append-only `audit_log` — all significant write actions recorded with actor, IP, and metadata |
| Security headers | `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, `Referrer-Policy` on all responses |

---

## 9. Data Model

14 tables, fully normalized. All schema changes managed through Flyway versioned migrations — `ddl-auto` is never used.

**Core entities:**

| Entity | Notes |
|---|---|
| `users` | Roles: STUDENT, INSTRUCTOR, ADMIN. Soft delete via `is_active` |
| `courses` | Created by instructors, archived not deleted |
| `course_enrollments` | Many-to-many: students ↔ courses |
| `course_instructors` | Many-to-many: instructors ↔ courses |
| `assignments` | Belongs to a course, has publish flag and lock deadline |
| `rubric_criteria` | Per-assignment scoring criteria with `max_score` and `display_order` |
| `teams` | Per-assignment, `is_locked` flag prevents membership changes |
| `team_members` | Status lifecycle: `PENDING → ACCEPTED \| DECLINED` |
| `submissions` | Versioned per team + assignment — no overwrites |
| `evaluations` | Per submission, `is_draft` gate controls student visibility |
| `rubric_scores` | Line-item scores per criterion per evaluation |
| `notifications` | Per user, `related_entity_type` + `related_entity_id` for deep-linking |
| `refresh_tokens` | Hashed, single-use, with `is_revoked` flag |
| `audit_log` | Append-only, actor email + IP + action + metadata |

---

## 10. Failure Scenarios

### Authentication
- Invalid credentials → rate-limited rejection, generic error message (no hint of which field failed)
- Missing or expired JWT → blocked at filter, never reaches a controller
- Fingerprint mismatch → 401, token treated as stolen from another device
- Refresh token reuse → session invalidated (rotation enforced)

### Authorization
- Student requesting a draft evaluation → 404 (existence not revealed, not 403)
- Instructor accessing a course they do not own → 403
- Student accessing another team's submission → 403
- Invite sent to a locked team → 409 TEAM_LOCKED

### File upload
- Blocked extension → 400 BLOCKED_FILE_TYPE (Stage 1 — cheapest check first)
- MIME type mismatch → 400 MIME_MISMATCH (Stage 2)
- File too large → 400 FILE_TOO_LARGE (Stage 3)
- Malware detected → 400 MALWARE_DETECTED (Stage 4)

### Infrastructure
- WebSocket disconnect → notification persists in DB, client recovers on reconnect
- ClamAV unavailable → fail-open locally, fail-closed in production
- S3 upload failure → submission record not persisted (transaction rolled back)
- Notification delivery failure → DB record preserved, never lost

---

## 11. Known Limitations

| Limitation | Reason |
|---|---|
| No distributed transaction management | Monolith scope — all operations share one DB connection and ACID guarantees |
| No global rate limiting across nodes | Per-instance in-memory only — requires Redis before horizontal scale |
| WebSocket requires sticky sessions for multi-node | Needs a Redis broker before horizontal scale is viable |
| Cache is not shared across nodes | In-memory Caffeine — acceptable until horizontal scale is needed |
| No automated grading pipeline | Manual instructor grading only — by design for this phase |

---

## 12. Architecture Evolution Path

ReviewFlow is structured for service extraction. Domain boundaries are already defined — the codebase is organized by domain, not by technical layer. Extraction becomes a matter of deployment topology, not refactoring.

**Services ready for extraction:**

| Service | Extraction trigger |
|---|---|
| `FileService` | Upload throughput needs independent scaling |
| `NotificationService` | WebSocket needs a dedicated broker |
| `EvaluationService` | Grading workflows need independent deployment |
| `AuthService` | SSO or multi-tenant auth is required |

**Infrastructure prerequisites before any extraction:**
- Kafka or RabbitMQ to replace Spring `ApplicationEvent` for cross-service communication
- Redis for distributed cache and WebSocket broker
- API Gateway for routing, auth delegation, and cross-cutting concerns
