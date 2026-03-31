# ReviewFlow — Module 12: Monitoring & Logging

> Controllers: `ActuatorController` (Spring Boot Actuator), `LoggingService`, `AuditService`
> Base path: `/actuator/`, `/health`, metrics aggregation to CloudWatch
> Related PRD: [PRD-08 Logging & Monitoring](../Features/PRD_08_logging_monitoring.md)
> Related Decisions: [Decision 15, 19](../DECISIONS.md)

---

## 12.1 Module Overview

### Purpose

The Monitoring & Logging module provides comprehensive observability for the ReviewFlow backend through:

- **Health checks** — system readiness and database connectivity
- **Metrics** — JVM memory, threads, GC, database pool, HTTP latency
- **Structured logging** — JSON-formatted logs with request tracing to CloudWatch
- **Audit trail** — immutable record of all CREATE, UPDATE, DELETE, and OVERRIDE actions
- **Request tracing** — X-Trace-Id propagation across logs, metrics, and database

### Scope & Integration

- **Spring Boot Actuator endpoints** — restricted subset enabled (health, metrics, env for admins)
- **CloudWatch Logs** — Primary logging sink; all structured JSON logs streamed to `/reviewflow/backend` log group
- **CloudWatch Metrics** — Custom and JVM metrics published automatically
- **Audit persistence** — Immutable audit table via service layer (never from controller)
- **Request correlation** — TraceIdFilter generates/propagates X-Trace-Id UUID across entire request lifecycle

---

## 12.2 Authentication & Authorization

### Rules

| Endpoint                             | Role Required         | Notes                                                             |
| ------------------------------------ | --------------------- | ----------------------------------------------------------------- |
| `GET /actuator/health`               | **Public**            | No authentication required; always accessible for uptime monitors |
| `GET /actuator/health/db`            | **Public**            | Database connectivity check; no auth required                     |
| `GET /actuator/metrics`              | **Public**            | Metrics index (metric names only); sensitive data excluded        |
| `GET /actuator/metrics/{metricName}` | **Public**            | Individual metric detail (filtered non-sensitive metrics only)    |
| `GET /actuator/env`                  | **SYSTEM_ADMIN only** | Exposes environment variables; highly restricted                  |
| `GET /actuator/beans`                | **SYSTEM_ADMIN only** | Exposes Spring bean details; highly restricted                    |

### Response Codes

- `403 FORBIDDEN` — Non-SYSTEM_ADMIN accessing `/env` or `/beans` → `{ code: "ACTUATOR_FORBIDDEN", message: "Admin access required for this endpoint" }`
- `401 UNAUTHORIZED` — Invalid/expired session (if auth headers present but invalid)
- `200 OK` — All public actuator endpoints return 200 when healthy/accessible

### Security Notes

- [ ] Actuator endpoints are **not** subject to role hierarchy — SYSTEM_ADMIN is explicitly required (ADMIN cannot access `/env` or `/beans`)
- [ ] Public health endpoints return minimal info (no secrets exposed)
- [ ] Sensitive environment variables are **never** returned in `/env` responses
- [ ] Beans endpoint is safe (Spring bean structure, no credentials)

---

## 12.3 Actuator Endpoints (Enabled Subset)

### Endpoint Summary

| Method | Endpoint                         | Purpose                 | Auth         | Response Type      |
| ------ | -------------------------------- | ----------------------- | ------------ | ------------------ |
| GET    | `/actuator/health`               | System health status    | Public       | `application/json` |
| GET    | `/actuator/health/db`            | Database connectivity   | Public       | `application/json` |
| GET    | `/actuator/metrics`              | Available metrics index | Public       | `application/json` |
| GET    | `/actuator/metrics/{metricName}` | Specific metric detail  | Public       | `application/json` |
| GET    | `/actuator/env`                  | Environment properties  | SYSTEM_ADMIN | `application/json` |
| GET    | `/actuator/beans`                | Spring bean registry    | SYSTEM_ADMIN | `application/json` |

### 12.3.1 GET /actuator/health

### Must Have

- [ ] Returns overall system health status
- [ ] Always accessible (no authentication)
- [ ] Includes database connectivity check
- [ ] Response: `{ status: "UP" | "DOWN" | "DEGRADED", components: { db: { status } } }`

