# Testing

How tests are organized and run in this repository. Counts below were measured from the working tree (method stated at the end).

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
.\mvnw.cmd -o test -Dtest=ClassA,ClassB
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
- `ComplianceAnalysisControllerTest`
- `CompanyControllerTest`
- `AssistantControllerTest`
- `ReportControllerTest`
- `ReadEndpointQueryCountTest`
- `ApiBadRequestInputTest` (exception package; exercises missing/invalid request params via MockMvc)

### Auth config

- `SupabaseAuthFilterTest`
- `SupabaseAuthFilterCacheTest`

### Document ingestion

- `DocumentServiceTest`
- `PdfTextExtractionServiceTest`
- `TextChunkingServiceTest`

### Compliance analysis pipeline

- `ComplianceAnalysisServiceTest`
- `ComplianceAnalysisDispatchTest`
- `ComplianceAnalysisDispatchRejectionTest`
- `ComplianceAnalysisAsyncExecutionTest`
- `ComplianceAnalysisStartupRecoveryTest`
- `ComplianceAnalysisClassificationTest`
- `PageAwareEvidenceProvenanceTest`
- `GapAssessmentPdfServiceTest`

### Retrieval

- `LexicalEvidenceRetrievalServiceTest`
- `ParaphrasedDisclosureRetrievalTest`
- `InfosysBrsrRetrievalSmokeTest`

### Classification helpers / Gemini provider (mocked HTTP)

- `DeterministicNoEvidenceClassifierTest`
- `ClassificationEvidenceBuilderTest`
- `EvidenceContextExpanderTest`
- `ComplianceClassificationPromptBuilderTest`
- `ComplianceClassificationResultParserTest`
- `GeminiComplianceClassificationProviderTest`
- `ComplianceClassificationProductionWiringTest`

### Assistant

- `DocumentAssistantServiceTest`
- `DocumentAssistantEvidenceServiceTest`
- `GeminiAssistantAnswerProviderTest`

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
- `src/lib/navigation-search.test.ts`
- `src/lib/use-session-user.test.ts`

## Verified test counts

Measured on the local tree with PowerShell (no test runner executed for this documentation change):

```powershell
# Backend JUnit methods
(Get-ChildItem -Recurse -Path "backend\src\test" -Filter "*.java" |
  Select-String -Pattern '@Test\b').Count
# → 251

# Frontend Vitest cases (it(...))
(Get-ChildItem -Recurse -Path "src" -Include "*.test.ts","*.test.tsx" |
  Select-String -Pattern '^\s*it\s*\(').Count
# → 90
```

Also noted: 14 requirement codes matching `ENV-|SOC-|GOV-` in `V4__seed_brsr_requirements.sql` (same PowerShell `Select-String` count).

These counts will drift as other chats add tests; re-run the commands above to refresh.

## Known testing limitations

- Backend CI-style runs use H2, not a real Postgres instance.
- Compliance classification in Spring tests is a stub; behaviour of live Gemini (quota, safety blocks) is covered only via mocked HTTP provider tests.
- Auth filter is unit-tested in isolation; most `@SpringBootTest` / MockMvc paths do not exercise Supabase validation.
- Retrieval smoke tests use fixture/document text in-repo, not live network downloads of company reports.

## VERIFY

| Section | Primary sources |
| --- | --- |
| Run commands | `package.json`; `backend/pom.xml` (H2 test dependency); user/project conventions in this task |
| H2 / Flyway | `backend/src/test/resources/application-test.yml`; `application-wiring.yml`; `application.yml` |
| No live Gemini / auth | `TestComplianceClassificationConfig.java`; `TestAssistantAnswerConfig.java`; `ComplianceClassificationConfig.java`; `AssistantAnswerConfig.java`; `SupabaseAuthConfig.java` (`@Profile("!test")`); Gemini `*ProviderTest.java` (`MockRestServiceServer`) |
| Class inventory | All `*Test.java` under `backend/src/test/java`; `src/**/*.test.ts` |
| Counts | PowerShell `Select-String` on `@Test\b` and `^\s*it\s*\(` as quoted above |
