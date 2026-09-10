-- V6: Seed demo companies with prototype ESG rating data
--
-- IMPORTANT: All scores, ratings, and events in this migration are ILLUSTRATIVE
-- PROTOTYPE DATA used to validate the ESGenius rating architecture. They are NOT
-- official MSCI or third-party ESG ratings, and do NOT represent real current
-- company performance or events. This is demonstration data only.
--
-- Preserves the ABC Industries demo organization from V2.
-- Resolves organizations and key issues by stable natural identifiers (ticker, code).

-- Seed four listed comparison companies (Indian IT services)
INSERT INTO organization (name, ticker, industry, sector)
SELECT 'Infosys Limited', 'INFY', 'Information Technology', 'IT Services'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE ticker = 'INFY');

INSERT INTO organization (name, ticker, industry, sector)
SELECT 'Tata Consultancy Services', 'TCS', 'Information Technology', 'IT Services'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE ticker = 'TCS');

INSERT INTO organization (name, ticker, industry, sector)
SELECT 'Wipro Limited', 'WPRO', 'Information Technology', 'IT Services'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE ticker = 'WPRO');

INSERT INTO organization (name, ticker, industry, sector)
SELECT 'HCLTech', 'HCLT', 'Information Technology', 'IT Services'
WHERE NOT EXISTS (SELECT 1 FROM organization WHERE ticker = 'HCLT');

-- Seed ESG Key Issues (5 material issues)
INSERT INTO esg_key_issue (code, name, pillar, description)
SELECT 'CARBON_EMISSIONS', 'Carbon Emissions', 'ENVIRONMENTAL', 'Scope 1, 2, and 3 greenhouse gas emissions'
WHERE NOT EXISTS (SELECT 1 FROM esg_key_issue WHERE code = 'CARBON_EMISSIONS');

INSERT INTO esg_key_issue (code, name, pillar, description)
SELECT 'HUMAN_CAPITAL', 'Human Capital', 'SOCIAL', 'Employee practices, training, retention, diversity'
WHERE NOT EXISTS (SELECT 1 FROM esg_key_issue WHERE code = 'HUMAN_CAPITAL');

INSERT INTO esg_key_issue (code, name, pillar, description)
SELECT 'DATA_PRIVACY', 'Data Privacy & Security', 'GOVERNANCE', 'Information security and customer data protection'
WHERE NOT EXISTS (SELECT 1 FROM esg_key_issue WHERE code = 'DATA_PRIVACY');

INSERT INTO esg_key_issue (code, name, pillar, description)
SELECT 'CORP_GOVERNANCE', 'Corporate Governance', 'GOVERNANCE', 'Board independence, ethics, compliance'
WHERE NOT EXISTS (SELECT 1 FROM esg_key_issue WHERE code = 'CORP_GOVERNANCE');

INSERT INTO esg_key_issue (code, name, pillar, description)
SELECT 'BUSINESS_ETHICS', 'Business Ethics', 'GOVERNANCE', 'Anti-corruption, supply chain integrity, compliance'
WHERE NOT EXISTS (SELECT 1 FROM esg_key_issue WHERE code = 'BUSINESS_ETHICS');

-- =============================================================================
-- INFOSYS LIMITED (INFY) - Prototype Rating: AA (7.8 overall)
-- =============================================================================

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 7.5, 7.0, 8.0, 7.8, 'AA', DATE '2025-12-31', 7.2
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2025-12-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 7.6, 7.1, 8.1, 7.9, 'AA', DATE '2026-03-31', 7.5
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-03-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 7.7, 7.2, 8.2, 8.0, 'AA', DATE '2026-06-30', 7.6
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-06-30'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 7.8, 7.3, 8.2, 8.0, 'AA', DATE '2026-09-07', 7.7
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.5, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'INFY' AND k.code = 'CARBON_EMISSIONS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 8.4, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'INFY' AND k.code = 'HUMAN_CAPITAL'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.9, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'INFY' AND k.code = 'DATA_PRIVACY'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 8.2, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'INFY' AND k.code = 'CORP_GOVERNANCE'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 8.0, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'INFY' AND k.code = 'BUSINESS_ETHICS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Renewable Energy Commitment Announcement', 'Prototype data: Announced renewable energy target of 55% by 2030', 'ENVIRONMENTAL', 'LOW', DATE '2026-08-15', 0.2, TRUE
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Renewable Energy Commitment Announcement'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Women in Leadership Initiative', 'Prototype data: Launched programme to achieve 40% women in leadership', 'SOCIAL', 'LOW', DATE '2026-07-22', 0.15, TRUE
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Women in Leadership Initiative'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Supply Chain ESG Audit Gap Identified', 'Prototype data: Audit identified gaps in Tier 2 supplier assessments', 'ENVIRONMENTAL', 'MEDIUM', DATE '2026-06-10', -0.1, TRUE
FROM organization o
WHERE o.ticker = 'INFY'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Supply Chain ESG Audit Gap Identified'
  );

