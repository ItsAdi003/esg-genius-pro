# -*- coding: utf-8 -*-
"""Step 2 of the classification-accuracy experiment: build blind labelling sheets.

Input : the JSON returned by GET /api/v1/analyses/{id} for ONE completed 140-requirement analysis.
Output (in --out):
  annotator1.csv, annotator2.csv  blind sheets (no model output, shuffled differently per annotator)
  sample_key.csv                  model status / confidence / evidence pages per sampled requirement
  strata.json                     population and sample size per predicted status (needed for scoring)

Sampling is stratified by the model's predicted status and seeded, so it can be reproduced.
HUMAN_REVIEW_REQUIRED results are not sampled: they are fallbacks, reported as a rate.
"""
import argparse
import csv
import json
import random
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
V12 = HERE.parents[1] / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V12__brsr_full_framework.sql"
STRATA = ["COVERED", "PARTIALLY_COVERED", "NOT_COVERED"]


def load_requirement_text():
    """code -> (description, framework_text) parsed from the V12 seed."""
    sql = V12.read_text(encoding="utf-8")
    pat = re.compile(r"'([ABP][0-9]*-[A-Z]?[0-9]+)',\s*'((?:[^']|'')*)',\s*'[A-Z]+',\s*"
                     r"'((?:[^']|'')*)',\s*'((?:[^']|'')*)',")
    return {m.group(1): (m.group(3).replace("''", "'"), m.group(4).replace("''", "'"))
            for m in pat.finditer(sql)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("analysis_json")
    ap.add_argument("--out", default=str(HERE / "round1"))
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--n-covered", type=int, default=20)
    ap.add_argument("--n-partial", type=int, default=10)
    ap.add_argument("--n-not", type=int, default=10)
    a = ap.parse_args()

    data = json.loads(Path(a.analysis_json).read_text(encoding="utf-8"))
    if data.get("status") != "COMPLETED":
        raise SystemExit(f"Analysis status is {data.get('status')}, expected COMPLETED")
    assessments = data["assessments"]
    texts = load_requirement_text()

    by_status = {}
    for r in assessments:
        by_status.setdefault(r["assessmentStatus"], []).append(r)
    quotas = dict(zip(STRATA, [a.n_covered, a.n_partial, a.n_not]))

    rng = random.Random(a.seed)
    sample, strata = [], {}
    for s in STRATA:
        pop = sorted(by_status.get(s, []), key=lambda r: r["requirementCode"])
        k = min(quotas[s], len(pop))
        chosen = rng.sample(pop, k)
        sample += chosen
        strata[s] = {"population": len(pop), "sampled": k}
    fallbacks = len(by_status.get("HUMAN_REVIEW_REQUIRED", []))
    strata_meta = {"analysis_id": data["id"], "total": len(assessments), "seed": a.seed,
                   "human_review_fallbacks": fallbacks, "strata": strata,
                   "other_statuses": {k: len(v) for k, v in by_status.items()
                                      if k not in STRATA and k != "HUMAN_REVIEW_REQUIRED"}}

    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    (out / "strata.json").write_text(json.dumps(strata_meta, indent=2), encoding="utf-8")

    with open(out / "sample_key.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["requirement_code", "predicted_status", "confidence", "evidence_pages", "explanation"])
        for r in sorted(sample, key=lambda r: r["requirementCode"]):
            pages = ",".join(str(c.get("pageNumber")) for c in (r.get("evidenceChunks") or []))
            w.writerow([r["requirementCode"], r["assessmentStatus"], r.get("confidence"), pages,
                        (r.get("explanation") or "").replace("\n", " ")])

    header = ["requirement_code", "title", "description", "framework_text",
              "label (COVERED / PARTIALLY_COVERED / NOT_COVERED)", "pages_where_found", "notes"]
    for n in (1, 2):
        order = sample[:]
        random.Random(a.seed * 10 + n).shuffle(order)
        with open(out / f"annotator{n}.csv", "w", newline="", encoding="utf-8-sig") as f:
            w = csv.writer(f)
            w.writerow(header)
            for r in order:
                desc, fw = texts.get(r["requirementCode"], ("", ""))
                w.writerow([r["requirementCode"], r["requirementTitle"], desc, fw, "", "", ""])
    print(json.dumps(strata_meta, indent=2))
    print(f"Wrote {len(sample)} sampled requirements to {out}")


if __name__ == "__main__":
    main()
