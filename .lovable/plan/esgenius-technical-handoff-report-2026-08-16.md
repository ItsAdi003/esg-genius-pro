# ESGenius — Technical Handoff Report

No code was changed. This is a description of the project exactly as it stands.

## 1. Project Overview

- Name: ESGenius — AI-Powered ESG & Sustainability Compliance Assistant (package name in `package.json` is still `tanstack_start_ts`).
- Purpose: UI prototype for uploading sustainability documents, comparing them to ESG frameworks (SEBI BRSR), showing compliance gaps with evidence citations, an ESG chat assistant, and report generation. Frontend only.
- Framework: React 19.2 + TanStack Start 1.168.32 (SSR-capable, Cloudflare/nitro target).
- Build tool: Vite 8.2 via `@lovable.dev/vite-tanstack-config` (`vite.config.ts` is 8 lines; plugins are preconfigured).
- Routing: TanStack Router 1.170.18, file-based, generated `src/routeTree.gen.ts`.
- Styling: Tailwind CSS v4.2 configured entirely in `src/styles.css` (no `tailwind.config.js`), OKLCH design tokens, `tw-animate-css`.
- UI kit: shadcn/ui (new-york, slate base) on Radix primitives — full set present in `src/components/ui/` (49 files).
- Charts: recharts 2.15.4. Icons: lucide-react 0.575. Toasts: sonner 2.0.7.
- State libs: `@tanstack/react-query` 5.101 is installed and a `QueryClient` is created in `src/router.tsx` and `__root.tsx`, but **no query is used anywhere**.
- Forms: react-hook-form 7.71 + zod 3.24 + `@hookform/resolvers` installed, **not used by any page**.
- Other: date-fns, cmdk, embla-carousel, vaul, input-otp, react-resizable-panels — all shipped by the template, unused by app pages.
- Tooling: TypeScript 5.8, ESLint 9 + prettier plugin, Bun (`bun.lock`, `bunfig.toml`).

## 2. Project Structure

```
src/
  routes/            file-based routes (see §3) + __root.tsx shell
  components/        app-layout, global-search, status-badge,
                     radial-score, theme-toggle, upload-dialog
  components/ui/     shadcn primitives (untouched template code)
  hooks/             use-count-up.ts, use-mobile.tsx
  lib/               esg-data.ts (ALL mock data + types + mock services),
                     utils.ts, error-page.ts, error-capture.ts,
                     lovable-error-reporting.ts
  router.tsx         router factory + QueryClient
  start.ts           Start config: error middleware + CSRF middleware
  server.ts          SSR fetch wrapper that renders an error page on 500
  styles.css         design tokens, glassmorphism utilities, animations
  routeTree.gen.ts   GENERATED — do not edit
```

There is no `src/data/`, no `src/services/`, no `src/api/`. `src/lib/esg-data.ts` (994 lines) is the single data module.

## 3. Route List

All routes are public, none are lazy-gated, all render inside `AppLayout`.

- `/` — `Dashboard` (`src/routes/index.tsx`, 348 lines). Complete UI. Mock. Readiness dashboard: radial score, 5 animated stat cards, 4 recharts panels, priority-gap table, AI insight list. Links to `/reports`, `/compliance`, `/compliance/$requirementId`.
- `/documents` — `Documents` (`src/routes/documents/index.tsx`). Complete UI. Mock. Stats, document table, upload dialog, view/analyze/delete actions. Links to `/documents/$documentId`, `/compliance`.
- `/documents/$documentId` — `DocumentDetailPage` (316 lines). Complete UI. Mock. Simulated page viewer with prev/next, page Select, zoom 70–150%, tabs Text/Entities/Metrics/History, run-analysis, download, delete. Renders an in-page "Document not found" panel (not a router `notFound()`).
- `/compliance` — `ComplianceAnalysis` (`src/routes/compliance/index.tsx`). Complete UI. Mock. Search box + category/status/priority selects filtering a 14-row requirement table client-side. Links to `/reports/gap-assessment`, `/compliance/$requirementId`.
- `/compliance/$requirementId` — `RequirementDetails` (199 lines). Complete UI. Mock. Uses a real router `loader` with `getRequirement()` and `throw notFound()`; head() derives title from loaderData. Evidence card, AI analysis, gap, recommendation, reviewer actions, comment box, metadata.
- `/frameworks` — `Frameworks`. Complete UI. Mock. 4 framework cards; only BRSR is enabled, GRI/IFRS/ESRS buttons are `disabled`.
- `/frameworks/brsr` — `BrsrRequirements`. Complete UI. Mock. Category tabs over the same 14 requirements; footer text claims "of 56 total".
- `/assistant` — `Assistant` (170 lines). Complete UI, no typing animation currently in the component. Mock. Canned Q&A with citation cards and suggested-question buttons.
- `/reports` — `Reports`. Complete UI. Mock. 4 template cards + generated-reports table. Links to `/reports/gap-assessment`.
- `/reports/gap-assessment` — Gap report preview (209 lines). Complete UI. Mock. Print/download are toasts.
- `/settings` — `SettingsPage` (250 lines). UI only, nothing persists.
- 404 — `NotFoundComponent` in `src/routes/__root.tsx`.

