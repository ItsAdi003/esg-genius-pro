# Testing

How tests are organized and run in this repository. Test counts change often; use the commands in [Counting tests](#counting-tests) instead of hard-coded totals in this file.

## How to run

### Frontend (repo root)

```bash
npx vitest run src
npx tsc --noEmit
# optional when routes change:
npx vite build
```

`package.json` script `"test": "vitest run"` is equivalent to the Vitest command above when invoked via `npm test`.

### Backend (`backend/`)

Full suite (only when intentionally requested):

```bash
cd backend
.\mvnw.cmd -o test
```

Targeted classes (preferred while other agents share the tree):

```bash
cd backend
.\mvnw.cmd -o test "-Dtest=ClassA,ClassB"
```

If Maven fails because another process locked `backend/target/`, wait ~60s and retry. Do not delete or clean the target directory from an unrelated documentation change.

## H2 vs PostgreSQL

| Environment | Database |
| --- | --- |
| **Runtime** (`application.yml`) | PostgreSQL (`DB_URL` / default `jdbc:postgresql://localhost:5432/esgenius`) |
| **Tests** (`application-test.yml`, `application-wiring.yml`) | In-memory H2 with `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` |

Flyway still runs the same `classpath:db/migration` scripts in tests. Hibernate `ddl-auto` remains `validate`. Test uploads go to `target/test-uploads`.

## No live Gemini (or Supabase Auth) in tests

| Production (`!test`) | Test profile |
| --- | --- |
| `GeminiComplianceClassificationProvider` | `TestComplianceClassificationConfig` → in-process fake returning fixed `COVERED` |
| `GeminiAssistantAnswerProvider` | `TestAssistantAnswerConfig` → in-process fake answer |
| `SupabaseAuthFilter` registered | Auth config profile `!test` — filter **not** registered in Spring tests |

Provider unit tests (`GeminiComplianceClassificationProviderTest`, `GeminiAssistantAnswerProviderTest`) use Spring `MockRestServiceServer` against a local `RestClient` — they never call Google. Auth unit tests mock `HttpClient`. Integration tests therefore do not require `GEMINI_API_KEY` or live Supabase.

## Backend test groups (by area)

Classes named `*Test.java` under `backend/src/test/java` (support fixtures omitted).

### Controllers / HTTP

- `HealthControllerTest`
- `FrameworkControllerTest`
- `DocumentControllerTest`
- `DocumentOwnershipAccessTest` — `owner_user_id`, `shared` / `canModify`, **403** on shared writes
- `ComplianceAnalysisControllerTest`
- `CompanyControllerTest`
- `AssistantControllerTest`
- `ReportControllerTest`
- `ReportControllerRecordingFailureTest` — PDF download still succeeds when export recording fails
- `ReportExportListMvcTest` — `GET /api/v1/reports`
- `UsageLimitMvcTest` — **429** + `Retry-After` on uploads and analyses
- `ReadEndpointQueryCountTest`
- `ApiBadRequestInputTest` (exception package; exercises missing/invalid request params via MockMvc)

### Auth config

- `SupabaseAuthFilterTest`
- `SupabaseAuthFilterCacheTest`

### Rate limits / PDF bounds / Gemini config

- `UsageLimiterTest`
- `PdfLimitsPropertiesTest`
- `GeminiPropertiesTest`
- `ComplianceClassificationProductionWiringTest`

### Document ingestion

- `DocumentServiceTest`
- `DocumentAsyncProcessingTest` (queue-backed executor: PROCESSING → READY/FAILED, busy rejection, deleted-before-run, restart recovery, analysis refused while processing)
- `PdfTextExtractionServiceTest`
- `TextChunkingServiceTest`

### Compliance analysis pipeline

- `ComplianceAnalysisServiceTest`
- `ComplianceAnalysisDispatchTest`
- `ComplianceAnalysisDispatchRejectionTest`
- `ComplianceAnalysisAsyncExecutionTest`
- `ComplianceAnalysisStartupRecoveryTest`
- `ComplianceAnalysisClassificationTest`
- `ComplianceAnalysisConcurrencyTest`
- `ComplianceAnalysisConcurrencyParallelTest` — `GEMINI_MAX_CONCURRENCY` pool behaviour
- `PageAwareEvidenceProvenanceTest`
- `GapAssessmentPdfServiceTest`

### Retrieval

- `LexicalEvidenceRetrievalServiceTest`
- `ParaphrasedDisclosureRetrievalTest`
- `InfosysBrsrRetrievalSmokeTest`

### Classification helpers / Gemini provider (mocked HTTP)

- `DeterministicNoEvidenceClassifierTest`
- `ClassificationEvidenceBuilderTest`
- `ClassificationFailureHandlerTest`
- `EvidenceContextExpanderTest`
- `ComplianceClassificationPromptBuilderTest`
- `ComplianceClassificationResultParserTest`
- `GeminiComplianceClassificationProviderTest`
- `QuotaAwareComplianceClassificationProviderTest`

### Assistant

- `DocumentAssistantServiceTest`
- `DocumentAssistantEvidenceServiceTest`
- `GeminiAssistantAnswerProviderTest`

### Exception handling

- `GlobalExceptionHandlerTest`
- `AssistantUnavailableExceptionHandlerTest` — assistant **503** quota path

### Company / framework services & repositories

- `CompanyEsgServiceTest`
- `FrameworkServiceTest`
- `FrameworkRepositoryTest`
- `EsgRatingSnapshotRepositoryTest`
- `EsgEventRepositoryTest`
- `FlywaySeedDataIntegrationTest`

### Domain / app smoke

- `AssessmentStatusBackwardCompatibilityTest`
- `EsgGeniusApplicationTests`

## Frontend test files

Vitest specs under `src/**/*.test.ts`:

- `src/lib/api-config.test.ts`
- `src/lib/company-comparison-adapters.test.ts`
- `src/lib/compliance-api.test.ts`
- `src/lib/compliance-polling.test.ts`
- `src/lib/compliance-progress.test.ts`
- `src/lib/document-api.test.ts`
- `src/lib/me-api.test.ts` — `GET /api/v1/me` client + usage meters
- `src/lib/navigation-search.test.ts`
- `src/lib/report-api.test.ts` — `GET /api/v1/reports` client
- `src/lib/use-session-user.test.ts`

## Counting tests

Re-run these from the repo root when you need current numbers or to verify a new test class landed:

```powershell
# Backend JUnit test methods
(Get-ChildItem -Recurse -Path "backend\src\test" -Filter "*.java" |
  Select-String -Pattern '@Test\b').Count

# Frontend Vitest cases (it(...))
(Get-ChildItem -Recurse -Path "src" -Include "*.test.ts","*.test.tsx" |
  Select-String -Pattern '^\s*it\s*\(').Count

# List backend test classes (inventory check)
Get-ChildItem -Recurse -Path "backend\src\test" -Filter "*Test.java" |
  ForEach-Object { $_.FullName.Replace((Get-Location).Path + '\', '') }
```

Optional sanity check for the BRSR MVP seed size:

```powershell
(Select-String -Path "backend\src\main\resources\db\migration\V4__seed_brsr_requirements.sql" -Pattern "ENV-|SOC-|GOV-").Count
```

## Known testing limitations

- Backend CI-style runs use H2, not a real Postgres instance.
- Compliance classification in Spring tests is a stub; behaviour of live Gemini (quota, safety blocks) is covered via mocked HTTP provider tests and quota-aware unit tests.
- Auth filter is unit-tested in isolation; most `@SpringBootTest` / MockMvc paths do not exercise live Supabase validation (ownership tests inject synthetic users).
- `MeController` has no dedicated MockMvc test yet; `me-api.test.ts` covers the frontend client against mocked `apiFetch`.
- Retrieval smoke tests use fixture/document text in-repo, not live network downloads of company reports.
- Usage limits are in-memory; multi-instance behaviour is not integration-tested.

## VERIFY

| Section | Primary sources |
| --- | --- |
| Run commands | `package.json`; `backend/pom.xml` (H2 test dependency); project conventions |
| H2 / Flyway | `backend/src/test/resources/application-test.yml`; `application-wiring.yml`; `application.yml` |
| No live Gemini / auth | `TestComplianceClassificationConfig.java`; `TestAssistantAnswerConfig.java`; `ComplianceClassificationConfig.java`; `AssistantAnswerConfig.java`; `SupabaseAuthConfig.java` (`@Profile("!test")`); Gemini `*ProviderTest.java` (`MockRestServiceServer`) |
| Class inventory | All `*Test.java` under `backend/src/test/java`; `src/**/*.test.ts` |
| Counting commands | PowerShell snippets in [Counting tests](#counting-tests) |
