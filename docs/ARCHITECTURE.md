# Architecture

Technical architecture of ESGenius as implemented in this repository. Descriptions below are derived from the source files listed in [VERIFY](#verify).

## Components and request flow

```mermaid
flowchart TB
  subgraph frontend ["Frontend"]
    UI["React + TanStack Start / Router / Query"]
    SupaClient["@supabase/supabase-js session"]
  end

  subgraph backend ["Spring Boot backend :8081"]
    AuthFilter["SupabaseAuthFilter /api/**"]
    Controllers["REST controllers"]
    Services["Domain services"]
    GeminiClass["GeminiComplianceClassificationProvider"]
    GeminiAssist["GeminiAssistantAnswerProvider"]
    LocalFS["LocalFileStorageService uploads/"]
  end

  subgraph data ["Persistence"]
    PG[(PostgreSQL)]
    Flyway["Flyway V1–V9"]
  end

  UI -->|"Bearer access token"| AuthFilter
  SupaClient --> UI
  AuthFilter --> Controllers
  Controllers --> Services
  Services --> PG
  Flyway --> PG
  Services --> LocalFS
  Services --> GeminiClass
  Services --> GeminiAssist
```

| Layer | Role in code |
| --- | --- |
| **Frontend** | TanStack Start / React Router app (`package.json`, `src/routes/**`). Calls the backend via `apiFetch` helpers in `src/lib/*-api.ts`. Supabase client holds the user session; the access token is sent as `Authorization: Bearer …`. |
| **Auth filter** | `SupabaseAuthFilter` validates tokens against Supabase Auth `GET {url}/auth/v1/user`, caches successes by SHA-256 of the token, and sets request attribute `authenticatedUser`. Registered for `/api/*` outside the `test` profile. Skips `OPTIONS` and `GET /api/v1/health`. |
| **API / services** | Controllers under `dev.esgenius.controller` delegate to services (documents, compliance analysis, frameworks, companies, assistant, PDF report). |
| **Postgres + Flyway** | Runtime datasource is PostgreSQL (`application.yml`). Schema and seed data come from `classpath:db/migration` V1–V9; JPA `ddl-auto: validate`. |
| **Local file storage** | Uploaded PDFs are stored under `app.storage.upload-dir` (default `uploads`) as `{uuid}.pdf` via `LocalFileStorageService`. Metadata and extracted text live in Postgres. |
| **Gemini providers** | Production (`!test`) wires `GeminiComplianceClassificationProvider` and `GeminiAssistantAnswerProvider` when `GEMINI_API_KEY` is set. Test profile replaces both with in-process fakes. |

**Auth identity vs data access:** the filter authenticates the caller but services do not scope queries by `AuthenticatedUser`. Document and analysis APIs take explicit `organizationId` / resource ids. Any authenticated caller who knows those ids can access the shared dataset.

## Analysis pipeline

Triggered by `POST /api/v1/documents/{documentId}/analyses` → `ComplianceAnalysisService.startAnalysis` → async `ComplianceAnalysisProcessor`.

1. **Create row** — `ComplianceAnalysisCreationService` requires document `READY` with non-blank extracted text, a resolvable framework (`frameworkId` or `frameworkCode`), at least one requirement, and no existing `IN_PROGRESS` analysis for the same document+framework. Status starts as `IN_PROGRESS`.
2. **Dispatch** — `ComplianceAnalysisProcessor` submits work to `complianceAnalysisExecutor` (core 1 / max 2 / queue 8, abort on reject). Rejection or uncaught failure marks the analysis `FAILED`.
3. **Extraction (already done at upload)** — `DocumentService` + `PdfTextExtractionService` (PDFBox, no OCR) produce full text and `document_page` rows at upload time. Analysis reuses stored text/pages.
4. **Chunking** — `TextChunkingService.chunkPages` (preferred when pages exist) or legacy `chunk` (~1200–1800 chars, 200 overlap). Page-aware chunks never cross page boundaries.
5. **Lexical retrieval** — `LexicalEvidenceRetrievalService` scores chunks with BM25-inspired / IDF lexical matching, requirement anchor gates, and ESG aliases. Top 3 chunks above score threshold `0.15`. **No embeddings / vector search.**
6. **Classification**
   - **No evidence** → `DeterministicNoEvidenceClassifier` (always `NOT_COVERED`, confidence `0.75`). Gemini is **not** called.
   - **Has evidence** → expand passages → `ComplianceClassificationProvider.classify` (Gemini in production). On `ComplianceClassificationException`, `ClassificationFailureHandler` returns `HUMAN_REVIEW_REQUIRED` (quota-specific explanation when `QuotaExhaustionScope` is exhausted).
7. **Async persistence** — each assessment is saved in a new transaction via `ComplianceAnalysisPersistenceService.persistRequirementAssessment`; then `finalizeCompleted` sets analysis `COMPLETED`.
8. **Startup recovery** — `ComplianceAnalysisStartupRecovery` on `ApplicationReadyEvent` marks leftover `IN_PROGRESS` rows `FAILED` with reason *"Analysis was interrupted when the application restarted."*

Inter-request pacing between Gemini classifications uses `app.ai.gemini.inter-request-delay` (default 300 ms).

## Human-review fallback

| Situation | Resulting assessment |
| --- | --- |
| Gemini / classification provider throws | `HUMAN_REVIEW_REQUIRED`, confidence `null`, recommendation to have a human assess; explanation is either the generic failure text or the quota-exhausted text |
| Daily quota already marked exhausted on the analysis thread | Further classify calls fail immediately; failure handler uses the quota explanation |
| No retrieved evidence | **Not** human-review: deterministic `NOT_COVERED` |

Legacy statuses `EVIDENCE_RETRIEVED` / `NO_EVIDENCE_FOUND` remain readable for old rows but must not be persisted by new analyses.

## Where AI is not trusted

- **No-evidence path:** classification is fully deterministic (`DeterministicNoEvidenceClassifier`); model output is never used to invent coverage.
- **Retrieval:** evidence selection is lexical and rule-gated (`LexicalEvidenceRetrievalService`, `RequirementRetrievalRules`), not model-chosen.
- **Assistant empty retrieval:** if no chunks match the question, the API returns a fixed not-found answer with `grounded: false` and does not call Gemini.
- **Failure path:** model/provider errors degrade to `HUMAN_REVIEW_REQUIRED` rather than guessing `COVERED`.
- **Comparison / rating data:** company ESG scores from seed migrations are labelled prototype / illustrative in controller and migration comments — not model outputs and not official MSCI ratings.
- **PDF export disclaimer:** `GapAssessmentPdfService` embeds prototype and human-validation disclaimers in the generated report.

## Sequence: upload → analyze → poll → export PDF

```mermaid
sequenceDiagram
  actor User
  participant FE as TanStack Start UI
  participant API as Spring Boot API
  participant Store as Local uploads/ + Postgres
  participant Exec as complianceAnalysisExecutor
  participant Gemini as Gemini classification API

  User->>FE: Upload PDF
  FE->>API: POST /api/v1/documents (multipart)
  API->>Store: store file, extract text/pages, status READY
  API-->>FE: 201 DocumentDetailResponse

  User->>FE: Start BRSR analysis
  FE->>API: POST /api/v1/documents/{id}/analyses
  API->>Store: insert compliance_analysis IN_PROGRESS
  API->>Exec: processAnalysis(analysisId)
  API-->>FE: 202 ComplianceAnalysisResponse

  loop while status IN_PROGRESS
    FE->>API: GET /api/v1/analyses/{analysisId}
    API-->>FE: status + assessments so far
  end

  Exec->>Store: chunk + lexical retrieve
  alt evidence found
    Exec->>Gemini: classify requirement
    Gemini-->>Exec: COVERED / PARTIAL / NOT_COVERED
  else no evidence
    Exec->>Exec: DeterministicNoEvidenceClassifier
  end
  Exec->>Store: persist assessments, mark COMPLETED

  FE->>API: GET /api/v1/analyses/{id}/report.pdf
  API->>Store: load completed analysis + document
  API-->>FE: application/pdf attachment
```

Frontend polling uses TanStack Query `refetchInterval` driven by `resolveAnalysisPollingInterval` in `src/lib/compliance-api.ts` (poll only while `IN_PROGRESS`).

## Known limitations (as implemented)

- **Single shared dataset:** auth does not bind users to organizations; demo org and comparison companies are shared seed data.
- **Free-tier Gemini quota:** providers detect quota exhaustion; analysis continues with `HUMAN_REVIEW_REQUIRED` for remaining evidenced requirements; assistant maps quota to an unavailable error path.
- **Lexical retrieval only:** no semantic / vector index.
- **Prototype 14-requirement BRSR subset:** Flyway V4 seeds 14 requirements (`MVP-2026`), not the full SEBI BRSR set; PDF export states this explicitly.
- **No OCR:** scanned / image-only PDFs fail upload with a scanned-PDF message.
- **Upload size:** multipart max 25 MB.

## VERIFY

| Section | Primary sources |
| --- | --- |
| Components / flow | `package.json`; `src/lib/*-api.ts`; `application.yml`; `SupabaseAuthConfig.java`; `SupabaseAuthFilter.java`; `AuthenticatedUser.java`; `LocalFileStorageService.java`; `ComplianceClassificationConfig.java`; `AssistantAnswerConfig.java`; controllers under `controller/` |
| Analysis pipeline | `ComplianceAnalysisService.java`; `ComplianceAnalysisCreationService.java`; `ComplianceAnalysisProcessor.java`; `ComplianceAnalysisPersistenceService.java`; `ComplianceAnalysisStartupRecovery.java`; `ComplianceAnalysisAsyncConfig.java`; `DocumentService.java`; `PdfTextExtractionService.java`; `TextChunkingService.java`; `LexicalEvidenceRetrievalService.java`; `DeterministicNoEvidenceClassifier.java`; `ClassificationFailureHandler.java`; `QuotaExhaustionScope.java`; `GeminiComplianceClassificationProvider.java` |
| Human-review / AI trust | `ClassificationFailureHandler.java`; `DeterministicNoEvidenceClassifier.java`; `DocumentAssistantService.java`; `AssessmentStatus.java`; `CompanyController.java` (prototype comment); `GapAssessmentPdfService.java`; `V4__seed_brsr_requirements.sql`; `V6__seed_demo_companies_with_esg_data.sql` |
| Sequence diagram | Controllers above; `src/lib/document-api.ts`; `src/lib/compliance-api.ts`; `src/lib/compliance-polling.test.ts`; `src/routes/compliance/index.tsx`; `ReportController.java` |