## 4. Navigation

`src/components/app-layout.tsx` (219 lines) is the shell used by every page.

- Sidebar: 7 items (Dashboard, Documents, Compliance Analysis, ESG Frameworks, AI ESG Assistant, Reports, Settings). Active state computed from `useRouterState` pathname — working.
- Collapsible sidebar: working, local `useState`, 72px collapsed with Radix tooltips. Not persisted across reloads.
- Mobile: Sheet drawer via hamburger — working.
- Global search: working, local only (see §15).
- Theme toggle: working, persists to `localStorage` key in `src/components/theme-toggle.tsx`.
- Notifications bell: **placeholder** — icon + red dot, no handler, no dropdown.
- Org switcher chip (`ABC Industries Ltd.` + chevron): **placeholder**, not a menu.
- Profile block (`PN / Priya Nair / ESG Lead`): **placeholder**, hardcoded, no menu, no auth.

## 5. Current User Flow (all transitions work)

Dashboard → "Open Compliance Analysis" → `/compliance` → filter/search → "View Details" → `/compliance/$requirementId` → back.
Dashboard → Documents → click name or "View" → `/documents/$documentId` → "Run Compliance Analysis" (toast + redirect to `/compliance`).
Documents → "Upload Document" → simulated 5-stage pipeline dialog → closing does **not** add a row.
Sidebar → Frameworks → BRSR requirement library.
Sidebar → AI Assistant → click a suggested question → canned answer with citations.
Sidebar → Reports → "Preview" on Gap Assessment → `/reports/gap-assessment`.
Global search → jumps to requirement / document / reports / frameworks.

## 6. Mock Data Audit — everything lives in `src/lib/esg-data.ts`

| Export | Shape | Consumed by |
|---|---|---|
| `ORG` | object literal (name, framework, reportingPeriod, readiness 78, environmental/social/governance, requirementsCovered 42, partiallyCovered 8, evidenceMissing 6, documentsAnalyzed 12) | dashboard, app-layout, compliance, reports, gap report, settings |
| `requirements` (14 items) | `Requirement[]` | `/compliance`, `/compliance/$id`, `/frameworks/brsr`, gap report, globalSearch |
| `priorityGaps` (6) | `as const` literal — duplicates ids/titles/status from `requirements` | dashboard |
| `categoryScores`, `statusDistribution`, `monthlyImprovement`, `gapBreakdown` | chart arrays; counts hardcoded and **not derived** from `requirements` | dashboard |
| `aiInsights` (4) | title/body/impact | dashboard, gap report |
| `documents` (7) | `DocumentRecord[]` | `/documents`, `/documents/$id`, globalSearch |
| `documentDetails` | `Record<string, DocumentDetail>` — only `doc-1` is authored; `getDocumentDetail()` synthesizes generic pages/metrics for the other 6 | `/documents/$id` |
| `frameworks` (4) | untyped literal | `/frameworks`, globalSearch |
| `reportTemplates` (4), `generatedReports` (4) | untyped literals | `/reports`, globalSearch |
| `cannedAnswers` (5), `defaultAnswer`, `suggestedQuestions` (5) | `ChatMessage` | `/assistant` |
| `globalSearch()`, `getRequirement()`, `getDocument()`, `getDocumentDetail()` | synchronous mock "services" | search + detail routes |