### Responses

- [ ] `200 OK` — status is UP
  ```json
  {
    "status": "UP",
    "components": {
      "db": {
        "status": "UP",
        "details": {
          "database": "PostgreSQL",
          "result": 1
        }
      },
      "diskSpace": {
        "status": "UP",
        "details": {
          "total": 1099511627776,
          "free": 549755813888,
          "threshold": 10485760,
          "exists": true
        }
      }
    }
  }
  ```
- [ ] `503 Service Unavailable` — status is DOWN or DEGRADED (cannot connect to database)
  ```json
  {
    "status": "DOWN",
    "components": {
      "db": {
        "status": "DOWN",
        "details": {
          "error": "Connection refused: localhost:5432"
        }
      }
    }
  }
  ```

---

### 12.3.2 GET /actuator/health/db

### Must Have

- [ ] Database-specific health check
- [ ] Executes simple query; no auth required
- [ ] Response: `{ status: "UP" | "DOWN", database: "PostgreSQL", result: 1 }`

### Responses

- [ ] `200 OK` — database is reachable and responsive
  ```json
  {
    "status": "UP",
    "details": {
      "database": "PostgreSQL",
      "result": 1
    }
  }
  ```
- [ ] `503 Service Unavailable` — database connection failed
  ```json
  {
    "status": "DOWN",
    "details": {
      "error": "Connection refused",
      "database": "PostgreSQL"
    }
  }
  ```

---

### 12.3.3 GET /actuator/metrics

### Must Have

- [ ] Returns list of available metric names
- [ ] No authentication required
- [ ] Response: `{ names: ["jvm.memory.used", "jvm.memory.max", "http.server.requests", ...] }`

### Responses

- [ ] `200 OK` — metrics list
  ```json
  {
    "names": [
      "jvm.memory.used",
      "jvm.memory.max",
      "jvm.threads.live",
      "jvm.threads.peak",
      "jvm.gc.max.data.size",
      "jvm.gc.memory.allocated",
      "jvm.gc.memory.promoted",
      "jvm.gc.pause",
      "process.cpu.usage",
      "process.uptime",
      "system.cpu.usage",
      "system.load.average.1m",
      "http.server.requests",
      "db.connection.active",
      "db.connection.idle",
      "cache.hit.ratio"
    ]
  }
  ```

---

### 12.3.4 GET /actuator/metrics/{metricName}

### Must Have

- [ ] Returns detailed data for a specific metric
- [ ] Query parameter: `?tag=key:value` (optional; filters by dimension)
- [ ] Response: `{ name, unit, measurements: [{ statistic, value }], baseUnit }`

### Responses

- [ ] `200 OK` — metric detail
  ```json
  {
    "name": "jvm.memory.used",
    "description": "The amount of used memory",
    "baseUnit": "bytes",
    "measurements": [
      {
        "statistic": "VALUE",
        "value": 536870912
      }
    ],
    "availableTags": [
      {
        "tag": "area",
        "values": ["heap", "nonheap"]
      }
    ]
  }
  ```
- [ ] `200 OK` — HTTP request latencies (tagged by endpoint)
  ```json
  {
    "name": "http.server.requests",
    "description": "HTTP Server request duration",
    "baseUnit": "seconds",
    "measurements": [
      {
        "statistic": "COUNT",
        "value": 1523
      },
      {
        "statistic": "TOTAL_TIME",
        "value": 45.23
      },
      {
        "statistic": "MAX",
        "value": 2.1
      }
    ],
    "availableTags": [
      {
        "tag": "method",
        "values": ["GET", "POST", "PUT", "DELETE"]
      },
      {
        "tag": "uri",
        "values": [
          "/api/v1/assignments",
          "/api/v1/submissions",
          "/api/v1/evaluations"
        ]
      }
    ]
  }
  ```
- [ ] `404 Not Found` — metric name doesn't exist

---

### 12.3.5 GET /actuator/env

### Must Have

- [ ] **SYSTEM_ADMIN only**
- [ ] Returns environment properties and active profiles
- [ ] Redacts sensitive values (API keys, passwords, DB connection strings)
- [ ] Response includes active profiles, property sources, properties

