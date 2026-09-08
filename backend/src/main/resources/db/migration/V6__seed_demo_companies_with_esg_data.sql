-- V6: Seed demo companies with prototype ESG rating data
-- 
-- IMPORTANT: All scores, ratings, and events in this migration are ILLUSTRATIVE
-- PROTOTYPE DATA used to validate the ESGenius rating architecture. They are NOT
-- official MSCI or third-party ESG ratings, and do NOT represent real current
-- company performance or events. This is demonstration data only.

-- Delete existing organizations and re-seed with comparison data
DELETE FROM esg_event;
DELETE FROM company_key_issue_assessment;
DELETE FROM esg_rating_snapshot;
DELETE FROM esg_key_issue;
DELETE FROM organization;

-- Seed four demo companies (Indian IT services)
INSERT INTO organization (name, ticker, industry, sector)
VALUES
    ('Infosys Limited', 'INFY', 'Information Technology', 'IT Services'),
    ('Tata Consultancy Services', 'TCS', 'Information Technology', 'IT Services'),
    ('Wipro Limited', 'WPRO', 'Information Technology', 'IT Services'),
    ('HCLTech', 'HCLT', 'Information Technology', 'IT Services');

-- Seed ESG Key Issues (5 material issues)
INSERT INTO esg_key_issue (code, name, pillar, description)
VALUES
    ('CARBON_EMISSIONS', 'Carbon Emissions', 'ENVIRONMENTAL', 'Scope 1, 2, and 3 greenhouse gas emissions'),
    ('HUMAN_CAPITAL', 'Human Capital', 'SOCIAL', 'Employee practices, training, retention, diversity'),
    ('DATA_PRIVACY', 'Data Privacy & Security', 'GOVERNANCE', 'Information security and customer data protection'),
    ('CORP_GOVERNANCE', 'Corporate Governance', 'GOVERNANCE', 'Board independence, ethics, compliance'),
    ('BUSINESS_ETHICS', 'Business Ethics', 'GOVERNANCE', 'Anti-corruption, supply chain integrity, compliance');

-- =============================================================================
-- INFOSYS LIMITED (INFY) - Prototype Rating: AA (7.8 overall)
-- =============================================================================

-- Rating history: Q4 2025 – Q3 2026
INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
VALUES
    (1, 7.5, 7.0, 8.0, 7.8, 'AA', '2025-12-31', 7.2),
    (1, 7.6, 7.1, 8.1, 7.9, 'AA', '2026-03-31', 7.5),
    (1, 7.7, 7.2, 8.2, 8.0, 'AA', '2026-06-30', 7.6),
    (1, 7.8, 7.3, 8.2, 8.0, 'AA', '2026-09-07', 7.7);

-- Key issue assessments for Infosys
INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
VALUES
    (1, 1, 7.5, 'MODERATE', '2026-09-07'),   -- Carbon Emissions: Moderate risk
    (1, 2, 8.4, 'LOW', '2026-09-07'),        -- Human Capital: Low risk (strong practice)
    (1, 3, 7.9, 'LOW', '2026-09-07'),        -- Data Privacy: Low risk
    (1, 4, 8.2, 'LOW', '2026-09-07'),        -- Corporate Governance: Low risk
    (1, 5, 8.0, 'LOW', '2026-09-07');        -- Business Ethics: Low risk

-- Demo ESG events for Infosys
INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
VALUES
    (1, 'Renewable Energy Commitment Announcement', 'Prototype data: Announced renewable energy target of 55% by 2030', 'ENVIRONMENTAL', 'LOW', '2026-08-15', 0.2, TRUE),
    (1, 'Women in Leadership Initiative', 'Prototype data: Launched programme to achieve 40% women in leadership', 'SOCIAL', 'LOW', '2026-07-22', 0.15, TRUE),
    (1, 'Supply Chain ESG Audit Gap Identified', 'Prototype data: Audit identified gaps in Tier 2 supplier assessments', 'ENVIRONMENTAL', 'MEDIUM', '2026-06-10', -0.1, TRUE);

-- =============================================================================
-- TATA CONSULTANCY SERVICES (TCS) - Prototype Rating: A (6.9 overall)
-- =============================================================================

-- Rating history: Q4 2025 – Q3 2026
INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
VALUES
    (2, 7.0, 6.8, 7.2, 7.5, 'A', '2025-12-31', 7.1),
    (2, 6.95, 6.75, 7.3, 7.6, 'A', '2026-03-31', 7.0),
    (2, 6.92, 6.7, 7.4, 7.7, 'A', '2026-06-30', 6.95),
    (2, 6.9, 6.7, 7.4, 7.7, 'A', '2026-09-07', 6.92);

-- Key issue assessments for TCS
INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
VALUES
    (2, 1, 6.2, 'MODERATE', '2026-09-07'),   -- Carbon Emissions: Moderate risk
    (2, 2, 7.5, 'LOW', '2026-09-07'),        -- Human Capital: Low risk
    (2, 3, 7.3, 'LOW', '2026-09-07'),        -- Data Privacy: Low risk (with recent inquiry)
    (2, 4, 7.9, 'LOW', '2026-09-07'),        -- Corporate Governance: Low risk
    (2, 5, 7.4, 'LOW', '2026-09-07');        -- Business Ethics: Low risk