Hardcoded inside components (not centralized): user identity `Priya Nair / PN / ESG Lead` and initials `AB` in `app-layout.tsx`; assistant greeting `initial[]` in `assistant.tsx`; upload-pipeline `stages[]` and all upload form options in `upload-dialog.tsx`; settings values (`L12345MH2004PLC145678`, `priya.nair@abcindustries.in`, confidence slider 80, retention 7y) in `settings.tsx`; the "56 total BRSR disclosures" copy in `brsr.tsx`; grounding-sources copy "12 analysed documents · 56 disclosures" in `assistant.tsx`.

## 7. Types / Interfaces (all in `src/lib/esg-data.ts`)

- `Status` (Covered | Partially Covered | Evidence Not Found | Human Review Required), `Priority`, `Category` — union types.
- `Requirement`: id, title, category, status, confidence, priority, `evidence: {document,page,snippet} | null`, frameworkText, analysis, gap, recommendation, mandatory, description, version.
- `DocumentRecord`: id, name, type, year, uploaded (display string, not ISO), status (Analyzed|Processing|Queued), pages, category.
- `DocumentPage`, `DetectedMetric`, `EsgEntity`, `AnalysisEvent`, `DocumentDetail` (pagesContent, extractedText, entities, metrics, history).
- `ChatMessage`: role, content, optional citations[].
- `SearchResult`: group, id, title, subtitle, to, params.

Not typed at all (inferred literals): `ORG`, `frameworks`, `reportTemplates`, `generatedReports`, `priorityGaps`, all chart arrays. `StatusBadge` accepts a plain `string`, so document statuses and framework statuses share the requirement status component.

## 8. Component Architecture

- `AppLayout` (`components/app-layout.tsx`) — props `{title, description?, actions?, children}`. Sidebar + header + page-enter animation. Used by every route. **Preserve.**
- `GlobalSearch` (`components/global-search.tsx`, 77 lines) — no props; calls `globalSearch()`, groups results, navigates. Swap the data source only.
- `StatusBadge`, `PriorityBadge`, `ConfidenceMeter` (`components/status-badge.tsx`) — props `{status: string}` / `{priority}` / `{value:number}`. Used by dashboard, compliance, documents, frameworks, gap report. **Preserve.**
- `RadialScore` (`components/radial-score.tsx`) — `{value, label}`, animated SVG ring. Dashboard.
- `ThemeToggle` — localStorage-backed dark class toggle.
- `UploadDialog` (`components/upload-dialog.tsx`, 228 lines) — `{trigger?}`. Fully simulated pipeline. This is the main file the real upload API will replace internally.
- `useCountUp` (`hooks/use-count-up.ts`) — number animation for stat cards.
- Local, page-scoped components: `StatCard` + `Panel` in `index.tsx`, `Block` in `$requirementId.tsx`, `Citations` in `assistant.tsx`, `Section` in `settings.tsx`.
- `components/ui/*` — unmodified shadcn; `sidebar.tsx` (744 lines) is present but **unused** by the app.

## 9. Dashboard Status

Working: radial readiness ring, five `useCountUp` stat cards, three category progress bars, four recharts panels (bar, donut, line, stacked bar) with themed tooltips, priority-gap table with working `View` links, AI insight list, two header CTAs. Glass panels with hover lift.
Simulated / absent: no filters, no date-range control, no refresh, no loading or skeleton state, no error state. Chart totals (42/8/6/4 = 60) do not reconcile with the 14 requirements actually in the dataset. Counter animation runs on mount only.

## 10. Documents Module

Real frontend logic: table rendering, derived stats, delete confirmation via `AlertDialog` with in-memory `removed[]` state (row disappears until reload), navigation to detail, page/zoom/tab state on the detail page.
Simulated: upload (no `<input type=file>`; "Browse files" sets a fixed filename string, drag-drop reads only `files[0].name`; the 5 stages advance on 1100 ms timers; no row is ever added); "Analyze" = toast + redirect; "Download" = toast; PDF preview is styled text blocks, no PDF renderer; extracted text, entities, metrics and history come from `documentDetails["doc-1"]` or the generic synthesizer.
Files: `routes/documents/index.tsx`, `routes/documents/$documentId.tsx`, `components/upload-dialog.tsx`, `lib/esg-data.ts`.

## 11. Compliance Analysis Module

