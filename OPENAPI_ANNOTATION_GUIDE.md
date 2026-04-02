# OpenAPI SpringDoc Annotation Guide

**Phase 4 Objective**: Comprehensive OpenAPI documentation for all REST controllers.

## Overview

This guide documents the annotation patterns and best practices for SpringDoc-OpenAPI (OpenAPI 3.0) in the ReviewFlow backend. Two example controllers are fully annotated: `AuthController` and `CourseController`. This guide enables other controllers to be annotated consistently.

## Completed Examples

### ✅ AuthController
- **Location**: `src/main/java/com/reviewflow/controller/AuthController.java`
- **Endpoints**: 5 endpoints (login, refresh, logout, current user, token for WebSocket)
- **Pattern**: Authentication-focused with security-specific response descriptions
- **Status**: Fully annotated with all @ApiResponse content schemas

### ✅ CourseController
- **Location**: `src/main/java/com/reviewflow/controller/CourseController.java`
- **Endpoints**: 11 endpoints (CRUD + enrollment management)
- **Pattern**: CRUD operations with role-based authorization
- **Status**: Fully annotated with all @ApiResponse content schemas

## Annotation Pattern - Step by Step

### 1. Import Required Classes

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

**⚠️ IMPORTANT**: Do NOT import `ApiResponse` from `io.swagger.v3.oas.annotations.responses`. This conflicts with the response DTO class `com.reviewflow.model.dto.response.ApiResponse`. Always use the fully qualified name `@io.swagger.v3.oas.annotations.responses.ApiResponse` when needed.

### 2. Add @Tag to Controller Class

Apply to the class declaration to group all endpoints under a documentation section:

```java
@RestController
@RequestMapping("/api/v1/resource")
@RequiredArgsConstructor
@Tag(
    name = "Resource Module",  // Swagger UI group name
    description = "Description of what this controller manages. Mention key features, " +
                "authorization requirements, and main use cases."
)
public class ResourceController {
```

**Guidelines**:
- Use singular, descriptive names (e.g., "Course" not "Courses")
- Description should be 1-2 sentences explaining the module's purpose
- Mention key role requirements if most endpoints have them

### 3. Annotate Each Endpoint with @Operation

Place directly before the HTTP verb annotation (@GetMapping, @PostMapping, etc.):

```java
@Operation(
    summary = "Action verb + resource (3-5 words max)",
    description = "Complete description explaining what the endpoint does, who can use it, " +
                  "side effects, and edge cases. Should be 1-3 sentences."
)
```

**Summary Guidelines**:
- Be action-oriented: "Create user", "Update course", "List assignments"
- Keep to 8-35 characters
- Examples from completed controllers:
  - "Login" (5 chars)
  - "Refresh access token" (19 chars)
  - "Get current user profile" (23 chars)
  - "List courses" (12 chars)
  - "Assign instructor to course" (26 chars)

**Description Guidelines**:
- Explain the purpose and main behavior
- Mention required roles (@PreAuthorize restrictions)
- Note any special behaviors (rate limiting, immutability, etc.)
- Examples from completed controllers:
  - "Authenticate user with email and password credentials. Sets HTTP-only secure cookies for access and refresh tokens. Implements rate limiting on failed attempts."
  - "Create new course. Requires ADMIN role. Initializes course with code, name, term, and optional description."
  - "Retrieve paginated list of courses accessible to authenticated user. Admins see all courses, instructors see courses they teach, students see enrolled courses."

### 4. Add @ApiResponses with Comprehensive Status Codes

Goes immediately after @Operation. Includes all possible HTTP response codes:

```java
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",  // Standard success response
        description = "Operation successful, [what was created/updated/retrieved]",
        content = @Content(schema = @Schema(implementation = ResponseDtoClass.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",  // Used for POST that creates resources
        description = "Resource created successfully",
        content = @Content(schema = @Schema(implementation = ResponseDtoClass.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",  // Bad Request: validation errors, missing required fields
        description = "Bad Request - invalid request data [specify what was wrong]",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",  // Unauthorized: not authenticated
        description = "Unauthorized - authentication required",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",  // Forbidden: authenticated but lacks permission
        description = "Forbidden - [reason, e.g., ADMIN role required]",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",  // Not Found: resource doesn't exist
        description = "Not Found - [resource name] does not exist",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",  // Conflict: business logic violation (e.g., duplicate ID)
        description = "Conflict - [explain the conflict, e.g., course code already exists]",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "429",  // Too Many Requests: rate limiting
        description = "Too Many Requests - [reason, e.g., too many failed login attempts]",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",  // Server error (usually included implicitly)
        description = "Internal Server Error - [context if known]",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
```

**Response Code Selection**:

