# ReviewFlow — Module 9: S3 Storage

> Controller: `S3StorageController.java`  
> Service: `S3Service.java`  
> Base path: `/api/v1/files` and `/api/v1/courses` and `/api/v1/system`

> **Overview:** Centralised S3-based file storage for avatars (PRD-02), submission files (via presigned URL), evaluation PDFs, and inline file preview (PRD-07). All file access is stateless via presigned S3 URLs with 15-minute TTL. No backend proxy or streaming of file content.

> **Related PRDs:** [PRD-02 Profile Pictures](../Features/PRD_02_profile_pictures.md) | [PRD-07 File Preview](../Features/PRD_07_file_preview.md) | [PRD-S3 Storage](../Features/PRD_S3_storage.md)  
> **Related Decisions:** [Decision 13: Presigned URLs](../DECISIONS.md#13-s3-presigned-urls-for-direct-client-access)

---

## 9.1 Module Overview

The S3 Storage module provides a standardised, secure interface for all file operations in ReviewFlow. It replaces inconsistent file handling with a single strategy:

1. **Avatars** — User profile pictures (max 5MB, JPG/PNG/GIF)
2. **Submission Files** — Student-uploaded project files (max 50MB, validated MIME types)
3. **Evaluation PDFs** — Instructor-generated grading reports (max 50MB)
4. **File Preview** — Inline rendering of PDFs and images via presigned URLs

### Design Philosophy

- **No backend proxy:** Files are served directly from S3 via presigned URLs. Backend generates the URL, client requests S3 directly.
- **Stateless:** Presigned URLs require no session tracking or state persistence.
- **Secure by default:** 15-minute expiry, role-based access control, MIME validation on upload.
- **Future-proof:** Upgrade path to CloudFront with zero code changes (see PRD-07).

### Key Constraints

| Constraint                  | Value                      | Rationale                                                                   |
| --------------------------- | -------------------------- | --------------------------------------------------------------------------- |
| Presigned URL TTL           | 15 minutes                 | Balances security risk window with user experience (study session duration) |
| Avatar max size             | 5MB                        | Reasonable for profile images; avoids avatar download fatigue               |
| File upload limit           | 50MB                       | Submission files stay within database backup and restore limits             |
| Preview MIME whitelist      | PDF, JPEG, PNG, WebP, text | Browser-native rendering only; no external viewers                          |
| Concurrent uploads per team | 1                          | Atomic versioning requires upload serialisation                             |

---

## 9.2 Authentication & Authorization

### Role-Based Access Control

| Action                         | STUDENT       | INSTRUCTOR    | ADMIN | SYSTEM_ADMIN |
| ------------------------------ | ------------- | ------------- | ----- | ------------ |
| Upload avatar (own)            | ✓             | ✓             | ✓     | ✓            |
| Delete avatar (own)            | ✓             | ✓             | ✓     | ✓            |
| Delete any user's avatar       | ✗             | ✗             | ✓     | ✓            |
| Download submission (own team) | ✓             | ✓ (if course) | ✓     | ✓            |
| Download submission (any team) | ✗             | ✓ (if course) | ✓     | ✓            |
| Preview submission (own team)  | ✓             | ✓ (if course) | ✓     | ✓            |
| Preview submission (any team)  | ✗             | ✓ (if course) | ✓     | ✓            |
| Download evaluation PDF        | ✓ (published) | ✓             | ✓     | ✓            |
| Configure S3 (system settings) | ✗             | ✗             | ✗     | ✓            |

### Access Control Rules

- **Avatar download:** Public via CDN (future) or presigned URL (current). No authentication required for valid presigned URL.
- **Submission access:** Restricted by course membership and team membership (see [PRD-05: Submissions](../Features/PRD_05_submission_type.md)).
- **Evaluation PDF access:** Restricted by student role (only published PDFs) or instructor/admin for any PDF.
- **S3 config:** SYSTEM_ADMIN only. Returns `403 FORBIDDEN` for all other roles.

---

## 9.3 API Endpoints

### 9.3.1 POST /courses/{courseId}/users/profile-picture

Upload or replace user's avatar image.

#### Must Have

- [ ] Accepts `Content-Type: multipart/form-data`
- [ ] Form field: `file` (required, image file only)
- [ ] Requires authentication and valid `courseId` (for role verification)
- [ ] Validates file extension — `.jpg`, `.jpeg`, `.png`, `.gif` only
- [ ] Validates MIME type by reading file bytes (Apache Tika) — NOT `Content-Type` header
- [ ] Validates file size ≤ 5MB
- [ ] Strips EXIF metadata (GPS, device info, timestamps) before upload via `EXIFStripperService`
- [ ] Uploads to S3 key: `avatars/{hashedUserId}/avatar.{ext}`
- [ ] Updates `users.avatar_url` in database
- [ ] Avatar URL includes cache-bust query param: `?v={System.currentTimeMillis()}`
- [ ] Logs `AVATAR_UPLOADED` to `audit_log`

#### Request

```
POST /api/v1/courses/abc123/users/profile-picture HTTP/1.1
Content-Type: multipart/form-data; boundary=---WebKitBoundary

-----WebKitBoundary
Content-Disposition: form-data; name="file"; filename="photo.jpg"
Content-Type: image/jpeg

[binary image data]
-----WebKitBoundary--
```

#### Response Success

```json
{
  "success": true,
  "data": {
    "userId": "user_5",
    "avatarUrl": "https://reviewflow-storage.s3.ca-central-1.amazonaws.com/avatars/k3N9mQ2p/avatar.jpg?v=1709255486000",
    "uploadedAt": "2026-03-30T14:31:26.000Z"
  },
  "timestamp": "2026-03-30T14:31:26.000Z"
}
```

#### Response Errors

- `400 Bad Request` — file missing → `{ code: "FILE_MISSING", message: "File required in form field 'file'" }`
- `400 Bad Request` — invalid extension → `{ code: "AVATAR_INVALID_TYPE", message: "Avatar must be JPG, PNG, or GIF. Provided: .webp" }`
- `400 Bad Request` — MIME type mismatch → `{ code: "AVATAR_INVALID_TYPE", message: "File content does not match extension" }`
- `400 Bad Request` — file too large → `{ code: "AVATAR_TOO_LARGE", message: "Avatar size 6MB exceeds maximum of 5MB" }`
- `401 Unauthorized` — no valid session
- `404 Not Found` — course doesn't exist
- `500 Internal Server Error` — S3 upload failed → `{ code: "AVATAR_UPLOAD_FAILED", message: "Error uploading to S3" }`

#### Edge Cases

- [ ] Upload valid 5MB JPG → `200`, cached URL with `?v=` param
- [ ] Upload 5.1MB JPG → `400 AVATAR_TOO_LARGE`
- [ ] Upload `.png` file with JPG extension → `400 AVATAR_INVALID_TYPE`
- [ ] Upload same file twice → second replaces first (overwrites S3 key)
- [ ] S3 upload fails (network error) → `500 AVATAR_UPLOAD_FAILED`
- [ ] EXIF data in JPG → stripped before upload
- [ ] Non-existent `courseId` → `404`

---

### 9.3.2 GET /files/{submissionId}/{fileName}

Download a submission file (with `Content-Disposition: attachment`).

#### Must Have

- [ ] Same access rules as [Module 5: GET /submissions/{id}/download](../controller_specs/05_Module_Submissions.md#53-get-submissionsiddownload)
- [ ] Generates presigned S3 URL with 15-minute expiry
- [ ] Sets `Content-Disposition: attachment; filename="{originalFileName}"`
- [ ] Returns URL and metadata WITHOUT streaming file content
- [ ] Logs `FILE_DOWNLOADED` to `audit_log` (by submissionId, userId, timestamp)

#### Request

```
GET /api/v1/files/sub_abc123/Project.zip HTTP/1.1
Authorization: Bearer [access token]
```

#### Response Success (Presigned URL Download)

```json
{
  "success": true,
  "data": {
    "submissionId": "sub_abc123",
    "fileName": "Project.zip",
    "fileSizeBytes": 25600000,
    "contentType": "application/zip",
    "presignedUrl": "https://reviewflow-storage.s3.ca-central-1.amazonaws.com/submissions/k3N9mQ2p/team_xyz/v1/Project.zip?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...(truncated)",
    "expiresIn": 900,
    "expiresAt": "2026-03-30T14:46:26.000Z"
  },
  "timestamp": "2026-03-30T14:31:26.000Z"
}
```

#### Response Errors

- `401 Unauthorized` — no valid session
- `403 Forbidden` — student not in team, instructor not in course
- `404 Not Found` — submission doesn't exist
- `404 Not Found` — file not found in storage → `{ code: "FILE_NOT_FOUND_IN_STORAGE" }`
- `500 Internal Server Error` — presigned URL generation failed

#### Edge Cases

- [ ] Submission by own team → presigned URL returned
- [ ] Submission by different team (student) → `403`
- [ ] Submission by own course (instructor) → presigned URL returned
- [ ] Presigned URL accessed within 15 minutes → S3 serves file
- [ ] Presigned URL accessed after 15 minutes → S3 returns `403` (expired)
- [ ] Concurrent requests for same file → each gets independent presigned URL

---

### 9.3.3 GET /files/{submissionId}/{fileName}/preview

Generate inline preview URL for submission file (PDF or image only).

#### Must Have

- [ ] Same access rules as `GET /files/{submissionId}/{fileName}`
- [ ] Validates MIME type is preview-supported (PDF, JPEG, PNG, WebP, plain text)
- [ ] Generates presigned S3 URL with `Content-Disposition: inline`
- [ ] Returns `409 PREVIEW_NOT_SUPPORTED` for non-previewable types (ZIP, DOCX, etc.)
- [ ] Returns `409 PREVIEW_NOT_SUPPORTED` for files > 50MB
- [ ] TTL is 15 minutes (same as download)
- [ ] Logs `FILE_PREVIEW` to `audit_log`

#### Request

```
GET /api/v1/files/sub_abc123/Report.pdf/preview HTTP/1.1
Authorization: Bearer [access token]
```

#### Response Success (Previewable)

```json
{
  "success": true,
  "data": {
    "submissionId": "sub_abc123",
    "fileName": "Report.pdf",
    "mimeType": "application/pdf",
    "fileSizeBytes": 2500000,
    "previewUrl": "https://reviewflow-storage.s3.ca-central-1.amazonaws.com/submissions/k3N9mQ2p/team_xyz/v1/Report.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&...(truncated)&x-amz-meta-disposition=inline",
    "expiresIn": 900,
    "expiresAt": "2026-03-30T14:46:26.000Z"
  },
  "timestamp": "2026-03-30T14:31:26.000Z"
}
```

#### Response Errors

- `401 Unauthorized` — no valid session
- `403 Forbidden` — access denied (student not in team, etc.)
- `404 Not Found` — submission or file doesn't exist
- `409 PREVIEW_NOT_SUPPORTED` — MIME type not previewable (ZIP, DOCX) with fallback message: `"Preview not supported for application/zip. Use download endpoint instead."`
- `409 PREVIEW_NOT_SUPPORTED` — file size > 50MB

#### Supported Preview MIME Types

```
application/pdf
image/jpeg
image/png
image/webp
text/plain
text/html
text/css
text/xml
```

#### Edge Cases

- [ ] Request preview of PDF → URL with `inline` disposition
- [ ] Request preview of ZIP → `409 PREVIEW_NOT_SUPPORTED` (not in whitelist)
- [ ] Request preview of 55MB PDF → `409 PREVIEW_NOT_SUPPORTED` (exceeds 50MB)
- [ ] Request preview of 5MB image → presigned URL returned
- [ ] Browser fetches presigned URL with `inline` disposition → PDF viewer/image viewer loads
- [ ] Preview URL expires after 15 minutes → S3 returns `403` (expired)

---

### 9.3.4 POST /system/s3-config

Configure S3 bucket settings (region, bucket name, lifecycle rules). SYSTEM_ADMIN only.

#### Must Have

- [ ] SYSTEM_ADMIN role required — returns `403 FORBIDDEN` for all other roles
- [ ] Optional fields: `region`, `bucketName`, `enableVersioning`, `lifecycleRuleDays`
- [ ] Updates `.env` file (or config service) with new values
- [ ] Validates AWS credentials by attempting HEAD request to bucket
- [ ] Does NOT modify or recreate bucket — only connection parameters
- [ ] Returns current S3 configuration (bucket name, region, presigned URL TTL)
- [ ] Logs `S3_CONFIG_UPDATED` to `audit_log` with user ID and old/new values
- [ ] Requires restart to fully take effect (connection pool refresh)

#### Request

```json
POST /api/v1/system/s3-config HTTP/1.1
Authorization: Bearer [access token]
Content-Type: application/json

{
  "region": "ca-central-1",
  "bucketName": "reviewflow-storage-prod",
  "presignedUrlExpiryMinutes": 15,
  "enableVersioning": false
}
```

#### Response Success

```json
{
  "success": true,
  "data": {
    "region": "ca-central-1",
    "bucketName": "reviewflow-storage",
    "presignedUrlExpiryMinutes": 15,
    "enableVersioning": false,
    "sseEncryption": "AES256",
    "blockPublicAccess": true,
    "accessVerified": true,
    "lastVerifiedAt": "2026-03-30T14:31:26.000Z"
  },
  "timestamp": "2026-03-30T14:31:26.000Z"
}
```

#### Response Errors

- `400 Bad Request` — invalid region code → `{ code: "INVALID_AWS_REGION" }`
- `400 Bad Request` — presigned URL expiry > 60 min or < 1 min
- `401 Unauthorized` — no valid session
- `403 Forbidden` — not SYSTEM_ADMIN role
- `503 Service Unavailable` — cannot verify AWS credentials → S3 not reachable
- `500 Internal Server Error` — config update failed

#### Edge Cases

- [ ] Update only region → other fields unchanged
- [ ] Verify credentials fail (bucket doesn't exist) → `503`
- [ ] Update presigned URL expiry to 30 minutes → new URLs generated with new TTL
- [ ] Multi-region upgrade (future): changes region → presigned URLs now point to new bucket
- [ ] Request from ADMIN (not SYSTEM_ADMIN) → `403`

---

## 9.4 Error Codes

### File Validation Errors

| Code                    | Status | Message                                   |
| ----------------------- | ------ | ----------------------------------------- |
| `FILE_MISSING`          | 400    | File required in form field               |
| `AVATAR_INVALID_TYPE`   | 400    | Avatar must be JPG, PNG, or GIF           |
| `AVATAR_TOO_LARGE`      | 400    | Avatar size exceeds maximum of 5MB        |
| `FILE_TOO_LARGE`        | 400    | File size exceeds maximum of 50MB         |
| `INVALID_FILE_TYPE`     | 400    | File type not allowed (from GLOBAL_RULES) |
| `INVALID_MIME_TYPE`     | 400    | File content does not match extension     |
| `PREVIEW_NOT_SUPPORTED` | 409    | Preview not supported for this file type  |

### S3 & Storage Errors

| Code                              | Status | Message                         |
| --------------------------------- | ------ | ------------------------------- |
| `FILE_NOT_FOUND_IN_STORAGE`       | 404    | File exists in DB but not in S3 |
| `AVATAR_UPLOAD_FAILED`            | 500    | Error uploading avatar to S3    |
| `PRESIGNED_URL_GENERATION_FAILED` | 500    | Cannot generate presigned URL   |
| `INVALID_AWS_REGION`              | 400    | Invalid AWS region code         |

### Access Control Errors

| Code            | Status | Message                                                  |
| --------------- | ------ | -------------------------------------------------------- |
| `ACCESS_DENIED` | 403    | User does not have access to this file                   |
| `FORBIDDEN`     | 403    | Insufficient role for this operation (from GLOBAL_RULES) |

---

## 9.5 Implementation Notes

### S3 Key Naming Convention

All S3 keys use hashed IDs (never raw database integers):

```
avatars/{hashedUserId}/avatar.{ext}
submissions/{hashedAssignmentId}/{hashedTeamOrStudentId}/v{n}/{originalFilename}
pdfs/{hashedEvaluationId}/report.pdf
test/connection-check.txt  # Used only for S3ConnectionTest
```

**Rules:**

- Always use hashed IDs from `HashidService`
- Version number `v{n}` is the submission version from database
- Original filenames sanitised — alphanumeric, hyphens, dots, underscores only, max 100 chars
- All keys lowercase

**S3KeyBuilder utility:**

```java
public class S3KeyBuilder {

    public static String avatarKey(String hashedUserId, String ext) {
        return String.format("avatars/%s/avatar.%s", hashedUserId, ext.toLowerCase());
    }

    public static String submissionKey(String hashedAssignmentId,
                                       String hashedTeamOrStudentId,
                                       int version,
                                       String originalFilename) {
        String sanitised = sanitiseFilename(originalFilename);
        return String.format("submissions/%s/%s/v%d/%s",
            hashedAssignmentId, hashedTeamOrStudentId, version, sanitised);
    }

    public static String pdfKey(String hashedEvaluationId) {
        return String.format("pdfs/%s/report.pdf", hashedEvaluationId);
    }

    private static String sanitiseFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_")
                       .toLowerCase()
                       .substring(0, Math.min(filename.length(), 100));
    }
}
```

### Presigned URL Strategy

**Generate presigned URLs, not streamed files.**

```java
@Service
public class S3Service {

    private S3Presigner s3Presigner;

    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 15;

    // Download presigned URL (attachment disposal)
    public PresignedGetObjectResponse generatePresignedDownloadUrl(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("reviewflow-storage")
                .key(s3Key)
                .responseContentDisposition("attachment")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(request)
                .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRY_MINUTES))
                .build();

        return s3Presigner.presignGetObject(presignRequest);
    }

    // Preview presigned URL (inline disposal)
    public PresignedGetObjectResponse generatePresignedPreviewUrl(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("reviewflow-storage")
                .key(s3Key)
                .responseContentDisposition("inline")  // Browser renders, doesn't download
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(request)
                .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRY_MINUTES))
                .build();

        return s3Presigner.presignGetObject(presignRequest);
    }

    // Upload file to S3
    public void putObject(String s3Key, byte[] fileBytes, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket("reviewflow-storage")
                .key(s3Key)
                .contentType(contentType)
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(fileBytes));
    }
}
```

### Avatar Upload Flow

1. **Receive multipart form:** `file` field required
2. **Validate extension & MIME:** JPG, PNG, GIF only
3. **Validate size:** ≤ 5MB
4. **Strip EXIF metadata** via `EXIFStripperService`
5. **Upload to S3:** Key = `avatars/{hashedUserId}/avatar.{ext}`
6. **Update database:** `users.avatar_url` with cache-bust param `?v={timestamp}`
7. **Log audit event:** `AVATAR_UPLOADED`

### File Preview Whitelist

**Inline rendering supported for:**

```
application/pdf          → Browser PDF viewer
image/jpeg              → <img> tag
image/png               → <img> tag
image/webp              → <img> tag
text/plain              → <pre> or <code> tag
text/html               → Browser render (CORS, CSP apply)
text/css                → <pre> tag
text/xml                → Browser XML viewer
```

**All other types:** Return `409 PREVIEW_NOT_SUPPORTED` with redirect to download endpoint.

**Size limit for preview:** 50MB maximum. Larger files return `409 PREVIEW_NOT_SUPPORTED`.

### Error Handling

All S3 client exceptions must be caught and translated to domain errors:

```java
try {
    s3Client.putObject(request, requestBody);
} catch (S3Exception e) {
    if (e.statusCode() == 403) {
        throw new S3AccessDeniedException("Insufficient S3 permissions");
    } else if (e.statusCode() == 404) {
        throw new S3BucketNotFoundException("Bucket not found: " + bucket);
    } else {
        throw new S3UploadException("S3 upload failed: " + e.getMessage());
    }
}
```

Never expose raw AWS SDK exceptions to API clients.

### Multi-Region Future Upgrade Path

When expanding to multiple regions:

1. Add `.env` variable: `AWS_S3_BUCKET_REGION`
2. Support region selection in `POST /system/s3-config`
3. Bucket name can vary by region: `reviewflow-storage-{region}`
4. S3Key generation remains unchanged — region is S3Client configuration only
5. Presigned URLs automatically point to correct region

**No controller or service method changes required** — only `S3Service` bean configuration.

---

## 9.6 Security Considerations

### Access Control

- **Presigned URLs are time-bound:** 15-minute expiry prevents indefinite sharing
- **Access logged:** Download and preview requests logged to `audit_log` for accountability
- **Role enforcement:** Backend verifies access before generating presigned URL — client cannot bypass role checks
- **File immutability:** Once stored, files cannot be modified via API — only deleted (future feature)

### Data Protection

- **Encryption at rest:** S3 bucket configured with SSE-S3 (AES-256)
- **Encryption in transit:** HTTPS enforced for all presigned URL connections
- **EXIF stripping:** Avatar metadata removed before storage to prevent location/device leaks
- **MIME validation:** File extension spoofing detected via Apache Tika byte inspection

### Attack Surface Mitigation

| Attack                    | Mitigation                                                                     |
| ------------------------- | ------------------------------------------------------------------------------ |
| Enumeration of S3 keys    | Hashed IDs in URLs; no sequential integers exposed                             |
| Presigned URL brute-force | AWS SigV4 signatures are cryptographically secure; 15-min expiry limits window |
| Avatar enumeration        | Not possible — avatar URLs are only returned to authenticated users            |
| MIME type spoofing        | Apache Tika validates file content matches extension                           |
| EXIF metadata leakage     | EXIFStripperService removes all metadata before S3 upload                      |
| File overwrite attacks    | Versioning prevents accidental overwrites; explicit version numbers required   |

---

## 9.7 Performance

### Presigned URL Caching

**Client-side caching strategy** (recommended):

- **Cache TTL:** 1–5 minutes (shorter than 15-min S3 expiry)
- **Rationale:** Presigned URL generation is fast (~5–10ms), but caching reduces API roundtrips
- **Invalidation:** Clear cache on logout, session rotation, or permission change

**Backend generation cost:**

- S3Presigner is stateless and fast — no database queries required
- Per URL: ~5–10ms CPU time including Sig4 computation
- Concurrent requests for same file each get independent URL (OK — S3 validates on access)

### Batch Operations

**Upload strategy for large submissions:**

1. Frontend splits file into 5–50MB chunks
2. Each chunk uploaded via separate presigned URL (request for each chunk)
3. S3 multipart upload assembles on backend (future: S3 Accelerate for speed)
4. Database records single submission with total size

**Not yet implemented** — single file upload sufficient for current scale.

### Network Bandwidth

- **Backend:** Zero file streaming — presigned URLs eliminate backend proxy overhead
- **User bandwidth:** Unaffected — same S3 HTTPS download speed as backend proxy
- **S3 data transfer:** Billed per GB; presigned URLs don't reduce costs (direct S3 access still charged)

---

## 9.8 Related PRDs

- **[PRD-02: Profile Pictures](../Features/PRD_02_profile_pictures.md)** — Avatar upload, storage, EXIF stripping
- **[PRD-07: File Preview](../Features/PRD_07_file_preview.md)** — Inline preview for PDFs and images; upgrade path to CloudFront signed URLs
- **[PRD-S3: S3 Storage](../Features/PRD_S3_storage.md)** — Complete S3 configuration, key naming, bucket setup

---

## 9.9 Related Decisions

- **[Decision 13: S3 Presigned URLs](../DECISIONS.md#13-s3-presigned-urls-for-direct-client-access)** — Why presigned URLs instead of backend proxy; upgrade path to CloudFront without code changes
- **[Decision 4: Hashids over UUIDs](../DECISIONS.md#4-hashids-over-uuids-for-external-ids)** — Why S3 keys use hashed IDs (enumeration prevention)

---

## 9.10 Testing Checklist

### S3 Connection & Configuration

- [ ] `S3ConnectionTest` passes — bucket accessible, permissions verified
- [ ] Credentials in `.env` are valid (updated by operations team on deployment)
- [ ] Region configured correctly in `application.properties`
- [ ] Block public access verified in AWS console

### Avatar Upload

- [ ] Upload valid JPG (5MB) → `200` with presigned URL
- [ ] Upload SPG (spoofed file) → `400 INVALID_MIME_TYPE`
- [ ] Upload 6MB PNG → `400 AVATAR_TOO_LARGE`
- [ ] Original EXIF data stripped from stored file (verify via S3 download + metadata inspection)
- [ ] Avatar URL includes cache-bust `?v=` parameter
- [ ] Second upload replaces first (S3 key overwrite)

### Download Endpoint

- [ ] Presigned URL expires after 15 minutes (verify by timing test)
- [ ] Download fails with `403` after expiry
- [ ] Concurrent requests return different presigned URLs (both valid)
- [ ] Student can download own team submission
- [ ] Student cannot download another team's submission → `403`
- [ ] Instructor can download submission from own course
- [ ] Instructor cannot download submission from another course → `403`

### Preview Endpoint

- [ ] PDF preview returns presigned URL with `inline` disposition
- [ ] Image preview returns presigned URL with `inline` disposition
- [ ] ZIP file preview returns `409 PREVIEW_NOT_SUPPORTED`
- [ ] File > 50MB returns `409 PREVIEW_NOT_SUPPORTED`
- [ ] Browser loads PDF inline (no download dialog)
- [ ] Same access rules as download endpoint

### S3 Config Endpoint

- [ ] SYSTEM_ADMIN can view config → `200`
- [ ] SYSTEM_ADMIN can update region → `200` (restart required to take effect)
- [ ] ADMIN tries to access → `403 FORBIDDEN`
- [ ] Invalid region code → `400 INVALID_AWS_REGION`
- [ ] Bucket unreachable → `503 SERVICE_UNAVAILABLE`

### Error Handling

- [ ] All S3 exceptions translated to domain errors (no raw AWS SDK errors)
- [ ] File not found in storage (DB record exists but S3 missing) → `404 FILE_NOT_FOUND_IN_STORAGE`
- [ ] S3 upload fails → `500 AVATAR_UPLOAD_FAILED` (not `500 Generic Internal Server Error`)

### Audit Logging

- [ ] Avatar upload logged as `AVATAR_UPLOADED` with user, timestamp, S3 key
- [ ] File download logged as `FILE_DOWNLOADED` with user, submission ID, IP
- [ ] File preview logged as `FILE_PREVIEW` with user, submission ID, file type
- [ ] S3 config update logged as `S3_CONFIG_UPDATED` with old/new values