Works locally: `useMemo` filtering over `requirements` by search text (id+title), category, status, priority; row count "X of 14"; empty-state row when no match; status/priority badges; confidence meter; evidence cell with document + page or "Evidence not found"; detail route loader with `notFound()`; evidence blockquote, AI analysis, gap, recommendation blocks; metadata panel.
Mock/stub: reviewer actions ("Accept AI Assessment", "Mark for Human Review", "Add Comment") are toasts with no state change; comment `Textarea` is uncontrolled and discarded; no pagination, no sorting, no bulk actions.
Supporting structures: `Requirement`, `getRequirement()`.
Endpoints eventually needed (not built): list requirements with assessment results, single requirement detail, accept/override an assessment, post a reviewer comment.

## 12. ESG Framework Module

`frameworks` array in `esg-data.ts` holds 4 entries; only `brsr` is `Active` with `requirements: 56`; GRI, IFRS S1/S2 and ESRS are `Planned` with `requirements: 0` and disabled buttons. `/frameworks/brsr` filters the shared `requirements` array by category tabs — so framework definitions and assessment results are the same 14 objects, which a real backend must split apart.

## 13. AI ESG Assistant

Chat UI, suggested questions, citation cards with requirement + document + page tags and grounding-source panel are all present. Answers come from exact string lookup `cannedAnswers[q] ?? defaultAnswer`, appended synchronously — **no typing indicator or delay is implemented in `assistant.tsx`** (a `typing-dot` utility exists in `styles.css` but is unused). No LLM API, no RAG, no embeddings, no vector store, no retrieval — none of these exist.

## 14. Reports Module

4 template cards, "Generate Report" = success toast only (no modal, no animation, no artifact), "Preview" opens the real preview page for `gap-assessment` and a toast for the other three, "Download PDF" = toast. The generated-reports table is 4 static rows; its download buttons are toasts. `/reports/gap-assessment` is a genuine print-styled document built from `ORG`, `requirements` and `aiInsights`, so it stays consistent with the compliance page; the dashboard's chart counts do not.

## 15. Global Search

`GlobalSearch` calls `globalSearch(query)` in `esg-data.ts`, which does case-insensitive substring matching across requirements (id/title/category/description/status), documents, generated reports and frameworks, capped at 12 results, grouped in a glass dropdown, closing on outside click. Purely local and synchronous. Navigation uses typed `to` + `params`; reports results all land on `/reports`. No keyboard shortcut, no arrow-key navigation.

## 16. Settings

Five sections — Organization Profile, ESG Framework Preferences, AI Analysis Settings (confidence threshold slider, toggles), Document Retention, Notifications. Every field is uncontrolled (`defaultValue` / `defaultChecked`); "Save changes" fires `toast.success("Settings saved")`. **Nothing persists** — the only persisted preference anywhere in the app is the theme.

## 17. Theme / Visual System — all in `src/styles.css`

- Tokens in OKLCH under `:root` and `.dark`. Primary teal `oklch(0.52 0.093 175)` (dark: `0.72 0.11 172`), near-white background `oklch(0.988 0.004 170)`, semantic `success / warning / danger / info` each with a `-soft` variant, 5 chart colors, full sidebar token set.
- Radius `--radius: 0.625rem` with sm→4xl derivations; `--shadow-card`, `--shadow-elevated`.
- Glassmorphism: `--glass-bg/-border/-shadow/-highlight` plus `@utility glass-panel` (blur 16px, saturate 140%), `@utility glass-hover` (lift + primary glow), `@utility surface-card` (solid variant still used by compliance/assistant/reports/settings).
- Ambient background: `@utility ambient-bg`, three fixed radial gradients, rendered once in `AppLayout`.
- Animations: `esg-fade-up` → `@utility page-enter` (keyed on pathname), `esg-dot` → `@utility typing-dot` (unused), plus a `prefers-reduced-motion` reset.
- Dark mode: `.dark` class on `<html>`, toggled and persisted by `ThemeToggle`. No system-preference detection.
- Typography: no custom font is loaded — the Tailwind default stack is used, with `font-feature-settings` tweaks on body.

## 18. State Management

React `useState`/`useMemo` inside page components only. No Context, Zustand or Redux. `QueryClient` exists in `src/router.tsx` and is provided in `__root.tsx`, but no `useQuery`, no `queryOptions`, no route loader uses it. Only `/compliance/$requirementId` uses a router loader (synchronous, from the static array). Everything else is a static ES import. **There is no service/API abstraction layer.**

## 19. Service / API Layer

Searched for `fetch(`, `axios`, `supabase`, `createServerFn`, `import.meta.env`, `process.env`: the only matches are the SSR plumbing in `src/server.ts` and `localStorage` in `theme-toggle.tsx`. **The current project is frontend-only and has no real backend integration.**

