# ESGenius

AI-assisted **SEBI BRSR** compliance gap analysis for uploaded sustainability PDFs. Assessments are decision support for human reviewers — the product does **not** certify compliance, declare official compliance, or replace professional judgement.

## What is implemented

- **PDF upload + page-aware extraction** — Documents page uploads PDFs only; the upload returns immediately and a background worker extracts text (Apache PDFBox), enforces page/character limits, and stores per-page text (`document_page`); the UI polls until the document is ready.
- **BRSR evidence retrieval** — Lexical retrieval against a seeded SEBI BRSR requirement subset.
- **Gemini classification** — Retrieved evidence is classified per requirement (covered / partial / missing / human review), with explanation, gap, and recommendation text.
- **Async analysis + progress UI** — Analyses run asynchronously; the compliance UI polls status, shows in-run progress from assessment counts, and lists prior runs per document.
- **Document ownership** — Uploads are tied to the signed-in Supabase user (`owner_user_id`); shared samples (`NULL` owner) are view-only unless you are an admin (`ADMIN_USER_IDS`).
- **Usage limits** — Rolling per-user caps on uploads, analyses, and assistant questions; `GET /api/v1/me` drives usage meters on `/settings`. Over-limit API calls return **429** with `Retry-After`.
- **Gap Assessment PDF export + history** — Download from a completed analysis (`GET …/report.pdf`); `/reports` lists recent exports via `GET /api/v1/reports`. Other report cards on `/reports` remain UI previews only.
- **Document-grounded assistant** — Chat about one uploaded document; answers cite passages from that document when evidence is found.
- **Company comparison** — Side-by-side ESG rating snapshots, key issues, and events from seeded comparison data.
- **Supabase Auth** — Email/password sign-in; the frontend sends the access token to the API.

### Not built

- **Frameworks** other than SEBI BRSR (GRI, IFRS S1/S2, ESRS appear as planned on `/frameworks`).
- **Report types** other than Gap Assessment (environmental summary, executive summary, missing-evidence report, and generate-from-templates remain previews).
- **Persisted workspace settings and notifications** — `/settings` shows live account email, admin badge, and usage limits from the API, but organization profile, framework sliders, retention, and notification toggles are not saved server-side and do not send alerts.

Routes: `/` dashboard, `/documents`, `/compliance`, `/comparison`, `/frameworks` (+ `/frameworks/brsr`), `/assistant`, `/reports`, `/settings`, `/login`.

## Architecture

| Piece | Stack |
| --- | --- |
| Frontend | React 19, TanStack Router / Start / Query, Vite, TypeScript |
| Backend | Java 21, Spring Boot 3.4.1 (`esgenius-backend`) |
| Database | PostgreSQL + Flyway (`classpath:db/migration`, V1–V11) |
| AI | Google Gemini (`GEMINI_MODEL`, default `gemini-3.5-flash`) |
| Auth | Supabase Auth (JWT on API requests) |
| Deploy | Render web service from `backend/Dockerfile` (`runtime: docker`), health check `/api/v1/health` |

Flyway covers organizations and BRSR requirements, comparison/rating tables, documents (including `owner_user_id`), page text, compliance analyses, and `report_export` history.

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
- `GEMINI_MAX_CONCURRENCY` (1–8, default 1)
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `AUTH_REQUIRED`
- `ADMIN_USER_IDS`
- `SUPABASE_TOKEN_CACHE_TTL`
- `ALLOWED_ORIGINS`
- `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` (default `5`)
- `PDF_MAX_PAGES` (default `400`)
- `PDF_MAX_EXTRACTED_CHARS` (default `3000000`)
- `RATE_LIMIT_UPLOADS_PER_DAY` (default `20`)
- `RATE_LIMIT_ANALYSES_PER_DAY` (default `5`)
- `RATE_LIMIT_ASSISTANT_PER_HOUR` (default `30`)
- `RATE_LIMIT_GLOBAL_ANALYSES_PER_DAY` (default `20`)

Local runs without Supabase need `AUTH_REQUIRED=false`. `ADMIN_USER_IDS` is a comma-separated list of user UUIDs that can manage every document, including shared samples.

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

## Deploying

**Backend (Render)** — `render.yaml` defines a Docker web service from `backend/Dockerfile` with health check `/api/v1/health`. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to a Supabase **Session** pooler connection on port **5432** with `sslmode=require` (not the transaction pooler on 6543). Set `ALLOWED_ORIGINS` to the exact frontend origin(s) (scheme + host, no trailing slash). Keep `AUTH_REQUIRED=true` in production; set `ADMIN_USER_IDS` to comma-separated Supabase user UUIDs that may access all documents. `GEMINI_MAX_CONCURRENCY`, PDF limits, rate limits, and `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` have safe defaults in `render.yaml` and can be overridden in the Render dashboard.

**Frontend** — Build and host via Lovable / Cloudflare (Nitro SSR), not as a plain static bucket. `VITE_API_BASE_URL`, `VITE_SUPABASE_URL`, and `VITE_SUPABASE_ANON_KEY` are read at **build** time; rebuild after changing them. In Supabase Auth, set **Site URL** and **Redirect URLs** to match your deployed frontend origin.

**Smoke test** — After deploy, sign in, upload a small PDF, wait for processing, and run a short compliance analysis to confirm DB, storage, Gemini, and auth end-to-end.

## Lovable

This repository is linked to [Lovable](https://lovable.dev). Commits on the connected branch sync to the Lovable editor. Do not rewrite published git history (force-push, rebase, amend, or squash of already-pushed commits). Continue in the [Lovable project](https://lovable.dev/projects/335a7791-c092-4551-8b1f-701414a2b195) if you use that workflow.