### Responses

- [ ] `200 OK` (SYSTEM_ADMIN) — environment detail
  ```json
  {
    "propertySources": [
      {
        "name": "systemEnvironment",
        "properties": {
          "JAVA_HOME": {
            "value": "/usr/lib/jvm/java-21-openjdk"
          },
          "SPRING_PROFILES_ACTIVE": {
            "value": "prod"
          },
          "DATABASE_URL": {
            "value": "****"
          }
        }
      },
      {
        "name": "systemProperties",
        "properties": {
          "java.version": {
            "value": "21.0.1"
          }
        }
      }
    ],
    "activeProfiles": ["prod"],
    "defaultProfiles": ["default"]
  }
  ```
- [ ] `403 Forbidden` (non-SYSTEM_ADMIN) — `{ code: "ACTUATOR_FORBIDDEN", message: "Admin access required" }`

---

### 12.3.6 GET /actuator/beans

### Must Have

- [ ] **SYSTEM_ADMIN only**
- [ ] Returns all registered Spring beans and dependencies
- [ ] Useful for debugging bean wiring issues
- [ ] Response: `{ contexts: { "application": { beans: { "beanName": {...} } } } }`

### Responses

- [ ] `200 OK` (SYSTEM_ADMIN) — bean registry
  ```json
  {
    "contexts": {
      "application": {
        "beans": {
          "courseService": {
            "aliases": [],
            "scope": "singleton",
            "type": "com.reviewflow.service.CourseService",
            "resourceUrl": "file:[...].jar!/BOOT-INF/classes/com/reviewflow/service/CourseService.class",
            "dependencies": ["courseRepository", "userRepository"]
          },
          "evaluationController": {
            "aliases": [],
            "scope": "singleton",
            "type": "com.reviewflow.controller.EvaluationController",
            "resourceUrl": "file:[...].jar!/BOOT-INF/classes/com/reviewflow/controller/EvaluationController.class",
            "dependencies": ["evaluationService", "submissionService"]
          }
        }
      }
    }
  }
  ```
- [ ] `403 Forbidden` (non-SYSTEM_ADMIN)

---

## 12.4 Request/Response Examples

### Health Check Example (Uptime Monitor)

**Request:**

```http
GET /actuator/health HTTP/1.1
Host: reviewflow-backend.example.com
```

**Response:**

```http
HTTP/1.1 200 OK
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "result": 1
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1099511627776,
        "free": 549755813888,
        "threshold": 10485760
      }
    }
  },
  "timestamp": "2026-03-30T14:22:10.182Z"
}
```

### Metrics Query Example

**Request:**

```http
GET /actuator/metrics/http.server.requests?tag=method:POST HTTP/1.1
Host: reviewflow-backend.example.com
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440001
```

**Response:**

```http
HTTP/1.1 200 OK
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440001
Content-Type: application/json

{
  "name": "http.server.requests",
  "description": "HTTP requests with latency",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 342
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 12.45
    },
    {
      "statistic": "MAX",
      "value": 0.85
    }
  ],
  "availableTags": [
    {
      "tag": "method",
      "values": ["POST"]
    },
    {
      "tag": "uri",
      "values": ["/api/v1/submissions", "/api/v1/evaluations"]
    },
    {
      "tag": "status",
      "values": ["200", "400", "403", "500"]
    }
  ]
}
```

---

## 12.5 Structured Logging (JSON to CloudWatch)

### Architecture

All application logs are formatted as **JSON lines** and streamed to CloudWatch:

- **Encoder:** `LogstashEncoder` (Logback with JSON output)
- **Output:** CloudWatch Logs via CloudWatch Logs Appender
- **Log Group:** `/reviewflow/backend`
- **Stream:** `{environment}-{instanceId}` (e.g., `prod-i-0a1b2c3d`)
- **Retention:** 30 days (production)

### MDC (Mapped Diagnostic Context) Fields

Every log entry includes these context fields via MDC:

| Field        | Type     | Source                                 | Example                                | Purpose                                |
| ------------ | -------- | -------------------------------------- | -------------------------------------- | -------------------------------------- |
| `traceId`    | UUID     | X-Trace-Id header or generated         | `550e8400-e29b-41d4-a716-446655440000` | Correlates all logs for single request |
| `userId`     | Number   | Authenticated user                     | `42`                                   | Identifies actor in audit logs         |
| `courseId`   | Number   | Current course context (if applicable) | `5`                                    | Scopes action to course                |
| `actionType` | String   | Endpoint/method name                   | `POST_SUBMISSION`, `UPDATE_EVALUATION` | What action is being performed         |
| `sessionId`  | UUID     | Session token                          | `abc-def-ghi`                          | Session correlation                    |
| `timestamp`  | ISO 8601 | LogstashEncoder                        | `2026-03-30T14:22:10.182Z`             | When event occurred                    |

### JSON Log Format Example

**Application log entry (successfully created submission):**

```json
{
  "timestamp": "2026-03-30T14:22:10.182Z",
  "level": "INFO",
  "logger": "com.reviewflow.service.SubmissionService",
  "message": "Submission created",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 42,
  "courseId": 5,
  "actionType": "CREATE_SUBMISSION",
  "sessionId": "abc-def-ghi-123",
  "submissionId": 99,
  "teamId": 7,
  "assignmentId": 3,
  "fileSize": 2048576,
  "thread": "http-nio-8081-exec-5"
}
```

**Error log entry (failed submission validation):**

```json
{
  "timestamp": "2026-03-30T14:23:45.291Z",
  "level": "WARN",
  "logger": "com.reviewflow.controller.SubmissionController",
  "message": "Validation error: File exceeds size limit",
  "traceId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": 43,
  "courseId": 5,
  "actionType": "POST_SUBMISSION",
  "sessionId": "xyz-uvw-rst",
  "error": "FILE_TOO_LARGE",
  "maxSize": 10485760,
  "submittedSize": 52428800,
  "thread": "http-nio-8081-exec-6",
  "exception": "com.reviewflow.exception.ValidationException"
}
```

**Suspicious action log entry (rate limit exceeded):**

```json
{
  "timestamp": "2026-03-30T14:24:00.000Z",
  "level": "WARN",
  "logger": "com.reviewflow.security.RateLimitFilter",
  "message": "Rate limit exceeded for user",
  "traceId": "550e8400-e29b-41d4-a716-446655440002",
  "userId": 44,
  "actionType": "GET_ASSIGNMENTS",
  "sessionId": "def-ghi-jkl",
  "requestsPerMinute": 61,
  "limit": 60,
  "ipAddress": "203.0.113.45",
  "thread": "http-nio-8081-exec-7"
}
```

### Log Levels

| Level     | When Used                                             | Example                                                                           |
| --------- | ----------------------------------------------------- | --------------------------------------------------------------------------------- |
| **ERROR** | Failures that require attention; unhandled exceptions | Database connection failed, out of disk space, service call error                 |
| **WARN**  | Suspicious activity, rate limits, validation failures | Invalid token, user locked out, file size exceeded, concurrent evaluation attempt |
| **INFO**  | Auditable actions; normal business operations         | Submission created, evaluation published, user invited to team                    |
| **DEBUG** | Development/troubleshooting (disabled in production)  | SQL parameters, internal state transitions                                        |

### CloudWatch Queries (Examples)

**Filter logs by trace ID to follow entire request:**

```
fields @timestamp, level, message, actionType
| filter traceId = "550e8400-e29b-41d4-a716-446655440000"
| sort @timestamp asc
```

**Find all errors for a specific user in the last hour:**

```
fields @timestamp, level, message, actionType, error
| filter userId = 42 and level = "ERROR"
| stats count() by actionType
```

**Monitor suspicious activity (WARN level):**

```
fields @timestamp, userId, actionType, message, ipAddress
| filter level = "WARN"
| stats count() as suspiciousEvents by userId
| filter suspiciousEvents > 5
```

---

## 12.6 Audit Trail (Immutable)

### Purpose

Maintain an append-only record of all data modifications for compliance, debugging, and accountability. Audit records are **never updated or deleted** after creation.

### Tracked Operations