## 20. Authentication

None. No login or signup route, no session, no auth provider, no protected routes, no roles. The header user is the hardcoded string "Priya Nair / ESG Lead".

## 21. Database / Supabase

None. Lovable Cloud is not enabled, there is no `src/integrations/`, no `supabase/` folder, no migrations, no schema, no storage, no DB env vars.

## 22. Backend

No backend. No `createServerFn` anywhere, no `src/routes/api/`, no server routes, no server actions. TanStack Start is used purely as an SSR React framework: `src/start.ts` registers an error middleware and a CSRF middleware, and `src/server.ts` wraps the SSR fetch handler to render an error page on 500. Effectively a frontend-only application.

## 23. AI / RAG Status

| Feature | Implemented? | Current state |
|---|---|---|
| PDF text extraction | No | Hardcoded strings in `documentDetails` |
| Text chunking | No | "Splitting document into sections" is a 1.1 s timer label |
| Embeddings | No | Not present |
| Vector database | No | Not present |
| Semantic retrieval | No | Not present |
| RAG | No | Not present |
| LLM API | No | Canned string-map lookup |
| Compliance classification | No | `status` fields authored by hand |
| Evidence extraction | No | `evidence` objects authored by hand |
| Gap detection | No | `gap` strings authored by hand |
| Recommendation generation | No | `recommendation` / `aiInsights` authored by hand |
| Report generation | No | Static JSX page + toasts |

## 24. Interactive Stub Audit

| Page | Action | Current behavior | Intended later |
|---|---|---|---|
| Header (all pages) | Notifications bell | No handler | Notification feed |
| Header | Org chip / profile block | Static markup | Org switch + account menu |
| Documents | Upload dialog | Fake stages, no file input, no row added | `POST /api/documents` multipart + polling |
| Documents / Document detail | Analyze | Toast + redirect to `/compliance` | `POST /api/analyses` |
| Documents | Delete | In-memory removal, lost on reload | `DELETE /api/documents/{id}` |
| Document detail | Download | Toast | Signed file URL |
| Document detail | Delete | Toast + navigate | `DELETE /api/documents/{id}` |
| Requirement detail | Accept AI Assessment / Mark for Human Review / Add Comment | Toasts, textarea discarded | Assessment override + comments API |
| Reports | Generate Report | `toast.success` | `POST /api/reports` + job status |
| Reports | Preview (3 non-gap templates) | Toast | Real preview routes |
| Reports (cards + table) | Download PDF | Toast, no file | `GET /api/reports/{id}/file` |
| Gap assessment | Print / Download | Toasts (no `window.print()`) | Print + PDF export |
| Settings | Save changes | Toast, no persistence | `PUT /api/settings` |
| Frameworks | GRI / IFRS / ESRS buttons | Disabled | Framework detail routes |

## 25. Error / Loading / Empty States

Present: 404 route + error boundary in `__root.tsx`; SSR 500 HTML page (`src/lib/error-page.ts`); empty-filter row on `/compliance`; "No matches for …" in global search; "Document not found" panel on `/documents/$documentId`; `notFound()` on unknown requirement ids; "Evidence not found" states.
Absent: no loading spinners or skeletons anywhere (`components/ui/skeleton.tsx` is unused), no API error handling, no empty-library state for documents (the array is never empty), no failed-analysis state, no upload-failure path, no retry affordances. All of these need to be added with the backend.

## 26. Environment Variables

None. There is no `.env`, `.env.example` or `.dev.vars`, and no code reads `import.meta.env` or `process.env`. Client-side vars must be prefixed `VITE_` when added.

## 27. Build and Run

```sh
git clone <repo> && cd <repo>
bun install          # or: npm install
bun run dev          # http://localhost:8080
bun run build        # production build (nitro/Cloudflare target)
bun run build:dev    # development-mode build
bun run preview
bun run lint         # eslint
bun run format       # prettier --write .
```
Node 20+ recommended; Bun is the project's package manager (`bun.lock`, `bunfig.toml` with a 24 h minimum-release-age supply-chain guard). There is no test runner configured. Dev port is 8080 (set by the Lovable Vite config, not by `vite.config.ts`).

## 28. Build Health (measured, not fixed)

