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
| Success | Sets request attribute `authenticatedUser` (`userId`, optional `email`); token validation results cached by SHA-256 digest for `token-cache-ttl` (default 60s) |

Controllers do not currently authorize by user id; resource access is by path/query ids.

## Global error statuses

| Status | Source | When |
| --- | --- | --- |
| **400** | `BadRequestException` and mapped servlet errors | Business validation failures; missing required query/form params; type mismatches; multipart > 25 MB (`"File size exceeds maximum allowed size of 25 MB"`) |
| **401** | `SupabaseAuthFilter` | Auth failures above (not via `GlobalExceptionHandler`) |
| **404** | `ResourceNotFoundException` | Missing document, analysis, framework, organization, company, etc. |
| **502** | `AssistantUnavailableException` | Assistant could not produce an answer after evidence was found. Handler always returns **502 Bad Gateway** (even if the exception was constructed with another status for quota messaging). |
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
| Response **201** | `DocumentDetailResponse`: `id`, `organizationId`, `organizationName`, `originalFilename`, `documentType`, `reportingYear`, `status`, `fileSize`, `pageCount`, `uploadedAt`, `processedAt`, `extractedText`, `failureReason` |
| Errors | **400** validation / non-PDF / too large / scanned-or-empty text / store failure; **404** organization not found; missing params → **400** |

### `GET /api/v1/documents`

| | |
| --- | --- |
| Query | `organizationId` (required) |
| Response **200** | `DocumentSummaryResponse[]` (same fields as detail except no `extractedText` / `failureReason`) |
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
| Errors | **404** |

---

## ComplianceAnalysisController — `/api/v1`

### `GET /api/v1/documents/{documentId}/analyses`

| | |
| --- | --- |
| Path | `documentId` |
| Response **200** | `ComplianceAnalysisSummaryResponse[]`: `id`, `documentId`, `frameworkId`, `frameworkCode`, `frameworkName`, `status`, `startedAt`, `completedAt`, `failureReason`, `requirementCount`, `coveredCount`, `partiallyCoveredCount`, `notCoveredCount`, `humanReviewRequiredCount`, `evidenceRetrievedCount`, `noEvidenceFoundCount` |
| Errors | **404** document not found |

### `POST /api/v1/documents/{documentId}/analyses`

| | |
| --- | --- |
| Path | `documentId` |
| Body | `StartAnalysisRequest`: `frameworkId` (optional `Long`), `frameworkCode` (optional `String`) — **one of the two required** |
| Response **202** | `ComplianceAnalysisResponse`: `id`, `documentId`, `frameworkId`, `frameworkCode`, `frameworkName`, `status`, `startedAt`, `completedAt`, `failureReason`, `requirementCount`, `assessments` (`RequirementAssessmentResponse[]`) |
| Assessment fields | `requirementId`, `requirementCode`, `requirementTitle`, `category`, `assessmentStatus`, `confidence`, `explanation`, `gap`, `recommendation`, `retrievalScore`, `evidenceText`, `evidenceChunks` (`chunkIndex`, `pageNumber`, `text`, `retrievalScore`) |
| Errors | **404** document/framework; **400** document not `READY`, blank extracted text, empty framework requirements, analysis already in progress, missing framework selector |

### `GET /api/v1/analyses/{analysisId}`

| | |
| --- | --- |
| Path | `analysisId` |
| Response **200** | `ComplianceAnalysisResponse` (full assessments) |
| Errors | **404** |

---

## ReportController — `/api/v1`

### `GET /api/v1/analyses/{analysisId}/report.pdf`

| | |
| --- | --- |
| Path | `analysisId` |
| Response **200** | Raw PDF bytes; `Content-Type: application/pdf`; `Content-Disposition: attachment; filename="gap-assessment-{id}.pdf"` |
| Errors | **404** if analysis (or its document) is missing |

---

## AssistantController — `/api/v1/documents/{documentId}/assistant`

### `POST /api/v1/documents/{documentId}/assistant/ask`

| | |
| --- | --- |
| Path | `documentId` |
| Body | `AssistantAskRequest`: `question` (required non-blank string, max length 500) |
| Response **200** | `AssistantAnswerResponse`: `answer`, `citations` (`pageNumber`, `chunkIndex`, `snippet`), `grounded` |
| Notes | Stateless — nothing persisted. Empty retrieval → fixed not-found answer, `grounded: false`, no Gemini call. |
| Errors | **404** document; **400** blank/too-long question; **502** assistant provider failure after evidence found |

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
| Auth / 401 | `SupabaseAuthFilter.java`; `SupabaseAuthConfig.java`; `SupabaseAuthProperties.java`; `AuthenticatedUser.java` |
| Global 400/404/502/500 | `GlobalExceptionHandler.java`; `BadRequestException.java`; `ResourceNotFoundException.java`; `AssistantUnavailableException.java` |
| Endpoints + DTOs | All files in `controller/`; all records in `dto/`; `DocumentService.java`; `ComplianceAnalysisCreationService.java`; `DocumentAssistantService.java`; `GapAssessmentPdfService.java`; `CompanyController.java` |