-- =============================================================================
-- TATA CONSULTANCY SERVICES (TCS) - Prototype Rating: A (6.9 overall)
-- =============================================================================

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 7.0, 6.8, 7.2, 7.5, 'A', DATE '2025-12-31', 7.1
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2025-12-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.95, 6.75, 7.3, 7.6, 'A', DATE '2026-03-31', 7.0
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-03-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.92, 6.7, 7.4, 7.7, 'A', DATE '2026-06-30', 6.95
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-06-30'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.9, 6.7, 7.4, 7.7, 'A', DATE '2026-09-07', 6.92
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 6.2, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'TCS' AND k.code = 'CARBON_EMISSIONS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.5, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'TCS' AND k.code = 'HUMAN_CAPITAL'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.3, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'TCS' AND k.code = 'DATA_PRIVACY'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.9, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'TCS' AND k.code = 'CORP_GOVERNANCE'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.4, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'TCS' AND k.code = 'BUSINESS_ETHICS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Net-Zero Carbon Operations Target Announced', 'Prototype data: Extended carbon neutrality goal to 2045', 'ENVIRONMENTAL', 'LOW', DATE '2026-08-01', 0.1, TRUE
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Net-Zero Carbon Operations Target Announced'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Customer Data Handling Regulatory Inquiry', 'Prototype data: Regulatory inquiry into Q2 2026 data practices', 'GOVERNANCE', 'HIGH', DATE '2026-07-15', -0.25, TRUE
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Customer Data Handling Regulatory Inquiry'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Diversity Report Published', 'Prototype data: Comprehensive diversity and inclusion report published', 'SOCIAL', 'LOW', DATE '2026-06-28', 0.05, TRUE
FROM organization o
WHERE o.ticker = 'TCS'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Diversity Report Published'
  );

-- =============================================================================
-- WIPRO LIMITED (WPRO) - Prototype Rating: BBB (6.2 overall)
-- =============================================================================

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.2, 5.8, 6.4, 6.8, 'BBB', DATE '2025-12-31', 6.1
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2025-12-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.1, 5.7, 6.3, 6.8, 'BBB', DATE '2026-03-31', 6.2
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-03-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.15, 5.8, 6.4, 6.9, 'BBB', DATE '2026-06-30', 6.1
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-06-30'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.2, 5.9, 6.5, 6.8, 'BBB', DATE '2026-09-07', 6.15
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 5.4, 'HIGH', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'WPRO' AND k.code = 'CARBON_EMISSIONS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 6.8, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'WPRO' AND k.code = 'HUMAN_CAPITAL'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 6.1, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'WPRO' AND k.code = 'DATA_PRIVACY'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 6.9, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'WPRO' AND k.code = 'CORP_GOVERNANCE'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 6.5, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'WPRO' AND k.code = 'BUSINESS_ETHICS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'ESG Strategy Overhaul Announced', 'Prototype data: New 5-year ESG roadmap with increased environmental targets', 'ENVIRONMENTAL', 'LOW', DATE '2026-08-10', 0.15, TRUE
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'ESG Strategy Overhaul Announced'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Labor Dispute Settlement', 'Prototype data: Resolved Bangalore facility labor dispute with settlement', 'SOCIAL', 'MEDIUM', DATE '2026-07-05', 0.08, TRUE
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Labor Dispute Settlement'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Environmental Compliance Violation Fine', 'Prototype data: Regulatory fine for water discharge violations', 'ENVIRONMENTAL', 'HIGH', DATE '2026-06-01', -0.3, TRUE
FROM organization o
WHERE o.ticker = 'WPRO'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Environmental Compliance Violation Fine'
  );

-- =============================================================================
-- HCLTECH (HCLT) - Prototype Rating: A (7.1 overall)
-- =============================================================================

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.6, 6.3, 6.9, 7.2, 'A', DATE '2025-12-31', 6.5
FROM organization o
WHERE o.ticker = 'HCLT'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2025-12-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.75, 6.5, 7.1, 7.3, 'A', DATE '2026-03-31', 6.6
FROM organization o
WHERE o.ticker = 'HCLT'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-03-31'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 6.9, 6.7, 7.2, 7.4, 'A', DATE '2026-06-30', 6.75
FROM organization o
WHERE o.ticker = 'HCLT'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-06-30'
  );

INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
SELECT o.id, 7.1, 6.8, 7.3, 7.5, 'A', DATE '2026-09-07', 6.9
FROM organization o
WHERE o.ticker = 'HCLT'
  AND NOT EXISTS (
    SELECT 1 FROM esg_rating_snapshot s
    WHERE s.organization_id = o.id AND s.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 6.8, 'MODERATE', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'HCLT' AND k.code = 'CARBON_EMISSIONS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.6, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'HCLT' AND k.code = 'HUMAN_CAPITAL'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.2, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'HCLT' AND k.code = 'DATA_PRIVACY'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.4, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'HCLT' AND k.code = 'CORP_GOVERNANCE'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
SELECT o.id, k.id, 7.3, 'LOW', DATE '2026-09-07'
FROM organization o
CROSS JOIN esg_key_issue k
WHERE o.ticker = 'HCLT' AND k.code = 'BUSINESS_ETHICS'
  AND NOT EXISTS (
    SELECT 1 FROM company_key_issue_assessment a
    WHERE a.organization_id = o.id AND a.key_issue_id = k.id AND a.assessment_date = DATE '2026-09-07'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'First Carbon-Neutral Data Center Commissioned', 'Prototype data: Commissioned first carbon-neutral facility in India', 'ENVIRONMENTAL', 'LOW', DATE '2026-08-08', 0.25, TRUE
FROM organization o
WHERE o.ticker = 'HCLT'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'First Carbon-Neutral Data Center Commissioned'
  );

INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
SELECT o.id, 'Skills Development Programme Launched', 'Prototype data: INR 50 crore skills programme for underprivileged youth', 'SOCIAL', 'LOW', DATE '2026-07-30', 0.2, TRUE
FROM organization o
WHERE o.ticker = 'HCLT'
  AND NOT EXISTS (
    SELECT 1 FROM esg_event e
    WHERE e.organization_id = o.id AND e.title = 'Skills Development Programme Launched'
  );