- TypeScript: compiles clean, zero errors.
- Lint: `bun run lint` **fails** — 89 problems (83 errors, 6 warnings). All 83 errors are `prettier/prettier` formatting nits (auto-fixable with `eslint --fix`); the 6 warnings are `react-refresh/only-export-components` in `esg-data.ts`, `sidebar.tsx`, `chart.tsx`, `form.tsx`, `sonner.tsx` and similar mixed-export files.
- Production build: not run as part of this audit (no code changes were made); the dev server compiles and serves all routes.
- Routes: all 11 routes resolve; `routeTree.gen.ts` matches the files on disk.
- Dead code: `components/ui/sidebar.tsx` (744 lines), `skeleton.tsx`, carousel/drawer/otp/calendar and other unused shadcn primitives; the `typing-dot` CSS utility; `@tanstack/react-query`, `react-hook-form`, `zod` are installed and wired at the root but unused by pages. There is a stray triple blank line in `src/routes/index.tsx` around line 201.
- No known runtime console errors on the built routes.

## 29. Files To Preserve

`src/styles.css` (entire design token + glass system), `src/components/app-layout.tsx`, `src/components/status-badge.tsx`, `src/components/radial-score.tsx`, `src/components/global-search.tsx`, `src/components/theme-toggle.tsx`, `src/hooks/use-count-up.ts`, all `src/components/ui/*`, and the JSX bodies of every route file. `src/routeTree.gen.ts` is generated — never hand-edit. `src/server.ts`, `src/start.ts` and `vite.config.ts` are Lovable platform plumbing; leave them alone.

## 30. Backend Integration Points

- Documents (`routes/documents/*`, `upload-dialog.tsx`): `GET /api/documents`, `POST /api/documents` (multipart), `GET /api/documents/{id}`, `GET /api/documents/{id}/pages`, `GET /api/documents/{id}/extraction` (text, entities, metrics), `GET /api/documents/{id}/history`, `GET /api/documents/{id}/file`, `DELETE /api/documents/{id}`.
- Analysis (`Analyze` buttons, `/compliance`): `POST /api/analyses`, `GET /api/analyses/{id}` (status polling), `GET /api/analyses/{id}/requirements`, `GET /api/analyses/{id}/requirements/{reqId}`, `POST /api/analyses/{id}/requirements/{reqId}/review`, `POST /api/.../comments`.
- Frameworks (`/frameworks`, `/frameworks/brsr`): `GET /api/frameworks`, `GET /api/frameworks/{id}/requirements`.
- Dashboard (`/`): `GET /api/dashboard/summary` returning the `ORG` counters, category scores, status distribution, monthly trend, gap breakdown, priority gaps and insights — today these are five unrelated hand-written arrays.
- Assistant (`/assistant`): `POST /api/assistant/query` returning `{content, citations[]}` matching `ChatMessage`; streaming optional.
- Reports (`/reports`): `GET /api/reports`, `POST /api/reports`, `GET /api/reports/{id}`, `GET /api/reports/{id}/file`.
- Settings + auth: `GET/PUT /api/settings`, `GET /api/me`.

## 31. Recommended Integration Order

1. Introduce `src/services/` with typed fetch clients and adopt TanStack Query (already installed) — do this before touching any page.
2. Frameworks + framework requirements (read-only, smallest blast radius; also forces the framework-vs-assessment data split).
3. Documents list + detail (`GET`), then upload (`POST`) and delete.
4. Compliance requirement list, then requirement detail.
5. Reviewer actions (accept / review / comment) — first write path with real state.
6. Dashboard aggregation (depends on real requirement + document counts).
7. Reports list, generation and download.
8. RAG assistant last (needs extraction, chunking, embeddings and retrieval in place).
9. Authentication + roles, then re-scope every endpoint per organization.

## 32. Technical Debt

Critical
- No service layer: pages import mock arrays directly at module scope. Every page must change when APIs arrive.
- Derived data is hand-written and inconsistent: `ORG` says 42/8/6 covered/partial/missing and 12 documents, but the arrays hold 14 requirements and 7 documents; `statusDistribution` totals 60. A real API will visibly change these numbers.
- Framework requirements and assessment results are the same `Requirement` objects — the backend model must separate `framework_requirement` from `assessment_result`, forcing a type split in the UI.
- No auth and no org scoping anywhere.