| Operation    | Entity Examples                                                                                                      | Trigger                                | Actor Required                  |
| ------------ | -------------------------------------------------------------------------------------------------------------------- | -------------------------------------- | ------------------------------- |
| **CREATE**   | User, Course, Assignment, Team, Submission, Evaluation                                                               | New entity persisted                   | User ID automatically captured  |
| **UPDATE**   | Evaluation (scores/comments), Submission (resubmit), Course (details), User (email), Assignment (deadline extension) | Existing entity modified               | User ID automatically captured  |
| **DELETE**   | Course (archived), User (rare deactivation), Team (dissolution)                                                      | Soft delete via flag; hard delete rare | Admin/SYSTEM_ADMIN only         |
| **OVERRIDE** | Reopen evaluation, force team unlock, extend deadline (admin action)                                                 | Admin/SYSTEM_ADMIN intervention        | Admin user ID + reason captured |

### Audit Table Schema

```sql
CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  action VARCHAR(50) NOT NULL,           -- CREATE, UPDATE, DELETE, OVERRIDE
  entity_type VARCHAR(100) NOT NULL,     -- User, Course, Submission, Evaluation, etc.
  entity_id BIGINT NOT NULL,             -- ID of affected entity
  actor_id BIGINT NOT NULL,              -- User ID performing the action
  actor_role VARCHAR(20) NOT NULL,       -- STUDENT, INSTRUCTOR, ADMIN, SYSTEM_ADMIN
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_set JSONB,                      -- Detailed changes (for UPDATE only)
  reason VARCHAR(500),                   -- Admin override reason
  trace_id UUID,                         -- X-Trace-Id for request correlation
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Immutability constraint: Only INSERT allowed (no UPDATE, no DELETE)
-- Indexes for performance
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_id, timestamp DESC);
CREATE INDEX idx_audit_action ON audit_log(action, timestamp DESC);
CREATE INDEX idx_audit_trace ON audit_log(trace_id);
```

### Audit Record Format (JSON)

**CREATE example (submission created):**

```json
{
  "id": 10001,
  "action": "CREATE",
  "entity_type": "Submission",
  "entity_id": 99,
  "actor_id": 42,
  "actor_role": "STUDENT",
  "timestamp": "2026-03-30T14:22:10.182Z",
  "change_set": {
    "teamId": 7,
    "assignmentId": 3,
    "submissionDate": "2026-03-30T14:22:10.182Z",
    "status": "SUBMITTED"
  },
  "reason": null,
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**UPDATE example (evaluation scores modified):**

```json
{
  "id": 10002,
  "action": "UPDATE",
  "entity_type": "Evaluation",
  "entity_id": 45,
  "actor_id": 15,
  "actor_role": "INSTRUCTOR",
  "timestamp": "2026-03-30T14:25:33.490Z",
  "change_set": {
    "before": {
      "totalScore": 0,
      "isDraft": true,
      "rubricScores": []
    },
    "after": {
      "totalScore": 85,
      "isDraft": true,
      "rubricScores": [
        { "criterionId": 1, "score": 20 },
        { "criterionId": 2, "score": 20 },
        { "criterionId": 3, "score": 18 },
        { "criterionId": 4, "score": 17 }
      ]
    },
    "modifications": ["totalScore", "rubricScores"]
  },
  "reason": null,
  "trace_id": "550e8400-e29b-41d4-a716-446655440001"
}
```

**OVERRIDE example (admin reopens evaluation):**

```json
{
  "id": 10003,
  "action": "OVERRIDE",
  "entity_type": "Evaluation",
  "entity_id": 45,
  "actor_id": 1,
  "actor_role": "SYSTEM_ADMIN",
  "timestamp": "2026-03-30T14:27:00.000Z",
  "change_set": {
    "fieldModified": "isDraft",
    "previousValue": false,
    "newValue": true
  },
  "reason": "Instructor requested reopening due to scoring error",
  "trace_id": "550e8400-e29b-41d4-a716-446655440002"
}
```

### Integration Point (Service Layer)

Audit records are **never created from controllers**. Instead, they are logged from the **service/business logic layer** where the actual data change happens:

```java
// Example: SubmissionService.createSubmission()
public Submission createSubmission(SubmissionRequest req, Long userId) {
  Submission submission = new Submission(...);
  submissionRepository.save(submission);

  // Audit trail created here, not in controller
  auditService.logCreate("Submission", submission.getId(), userId,
    getUserRole(userId), getTraceId(), submissionDetails);

  return submission;
}
```

### Immutability Guarantees

- [ ] Audit table schema enforces **INSERT-only** semantics
- [ ] No UPDATE of audit records after insertion
- [ ] No DELETE of audit records (ever)
- [ ] Database-level constraints prevent circumvention
- [ ] Application code **never** calls update/delete on audit records
- [ ] Row-level security (RLS) prevents unauthorized audit access (SYSTEM_ADMIN only)

---

## 12.7 Request Tracing (X-Trace-Id)

### Overview

Every HTTP request is assigned a unique **X-Trace-Id** (UUID) that propagates across:

- Request/response headers
- Structured logs (MDC field)
- Database audit trail
- Metrics tags

This enables complete request lifecycle visibility in CloudWatch.

### Trace ID Lifecycle

```
1. Incoming Request
   ↓
   TraceIdFilter checks for X-Trace-Id header
   ├─ If present: reuse it
   └─ If missing: generate new UUID
   ↓
