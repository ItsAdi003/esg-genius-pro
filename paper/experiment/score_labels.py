# -*- coding: utf-8 -*-
"""Step 4 of the classification-accuracy experiment: score the model against human labels.

Usage:
  python score_labels.py round1            # uses round1/annotator1.csv, annotator2.csv, sample_key.csv, strata.json
  python score_labels.py round1 --adjudicated round1/adjudicated.csv   # columns: requirement_code,label

The gold label is the adjudicated label; where both annotators agree it is used automatically.
Disagreements without an adjudicated label are listed and the script stops.
Metrics are computed per PREDICTED status (stratified sample), so they are unbiased for each stratum.
Recall is not estimated because requirements the model rated differently were not all labelled.
"""
import argparse
import csv
import json
import math
from pathlib import Path

CLASSES = ["COVERED", "PARTIALLY_COVERED", "NOT_COVERED"]
LABEL_COL = "label (COVERED / PARTIALLY_COVERED / NOT_COVERED)"


def read_labels(path):
    out = {}
    with open(path, newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            lab = (row.get(LABEL_COL) or row.get("label") or "").strip().upper()
            if lab not in CLASSES:
                raise SystemExit(f"{path}: invalid or empty label for {row['requirement_code']!r}: {lab!r}")
            out[row["requirement_code"]] = lab
    return out


def wilson(k, n, z=1.96):
    if n == 0:
        return (float("nan"), float("nan"))
    p = k / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return (max(0.0, c - h), min(1.0, c + h))


def cohen_kappa(a, b):
    codes = sorted(a)
    n = len(codes)
    po = sum(a[c] == b[c] for c in codes) / n
    pe = sum((sum(a[c] == k for c in codes) / n) * (sum(b[c] == k for c in codes) / n) for k in CLASSES)
    return po, (po - pe) / (1 - pe) if pe < 1 else 1.0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("folder")
    ap.add_argument("--adjudicated")
    a = ap.parse_args()
    d = Path(a.folder)

    meta = json.loads((d / "strata.json").read_text(encoding="utf-8"))
    pred = {}
    with open(d / "sample_key.csv", newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            pred[row["requirement_code"]] = row["predicted_status"]
    l1, l2 = read_labels(d / "annotator1.csv"), read_labels(d / "annotator2.csv")
    assert set(l1) == set(l2) == set(pred), "annotator sheets and key cover different requirements"

    agree, kappa = cohen_kappa(l1, l2)
    print(f"Inter-annotator agreement: raw {agree:.3f}, Cohen's kappa {kappa:.3f} (n={len(pred)})")

    adj = read_labels(a.adjudicated) if a.adjudicated else {}
    gold, open_items = {}, []
    for c in sorted(pred):
        if c in adj:
            gold[c] = adj[c]
        elif l1[c] == l2[c]:
            gold[c] = l1[c]
        else:
            open_items.append((c, l1[c], l2[c]))
    if open_items:
        print("\nDisagreements needing adjudication (discuss, then add to adjudicated.csv):")
        for c, x, y in open_items:
            print(f"  {c}: annotator1={x}  annotator2={y}")
        raise SystemExit(1)

    print("\nConfusion matrix (rows = model prediction, columns = gold label), sampled items only")
    print(f"{'':20}" + "".join(f"{k[:9]:>11}" for k in CLASSES))
    for p in CLASSES:
        print(f"{p:20}" + "".join(f"{sum(1 for c in pred if pred[c] == p and gold[c] == g):>11}" for g in CLASSES))

    print("\nPer predicted status (precision with 95% Wilson CI)")
    var, weighted, total_pop = 0.0, 0.0, sum(s["population"] for s in meta["strata"].values())
    rows = {}
    for s in CLASSES:
        st = meta["strata"][s]
        items = [c for c in pred if pred[c] == s]
        n, k = len(items), sum(gold[c] == s for c in items)
        lo, hi = wilson(k, n)
        p = k / n if n else float("nan")
        rows[s] = (st["population"], n, k, p, lo, hi)
        print(f"  {s:18} population={st['population']:3d} labelled={n:3d} correct={k:3d} "
              f"precision={p:.3f} [{lo:.3f}, {hi:.3f}]")
        if n:
            weighted += st["population"] * p
            if n > 1:
                fpc = 1 - n / st["population"]
                var += (st["population"] / total_pop) ** 2 * p * (1 - p) / (n - 1) * fpc

    acc = weighted / total_pop
    se = math.sqrt(var)
    print(f"\nWeighted accuracy over {total_pop} non-fallback requirements: {acc:.3f} "
          f"(95% CI {max(0, acc - 1.96 * se):.3f}-{min(1, acc + 1.96 * se):.3f})")

    cov = [c for c in pred if pred[c] == "COVERED"]
    false_cov = sum(gold[c] != "COVERED" for c in cov)
    lo, hi = wilson(false_cov, len(cov))
    print(f"False-COVERED rate (model COVERED, gold not COVERED): {false_cov}/{len(cov)} = "
          f"{false_cov / len(cov):.3f} [{lo:.3f}, {hi:.3f}]  <- the safety-critical number")
    over = sum(1 for c in pred if pred[c] != "COVERED" and gold[c] == "COVERED")
    print(f"Over-flagged (model PARTIAL/NOT, gold COVERED): {over} of "
          f"{sum(1 for c in pred if pred[c] != 'COVERED')} sampled")
    fb = meta["human_review_fallbacks"]
    print(f"Fallback rate (HUMAN_REVIEW_REQUIRED): {fb}/{meta['total']} = {fb / meta['total']:.3f}")
    print("\nNote: COVERED stratum is a sample, so its precision generalises to all "
          f"{meta['strata']['COVERED']['population']} COVERED results; recall is not estimated.")


if __name__ == "__main__":
    main()