Medium
- Untyped literals (`ORG`, `frameworks`, `reportTemplates`, `generatedReports`, chart arrays) and `StatusBadge(status: string)` accepting any string.
- Hardcoded ids and identity: `doc-1` is the only document with authored detail; `Priya Nair`, `ABC Industries Ltd.` and the CIN are inline in components.
- Large page components: `index.tsx` 348, `$documentId.tsx` 316, `settings.tsx` 250 lines with data, layout and behavior mixed.
- Dates are display strings ("12 Mar 2026"), not ISO — will not survive an API round-trip without a formatter.
- No loading/error/empty states to attach to async calls.
- Lint fails on formatting; CI would block.

Low
- Unused dependencies and dead shadcn files; unused `typing-dot` utility; assistant lacks the typing animation implied by the CSS; settings/report/reviewer actions all use toasts, so a real failure will look identical to success.

## 33. GitHub / Handoff Readiness

Ready to move. `.gitignore` covers `node_modules`, `dist`, `.output`, `.nitro`, `.tanstack`, `.wrangler`, `.dev.vars`, editor files and logs. `bun.lock` is committed (keep it, or delete it and commit `package-lock.json` if the team standardizes on npm). `src/routeTree.gen.ts` is generated but should stay committed — TanStack Router regenerates it on dev/build. There are no secrets, no `.env` files and no credentials in the repo. Before handoff: replace the Lovable boilerplate `README.md` with real setup notes, add a `.env.example` once the Spring Boot base URL exists, and optionally run `eslint --fix` to clear the 83 formatting errors. Do not commit `dist/`, `.output/`, `.nitro/` or `.wrangler/`.

## 34. Final Summary

CURRENTLY COMPLETE
- 11 routes with production-quality enterprise UI, all navigation working.
- Design system: OKLCH tokens, glassmorphism utilities, ambient gradients, dark mode with persistence, page transitions, reduced-motion support (`src/styles.css`).
- Shared shell (`AppLayout`), badges/confidence meter, radial score, animated counters, collapsible + mobile sidebar.
- Local interactivity: compliance filtering/search, global search, document page viewer with zoom/paging/tabs, delete confirmation, framework tabs, chat with citations.
- TypeScript compiles clean; all routes render.

CURRENTLY MOCKED
- Everything in `src/lib/esg-data.ts`: org profile, readiness scores, 14 requirements, 7 documents, document extraction/entities/metrics/history, 4 frameworks, 4 report templates, 4 generated reports, 5 canned assistant answers, all chart series, and the `globalSearch` / `getRequirement` / `getDocument` "services".
- Upload pipeline, analysis runs, report generation, downloads, reviewer actions, settings save, notifications, user identity.

NOT IMPLEMENTED
- Any backend, server function, API route, database, storage, migration or environment variable.
- Authentication, sessions, roles, protected routes, multi-tenancy.
- PDF extraction, chunking, embeddings, vector search, RAG, LLM calls, real classification/gap detection, PDF report rendering.
- Loading skeletons, API error handling, retries, pagination, sorting.

KNOWN ISSUES
- Mock counters disagree with mock arrays (42/8/6 and 12 documents vs 14 requirements and 7 documents; distribution totals 60).
- Framework requirements and assessment results share one type.
- `bun run lint` fails with 83 auto-fixable prettier errors + 6 fast-refresh warnings.
- Deleted documents and uploads reset on reload; settings never persist.
- Unused deps (react-query, react-hook-form, zod) and dead shadcn files; assistant typing animation defined in CSS but never used.

READY FOR BACKEND?
Yes. The UI is stable, types are mostly centralized in one file, and TanStack Query is already installed and provided at the root, so data fetching can be introduced without restructuring routes. The one prerequisite is inserting a service layer so pages stop importing `esg-data.ts` directly.

FIRST BACKEND TASK
Build the Documents API and cut `/documents` over to it. Concretely: create `src/services/documents.ts` exporting `listDocuments()`, `getDocument(id)`, `uploadDocument(file, meta)` and `deleteDocument(id)` against `GET/POST/GET/DELETE /api/documents[/{id}]`, typed with the existing `DocumentRecord` interface (change `uploaded` to an ISO string and format at the UI edge). Replace the static `documents` import in `src/routes/documents/index.tsx` with `useQuery`, add skeleton and error states to the table, wire the real `<input type="file">` and `POST` into `src/components/upload-dialog.tsx` (replacing the timer-driven `stages` array with backend processing status), and make the delete `AlertDialog` call the mutation plus `invalidateQueries` instead of the local `removed[]` array. Leave `src/lib/esg-data.ts` in place while the other modules still read from it.