2. MDC Setup (Logback)
   ├─ traceId = UUID from header/generated
   ├─ All log lines include traceId automatically
   ├─ Database audit_log.trace_id set
   └─ Response header X-Trace-Id returned to client
   ↓
3. Service Layer Processing
   └─ auditService.logCreate() includes trace_id
   ↓
4. Response
   └─ X-Trace-Id: {same UUID as request}
```

### TraceIdFilter Implementation

| Responsibility  | Details                                                       |
| --------------- | ------------------------------------------------------------- |
| **Generation**  | If X-Trace-Id header missing, generate new UUID v4            |
| **Propagation** | Pass UUID to MDC (Logback) for automatic log inclusion        |
| **Headers**     | Add X-Trace-Id to response headers (Spring response wrapper)  |
| **Immutable**   | Once set for request, never changes during request processing |

### Request/Response Example

**Request with pre-existing trace ID:**

```http
GET /api/v1/evaluations/45 HTTP/1.1
Host: reviewflow-backend.example.com
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer eyJh...
```

**Response (same trace ID):**

```http
HTTP/1.1 200 OK
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000
X-Total-Elements: 1
Content-Type: application/json

{
  "success": true,
  "data": { ... },
  "timestamp": "2026-03-30T14:22:10.182Z"
}
```

**Downstream logs (all include same trace ID):**

```
[2026-03-30 14:22:10] traceId=550e8400-e29b-41d4-a716-446655440000 userId=42 INFO EvaluationService - Fetching evaluation 45
[2026-03-30 14:22:10] traceId=550e8400-e29b-41d4-a716-446655440000 userId=42 INFO RubricService - Loaded 4 rubric criteria
[2026-03-30 14:22:10] traceId=550e8400-e29b-41d4-a716-446655440000 userId=42 INFO SubmissionService - Loaded submission 99
```

### CloudWatch Trace ID Queries

**Retrieve entire request lifecycle:**

```
fields @timestamp, logger, level, message, actionType, userId
| filter traceId = "550e8400-e29b-41d4-a716-446655440000"
| sort @timestamp asc
```

**Identify slowest requests by trace ID:**

```
fields @timestamp, traceId, @duration
| filter @duration > 1000
| sort @duration desc
| stats avg(@duration) as avgDuration by traceId
```

---

## 12.8 Metrics

### Available Metrics

| Category               | Metric                      | Description                    | Tags                  |
| ---------------------- | --------------------------- | ------------------------------ | --------------------- |
| **JVM Memory**         | `jvm.memory.used`           | Used memory (heap/non-heap)    | area (heap, nonheap)  |
|                        | `jvm.memory.max`            | Maximum allocatable memory     | area                  |
|                        | `jvm.memory.committed`      | Currently committed memory     | area                  |
| **Threads**            | `jvm.threads.live`          | Currently live thread count    | —                     |
|                        | `jvm.threads.peak`          | Peak thread count since start  | —                     |
|                        | `jvm.threads.started.total` | Total threads started          | —                     |
| **Garbage Collection** | `jvm.gc.pause`              | GC pause duration histogram    | action (minor, major) |
|                        | `jvm.gc.max.data.size`      | Max GC data region size        | —                     |
|                        | `jvm.gc.memory.allocated`   | Memory allocated per GC        | —                     |
|                        | `jvm.gc.memory.promoted`    | Memory promoted per GC         | —                     |
| **Process**            | `process.cpu.usage`         | Current process CPU usage %    | —                     |
|                        | `process.uptime`            | Application uptime (seconds)   | —                     |
| **System**             | `system.cpu.usage`          | System CPU usage %             | —                     |
|                        | `system.load.average.1m`    | 1-minute load average          | —                     |
|                        | `system.memory.usage`       | System memory usage %          | —                     |
| **HTTP**               | `http.server.requests`      | HTTP request latency histogram | method, uri, status   |
| **Database**           | `db.connection.active`      | Active database connections    | pool                  |
|                        | `db.connection.idle`        | Idle database connections      | pool                  |
|                        | `db.connection.max`         | Maximum connection pool size   | pool                  |
| **Cache**              | `cache.hit.ratio`           | Cache hit ratio per cache name | cache                 |
|                        | `cache.gets.miss`           | Cache misses                   | cache                 |

### Custom Metrics Example

**Cache hit rate query:**

```
fields @timestamp, cacheName, hitCount, missCount
| stats sum(hitCount) as totalHits, sum(missCount) as totalMisses by cacheName
| fields cacheName, hitRate = (totalHits / (totalHits + totalMisses) * 100)
```

**HTTP endpoint latency (95th percentile):**

```
fields @timestamp, uri, @duration = http.server.requests
| filter uri = "/api/v1/submissions"
| stats pct(@duration, 95) as p95Latency by method
```

---

## 12.9 Error Codes

### Monitoring Module Error Codes

| Code                  | HTTP | Meaning                                        | Response                                                                             |
| --------------------- | ---- | ---------------------------------------------- | ------------------------------------------------------------------------------------ |
| `ACTUATOR_FORBIDDEN`  | 403  | Non-SYSTEM_ADMIN accessing `/env` or `/beans`  | `{ code: "ACTUATOR_FORBIDDEN", message: "Admin access required for this endpoint" }` |
| `UNAUTHORIZED`        | 401  | Missing/invalid authentication token           | `{ code: "UNAUTHORIZED", message: "Access token required" }`                         |
| `ACCOUNT_DEACTIVATED` | 403  | User account is inactive even with valid token | `{ code: "ACCOUNT_DEACTIVATED", message: "Your account has been deactivated" }`      |
| `METRIC_NOT_FOUND`    | 404  | Requested metric name doesn't exist            | `{ code: "METRIC_NOT_FOUND", message: "Metric 'invalid.metric' not found" }`         |

---

## 12.10 Cloud Integration

### CloudWatch Logs Configuration

**Appender configuration (logback-spring.xml):**

```xml
<appender name="CLOUDWATCH" class="com.amazonaws.logging.CloudWatchAppender">
  <logGroupName>/reviewflow/backend</logGroupName>
  <logStreamName>${LOG_STREAM_NAME:-${environment}-${instanceId}}</logStreamName>
  <encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <fieldName>timestamp</fieldName>
    <includeContext>true</includeContext>
    <includeMdc>true</includeMdc>
    <includeTags>true</includeTags>
  </encoder>
  <retentionInDays>30</retentionInDays>