| Code | When to Use | Example |
|------|-----------|---------|
| 200 | GET, PUT, PATCH success | List, update, archive operations |
| 201 | POST success (creates resource) | Create user, create course |
| 204 | Success with no content | Delete operations (optional) |
| 400 | Validation errors, malformed requests | Missing email, invalid format, duplicate code |
| 401 | Not authenticated | Missing token, expired token |
| 403 | Authenticated but lacks permission | ADMIN-only endpoint, no access to resource |
| 404 | Resource doesn't exist | User not found, course doesn't exist |
| 409 | Business logic violation | Duplicate entry, invalid state transition |
| 429 | Rate limiting | Too many failed login attempts |
| 500 | Server error (usually implicit) | Database exception, unexpected error |

**Content Schema Specification**:

- **Success Responses** (200, 201): Use `implementation = ResponseDtoClass.class`
  ```java
  content = @Content(schema = @Schema(implementation = CourseResponse.class))
  content = @Content(schema = @Schema(implementation = AuthUserResponse.class))
  content = @Content(schema = @Schema(implementation = Map.class))  // For generic key-value responses
  content = @Content(schema = @Schema(implementation = List.class))  // For lists
  ```

- **Error Responses** (400, 401, 403, 404, 409, 429, 500): Use `ref = "#/components/schemas/ApiErrorResponse"`
  ```java
  content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
  ```

**Minimal vs. Comprehensive Guidelines**:

- **Minimal** (GET simple read): 200 + 401 (needed?)
- **Standard** (POST/PUT/DELETE): 200/201 + 400 + 401 + 403 + 404 (if ID-based)
- **Comprehensive** (Complex endpoints): All relevant codes
- **Auth endpoints special**: Include 429 for rate limiting

## ResponseEntity Patterns by HTTP Method

### GET Endpoints

```java
@Operation(
    summary = "Get resource",
    description = "Retrieve a specific resource by ID."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Resource retrieved",
        content = @Content(schema = @Schema(implementation = ResourceResponse.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ResourceResponse>> get(@PathVariable String id) {
    // ...
}
```

### POST Endpoints (Create)

```java
@Operation(
    summary = "Create resource",
    description = "Create a new resource with provided details."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Resource created",
        content = @Content(schema = @Schema(implementation = ResourceResponse.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Invalid resource data",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Insufficient permissions",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@PostMapping
public ResponseEntity<ApiResponse<ResourceResponse>> create(
        @Valid @RequestBody CreateResourceRequest request) {
    // ...
}
```

### PUT Endpoints (Update)

```java
@Operation(
    summary = "Update resource",
    description = "Update all fields of an existing resource."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Resource updated",
        content = @Content(schema = @Schema(implementation = ResourceResponse.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Invalid update data",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Permission denied",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ResourceResponse>> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateResourceRequest request) {
    // ...
}
```

### PATCH Endpoints (Partial Update/Action)

```java
@Operation(
    summary = "Archive resource",  // Or whatever state change
    description = "Archive (soft delete) a resource. Archived resources are hidden from listings."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Resource archived",
        content = @Content(schema = @Schema(implementation = ResourceResponse.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Permission denied",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@PatchMapping("/{id}/archive")
public ResponseEntity<ApiResponse<ResourceResponse>> archive(@PathVariable String id) {
    // ...
}
```

### DELETE Endpoints

```java
@Operation(
    summary = "Delete resource",
    description = "Permanently delete a resource from the system."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "204",  // or 200 with no content
        description = "Resource deleted successfully"
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Permission denied",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id) {
    // ...
}
```

## Controllers Remaining to Annotate

### Priority: HIGH - Core Controllers

1. **AssignmentController** (`src/main/java/com/reviewflow/controller/AssignmentController.java`)
   - Endpoints: ~15 (CRUD + submission listing + grading functions)
   - Key roles: ADMIN, INSTRUCTOR, STUDENT
   - Pattern: Complex nested resources (Course → Assignment → Submission)

2. **SubmissionController** (`src/main/java/com/reviewflow/controller/SubmissionController.java`)
   - Endpoints: ~12 (List, create, update, retrieve files)
   - Key roles: ADMIN, INSTRUCTOR, STUDENT
   - Files: Document S3 storage, file validation
   - Pattern: File upload/download operations

3. **TeamController** (`src/main/java/com/reviewflow/controller/TeamController.java`)
   - Endpoints: ~8 (CRUD teams, manage members)
   - Key roles: ADMIN, INSTRUCTOR, STUDENT
   - Pattern: Simple nested resources (Assignments → Teams)

4. **EvaluationController** (`src/main/java/com/reviewflow/controller/EvaluationController.java`)
   - Endpoints: ~10 (List, create evaluations, manage criteria)
   - Key roles: ADMIN, INSTRUCTOR
   - Pattern: Grading/evaluation workflows

### Priority: MEDIUM - Secondary Controllers

