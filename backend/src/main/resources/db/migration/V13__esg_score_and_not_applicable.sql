-- V13: MSCI-style ESG score inputs and reviewer-marked "not applicable" requirements
--
-- compliance_analysis.industry: sector chosen by the reviewer; drives the pillar weight profile and the
--   peer group used for the industry-adjusted score. Null until the reviewer picks one.
-- requirement_assessment.status_before_na / not_applicable_reason: a reviewer can mark a requirement
--   as not applicable (for example, a company that does not use water). The previous status is kept so
--   the decision can be reversed, and the reason is shown in the report.

ALTER TABLE compliance_analysis ADD COLUMN industry VARCHAR(60);

ALTER TABLE requirement_assessment ADD COLUMN status_before_na VARCHAR(30);
ALTER TABLE requirement_assessment ADD COLUMN not_applicable_reason TEXT;
