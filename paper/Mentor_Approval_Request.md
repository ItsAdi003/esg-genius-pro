# Mentor approval request — draft (NOT sent)

**To:** Dr. Amit Aylani
**Subject:** Request for review and approval of research paper draft — ESGenius (SEBI BRSR gap analysis)

Respected Sir,

We, the members of the ESGenius project group (Aditya Sinha, Sehaj Alag, Pradnesh Jadhav and Gaurav Deore), have prepared a research paper on our major project in IEEE conference format. We request your review and approval before we submit or publish it anywhere.

**Paper:** *ESGenius: Evidence-Grounded, Human-in-the-Loop Gap Analysis of SEBI BRSR Disclosures Using Lexical Retrieval and Batched LLM Classification* (5 pages; PDF attached).

**Summary:** The system checks an uploaded sustainability report against the 140 questions of SEBI's BRSR format. It retrieves page-cited evidence with a deterministic lexical retriever and uses an LLM only for classification, batched by section (15 model calls instead of 140). Missing evidence is classified as "not covered" without the model, and model failures fall back to "human review required". On the Infosys BRSR FY 2024-25, the top retrieved passage was relevant for 119 of 140 questions (85.0%).

**Points on which we need your decision:**

1. Whether you approve the paper for submission, and if not, the changes you want.
2. Whether you should appear as a co-author or only in the acknowledgment (currently the acknowledgment only).
3. The venue or platform you recommend (conference, journal or arXiv/institutional repository), and whether the institution requires an internal approval or plagiarism/similarity check first.
4. Whether the college's name and the project's source code may be named or linked in the paper.

**Known limitations we have stated in the paper:** classification accuracy against expert labels has not yet been measured; the retrieval audit uses one report and one grader so far.

We will not submit the paper anywhere until we have your written approval.

Thank you for your guidance.

Regards,
Aditya Sinha, Sehaj Alag, Pradnesh Jadhav, Gaurav Deore

---

# Before sending: checklist for the authors

These are things the draft cannot settle by itself. Tick each one before the paper goes to Dr. Aylani.

- [ ] **Affiliation.** Replace `[Department]`, `[Institution]`, `[City]` in `content.py` (or directly in the .docx).
- [ ] **Retrieval audit grades.** The relevant / partial / not-relevant labels in `retrieval_audit.csv` were produced by one rater (the assistant), not by the authors. At least two of you should grade the `top_passage_excerpt` of all 140 rows independently in the empty `grade_by_author_*` columns, compare with the draft grades, and update the numbers in the abstract, Table III and Fig. 2 (or report inter-rater agreement). The paper currently says "single rater"; edit it to reflect who actually graded.
- [ ] **Classification quality.** The paper has no accuracy figure for the LLM step. If you can spend one day of Gemini quota, run one analysis on the same report, have two authors label a sample (e.g. 30 requirements) as correct / incorrect, and add a table. Otherwise leave it as stated future work.
- [ ] **References.** All 12 were checked for existence, but confirm volume, page and DOI details against each original source and add URLs/DOIs where you have them. Add a proper citation for the Infosys report (title, year, URL) in Section VI-A.
- [ ] **Facts that change.** Test counts (370 backend, 124 frontend), 90 commits and the 20 Oct 2026 date were counted on 21 Oct 2026; recount if the code changes (`docs/TESTING.md` has the commands).
- [ ] **Originality / similarity check** required by your college, and the college's own paper template if it mandates one.
- [ ] **Publishing.** Do not post to arXiv, a conference portal or a website until Dr. Aylani approves in writing. A preprint can be hard to withdraw.