</appender>
```

### Log Group & Stream Naming

| Environment | Log Group                     | Stream Pattern                   |
| ----------- | ----------------------------- | -------------------------------- |
| Local dev   | `/reviewflow/backend-dev`     | `localhost-dev`                  |
| Staging     | `/reviewflow/backend-staging` | `staging-prod-i-xxxxx`           |
| Production  | `/reviewflow/backend`         | `prod-i-xxxxx` (AWS instance ID) |

### CloudWatch Metrics Publishing

- **Automatic:** Spring Actuator publishes metrics to CloudWatch every 60 seconds
- **Dimensions:** environment, instance-id, service-name
- **Namespace:** `ReviewFlow/Backend`
- **Custom metrics:** Published by application code via `MeterRegistry`

### CloudWatch Dashboards (Recommended Setup)

**System Health Dashboard:**

- Health status (UP/DOWN/DEGRADED)
- Database connection pool (active, idle, max)
- JVM memory usage (heap vs non-heap)
- Request latency (p95, p99)

**Error Tracking Dashboard:**

- Error rate by endpoint
- 5xx errors over time
- Top error types
- Rate limit violations

**Audit Trail Dashboard:**

- CREATE/UPDATE/DELETE/OVERRIDE events over time
- Actions by role
- Top modified entities
- Failed audit operations

### Future Upgrades

#### X-Ray Distributed Tracing

- **Goal:** Trace requests across multiple microservices (future backend services)
- **Setup:** Enable X-Ray daemon on EC2, update SDK configuration
- **Integration:** Wrap HTTP clients with X-Ray TracingHandler
- **Benefit:** Service map visualization, latency breakdown by service

#### Custom Metrics for Business Logic

- **Example:** Submission completion rates, team formation success rates, evaluation turnaround time
- **Implementation:** Inject `MeterRegistry` into services, record custom gauges/timers
- **Visualization:** Create business KPI dashboard

#### Alerting Configuration

- **Health threshold alerts:** When health status = DOWN for > 5 minutes
- **Error rate alerts:** When 5xx errors > 5% of requests
- **Performance alerts:** When p95 latency > 2 seconds
- **Audit alerts:** When DELETE/OVERRIDE actions > threshold

---

## 12.11 Related PRDs & Decisions

### Related PRDs

- **[PRD-08 Logging & Monitoring](../Features/PRD_08_logging_monitoring.md)** — Comprehensive logging requirements, CloudWatch integration, audit trail

### Related Decisions

- **[Decision 15](../DECISIONS.md)** — Actuator endpoint security configuration
- **[Decision 19](../DECISIONS.md)** — Audit trail immutability and retention policy

### Cross-References

- See [ARCHITECTURE.md](../ARCHITECTURE.md) for system design overview
- See [Global Rules & Reference](./00_Global_Rules_and_Reference.md) for authentication rules and response envelope
- See [AUTOMATED_TESTS_GUIDE.md](./AUTOMATED_TESTS_GUIDE.md) for testing monitoring endpoints

---

## 12.12 Implementation Checklist

### Controllers & Filters

- [ ] `TraceIdFilter` — Generates/propagates X-Trace-Id
- [ ] `AuditLoggingAspect` — Intercepts service calls to log audit records
- [ ] ActuatorEndpointFilter — Enforces SYSTEM_ADMIN / public restrictions
- [ ] `LoggingConfiguration` — CloudWatch appender + LogstashEncoder setup

### Services

- [ ] `AuditService.logCreate()` — Logs CREATE operations
- [ ] `AuditService.logUpdate()` — Logs UPDATE with before/after details
- [ ] `AuditService.logDelete()` — Logs DELETE operations
- [ ] `AuditService.logOverride()` — Logs admin overrides with reason

### Database

- [ ] `audit_log` table — Immutable, INSERT-only semantics
- [ ] Indexes on entity_id, actor_id, action, trace_id
- [ ] Row-level security (RLS) — SYSTEM_ADMIN only read access

### Testing

- [ ] Actuator endpoints return correct health status
- [ ] Public health/metrics endpoints are accessible without auth
- [ ] `/env` and `/beans` return 403 for non-SYSTEM_ADMIN
- [ ] Trace ID propagates through request → logs → audit trail
- [ ] Audit records are immutable (verify via INSERT-only constraints)
- [ ] CloudWatch logs contain structured JSON with all MDC fields

### Deployment

- [ ] CloudWatch log group created: `/reviewflow/backend`
- [ ] CloudWatch Logs IAM role configured for EC2 instances
- [ ] Log retention policy set (30 days for prod)
- [ ] CloudWatch dashboards created for health, errors, audit
- [ ] Log streams follow naming convention: `{environment}-{instanceId}`

---

**Last Updated:** 2026-03-30  
**Agent:** E4 (Monitoring Module Agent)  
**Status:** Ready for implementation
