# API

REST surface implemented under `backend/src/main/java/dev/esgenius/controller/**`. Base path prefix is `/api/v1` unless noted. JSON error bodies from `GlobalExceptionHandler` and `SupabaseAuthFilter` share the shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "timestamp": "<ISO-8601 instant>"
}
```

## Authentication

| Behaviour | Detail |
| --- | --- |
| Filter | `SupabaseAuthFilter` on `/api/*` (`SupabaseAuthConfig`, profile `!test`) |
| Required header | `Authorization: Bearer <supabase_access_token>` when Supabase is configured |
| Skipped | `OPTIONS` (CORS preflight); `GET /api/v1/health` |
| Unconfigured + `app.auth.supabase.required=true` (default) | All filtered `/api/**` requests → **401** `"Authentication is not configured"` |
| Unconfigured + `AUTH_REQUIRED=false` | Filter no-ops (local-dev opt-out); no identity attribute |
| Missing/invalid Bearer | **401** `"Authentication required"` / `"Invalid or expired access token"` / `"Unable to validate access token"` |
| Success | Sets request attribute `authenticatedUser` (`userId`, optional `email`); successful validations cached by SHA-256 digest of the token for `app.auth.supabase.token-cache-ttl` (`SUPABASE_TOKEN_CACHE_TTL`, default 60s). Failures are not cached. |

**Document access:** controllers that touch uploaded documents resolve a `Caller` via `DocumentAccessPolicy` from `authenticatedUser`. `ADMIN_USER_IDS` (`app.auth.admin-user-ids`) marks admins who can view/modify every document. Documents with `owner_user_id = NULL` are **shared samples** (viewable by any signed-in user; modify only by owner or admin). Cross-tenant ids return **404** (not **403**) when the caller cannot view. Attempts to delete or start analysis on a shared document without rights return **403** with a fixed message (see below). With `AUTH_REQUIRED=false` and Supabase unconfigured, the policy treats the caller as an unidentified admin (local dev).

**Usage limits:** in-memory rolling windows per JVM (`UsageLimiter`). Uploads, analysis starts, and assistant asks consume budget for identified non-admin users. Exceeded limits → **429** with `Retry-After` (seconds). Admins skip per-user limits but still count toward the global analyses-per-day cap.

## Global error statuses

| Status | Source | When |
| --- | --- | --- |
| **400** | `BadRequestException` and mapped servlet errors | Business validation failures; missing required query/form params; type mismatches; invalid `documentType` / `reportingYear`; non-PDF uploads; multipart > 25 MB; PDF page/character limits (`PdfExtractionLimitException` messages); scanned/empty PDF text |
| **401** | `SupabaseAuthFilter` | Auth failures above (not via `GlobalExceptionHandler`) |
| **403** | `DocumentAccessDeniedExceptionHandler` | Shared (or non-owned) document write: delete or `POST …/analyses` without modify rights — message *"Shared documents are view-only. Only the owner or an admin can delete or start an analysis."* |
| **404** | `ResourceNotFoundException` | Missing or inaccessible document, analysis, framework, organization, company, etc. |
| **429** | `UsageLimitExceededExceptionHandler` | Upload, analysis start, or assistant ask over rolling limit; JSON body + `Retry-After` header (seconds until the oldest event in the window expires) |
| **502** | `AssistantUnavailableException` (default status) | Assistant provider failure after evidence was found (non-quota) |
| **503** | `AssistantUnavailableException` (`SERVICE_UNAVAILABLE`) | Assistant daily Gemini quota exhausted — *"The AI service has reached its usage limit. Please try again later."* |
| **500** | Uncaught `Exception` | Unexpected server errors |

---

## HealthController

### `GET /api/v1/health`

| | |
| --- | --- |
| Auth | Skipped by filter |
| Params | None |
| Body | None |
| Response **200** | `HealthResponse`: `status` (e.g. `"UP"`), `service` (`"ESGenius Backend"`) |

---

## MeController — `/api/v1`

### `GET /api/v1/me`

| | |
| --- | --- |
| Auth | Bearer when Supabase is configured |
| Params | None |
| Response **200** | `MeResponse`: `email` (from token, or `null`), `admin` (`true` when user id is in `ADMIN_USER_IDS`), `limits` (`uploadsPerDay`, `analysesPerDay`, `assistantAsksPerHour` — each `Limit` with `limit`, `used`, `resetsInSeconds`, or `null` when that window is unlimited / not applicable) |
| Notes | Read-only snapshot; does not consume usage. Admins and unidentified local-dev callers get `null` limit objects (unlimited per-user windows). Does not expose the global analyses-per-day cap. |

---

## FrameworkController — `/api/v1/frameworks`

### `GET /api/v1/frameworks`

| | |
| --- | --- |
| Params | None |
| Response **200** | `FrameworkResponse[]`: `id`, `code`, `name`, `fullName`, `region`, `version`, `status`, `requirementCount` |

### `GET /api/v1/frameworks/{frameworkId}`

| | |
| --- | --- |
| Path | `frameworkId` (required, `Long`) |
| Response **200** | `FrameworkResponse` |
| Errors | **404** framework not found |

### `GET /api/v1/frameworks/{frameworkId}/requirements`

| | |
| --- | --- |
| Path | `frameworkId` (required) |
| Query | `category` — optional `EsgCategory` enum (`ENVIRONMENTAL` / `SOCIAL` / `GOVERNANCE`) |
| Response **200** | `FrameworkRequirementResponse[]`: `id`, `requirementCode`, `title`, `category`, `description`, `frameworkText`, `mandatory`, `version` |
| Errors | **404** framework not found; **400** invalid `category` value |

---

## DocumentController — `/api/v1/documents`

### `POST /api/v1/documents`

| | |
| --- | --- |
| Content-Type | `multipart/form-data` |
| Parts / params | `file` (required PDF); `organizationId` (required `Long`); `documentType` (required string → `BRSR`, `SUSTAINABILITY_REPORT`, `ANNUAL_REPORT`, `POLICY`, `OTHER`); `reportingYear` (optional `Integer`) |
| Response **201** | `DocumentDetailResponse`: `id`, `organizationId`, `organizationName`, `originalFilename`, `documentType`, `reportingYear`, `status`, `fileSize`, `pageCount`, `uploadedAt`, `processedAt`, `extractedText`, `failureReason`, `shared`, `canModify` |
| Notes | The response returns immediately with `status` **`PROCESSING`**; PDFBox extraction then runs in a background worker and the document becomes `READY` or `FAILED` (poll `GET /api/v1/documents/{id}`). Analysis start and assistant questions return **400** until the document is `READY`. Documents interrupted by a restart become `FAILED`. A full processing queue fails the document with a "server is busy" reason. Sets `owner_user_id` to the authenticated user when present. Counts against per-user upload limit. |
| Errors | **400** validation / non-PDF / too large / page or extracted-character limits / scanned-or-empty text / store failure; **404** organization not found; **429** upload limit; missing params → **400** |

### `GET /api/v1/documents`

| | |
| --- | --- |
| Query | `organizationId` (required) |
| Response **200** | `DocumentSummaryResponse[]`: same as detail except no `extractedText` / `failureReason`; includes `shared`, `canModify` |
| Notes | Non-admins see only documents they own plus shared samples (`owner_user_id IS NULL`). Admins see all documents for the org. |
| Errors | **404** organization; **400** missing/invalid `organizationId` |

### `GET /api/v1/documents/{documentId}`

| | |
| --- | --- |
| Path | `documentId` (required) |
| Response **200** | `DocumentDetailResponse` |
| Errors | **404** |

### `GET /api/v1/documents/{documentId}/pages`

| | |
| --- | --- |
| Path | `documentId` |
| Response **200** | `DocumentPageResponse[]`: `pageNumber`, `text` |
| Errors | **404** |

### `DELETE /api/v1/documents/{documentId}`

| | |
| --- | --- |
| Path | `documentId` |
| Response **204** | Empty body |
| Errors | **404** not found or not visible; **403** shared / non-owned modify |

---

## ComplianceAnalysisController — `/api/v1`

### `GET /api/v1/documents/{documentId}/analyses`

| | |
| --- | --- |
| Path | `documentId` |
| Response **200** | `ComplianceAnalysisSummaryResponse[]`: `id`, `documentId`, `frameworkId`, `frameworkCode`, `frameworkName`, `status`, `startedAt`, `completedAt`, `failureReason`, `requirementCount`, `coveredCount`, `partiallyCoveredCount`, `notCoveredCount`, `humanReviewRequiredCount`, `evidenceRetrievedCount`, `noEvidenceFoundCount` |
| Errors | **404** document not found or not visible |

### `POST /api/v1/documents/{documentId}/analyses`

| | |
| --- | --- |
| Path | `documentId` |
| Body | `StartAnalysisRequest`: `frameworkId` (optional `Long`), `frameworkCode` (optional `String`) — **one of the two required** |
| Response **202** | `ComplianceAnalysisResponse`: `id`, `documentId`, `frameworkId`, `frameworkCode`, `frameworkName`, `status`, `startedAt`, `completedAt`, `failureReason`, `requirementCount`, `assessments` (`RequirementAssessmentResponse[]`) |
| Assessment fields | `requirementId`, `requirementCode`, `requirementTitle`, `category`, `assessmentStatus`, `confidence`, `explanation`, `gap`, `recommendation`, `retrievalScore`, `evidenceText`, `evidenceChunks` (`chunkIndex`, `pageNumber`, `text`, `retrievalScore`) |
| Errors | **404** document/framework or document not visible; **403** shared / non-owned modify; **400** document not `READY`, blank extracted text, empty framework requirements, analysis already in progress, missing framework selector; **429** per-user or global analysis limit (in-progress row is rolled back on **429**) |

### `GET /api/v1/analyses/{analysisId}`

| | |
| --- | --- |
| Path | `analysisId` |
| Response **200** | `ComplianceAnalysisResponse` (full assessments) |
| Errors | **404** analysis or parent document not visible |

---

## ReportController — `/api/v1`

### `GET /api/v1/analyses/{analysisId}/report.pdf`

| | |
| --- | --- |
| Path | `analysisId` |
| Response **200** | Raw PDF bytes; `Content-Type: application/pdf`; `Content-Disposition: attachment; filename="gap-assessment-{id}.pdf"` |
| Notes | On success, records a `report_export` row (failures to record are logged only; PDF still returned). |
| Errors | **404** if analysis or its document is missing / not visible |

---

## ReportExportController — `/api/v1/reports`

### `GET /api/v1/reports`

| | |
| --- | --- |
| Auth | Bearer when configured |
| Params | None |
| Response **200** | `ReportExportResponse[]` (up to 50, newest first): `id`, `analysisId`, `documentId`, `documentName`, `frameworkCode`, `format`, `sizeBytes`, `generatedAt` |
| Notes | Non-admins: exports for their `user_id` where the underlying document is still viewable. Admins: recent exports across users, filtered by document view policy. Unidentified callers: empty list. |

---

## AssistantController — `/api/v1/documents/{documentId}/assistant`

### `POST /api/v1/documents/{documentId}/assistant/ask`

| | |
| --- | --- |
| Path | `documentId` |
| Body | `AssistantAskRequest`: `question` (required non-blank string, max length 500) |
| Response **200** | `AssistantAnswerResponse`: `answer`, `citations` (`pageNumber`, `chunkIndex`, `snippet`), `grounded` |
| Notes | Stateless — nothing persisted. Empty retrieval → fixed not-found answer, `grounded: false`, no Gemini call. |
| Notes | Requires view access to the document. Consumes assistant per-hour limit after retrieval, before Gemini (empty retrieval does not call Gemini). |
| Errors | **404** document not found or not visible; **400** blank/too-long question; **429** assistant limit; **502** assistant provider failure after evidence found; **503** Gemini daily quota on assistant |

---

## CompanyController — `/api/v1/companies`

Prototype ESG comparison data (illustrative scores; not official MSCI ratings).

### `GET /api/v1/companies`

| | |
| --- | --- |
| Response **200** | `CompanySummaryResponse[]`: `id`, `name`, `ticker`, `industry` |

### `GET /api/v1/companies/{companyId}/esg`

| | |
| --- | --- |
| Path | `companyId` |
| Response **200** | `CompanyEsgProfileResponse`: `id`, `name`, `ticker`, `industry`, `currentRating` (`overallScore`, `environmentalScore`, `socialScore`, `governanceScore`, `ratingBand`, `assessmentDate`), `ratingHistory` (`quarter`, `score`), `materialIssues` (`id`, `issueCode`, `issueName`, `pillar`, `score`, `riskLevel`), `recentEvents` (`id`, `title`, `description`, `pillar`, `severity`, `eventDate`, `scoreImpact`, `isPrototype`) |
| Errors | **404** |

### `GET /api/v1/companies/compare`

| | |
| --- | --- |
| Query | `companyA` (required), `companyB` (required) |
| Response **200** | `CompanyComparisonResponse`: `companyA`, `companyB` (profiles), `comparisonInsight` |
| Errors | **400** if `companyA == companyB` or missing/invalid params; **404** if either company missing |

---

## VERIFY

| Section | Primary sources |
| --- | --- |
| Auth / 401 / token cache | `SupabaseAuthFilter.java`; `SupabaseTokenValidationCache.java`; `SupabaseAuthConfig.java`; `SupabaseAuthProperties.java`; `AuthenticatedUser.java` |
| Ownership / 403 / 404 masking | `DocumentAccessPolicy.java`; `DocumentAccessDeniedExceptionHandler.java`; `AuthProperties.java`; `DocumentRepository.java` (`findVisibleByOrganization`) |
| Usage limits / 429 | `UsageLimiter.java`; `UsageLimitProperties.java`; `UsageLimitExceededExceptionHandler.java`; `DocumentController.java`; `ComplianceAnalysisService.java`; `DocumentAssistantService.java` |
| Global 400/404/502/503/500 | `GlobalExceptionHandler.java`; `BadRequestException.java`; `ResourceNotFoundException.java`; `AssistantUnavailableException.java`; `PdfTextExtractionService.java`; `PdfLimitsProperties.java` |
| Endpoints + DTOs | All files in `controller/`; DTOs including `MeResponse.java`, `ReportExportResponse.java`, `DocumentDetailResponse.java`; `DocumentService.java`; `ComplianceAnalysisCreationService.java`; `DocumentAssistantService.java`; `GapAssessmentPdfService.java`; `ReportExportService.java`; `CompanyController.java` |
