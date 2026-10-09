# ESGenius

AI-assisted **SEBI BRSR** compliance gap analysis for uploaded sustainability PDFs. Assessments are decision support for human reviewers — the product does **not** certify compliance, declare official compliance, or replace professional judgement.

## What is implemented

- **PDF upload + page-aware extraction** — Documents page uploads PDFs only; the Spring Boot backend extracts text (Apache PDFBox) and stores per-page text (`document_page`).
- **BRSR evidence retrieval** — Lexical retrieval against a seeded SEBI BRSR requirement subset.
- **Gemini classification** — Retrieved evidence is classified per requirement (covered / partial / missing / human review), with explanation, gap, and recommendation text.
- **Async analysis + history** — Analyses run asynchronously; the UI polls status and lists prior runs for a document.
- **Gap Assessment PDF export** — From a completed compliance analysis (`/reports/gap-assessment` + backend PDF). Other report cards on `/reports` are UI previews only.
- **Document-grounded assistant** — Chat about one uploaded document; answers cite passages from that document when evidence is found.
- **Company comparison** — Side-by-side ESG rating snapshots, key issues, and events from seeded comparison data.
- **Supabase Auth** — Email/password sign-in; the frontend sends the access token to the API.

### Not built

- **Frameworks** other than SEBI BRSR (GRI, IFRS S1/S2, ESRS appear as planned on `/frameworks`).
- **Report types** other than Gap Assessment (environmental summary, executive summary, missing-evidence report, and generate-from-templates remain previews).
- **Persisted settings and notifications** — `/settings` is a prototype form; changes are not saved. Notification toggles do not send alerts.

Routes: `/` dashboard, `/documents`, `/compliance`, `/comparison`, `/frameworks` (+ `/frameworks/brsr`), `/assistant`, `/reports`, `/settings`, `/login`.

## Architecture

| Piece | Stack |
| --- | --- |
| Frontend | React 19, TanStack Router / Start / Query, Vite, TypeScript |
| Backend | Java 21, Spring Boot 3.4.1 (`esgenius-backend`) |
| Database | PostgreSQL + Flyway (`classpath:db/migration`, V1–V9) |
| AI | Google Gemini (`GEMINI_MODEL`, default `gemini-3.5-flash`) |
| Auth | Supabase Auth (JWT on API requests) |
| Deploy | Render web service from `backend/Dockerfile` (`runtime: docker`), health check `/api/v1/health` |

Flyway covers organizations and BRSR requirements, comparison/rating tables, documents, page text, and compliance analyses.

Default local ports: API `8081` (`PORT`), frontend CORS allow-list includes `http://localhost:8080`.

## Local setup

**Prerequisites:** Node.js + npm, Java 21, PostgreSQL.

### Frontend

Env var names (see `.env.example`; copy to `.env.local`):

- `VITE_API_BASE_URL`
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY`

```sh
npm i
npm run dev
```

### Backend

From `backend/`. Env var names (see `backend/.env.example`; copy to `backend/.env`):

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `GEMINI_API_KEY`
- `GEMINI_MODEL`
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `AUTH_REQUIRED`
- `ALLOWED_ORIGINS`

Local runs without Supabase need `AUTH_REQUIRED=false`.

Create the Postgres database named in `DB_URL` (default database name `esgenius`). Then:

```sh
./mvnw spring-boot:run
```

On Windows cmd, use `mvnw.cmd`. That file is stored with LF line endings, which can fail under cmd; `.gitattributes` now sets `*.cmd text eol=crlf` (re-checkout the file if your working copy is still LF).

Uploads are written under `app.storage.upload-dir` (`uploads` by default). Multipart limit is 25MB.

### Tests

```sh
npm test
```

```sh
./mvnw test
```

(`npm test` runs `vitest run`. Backend tests use H2.)

## Lovable

This repository is linked to [Lovable](https://lovable.dev). Commits on the connected branch sync to the Lovable editor. Do not rewrite published git history (force-push, rebase, amend, or squash of already-pushed commits). Continue in the [Lovable project](https://lovable.dev/projects/335a7791-c092-4551-8b1f-701414a2b195) if you use that workflow.