5. **NotificationController**
6. **AdminController**
7. **SystemAdminController**
8. **MonitoringController**
9. **GradeExportController**

### Priority: LOW - Utility Controllers

10. **S3StorageController** (if exists as separate)
11. **FilePreviewController** (if exists as separate)

## Common Pitfalls & Solutions

### 1. ❌ Class Name Ambiguity
```java
// WRONG - This conflicts with response DTO
import io.swagger.v3.oas.annotations.responses.ApiResponse;
@ApiResponse(responseCode = "200", ...)  // Ambiguous!

// RIGHT - Use fully qualified name
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", ...)
```

### 2. ❌ Missing Content Schema
```java
// WRONG - Swagger UI shows "unknown" type
@ApiResponse(responseCode = "200", description = "Success")

// RIGHT - Always specify what type is returned
@ApiResponse(
    responseCode = "200",
    description = "User updated",
    content = @Content(schema = @Schema(implementation = UserResponse.class))
)
```

### 3. ❌ Forgetting @Tag on Controller
```java
// WRONG - All endpoints appear ungrouped
@RestController
public class UserController { }

// RIGHT - All endpoints grouped in Swagger UI
@Tag(name = "Users", description = "User management...")
@RestController
public class UserController { }
```

### 4. ❌ Incomplete Status Codes
```java
// WRONG - Client doesn't know what errors can occur
@Operation(summary = "Update user")
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> update(...) { }

// RIGHT - Document all possible failures
@Operation(summary = "Update user", description = "...")
@ApiResponses({
    @ApiResponse(responseCode = "200", ...),  // Success
    @ApiResponse(responseCode = "400", ...),  // Validation
    @ApiResponse(responseCode = "403", ...),  // Permission
    @ApiResponse(responseCode = "404", ...)   // Not found
})
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> update(...) { }
```

### 5. ❌ Non-Descriptive Summaries
```java
// WRONG - Too vague
@Operation(summary = "Handle request")

// RIGHT - Specific and action-oriented  
@Operation(summary = "Update assignment title")
```

## Validation Checklist

Before considering an endpoint fully annotated:

- [ ] @Operation annotation present with descriptive summary
- [ ] @Operation description explains purpose, roles, and edge cases
- [ ] @ApiResponses documents all possible HTTP status codes
- [ ] All success responses (200, 201) specify `implementation = ResponseClass.class`
- [ ] All error responses use `ref = "#/components/schemas/ApiErrorResponse"`
- [ ] Error descriptions are specific (not just "Error occurred")
- [ ] @Tag applied to controller class (if not already done)
- [ ] No import of `io.swagger.v3.oas.annotations.responses.ApiResponse` (use fully qualified)
- [ ] Code compiles without errors
- [ ] At least one detailed example (@ApiResponse)

## Testing & Verification

### Compile Check
```bash
# From Backend directory
.\mvnw.cmd clean compile
```

### Swagger UI Verification
Once backend runs, visit: `http://localhost:8080/swagger-ui.html`

- [ ] All controller endpoints visible
- [ ] Grouping by @Tag correct
- [ ] Required response status codes shown
- [ ] Response schemas correct
- [ ] Descriptions are readable and helpful
- [ ] Error responses consistent

## Examples for Copy-Paste

### Template: Standard GET by ID
```java
@Operation(
    summary = "[Verb] [resource]",
    description = "Retrieve [resource] by ID with full details. Requires appropriate access permissions."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "[Resource] retrieved successfully",
        content = @Content(schema = @Schema(implementation = ResourceResponse.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Unauthorized",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "[Resource] not found",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ResourceResponse>> get(@PathVariable String id) {
```

### Template: Standard POST Create
```java
@Operation(
    summary = "Create [resource]",
    description = "Create new [resource] with provided details. Requires ADMIN role."
)
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "[Resource] created successfully",
        content = @Content(schema = @Schema(implementation = ResourceResponse.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Bad Request - invalid data or duplicate [identifier]",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Forbidden - ADMIN role required",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
    )
})
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ApiResponse<ResourceResponse>> create(
        @Valid @RequestBody CreateResourceRequest request) {
```

## References

- **OpenAPI 3.0 Spec**: https://spec.openapis.org/oas/v3.0.3
- **SpringDoc OpenAPI Docs**: https://springdoc.org/
- **ReviewFlow Swagger UI** (when running): `http://localhost:8080/swagger-ui.html`
- **API Error Response DTO**: `src/main/java/com/reviewflow/model/dto/response/ApiErrorResponse.java`

---

**Phase 4 Completion Goal**: 
- ✅ AuthController (5 endpoints, fully documented)
- ✅ CourseController (11 endpoints, fully documented)
- 📋 Remaining: 8+ controllers with ~90+ endpoints total

Use this guide to maintain consistency and completeness across all controllers.
