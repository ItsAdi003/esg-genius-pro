# ESG Genius Pro

Lovable Prompt — ESG Compliance Assistant UI

Build a modern, professional, enterprise-style web application called:

ESGenius — AI-Powered ESG & Sustainability Compliance Assistant

The application helps organizations upload sustainability-related documents, compare them against ESG reporting frameworks such as SEBI BRSR, identify compliance/reporting gaps, retrieve supporting evidence, and generate AI-backed recommendations.

For now, build frontend/UI only using mock data. Do not build any backend, database, authentication logic, or real AI integration yet.

Design Style

Use a clean and professional enterprise SaaS design.

Avoid:

overly futuristic AI visuals

excessive glassmorphism

neon gradients

flashy animations

Prefer:

white/light neutral background

dark text

green/teal accents suitable for sustainability

subtle shadows

rounded cards

clean data tables

professional charts

clear status colors

responsive layout

The app should look suitable for a Final Year Engineering Project as well as a real enterprise ESG/compliance product.

Main Navigation

Create a left sidebar with:

Dashboard

Documents

Compliance Analysis

ESG Frameworks

AI ESG Assistant

Reports

Settings

Top bar should contain:

Search

Notifications

Current organization name

User profile

Use mock organization:

ABC Industries Ltd.

1. Dashboard

Create an ESG overview dashboard.

Top summary cards:

ESG Reporting Readiness Score — 78%

Requirements Covered — 42

Partially Covered — 8

Evidence Missing — 6

Documents Analyzed — 12

Show separate ESG category scores:

Environmental — 72%

Social — 84%

Governance — 81%

Add charts:

ESG category readiness

Compliance status distribution

Monthly compliance improvement

Environmental/Social/Governance gap breakdown

Add a section called:

Priority Compliance Gaps

Example rows:

Scope 3 GHG Emissions — Missing — High Priority

Renewable Energy Breakdown — Partial — Medium Priority

Waste Recycling Data — Partial — Medium Priority

Water Withdrawal Disclosure — Covered — Low Priority

Add an:

AI Insights & Recommendations

card with 3–4 useful sustainability/compliance recommendations.

2. Documents Page

Create a document management page.

Show uploaded documents in a table/cards.

Columns:

Document Name

Type

Reporting Year

Upload Date

Processing Status

Pages

Actions

Example documents:

Sustainability Report FY2025-26

Annual Report 2025-26

Energy Consumption Report

Environmental Policy

Employee Safety Policy

Include buttons:

Upload Document

View

Analyze

Delete

3. Upload Document Page / Modal

Create a professional drag-and-drop upload area.

Supported file labels:

PDF

DOCX

Fields:

Document Name

Document Type

Reporting Year

ESG Category

Upload File

After clicking upload, visually show processing stages:

Uploading document

Extracting text

Splitting document into sections

Preparing document for AI analysis

Ready for compliance analysis

Use simulated/mock progress only.

4. Compliance Analysis Page

This is the MOST IMPORTANT page.

At the top show:

Framework: SEBI BRSR

Organization: ABC Industries Ltd.

Overall ESG Reporting Readiness: 78%

Include filters:

ESG Category

Status

Priority

Search Requirement

Create a detailed table with columns:

Requirement ID

Requirement

ESG Category

Status

Confidence

Evidence

Priority

Action

Use statuses:

Covered

Partially Covered

Evidence Not Found

Human Review Required

Example rows:

ENV-001 — Total Energy Consumption — Environmental — Covered — 96%

ENV-002 — Renewable vs Non-Renewable Energy Breakdown — Environmental — Partially Covered — 87%

ENV-003 — Scope 1 GHG Emissions — Environmental — Covered — 94%

ENV-004 — Scope 2 GHG Emissions — Environmental — Covered — 92%

ENV-005 — Scope 3 GHG Emissions — Environmental — Evidence Not Found — 81%

SOC-001 — Employee Health & Safety — Social — Covered — 93%

GOV-001 — Anti-Corruption Policy — Governance — Covered — 95%

Each row should have a:

View Details

button.

Make the table look enterprise-grade and easy to scan.

5. Requirement Details Page

When a requirement is opened, show a detailed compliance-analysis view.

Example:

Requirement

ENV-002 — Renewable and Non-Renewable Energy Disclosure

Framework:
SEBI BRSR

Category:
Environmental

Status:
Partially Covered

Confidence:
87%

Framework Requirement

Display a mock regulatory requirement paragraph.

Evidence Found

Show:

Sustainability Report FY2025-26

Page 28

“ABC Industries consumed 120,000 kWh of electricity during FY2025-26, of which approximately 30% was supplied from renewable sources.”

Make the evidence appear inside a highlighted citation/evidence card.

AI Analysis

“Total electricity consumption and renewable-energy contribution are disclosed, but a separate value for non-renewable consumption is not clearly provided.”

Gap

“Explicit renewable and non-renewable energy totals are required for complete disclosure.”

Recommendation

“Provide separate annual values for renewable and non-renewable electricity consumption for the reporting period.”

Include buttons:

Mark for Human Review

Accept AI Assessment

Add Comment

6. ESG Frameworks Page

Create a framework-management page.

Show framework cards:

SEBI BRSR

Status: Active
Region: India
Requirements: 56

GRI Standards

Status: Planned
Region: Global

IFRS S1/S2

Status: Planned
Region: Global

ESRS

Status: Planned
Region: European Union

Only SEBI BRSR should appear active for the MVP.

Add:

View Framework Requirements

For BRSR.

7. Framework Requirements Page

Display BRSR requirements in a structured table.

Columns:

Requirement ID

Requirement Name

ESG Category

Description

Mandatory / Optional

Version

Filters:

Environmental

Social

Governance

This page represents the requirements stored in the application's ESG knowledge base.

8. AI ESG Assistant

Create a chat-style interface.

Title:

ESG Compliance Assistant

Example suggested questions:

What ESG requirements are currently missing?

Which environmental disclosures need attention?

Why is our renewable energy disclosure marked partial?

What evidence was found for Scope 1 emissions?

Summarize our top five compliance gaps.

Responses should visually include:

concise AI answer

cited ESG requirement

source document

page number

evidence snippet

Clearly show:

Responses are generated using organization documents and ESG framework sources.

Use mock conversations.

9. Reports Page

Create a reports dashboard.

Available report cards:

ESG Gap Assessment Report

Environmental Compliance Summary

Executive Sustainability Summary

Missing Evidence Report

Provide actions:

Generate Report

Preview

Download PDF

Show previous generated reports in a table.

10. ESG Gap Assessment Report Preview

Create a professional report preview containing:

Organization name

ESG framework

Reporting period

Overall readiness score

Environmental score

Social score

Governance score

Executive summary

Covered requirements

Partial requirements

Missing evidence

High-priority gaps

AI recommendations

Make it look like a real professional compliance assessment report.

11. Settings

Create basic settings sections:

Organization Profile

ESG Framework Preferences

AI Analysis Settings

Document Retention

Notifications

No real functionality required yet.

Mock Data Requirement

Use internally consistent mock data across all pages.

If ENV-005 Scope 3 Emissions is marked “Evidence Not Found” on the Compliance Analysis page, it must also appear as missing everywhere else.

If the overall score is 78%, dashboard/report pages should also show 78%.

Do not randomly generate conflicting data.

Important Product Language

Do NOT say:

“Officially compliant”

or

“AI certifies compliance.”

Use language such as:

ESG Reporting Readiness

Compliance Gap Analysis

Evidence Found

Evidence Not Found

Partially Covered

AI-Assisted Assessment

Human Review Required

The platform supports human compliance professionals and does not replace them.

Final Requirement

Build all these pages with working navigation and professional mock data.

The most polished pages should be:

Dashboard

Document Upload

Compliance Analysis

Requirement Details

AI ESG Assistant

Report Preview

Frontend only for now.

Do not connect Supabase or build a backend yet.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/335a7791-c092-4551-8b1f-701414a2b195).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