-- Demo ESG events for TCS
INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
VALUES
    (2, 'Net-Zero Carbon Operations Target Announced', 'Prototype data: Extended carbon neutrality goal to 2045', 'ENVIRONMENTAL', 'LOW', '2026-08-01', 0.1, TRUE),
    (2, 'Customer Data Handling Regulatory Inquiry', 'Prototype data: Regulatory inquiry into Q2 2026 data practices', 'GOVERNANCE', 'HIGH', '2026-07-15', -0.25, TRUE),
    (2, 'Diversity Report Published', 'Prototype data: Comprehensive diversity and inclusion report published', 'SOCIAL', 'LOW', '2026-06-28', 0.05, TRUE);

-- =============================================================================
-- WIPRO LIMITED (WPRO) - Prototype Rating: BBB (6.2 overall)
-- =============================================================================

-- Rating history: Q4 2025 – Q3 2026
INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
VALUES
    (3, 6.2, 5.8, 6.4, 6.8, 'BBB', '2025-12-31', 6.1),
    (3, 6.1, 5.7, 6.3, 6.8, 'BBB', '2026-03-31', 6.2),
    (3, 6.15, 5.8, 6.4, 6.9, 'BBB', '2026-06-30', 6.1),
    (3, 6.2, 5.9, 6.5, 6.8, 'BBB', '2026-09-07', 6.15);

-- Key issue assessments for Wipro
INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
VALUES
    (3, 1, 5.4, 'HIGH', '2026-09-07'),       -- Carbon Emissions: High risk
    (3, 2, 6.8, 'MODERATE', '2026-09-07'),   -- Human Capital: Moderate risk
    (3, 3, 6.1, 'MODERATE', '2026-09-07'),   -- Data Privacy: Moderate risk
    (3, 4, 6.9, 'MODERATE', '2026-09-07'),   -- Corporate Governance: Moderate risk
    (3, 5, 6.5, 'MODERATE', '2026-09-07');   -- Business Ethics: Moderate risk

-- Demo ESG events for Wipro
INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
VALUES
    (3, 'ESG Strategy Overhaul Announced', 'Prototype data: New 5-year ESG roadmap with increased environmental targets', 'ENVIRONMENTAL', 'LOW', '2026-08-10', 0.15, TRUE),
    (3, 'Labor Dispute Settlement', 'Prototype data: Resolved Bangalore facility labor dispute with settlement', 'SOCIAL', 'MEDIUM', '2026-07-05', 0.08, TRUE),
    (3, 'Environmental Compliance Violation Fine', 'Prototype data: Regulatory fine for water discharge violations', 'ENVIRONMENTAL', 'HIGH', '2026-06-01', -0.3, TRUE);

-- =============================================================================
-- HCLTECH (HCLT) - Prototype Rating: A (7.1 overall)
-- =============================================================================

-- Rating history: Q4 2025 – Q3 2026
INSERT INTO esg_rating_snapshot (organization_id, overall_score, environmental_score, social_score, governance_score, rating_band, assessment_date, previous_overall_score)
VALUES
    (4, 6.6, 6.3, 6.9, 7.2, 'A', '2025-12-31', 6.5),
    (4, 6.75, 6.5, 7.1, 7.3, 'A', '2026-03-31', 6.6),
    (4, 6.9, 6.7, 7.2, 7.4, 'A', '2026-06-30', 6.75),
    (4, 7.1, 6.8, 7.3, 7.5, 'A', '2026-09-07', 6.9);

-- Key issue assessments for HCLTech
INSERT INTO company_key_issue_assessment (organization_id, key_issue_id, score, risk_level, assessment_date)
VALUES
    (4, 1, 6.8, 'MODERATE', '2026-09-07'),   -- Carbon Emissions: Moderate risk
    (4, 2, 7.6, 'LOW', '2026-09-07'),        -- Human Capital: Low risk
    (4, 3, 7.2, 'LOW', '2026-09-07'),        -- Data Privacy: Low risk
    (4, 4, 7.4, 'LOW', '2026-09-07'),        -- Corporate Governance: Low risk
    (4, 5, 7.3, 'LOW', '2026-09-07');        -- Business Ethics: Low risk

-- Demo ESG events for HCLTech
INSERT INTO esg_event (organization_id, title, description, pillar, severity, event_date, score_impact, is_prototype)
VALUES
    (4, 'First Carbon-Neutral Data Center Commissioned', 'Prototype data: Commissioned first carbon-neutral facility in India', 'ENVIRONMENTAL', 'LOW', '2026-08-08', 0.25, TRUE),
    (4, 'Skills Development Programme Launched', 'Prototype data: INR 50 crore skills programme for underprivileged youth', 'SOCIAL', 'LOW', '2026-07-30', 0.2, TRUE);
