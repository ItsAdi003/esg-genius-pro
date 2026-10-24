# Classification-accuracy experiment (for paper Section VI-D)

**Question:** when the Gemini step says COVERED / PARTIALLY_COVERED / NOT_COVERED for a BRSR question on a real report, how often does a human reading the whole report agree — and, most importantly, how often does it say COVERED when the report does not actually cover the question?

**Cost:** at most one analysis (15 of the 20 daily free-tier Gemini calls) and about 3 hours of labelling per annotator. Nothing in the code changes.

## Design decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Document | The same Infosys BRSR FY 2024-25 used in the retrieval audit | One document keeps the paper coherent; say "single report" in the limitations |
| Model run | One completed 140-requirement analysis | Quota; also report model name, date and that it is a single non-repeated run |
| Gold labels | 3-class: COVERED / PARTIALLY_COVERED / NOT_COVERED, judged against the **whole PDF**, not only the retrieved passages | Otherwise retrieval misses are hidden and the number overstates quality |
| Rubric for annotators | Requirement title + description + framework text only (same as the model prompt) | Annotators must not add criteria the model was told not to use |
| Annotators | Two authors, independently and **blind** to the model output; shuffled order | Prevents anchoring; kappa measures reliability |
| Sampling | Stratified by the model's predicted status, seeded (42). Default 30 COVERED + 10 PARTIAL + 10 NOT = 50 | COVERED will dominate; stratifying gives usable precision for each status. HUMAN_REVIEW results are reported as a fallback rate, not labelled |
| Disagreements | Both annotators discuss and agree one label (still blind to the model) | Standard adjudication |

## Metrics reported

1. **Precision per predicted status**, with 95% Wilson intervals.
2. **False-COVERED rate** = model said COVERED, gold says not. This is the safety-critical number behind the paper's fail-safe claim.
3. **Over-flagging** = model said PARTIAL/NOT, gold says COVERED. Costs reviewer time but is the safe direction.
4. **Weighted accuracy** over non-fallback requirements, with stratified confidence interval.
5. **Fallback rate** = share of HUMAN_REVIEW_REQUIRED.
6. **Cohen's kappa** between the two annotators.

Recall is not estimated, because requirements the model rated differently are not all labelled. State this.

**Honest limits of n:** with 30 COVERED items and zero errors, the 95% upper bound on the false-COVERED rate is about 11%. With 20 items it is about 16%. Write "no false-COVERED in the sample (95% CI upper bound X%)", never "the model never errs".

## Steps

**0. Check for a free run first (no quota).** Open the Compliance page for the Infosys document and look for an existing *completed* analysis with 140 requirements and few HUMAN_REVIEW results. If one exists, use it and skip step 1.

**1. Run one analysis (only if step 0 found none).**
- Do this on a day you have not used Gemini; do **not** use the assistant or run other analyses that day.
- Confirm the backend uses the free-tier key and the intended model; note `GEMINI_MODEL` and the date for the paper.
- Start the analysis from the UI and wait (about 6 minutes or less with batching).
- If more than ~10% of results are HUMAN_REVIEW with the quota explanation, the run is invalid. Wait until the next day and rerun.

**2. Export the result.** In the browser DevTools Network tab, open the request `GET /api/v1/analyses/<id>`, copy the response body into `paper/experiment/analysis_<id>.json`. Do not copy or commit request headers (they contain your token). Then:

```bash
cd paper/experiment
python make_label_sheet.py analysis_<id>.json --n-covered 30 --n-partial 10 --n-not 10
```

This writes `round1/annotator1.csv`, `round1/annotator2.csv` (blind), `round1/sample_key.csv` (model output; **do not open until labelling is finished**) and `round1/strata.json`.

**3. Label independently.** Each annotator fills `label`, `pages_where_found` and `notes` in their own sheet, reading the PDF. Do not discuss until both are done. Do not open `sample_key.csv`.

**4. Adjudicate and score.**

```bash
python score_labels.py round1
```

It prints kappa and lists disagreements. Resolve them together and record them in `round1/adjudicated.csv` (columns `requirement_code,label`), then:

```bash
python score_labels.py round1 --adjudicated round1/adjudicated.csv
```

**5. Update the paper** (`paper/content.py`, then `python build_paper.py`):
- Add subsection "Classification Accuracy" in Section VI with a table (per-status population, labelled, correct, precision with CI; false-COVERED rate; kappa; fallback rate) and a short error analysis of every wrong case.
- Change the abstract, the Threats to Validity paragraph ("has not yet been measured") and the conclusion/future-work wording accordingly.
- Report: model name, run date, single run, two author annotators, kappa.
- Keep `retrieval_audit.csv`, the labelled sheets and `strata.json` as supplementary material.

## Things that could invalidate the result

- Annotators peeking at model output, or reading only the retrieved passages instead of the PDF.
- A run polluted by quota failures (check the fallback rate).
- Changing prompts, batch size or retrieval after the run. Freeze the code (note the git commit hash in the paper) before step 1.
- Using the 30 + 10 + 10 sample numbers as if they were population recall.

## What is already built and tested

`make_label_sheet.py` and `score_labels.py` were run end to end on synthetic data (random statuses and labels, used only to check that they work; no real result exists yet).
