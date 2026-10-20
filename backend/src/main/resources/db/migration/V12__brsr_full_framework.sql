-- V12: Complete SEBI BRSR requirement set (Annexure I format)
--
-- Loads every numbered disclosure question of the BRSR format: Section A (24 general disclosures),
-- Section B (12 management and process questions) and Section C (62 Essential and 42 Leadership
-- indicators across Principles 1-9) = 140 requirements. Source: SEBI BRSR format, Annexure I.
-- Later SEBI revisions (BRSR Core, value-chain disclosures) are not included.
--
-- The 14 MVP requirements seeded in V4 are kept (existing analyses reference them) but marked
-- inactive: new analyses, framework listings and requirement counts use active requirements only.

ALTER TABLE framework_requirement ADD COLUMN section VARCHAR(10);
ALTER TABLE framework_requirement ADD COLUMN indicator_type VARCHAR(20);
ALTER TABLE framework_requirement ADD COLUMN sort_order INT;
ALTER TABLE framework_requirement ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE framework_requirement SET active = FALSE
 WHERE framework_id = (SELECT id FROM framework WHERE code = 'BRSR');

UPDATE framework SET version = 'SEBI BRSR format (Annexure I)' WHERE code = 'BRSR';

INSERT INTO framework_requirement
  (framework_id, requirement_code, title, category, description, framework_text, mandatory, version,
   section, indicator_type, sort_order, active)
VALUES
  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-01', 'Corporate Identity Number (CIN)', 'GOVERNANCE',
   'Corporate Identity Number (CIN) of the listed entity.',
   'The entity shall disclose its Corporate Identity Number (CIN).',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 1, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-02', 'Name of the Listed Entity', 'GOVERNANCE',
   'Registered name of the listed entity.',
   'The entity shall disclose its name.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 2, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-03', 'Year of Incorporation', 'GOVERNANCE',
   'Year in which the listed entity was incorporated.',
   'The entity shall disclose its year of incorporation.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 3, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-04', 'Registered Office Address', 'GOVERNANCE',
   'Address of the registered office of the listed entity.',
   'The entity shall disclose the address of its registered office.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 4, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-05', 'Corporate Address', 'GOVERNANCE',
   'Corporate address of the listed entity.',
   'The entity shall disclose its corporate address.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 5, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-06', 'Entity E-mail', 'GOVERNANCE',
   'E-mail address of the listed entity.',
   'The entity shall disclose its e-mail address.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 6, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-07', 'Entity Telephone', 'GOVERNANCE',
   'Telephone number of the listed entity.',
   'The entity shall disclose its telephone number.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 7, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-08', 'Entity Website', 'GOVERNANCE',
   'Website address of the listed entity.',
   'The entity shall disclose its website address.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 8, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-09', 'Financial Year Reported', 'GOVERNANCE',
   'Financial year for which reporting is being done.',
   'The entity shall state the financial year for which reporting is being done.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 9, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-10', 'Stock Exchanges Where Listed', 'GOVERNANCE',
   'Names of the stock exchange(s) where the entity''s shares are listed.',
   'The entity shall disclose the name of the stock exchange(s) where its shares are listed.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 10, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-11', 'Paid-up Capital', 'GOVERNANCE',
   'Paid-up capital of the listed entity.',
   'The entity shall disclose its paid-up capital.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 11, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-12', 'Contact Person for BRSR Queries', 'GOVERNANCE',
   'Name and contact details (telephone, e-mail) of the person who may be contacted for queries on the BRSR report.',
   'The entity shall disclose the name and contact details (telephone, e-mail address) of the person who may be contacted in case of any queries on the BRSR report.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 12, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-13', 'Reporting Boundary (Standalone or Consolidated)', 'GOVERNANCE',
   'Whether disclosures are made on a standalone basis (entity only) or a consolidated basis (entity and all entities in its consolidated financial statements).',
   'The entity shall state whether the disclosures in the report are made on a standalone basis (only the entity) or on a consolidated basis (the entity and all entities forming part of its consolidated financial statements).',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 13, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-14', 'Details of Business Activities (90% of Turnover)', 'GOVERNANCE',
   'Main business activities accounting for 90% of the entity''s turnover, with percentage of turnover.',
   'The entity shall disclose details of business activities accounting for 90% of turnover: the description of the main activity, the description of the business activity, and the percentage of turnover of the entity.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 14, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-15', 'Products and Services Sold (90% of Turnover)', 'GOVERNANCE',
   'Products or services sold by the entity accounting for 90% of its turnover, with NIC code and share of turnover.',
   'The entity shall disclose the products/services sold that account for 90% of its turnover: the product/service, its NIC code, and the percentage of total turnover contributed.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 15, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-16', 'Number of Plants, Offices and Operating Locations', 'GOVERNANCE',
   'Number of locations where plants, operations or offices are situated, nationally and internationally.',
   'The entity shall disclose the number of locations where its plants and/or operations/offices are situated: the number of plants, number of offices and total, for national and international locations.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 16, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-17', 'Markets Served by the Entity', 'GOVERNANCE',
   'Markets served: number of national and international locations, export share of turnover and types of customers.',
   'The entity shall disclose the markets it serves: (a) the number of locations (national, by number of States; international, by number of countries), (b) the contribution of exports as a percentage of total turnover, and (c) a brief on the types of customers.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 17, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-18', 'Employees and Workers (Including Differently Abled)', 'SOCIAL',
   'Number of permanent and other-than-permanent employees and workers at year end, by gender, including differently abled employees and workers.',
   'The entity shall disclose, as at the end of the financial year, the number of employees and workers (permanent, other than permanent, and total) with the male and female count and percentage, and separately the same details for differently abled employees and workers.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 18, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-19', 'Participation of Women in Board and Key Management', 'SOCIAL',
   'Representation of women on the Board of Directors and among Key Management Personnel.',
   'The entity shall disclose the participation/inclusion/representation of women: the total number and the number and percentage of females for the Board of Directors and for Key Management Personnel.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 19, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-20', 'Turnover Rate of Permanent Employees and Workers', 'SOCIAL',
   'Employee and worker turnover rate by gender, trend for the past three financial years.',
   'The entity shall disclose the turnover rate (male, female, total) for permanent employees and permanent workers for the current financial year, the previous financial year and the year prior to the previous financial year.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 20, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-21', 'Holding, Subsidiary, Associate Companies and Joint Ventures', 'GOVERNANCE',
   'Names of holding, subsidiary, associate companies and joint ventures, ownership share and participation in business responsibility initiatives.',
   'The entity shall disclose the names of its holding, subsidiary, associate companies and joint ventures, whether each is a holding/subsidiary/associate/joint venture, the percentage of shares held by the listed entity, and whether the company participates in the business responsibility initiatives of the listed entity (Yes/No).',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 21, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-22', 'CSR Applicability, Turnover and Net Worth', 'SOCIAL',
   'Whether Corporate Social Responsibility applies under section 135 of the Companies Act, 2013, with turnover and net worth.',
   'The entity shall disclose whether CSR is applicable as per section 135 of the Companies Act, 2013 (Yes/No), its turnover (in Rs.) and its net worth (in Rs.).',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 22, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-23', 'Stakeholder Complaints and Grievances on the Nine Principles', 'GOVERNANCE',
   'Complaints received by stakeholder group (communities, investors, shareholders, employees and workers, customers, value chain partners) with grievance redressal mechanism.',
   'The entity shall disclose, for complaints/grievances on any of the Principles 1 to 9 of the NGRBC, by stakeholder group (communities, investors other than shareholders, shareholders, employees and workers, customers, value chain partners, other): whether a grievance redressal mechanism is in place (Yes/No, with web-link to the grievance redress policy), the number of complaints filed during the year, the number pending resolution at close of the year, and remarks, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 23, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'A-24', 'Material Responsible Business Conduct Issues', 'GOVERNANCE',
   'Material environmental and social issues that present a risk or an opportunity, with rationale, approach to mitigate and financial implications.',
   'The entity shall provide an overview of its material responsible business conduct and sustainability issues pertaining to environmental and social matters that present a risk or an opportunity: the material issue identified, whether it is a risk or opportunity (R/O), the rationale for identifying it, the approach to adapt or mitigate the risk, and the financial implications of the risk or opportunity (positive or negative).',
   TRUE, 'BRSR-FULL', 'A', 'GENERAL', 24, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-01', 'Policy Coverage of NGRBC Principles', 'GOVERNANCE',
   'Whether policies cover each NGRBC principle and its core elements, Board approval and web links, for each of Principles 1 to 9.',
   'The entity shall disclose, for each Principle 1 to 9: (a) whether its policy/policies cover each principle and its core elements of the NGRBCs (Yes/No), (b) whether the policy has been approved by the Board (Yes/No), and (c) the web link of the policies, if available.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 25, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-02', 'Translation of Policies into Procedures', 'GOVERNANCE',
   'Whether the entity has translated its policies into procedures, for each principle.',
   'The entity shall state, for each Principle 1 to 9, whether it has translated the policy into procedures (Yes/No).',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 26, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-03', 'Extension of Policies to Value Chain Partners', 'GOVERNANCE',
   'Whether the entity''s policies extend to its value chain partners, for each principle.',
   'The entity shall state, for each Principle 1 to 9, whether the enlisted policies extend to its value chain partners (Yes/No).',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 27, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-04', 'Codes, Certifications, Labels and Standards Adopted', 'GOVERNANCE',
   'National and international codes, certifications, labels and standards adopted and mapped to each principle.',
   'The entity shall disclose the names of the national and international codes/certifications/labels/standards (for example Forest Stewardship Council, Fairtrade, Rainforest Alliance, Trustea, SA 8000, OHSAS, ISO, BIS) adopted by the entity and mapped to each principle.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 28, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-05', 'Specific Commitments, Goals and Targets', 'GOVERNANCE',
   'Specific commitments, goals and targets set by the entity with defined timelines.',
   'The entity shall disclose the specific commitments, goals and targets set by the entity with defined timelines, if any.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 29, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-06', 'Performance Against Commitments, Goals and Targets', 'GOVERNANCE',
   'Performance against the specific commitments, goals and targets, with reasons where targets are not met.',
   'The entity shall disclose its performance against the specific commitments, goals and targets, along with reasons in case the same are not met.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 30, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-07', 'Director''s Statement on ESG Challenges, Targets and Achievements', 'GOVERNANCE',
   'Statement by the director responsible for the business responsibility report on ESG challenges, targets and achievements.',
   'The entity shall provide a statement by the director responsible for the business responsibility report, highlighting ESG related challenges, targets and achievements.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 31, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-08', 'Highest Authority Responsible for Business Responsibility Policies', 'GOVERNANCE',
   'Details of the highest authority responsible for implementation and oversight of the business responsibility policy(ies).',
   'The entity shall disclose details of the highest authority responsible for implementation and oversight of the Business Responsibility policy(ies).',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 32, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-09', 'Board Committee or Director Responsible for Sustainability Decisions', 'GOVERNANCE',
   'Whether a specified Committee of the Board or Director is responsible for sustainability decisions, with details.',
   'The entity shall state whether it has a specified Committee of the Board/Director responsible for decision making on sustainability related issues (Yes/No) and, if yes, provide details.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 33, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-10', 'Review of NGRBC Performance and Compliance', 'GOVERNANCE',
   'Review by a Director or Board Committee of performance against policies and statutory compliance, with frequency, for each principle.',
   'The entity shall disclose the details of review of the NGRBCs: for the subjects (performance against policies and follow-up action; compliance with statutory requirements of relevance to the principles and rectification of any non-compliances), whether the review was undertaken by a Director/Committee of the Board/any other Committee, and the frequency (annually/half yearly/quarterly/any other), for each Principle 1 to 9.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 34, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-11', 'Independent Assessment of Policies by an External Agency', 'GOVERNANCE',
   'Whether an external agency independently assessed the working of the entity''s policies, with the agency name.',
   'The entity shall state, for each Principle 1 to 9, whether it has carried out an independent assessment/evaluation of the working of its policies by an external agency (Yes/No) and, if yes, the name of the agency.',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 35, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'B-12', 'Reasons Where Principles Are Not Covered by a Policy', 'GOVERNANCE',
   'Reasons stated where not all principles are covered by a policy, such as materiality, lack of resources or planned adoption.',
   'If not all Principles are covered by a policy, the entity shall state the reasons for each principle: the entity does not consider the principle material to its business; it is not at a stage to formulate and implement policies on the specified principles; it does not have the financial/human and technical resources available; it is planned to be done in the next financial year; any other reason (please specify).',
   TRUE, 'BRSR-FULL', 'B', 'MANAGEMENT', 36, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E01', 'Training and Awareness Programmes on the Principles', 'GOVERNANCE',
   'Training and awareness programmes held for the Board of Directors, KMPs, employees and workers, with topics and percentage of persons covered.',
   'The entity shall disclose, for the Board of Directors, Key Managerial Personnel, employees other than BoD and KMPs, and workers: the total number of training and awareness programmes held, the topics/principles covered under the training and its impact, and the percentage of persons in the respective category covered by the programmes.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 37, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E02', 'Fines, Penalties, Settlements and Compounding Fees', 'GOVERNANCE',
   'Fines, penalties, punishment, settlement amounts and compounding fees paid in proceedings with regulators, law enforcement agencies or courts.',
   'The entity shall disclose fines/penalties/punishment/award/compounding fees/settlement amounts paid in proceedings (by the entity or by directors/KMPs) with regulators, law enforcement agencies or judicial institutions in the financial year, on the basis of materiality under Regulation 30 of SEBI LODR: for monetary actions (penalty/fine, settlement, compounding fee) the NGRBC principle, name of the regulatory/enforcement agency/judicial institution, amount in INR, brief of the case and whether an appeal has been preferred; for non-monetary actions (imprisonment, punishment) the NGRBC principle, name of the agency/institution, brief of the case and whether an appeal has been preferred.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 38, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E03', 'Appeals and Revisions Against Monetary or Non-Monetary Action', 'GOVERNANCE',
   'Details of appeal or revision preferred where monetary or non-monetary action was challenged.',
   'The entity shall provide, for the instances disclosed in the fines/penalties question, the details of the appeal/revision preferred in cases where monetary or non-monetary action has been appealed: the case details and the name of the regulatory/enforcement agencies/judicial institutions.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 39, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E04', 'Anti-Corruption or Anti-Bribery Policy', 'GOVERNANCE',
   'Whether the entity has an anti-corruption or anti-bribery policy, with brief details and a web link.',
   'The entity shall state whether it has an anti-corruption or anti-bribery policy (Yes/No) and, if yes, provide details in brief and, if available, a web-link to the policy.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 40, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E05', 'Disciplinary Action for Bribery or Corruption', 'GOVERNANCE',
   'Number of directors, KMPs, employees and workers against whom law enforcement agencies took disciplinary action for bribery or corruption.',
   'The entity shall disclose the number of Directors, KMPs, employees and workers against whom disciplinary action was taken by any law enforcement agency for the charges of bribery/corruption, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 41, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E06', 'Complaints on Conflict of Interest', 'GOVERNANCE',
   'Number of complaints received regarding conflict of interest of Directors and KMPs, with remarks.',
   'The entity shall disclose the number of complaints received in relation to issues of conflict of interest of the Directors and of the KMPs, with remarks, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 42, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-E07', 'Corrective Action on Corruption and Conflict of Interest Cases', 'GOVERNANCE',
   'Corrective action taken or underway on fines, penalties or regulator action in cases of corruption and conflicts of interest.',
   'The entity shall provide details of any corrective action taken or underway on issues related to fines/penalties/action taken by regulators/law enforcement agencies/judicial institutions, on cases of corruption and conflicts of interest.',
   TRUE, 'BRSR-FULL', 'P1', 'ESSENTIAL', 43, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-L01', 'Awareness Programmes for Value Chain Partners', 'GOVERNANCE',
   'Awareness programmes on the principles held for value chain partners, with topics and share of partners covered.',
   'The entity shall disclose the awareness programmes conducted for value chain partners on any of the Principles during the financial year: the total number of awareness programmes held, the topics/principles covered under the training, and the percentage of value chain partners covered (by value of business done with such partners) under the awareness programmes.',
   FALSE, 'BRSR-FULL', 'P1', 'LEADERSHIP', 44, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P1-L02', 'Processes to Avoid or Manage Board Conflicts of Interest', 'GOVERNANCE',
   'Whether the entity has processes in place to avoid or manage conflicts of interest involving members of the Board.',
   'The entity shall state whether it has processes in place to avoid/manage conflict of interests involving members of the Board (Yes/No) and, if yes, provide details of the same.',
   FALSE, 'BRSR-FULL', 'P1', 'LEADERSHIP', 45, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-E01', 'R&D and Capex Investment in Sustainable Technologies', 'ENVIRONMENTAL',
   'Percentage of research and development and capital expenditure invested in technologies that improve environmental and social impacts.',
   'The entity shall disclose the percentage of R&D and capital expenditure (capex) investments in specific technologies to improve the environmental and social impacts of products and processes to total R&D and capex investments, respectively, for the current and previous financial year, with details of the improvements in environmental and social impacts.',
   TRUE, 'BRSR-FULL', 'P2', 'ESSENTIAL', 46, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-E02', 'Sustainable Sourcing Procedures', 'ENVIRONMENTAL',
   'Whether procedures for sustainable sourcing exist and the percentage of inputs sourced sustainably.',
   'The entity shall state (a) whether it has procedures in place for sustainable sourcing (Yes/No) and (b) if yes, what percentage of inputs were sourced sustainably.',
   TRUE, 'BRSR-FULL', 'P2', 'ESSENTIAL', 47, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-E03', 'Reclaiming Products at End of Life', 'ENVIRONMENTAL',
   'Processes to safely reclaim products for reuse, recycling and disposal, for plastics, e-waste, hazardous waste and other waste.',
   'The entity shall describe the processes in place to safely reclaim its products for reusing, recycling and disposing at the end of life, for (a) plastics (including packaging), (b) e-waste, (c) hazardous waste and (d) other waste.',
   TRUE, 'BRSR-FULL', 'P2', 'ESSENTIAL', 48, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-E04', 'Extended Producer Responsibility (EPR)', 'ENVIRONMENTAL',
   'Whether Extended Producer Responsibility applies and whether the waste collection plan matches the EPR plan filed with Pollution Control Boards.',
   'The entity shall state whether Extended Producer Responsibility (EPR) is applicable to its activities (Yes/No); if yes, whether the waste collection plan is in line with the EPR plan submitted to Pollution Control Boards; and if not, the steps taken to address the same.',
   TRUE, 'BRSR-FULL', 'P2', 'ESSENTIAL', 49, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-L01', 'Life Cycle Assessments of Products or Services', 'ENVIRONMENTAL',
   'Whether life cycle perspective assessments were conducted for products or services, with boundary, independent assessment and public results.',
   'The entity shall state whether it has conducted Life Cycle Perspective/Assessments (LCA) for any of its products (manufacturing) or services (service industry) and, if yes, provide: the NIC code, name of product/service, percentage of total turnover contributed, boundary for which the LCA was conducted, whether it was conducted by an independent external agency (Yes/No), and whether the results were communicated in the public domain (Yes/No) with the web-link.',
   FALSE, 'BRSR-FULL', 'P2', 'LEADERSHIP', 50, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-L02', 'Significant Social or Environmental Risks from Products or Services', 'ENVIRONMENTAL',
   'Significant social or environmental concerns or risks from production or disposal of products and services, with mitigation action.',
   'The entity shall describe any significant social or environmental concerns and/or risks arising from production or disposal of its products/services, as identified in the Life Cycle Perspective/Assessments or through any other means, with the name of the product/service, the description of the risk/concern, and the action taken to mitigate the same.',
   FALSE, 'BRSR-FULL', 'P2', 'LEADERSHIP', 51, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-L03', 'Recycled or Reused Input Material', 'ENVIRONMENTAL',
   'Percentage of recycled or reused input material to total material used in production or services.',
   'The entity shall disclose the percentage of recycled or reused input material to total material (by value) used in production (manufacturing industry) or providing services (service industry), by input material, for the current and previous financial year.',
   FALSE, 'BRSR-FULL', 'P2', 'LEADERSHIP', 52, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-L04', 'Products and Packaging Reclaimed at End of Life', 'ENVIRONMENTAL',
   'Quantity of products and packaging reclaimed at end of life that were reused, recycled and safely disposed.',
   'The entity shall disclose, of the products and packaging reclaimed at end of life, the amount (in metric tonnes) reused, recycled and safely disposed, for plastics (including packaging), e-waste, hazardous waste and other waste, for the current and previous financial year.',
   FALSE, 'BRSR-FULL', 'P2', 'LEADERSHIP', 53, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P2-L05', 'Reclaimed Products as Percentage of Products Sold', 'ENVIRONMENTAL',
   'Reclaimed products and their packaging as a percentage of products sold for each product category.',
   'The entity shall disclose the reclaimed products and their packaging materials as a percentage of total products sold in the respective category, for each product category.',
   FALSE, 'BRSR-FULL', 'P2', 'LEADERSHIP', 54, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E01', 'Well-being Measures for Employees and Workers', 'SOCIAL',
   'Share of permanent and other-than-permanent employees and workers covered by health insurance, accident insurance, maternity and paternity benefits and day care facilities.',
   'The entity shall disclose the measures for the well-being of employees and of workers: the total number and the number and percentage covered by health insurance, accident insurance, maternity benefits, paternity benefits and day care facilities, by gender (male, female) and for permanent and other-than-permanent employees and workers.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 55, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E02', 'Retirement Benefits (PF, Gratuity, ESI)', 'SOCIAL',
   'Retirement benefits such as provident fund, gratuity and ESI: coverage of employees and workers and deposit with the authority.',
   'The entity shall disclose details of retirement benefits (PF, gratuity, ESI, others) for the current and previous financial year: the number of employees covered as a percentage of total employees, the number of workers covered as a percentage of total workers, and whether the amount was deducted and deposited with the authority (Y/N/N.A.).',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 56, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E03', 'Accessibility of Workplaces for Differently Abled Persons', 'SOCIAL',
   'Whether premises and offices are accessible to differently abled employees and workers under the Rights of Persons with Disabilities Act, 2016.',
   'The entity shall state whether its premises/offices are accessible to differently abled employees and workers, as per the requirements of the Rights of Persons with Disabilities Act, 2016, and, if not, whether any steps are being taken in this regard.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 57, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E04', 'Equal Opportunity Policy', 'SOCIAL',
   'Whether the entity has an equal opportunity policy under the Rights of Persons with Disabilities Act, 2016, with a web link.',
   'The entity shall state whether it has an equal opportunity policy as per the Rights of Persons with Disabilities Act, 2016 and, if so, provide a web-link to the policy.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 58, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E05', 'Parental Leave Return-to-Work and Retention Rates', 'SOCIAL',
   'Return to work and retention rates of permanent employees and workers who took parental leave, by gender.',
   'The entity shall disclose the return to work rate and the retention rate of permanent employees and permanent workers that took parental leave, by gender (male, female, total).',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 59, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E06', 'Grievance Redressal Mechanism for Employees and Workers', 'SOCIAL',
   'Whether a mechanism exists to receive and redress grievances of permanent and other-than-permanent employees and workers.',
   'The entity shall state whether a mechanism is available to receive and redress grievances for permanent workers, other than permanent workers, permanent employees and other than permanent employees (Yes/No) and, if yes, give details of the mechanism in brief.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 60, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E07', 'Membership in Associations or Unions', 'SOCIAL',
   'Employees and workers who are members of associations or unions recognised by the entity.',
   'The entity shall disclose the membership of employees and workers in association(s) or unions recognised by the listed entity: the total permanent employees and permanent workers (male, female) in the respective category, the number who are part of association(s) or union, and the percentage, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 61, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E08', 'Training Given to Employees and Workers', 'SOCIAL',
   'Training on health and safety measures and skill upgradation provided to employees and workers by gender.',
   'The entity shall disclose the details of training given to employees and workers by gender (male, female, total): the total, and the number and percentage trained on health and safety measures and on skill upgradation, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 62, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E09', 'Performance and Career Development Reviews', 'SOCIAL',
   'Employees and workers who received regular performance and career development reviews, by gender.',
   'The entity shall disclose the details of performance and career development reviews of employees and workers: the total, and the number and percentage reviewed, by gender (male, female, total), for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 63, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E10', 'Occupational Health and Safety Management System', 'SOCIAL',
   'Occupational health and safety management system coverage, hazard identification, hazard reporting and access to healthcare.',
   'The entity shall disclose its health and safety management system: (a) whether an occupational health and safety management system has been implemented (Yes/No) and its coverage, (b) the processes used to identify work-related hazards and assess risks on a routine and non-routine basis, (c) whether it has processes for workers to report work-related hazards and remove themselves from such risks (Y/N), and (d) whether employees/workers have access to non-occupational medical and healthcare services (Yes/No).',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 64, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E11', 'Safety Incidents (LTIFR, Injuries, Fatalities)', 'SOCIAL',
   'Lost time injury frequency rate, recordable injuries, fatalities and high-consequence injuries for employees and workers.',
   'The entity shall disclose safety related incidents for employees and workers for the current and previous financial year: the Lost Time Injury Frequency Rate (LTIFR) per one million-person hours worked, total recordable work-related injuries, the number of fatalities, and high consequence work-related injury or ill-health (excluding fatalities).',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 65, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E12', 'Measures for a Safe and Healthy Workplace', 'SOCIAL',
   'Measures taken to ensure a safe and healthy work place.',
   'The entity shall describe the measures taken to ensure a safe and healthy work place.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 66, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E13', 'Complaints on Working Conditions and Health and Safety', 'SOCIAL',
   'Number of complaints by employees and workers on working conditions and on health and safety, with pending status.',
   'The entity shall disclose the number of complaints made by employees and workers on working conditions and on health and safety: filed during the year, pending resolution at the end of the year, and remarks, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 67, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E14', 'Assessment of Plants for Health and Safety and Working Conditions', 'SOCIAL',
   'Percentage of plants and offices assessed for health and safety practices and working conditions.',
   'The entity shall disclose the percentage of its plants and offices that were assessed (by the entity or statutory authorities or third parties) for health and safety practices and for working conditions.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 68, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-E15', 'Corrective Action on Safety Incidents and Assessment Findings', 'SOCIAL',
   'Corrective action taken or underway on safety-related incidents and significant risks from health and safety assessments.',
   'The entity shall provide details of any corrective action taken or underway to address safety-related incidents (if any) and significant risks/concerns arising from assessments of health and safety practices and working conditions.',
   TRUE, 'BRSR-FULL', 'P3', 'ESSENTIAL', 69, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-L01', 'Life Insurance or Compensation on Death of Employees and Workers', 'SOCIAL',
   'Whether the entity extends life insurance or a compensatory package in the event of death of employees and workers.',
   'The entity shall state whether it extends any life insurance or any compensatory package in the event of death of (A) employees (Y/N) and (B) workers (Y/N).',
   FALSE, 'BRSR-FULL', 'P3', 'LEADERSHIP', 70, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-L02', 'Statutory Dues Deducted and Deposited by Value Chain Partners', 'SOCIAL',
   'Measures to ensure statutory dues have been deducted and deposited by value chain partners.',
   'The entity shall describe the measures undertaken to ensure that statutory dues have been deducted and deposited by the value chain partners.',
   FALSE, 'BRSR-FULL', 'P3', 'LEADERSHIP', 71, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-L03', 'Rehabilitation After High-Consequence Injury or Fatality', 'SOCIAL',
   'Employees and workers affected by high-consequence injury, ill-health or fatalities who were rehabilitated and placed in suitable employment.',
   'The entity shall provide the number of employees/workers having suffered high consequence work-related injury/ill-health/fatalities who have been rehabilitated and placed in suitable employment or whose family members have been placed in suitable employment: the total number affected and the number rehabilitated/placed, for employees and workers, for the current and previous financial year.',
   FALSE, 'BRSR-FULL', 'P3', 'LEADERSHIP', 72, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-L04', 'Transition Assistance Programs for Retirement or Termination', 'SOCIAL',
   'Whether transition assistance programs support continued employability and career endings from retirement or termination.',
   'The entity shall state whether it provides transition assistance programs to facilitate continued employability and the management of career endings resulting from retirement or termination of employment (Yes/No).',
   FALSE, 'BRSR-FULL', 'P3', 'LEADERSHIP', 73, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-L05', 'Assessment of Value Chain Partners on Health and Safety and Working Conditions', 'SOCIAL',
   'Percentage of value chain partners assessed for health and safety practices and working conditions.',
   'The entity shall disclose the percentage of value chain partners (by value of business done with such partners) that were assessed for health and safety practices and for working conditions.',
   FALSE, 'BRSR-FULL', 'P3', 'LEADERSHIP', 74, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P3-L06', 'Corrective Action from Value Chain Partner Assessments (Health and Safety)', 'SOCIAL',
   'Corrective actions on significant risks from health and safety and working conditions assessments of value chain partners.',
   'The entity shall provide details of any corrective actions taken or underway to address significant risks/concerns arising from assessments of health and safety practices and working conditions of value chain partners.',
   FALSE, 'BRSR-FULL', 'P3', 'LEADERSHIP', 75, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P4-E01', 'Process for Identifying Key Stakeholder Groups', 'SOCIAL',
   'Processes for identifying the entity''s key stakeholder groups.',
   'The entity shall describe the processes for identifying key stakeholder groups of the entity.',
   TRUE, 'BRSR-FULL', 'P4', 'ESSENTIAL', 76, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P4-E02', 'Key Stakeholder Groups and Engagement Frequency', 'SOCIAL',
   'Key stakeholder groups with channels of communication, frequency and purpose of engagement and whether vulnerable or marginalized.',
   'The entity shall list the stakeholder groups identified as key for the entity and, for each, whether it is identified as a vulnerable and marginalized group (Yes/No), the channels of communication, the frequency of engagement (annually/half yearly/quarterly/others), and the purpose and scope of engagement including key topics and concerns raised during such engagement.',
   TRUE, 'BRSR-FULL', 'P4', 'ESSENTIAL', 77, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P4-L01', 'Consultation Between Stakeholders and the Board', 'SOCIAL',
   'Processes for consultation between stakeholders and the Board on economic, environmental and social topics.',
   'The entity shall describe the processes for consultation between stakeholders and the Board on economic, environmental and social topics or, if consultation is delegated, how feedback from such consultations is provided to the Board.',
   FALSE, 'BRSR-FULL', 'P4', 'LEADERSHIP', 78, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P4-L02', 'Stakeholder Consultation Used to Manage Environmental and Social Topics', 'SOCIAL',
   'Whether stakeholder consultation supports identification and management of environmental and social topics, with examples of inputs incorporated.',
   'The entity shall state whether stakeholder consultation is used to support the identification and management of environmental and social topics (Yes/No) and, if so, provide details of instances as to how the inputs received from stakeholders on these topics were incorporated into policies and activities of the entity.',
   FALSE, 'BRSR-FULL', 'P4', 'LEADERSHIP', 79, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P4-L03', 'Engagement With Vulnerable or Marginalized Stakeholder Groups', 'SOCIAL',
   'Instances of engagement with and actions taken to address concerns of vulnerable or marginalized stakeholder groups.',
   'The entity shall provide details of instances of engagement with, and actions taken to, address the concerns of vulnerable/marginalized stakeholder groups.',
   FALSE, 'BRSR-FULL', 'P4', 'LEADERSHIP', 80, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E01', 'Human Rights Training for Employees and Workers', 'SOCIAL',
   'Employees and workers who received training on human rights issues and policies, permanent and other than permanent.',
   'The entity shall disclose the employees and workers who have been provided training on human rights issues and policy(ies) of the entity: for permanent and other-than-permanent employees and workers, the total, and the number and percentage covered, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 81, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E02', 'Minimum Wages Paid to Employees and Workers', 'SOCIAL',
   'Employees and workers paid equal to or more than the minimum wage, by gender and permanent or other-than-permanent status.',
   'The entity shall disclose details of minimum wages paid to employees and workers: by permanent/other than permanent and by male/female, the total, and the number and percentage paid equal to the minimum wage and more than the minimum wage, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 82, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E03', 'Median Remuneration of Board, KMPs, Employees and Workers', 'SOCIAL',
   'Number and median remuneration, salary or wages of Board members, KMPs, employees and workers, by gender.',
   'The entity shall disclose details of remuneration/salary/wages: for the Board of Directors (BoD), Key Managerial Personnel, employees other than BoD and KMP, and workers, the number and the median remuneration/salary/wages of the respective category, for male and female.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 83, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E04', 'Focal Point for Human Rights Issues', 'SOCIAL',
   'Whether an individual or committee is responsible for addressing human rights impacts or issues.',
   'The entity shall state whether it has a focal point (individual/committee) responsible for addressing human rights impacts or issues caused or contributed to by the business (Yes/No).',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 84, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E05', 'Internal Mechanisms to Redress Human Rights Grievances', 'SOCIAL',
   'Internal mechanisms in place to redress grievances related to human rights issues.',
   'The entity shall describe the internal mechanisms in place to redress grievances related to human rights issues.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 85, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E06', 'Human Rights Complaints by Employees and Workers', 'SOCIAL',
   'Complaints on sexual harassment, discrimination, child labour, forced labour, wages and other human rights issues, with pending status.',
   'The entity shall disclose the number of complaints made by employees and workers on sexual harassment, discrimination at workplace, child labour, forced labour/involuntary labour, wages, and other human rights related issues: filed during the year, pending resolution at the end of the year, and remarks, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 86, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E07', 'Protection of Complainants in Discrimination and Harassment Cases', 'SOCIAL',
   'Mechanisms to prevent adverse consequences to the complainant in discrimination and harassment cases.',
   'The entity shall describe the mechanisms to prevent adverse consequences to the complainant in discrimination and harassment cases.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 87, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E08', 'Human Rights Requirements in Business Agreements and Contracts', 'SOCIAL',
   'Whether human rights requirements form part of the entity''s business agreements and contracts.',
   'The entity shall state whether human rights requirements form part of its business agreements and contracts (Yes/No).',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 88, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E09', 'Assessment of Plants and Offices on Human Rights Issues', 'SOCIAL',
   'Percentage of plants and offices assessed for child labour, forced labour, sexual harassment, discrimination and wages.',
   'The entity shall disclose the percentage of its plants and offices that were assessed (by the entity or statutory authorities or third parties) for child labour, forced/involuntary labour, sexual harassment, discrimination at workplace, wages, and others (please specify).',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 89, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-E10', 'Corrective Action on Human Rights Assessment Findings', 'SOCIAL',
   'Corrective actions taken or underway on significant risks or concerns arising from human rights assessments.',
   'The entity shall provide details of any corrective actions taken or underway to address significant risks/concerns arising from the human rights assessments of its plants and offices.',
   TRUE, 'BRSR-FULL', 'P5', 'ESSENTIAL', 90, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-L01', 'Business Processes Modified After Human Rights Grievances', 'SOCIAL',
   'Business processes modified or introduced as a result of addressing human rights grievances or complaints.',
   'The entity shall provide details of a business process being modified/introduced as a result of addressing human rights grievances/complaints.',
   FALSE, 'BRSR-FULL', 'P5', 'LEADERSHIP', 91, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-L02', 'Human Rights Due Diligence Scope and Coverage', 'SOCIAL',
   'Scope and coverage of any human rights due diligence conducted by the entity.',
   'The entity shall provide details of the scope and coverage of any human rights due diligence conducted.',
   FALSE, 'BRSR-FULL', 'P5', 'LEADERSHIP', 92, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-L03', 'Premises Accessible to Differently Abled Visitors', 'SOCIAL',
   'Whether the premises or offices are accessible to differently abled visitors under the Rights of Persons with Disabilities Act, 2016.',
   'The entity shall state whether its premises/offices are accessible to differently abled visitors, as per the requirements of the Rights of Persons with Disabilities Act, 2016.',
   FALSE, 'BRSR-FULL', 'P5', 'LEADERSHIP', 93, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-L04', 'Assessment of Value Chain Partners on Human Rights Issues', 'SOCIAL',
   'Percentage of value chain partners assessed for sexual harassment, discrimination, child labour, forced labour and wages.',
   'The entity shall disclose the percentage of value chain partners (by value of business done with such partners) that were assessed for sexual harassment, discrimination at workplace, child labour, forced labour/involuntary labour, wages, and others (please specify).',
   FALSE, 'BRSR-FULL', 'P5', 'LEADERSHIP', 94, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P5-L05', 'Corrective Action from Value Chain Human Rights Assessments', 'SOCIAL',
   'Corrective actions on significant risks or concerns from human rights assessments of value chain partners.',
   'The entity shall provide details of any corrective actions taken or underway to address significant risks/concerns arising from the assessments of value chain partners on human rights issues.',
   FALSE, 'BRSR-FULL', 'P5', 'LEADERSHIP', 95, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E01', 'Total Energy Consumption and Energy Intensity', 'ENVIRONMENTAL',
   'Total electricity, fuel and other energy consumption in joules and energy intensity per rupee of turnover.',
   'The entity shall disclose total energy consumption (in joules or multiples) and energy intensity for the current and previous financial year: total electricity consumption (A), total fuel consumption (B), energy consumption through other sources (C), total energy consumption (A+B+C), energy intensity per rupee of turnover, and optionally energy intensity by a relevant metric selected by the entity; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency (Y/N) and its name.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 96, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E02', 'Designated Consumers under the PAT Scheme', 'ENVIRONMENTAL',
   'Whether sites are designated consumers under the Performance, Achieve and Trade (PAT) scheme and whether targets were achieved.',
   'The entity shall state whether it has any sites/facilities identified as designated consumers (DCs) under the Performance, Achieve and Trade (PAT) Scheme of the Government of India (Y/N); if yes, whether targets set under the PAT scheme have been achieved; and, in case targets have not been achieved, the remedial action taken, if any.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 97, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E03', 'Water Withdrawal, Consumption and Intensity', 'ENVIRONMENTAL',
   'Water withdrawal by source, total water withdrawal and consumption in kilolitres, and water intensity per rupee of turnover.',
   'The entity shall disclose water details for the current and previous financial year: water withdrawal by source in kilolitres (surface water, groundwater, third party water, seawater/desalinated water, others), total volume of water withdrawal, total volume of water consumption, water intensity per rupee of turnover (water consumed/turnover), and optionally water intensity by a relevant metric; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 98, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E04', 'Zero Liquid Discharge Mechanism', 'ENVIRONMENTAL',
   'Whether a mechanism for Zero Liquid Discharge has been implemented, with coverage and implementation details.',
   'The entity shall state whether it has implemented a mechanism for Zero Liquid Discharge and, if yes, provide details of its coverage and implementation.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 99, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E05', 'Air Emissions Other Than GHG (NOx, SOx, PM)', 'ENVIRONMENTAL',
   'Air emissions other than greenhouse gases: NOx, SOx, particulate matter, persistent organic pollutants, VOC and hazardous air pollutants.',
   'The entity shall disclose details of air emissions (other than GHG emissions) for the current and previous financial year, with the unit: NOx, SOx, particulate matter (PM), persistent organic pollutants (POP), volatile organic compounds (VOC), hazardous air pollutants (HAP), and others (please specify); and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 100, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E06', 'Scope 1 and Scope 2 GHG Emissions and Intensity', 'ENVIRONMENTAL',
   'Total Scope 1 and Scope 2 greenhouse gas emissions in tonnes of CO2 equivalent and intensity per rupee of turnover.',
   'The entity shall disclose greenhouse gas emissions and intensity for the current and previous financial year: total Scope 1 emissions in metric tonnes of CO2 equivalent (with break-up of the GHG into CO2, CH4, N2O, HFCs, PFCs, SF6, NF3, if available), total Scope 2 emissions in metric tonnes of CO2 equivalent (with the same break-up, if available), total Scope 1 and Scope 2 emissions per rupee of turnover, and optionally emission intensity by a relevant metric; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 101, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E07', 'Projects to Reduce Greenhouse Gas Emissions', 'ENVIRONMENTAL',
   'Whether the entity has any project related to reducing greenhouse gas emissions, with details.',
   'The entity shall state whether it has any project related to reducing greenhouse gas emissions (Yes/No) and, if yes, provide details.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 102, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E08', 'Waste Generated, Recovered and Disposed', 'ENVIRONMENTAL',
   'Waste generated by category in metric tonnes and recovered through recycling and re-use or disposed by incineration and landfilling.',
   'The entity shall disclose waste management details for the current and previous financial year: total waste generated in metric tonnes by category (plastic waste, e-waste, bio-medical waste, construction and demolition waste, battery waste, radioactive waste, other hazardous waste, other non-hazardous waste) and total; for each category of waste generated, the waste recovered through recycling, re-using or other recovery operations; for each category, the waste disposed by incineration, landfilling or other disposal operations; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 103, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E09', 'Waste Management Practices and Hazardous Chemicals Strategy', 'ENVIRONMENTAL',
   'Waste management practices and the strategy to reduce hazardous and toxic chemicals in products and processes.',
   'The entity shall briefly describe the waste management practices adopted in its establishments, the strategy adopted to reduce usage of hazardous and toxic chemicals in its products and processes, and the practices adopted to manage such wastes.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 104, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E10', 'Operations in or Near Ecologically Sensitive Areas', 'ENVIRONMENTAL',
   'Operations or offices in or around ecologically sensitive areas such as national parks, wildlife sanctuaries and wetlands where environmental clearances are required.',
   'The entity shall specify details of operations/offices in/around ecologically sensitive areas (such as national parks, wildlife sanctuaries, biosphere reserves, wetlands, biodiversity hotspots, forests, coastal regulation zones) where environmental approvals/clearances are required: the location of operations/offices, type of operations, and whether the conditions of the environmental approval/clearance are being complied with (Y/N), and if no, the reasons and corrective action taken.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 105, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E11', 'Environmental Impact Assessments of Projects', 'ENVIRONMENTAL',
   'Environmental impact assessments of projects undertaken in the current financial year, with notification number, date and results in the public domain.',
   'The entity shall disclose details of environmental impact assessments of projects undertaken based on applicable laws, in the current financial year: the name and brief details of the project, the EIA notification number, the date, whether conducted by an independent external agency (Yes/No), whether the results were communicated in the public domain (Yes/No), and the relevant web link.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 106, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-E12', 'Compliance With Environmental Laws and Regulations', 'ENVIRONMENTAL',
   'Whether the entity complies with applicable environmental laws such as the Water Act, Air Act and Environment Protection Act, with non-compliance details.',
   'The entity shall state whether it is compliant with the applicable environmental law/regulations/guidelines in India, such as the Water (Prevention and Control of Pollution) Act, Air (Prevention and Control of Pollution) Act, Environment Protection Act and rules thereunder (Y/N); and, if not, provide details of all such non-compliances: the law/regulation/guidelines not complied with, details of the non-compliance, any fines/penalties/action taken by regulatory agencies such as pollution control boards or by courts, and corrective action taken, if any.',
   TRUE, 'BRSR-FULL', 'P6', 'ESSENTIAL', 107, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L01', 'Energy Consumption From Renewable and Non-Renewable Sources', 'ENVIRONMENTAL',
   'Energy consumed from renewable and non-renewable sources, split into electricity, fuel and other sources.',
   'The entity shall provide the break-up of the total energy consumed (in joules or multiples) from renewable and non-renewable sources for the current and previous financial year: from renewable sources, total electricity consumption (A), total fuel consumption (B), energy consumption through other sources (C) and total energy consumed from renewable sources (A+B+C); from non-renewable sources, total electricity consumption (D), total fuel consumption (E), energy consumption through other sources (F) and total energy consumed from non-renewable sources (D+E+F); and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 108, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L02', 'Water Discharge by Destination and Level of Treatment', 'ENVIRONMENTAL',
   'Water discharged in kilolitres by destination (surface water, groundwater, seawater, third parties) and level of treatment.',
   'The entity shall provide details related to water discharged for the current and previous financial year: water discharge by destination and level of treatment in kilolitres (to surface water, to groundwater, to seawater, sent to third-parties, others), each with no treatment or with treatment (specifying the level of treatment), and total water discharged; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 109, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L03', 'Water Withdrawal, Consumption and Discharge in Water-Stressed Areas', 'ENVIRONMENTAL',
   'Water withdrawal, consumption and discharge for each facility located in areas of water stress.',
   'The entity shall provide, for each facility/plant located in areas of water stress: the name of the area, the nature of operations, and water withdrawal by source (surface water, groundwater, third party water, seawater/desalinated water, others), total water withdrawal, total water consumption, water intensity, and water discharge by destination and level of treatment, in kilolitres, for the current and previous financial year; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 110, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L04', 'Scope 3 GHG Emissions and Intensity', 'ENVIRONMENTAL',
   'Total Scope 3 greenhouse gas emissions in tonnes of CO2 equivalent and intensity per rupee of turnover.',
   'The entity shall disclose total Scope 3 emissions and intensity for the current and previous financial year: total Scope 3 emissions in metric tonnes of CO2 equivalent (with break-up of the GHG into CO2, CH4, N2O, HFCs, PFCs, SF6, NF3, if available), total Scope 3 emissions per rupee of turnover, and optionally Scope 3 emission intensity by a relevant metric; and indicate whether any independent assessment/evaluation/assurance was carried out by an external agency.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 111, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L05', 'Biodiversity Impact in Ecologically Sensitive Areas', 'ENVIRONMENTAL',
   'Significant direct and indirect impact on biodiversity in ecologically sensitive areas, with prevention and remediation activities.',
   'With respect to the ecologically sensitive areas reported in the essential indicators, the entity shall provide details of its significant direct and indirect impact on biodiversity in such areas along with prevention and remediation activities.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 112, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L06', 'Resource Efficiency and Emission Reduction Initiatives', 'ENVIRONMENTAL',
   'Specific initiatives or innovative technology used to improve resource efficiency or reduce emissions, effluent discharge or waste, with outcomes.',
   'The entity shall provide details of any specific initiatives undertaken or innovative technology or solutions used to improve resource efficiency, or reduce impact due to emissions/effluent discharge/waste generated, as well as the outcome of such initiatives: the initiative undertaken, the details of the initiative (web-link, if any, may be provided along with the summary), and the outcome of the initiative.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 113, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L07', 'Business Continuity and Disaster Management Plan', 'ENVIRONMENTAL',
   'Whether the entity has a business continuity and disaster management plan, with details.',
   'The entity shall state whether it has a business continuity and disaster management plan and give details in 100 words or a web link.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 114, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L08', 'Significant Environmental Impact from the Value Chain', 'ENVIRONMENTAL',
   'Significant adverse environmental impact arising from the value chain and the mitigation or adaptation measures taken.',
   'The entity shall disclose any significant adverse impact to the environment arising from the value chain of the entity, and the mitigation or adaptation measures taken by the entity in this regard.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 115, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P6-L09', 'Value Chain Partners Assessed for Environmental Impacts', 'ENVIRONMENTAL',
   'Percentage of value chain partners, by value of business, assessed for environmental impacts.',
   'The entity shall disclose the percentage of value chain partners (by value of business done with such partners) that were assessed for environmental impacts.',
   FALSE, 'BRSR-FULL', 'P6', 'LEADERSHIP', 116, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P7-E01', 'Trade and Industry Chamber Affiliations', 'GOVERNANCE',
   'Number of affiliations with trade and industry chambers or associations and the top ten, with their reach.',
   'The entity shall disclose (a) the number of affiliations with trade and industry chambers/associations and (b) the top 10 trade and industry chambers/associations (determined based on the total members of such body) the entity is a member of/affiliated to, with the name of each and its reach (State/National).',
   TRUE, 'BRSR-FULL', 'P7', 'ESSENTIAL', 117, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P7-E02', 'Corrective Action on Anti-Competitive Conduct', 'GOVERNANCE',
   'Corrective action taken on issues related to anti-competitive conduct based on adverse orders from regulatory authorities.',
   'The entity shall provide details of corrective action taken or underway on any issues related to anti-competitive conduct by the entity, based on adverse orders from regulatory authorities: the name of the authority, the brief of the case, and the corrective action taken.',
   TRUE, 'BRSR-FULL', 'P7', 'ESSENTIAL', 118, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P7-L01', 'Public Policy Positions Advocated', 'GOVERNANCE',
   'Public policy positions advocated by the entity, the method used, public availability, Board review frequency and web link.',
   'The entity shall provide details of public policy positions advocated: the public policy advocated, the method resorted to for such advocacy, whether the information is available in the public domain (Yes/No), the frequency of review by the Board (annually/half yearly/quarterly/others), and the web link, if available.',
   FALSE, 'BRSR-FULL', 'P7', 'LEADERSHIP', 119, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-E01', 'Social Impact Assessments of Projects', 'SOCIAL',
   'Social impact assessments of projects in the current financial year, with notification number, independence and public availability of results.',
   'The entity shall disclose details of Social Impact Assessments (SIA) of projects undertaken based on applicable laws, in the current financial year: the name and brief details of the project, the SIA notification number, the date of notification, whether conducted by an independent external agency (Yes/No), whether the results were communicated in the public domain (Yes/No), and the relevant web link.',
   TRUE, 'BRSR-FULL', 'P8', 'ESSENTIAL', 120, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-E02', 'Rehabilitation and Resettlement (R&R) Projects', 'SOCIAL',
   'Projects with ongoing rehabilitation and resettlement, the families affected, share covered and amounts paid.',
   'The entity shall provide information on project(s) for which ongoing Rehabilitation and Resettlement (R&R) is being undertaken: the name of the project, state, district, the number of Project Affected Families (PAFs), the percentage of PAFs covered by R&R, and the amounts paid to PAFs in the financial year (in INR).',
   TRUE, 'BRSR-FULL', 'P8', 'ESSENTIAL', 121, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-E03', 'Community Grievance Redressal Mechanisms', 'SOCIAL',
   'Mechanisms to receive and redress grievances of the community.',
   'The entity shall describe the mechanisms to receive and redress grievances of the community.',
   TRUE, 'BRSR-FULL', 'P8', 'ESSENTIAL', 122, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-E04', 'Input Material Sourced from MSMEs and Local Suppliers', 'SOCIAL',
   'Percentage of input material sourced directly from MSMEs or small producers and from within the district and neighbouring districts.',
   'The entity shall disclose the percentage of input material (inputs to total inputs by value) sourced from suppliers: directly sourced from MSMEs/small producers, and sourced directly from within the district and neighbouring districts, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P8', 'ESSENTIAL', 123, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-L01', 'Actions to Mitigate Negative Social Impacts from SIAs', 'SOCIAL',
   'Actions taken to mitigate negative social impacts identified in the social impact assessments.',
   'The entity shall provide details of actions taken to mitigate any negative social impacts identified in the Social Impact Assessments: the negative social impact identified and the corrective action taken.',
   FALSE, 'BRSR-FULL', 'P8', 'LEADERSHIP', 124, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-L02', 'CSR Projects in Aspirational Districts', 'SOCIAL',
   'CSR projects undertaken in designated aspirational districts, with state, district and amount spent.',
   'The entity shall provide information on CSR projects undertaken in designated aspirational districts identified by government bodies: the state, the aspirational district, and the amount spent (in INR).',
   FALSE, 'BRSR-FULL', 'P8', 'LEADERSHIP', 125, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-L03', 'Preferential Procurement from Marginalized or Vulnerable Groups', 'SOCIAL',
   'Preferential procurement policy favouring suppliers from marginalized or vulnerable groups and its share of total procurement.',
   'The entity shall state (a) whether it has a preferential procurement policy giving preference to purchase from suppliers comprising marginalized/vulnerable groups (Yes/No), (b) from which marginalized/vulnerable groups it procures, and (c) what percentage of total procurement (by value) it constitutes.',
   FALSE, 'BRSR-FULL', 'P8', 'LEADERSHIP', 126, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-L04', 'Benefits from Intellectual Property Based on Traditional Knowledge', 'SOCIAL',
   'Benefits derived and shared from intellectual property owned or acquired that is based on traditional knowledge.',
   'The entity shall provide details of the benefits derived and shared from the intellectual properties owned or acquired (in the current financial year), based on traditional knowledge: the intellectual property based on traditional knowledge, whether owned/acquired (Yes/No), whether the benefit is shared (Yes/No), and the basis of calculating the benefit share.',
   FALSE, 'BRSR-FULL', 'P8', 'LEADERSHIP', 127, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-L05', 'Corrective Action on Traditional Knowledge IP Disputes', 'SOCIAL',
   'Corrective actions based on adverse orders in intellectual property disputes involving use of traditional knowledge.',
   'The entity shall provide details of corrective actions taken or underway, based on any adverse order in intellectual property related disputes wherein usage of traditional knowledge is involved: the name of the authority, the brief of the case, and the corrective action taken.',
   FALSE, 'BRSR-FULL', 'P8', 'LEADERSHIP', 128, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P8-L06', 'Beneficiaries of CSR Projects', 'SOCIAL',
   'Number of persons benefitted from CSR projects and the share from vulnerable and marginalized groups.',
   'The entity shall provide details of beneficiaries of CSR projects: for each CSR project, the number of persons benefitted and the percentage of beneficiaries from vulnerable and marginalized groups.',
   FALSE, 'BRSR-FULL', 'P8', 'LEADERSHIP', 129, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-E01', 'Consumer Complaints and Feedback Mechanisms', 'SOCIAL',
   'Mechanisms in place to receive and respond to consumer complaints and feedback.',
   'The entity shall describe the mechanisms in place to receive and respond to consumer complaints and feedback.',
   TRUE, 'BRSR-FULL', 'P9', 'ESSENTIAL', 130, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-E02', 'Product Information on Environmental and Social Parameters', 'SOCIAL',
   'Share of turnover from products and services carrying information on environmental and social parameters, safe usage and recycling.',
   'The entity shall disclose the turnover of products and/or services, as a percentage of turnover from all products/services, that carry information about: environmental and social parameters relevant to the product, safe and responsible usage, and recycling and/or safe disposal.',
   TRUE, 'BRSR-FULL', 'P9', 'ESSENTIAL', 131, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-E03', 'Consumer Complaints by Category', 'SOCIAL',
   'Consumer complaints on data privacy, advertising, cyber-security, delivery of essential services and trade practices, with pending status.',
   'The entity shall disclose the number of consumer complaints in respect of data privacy, advertising, cyber-security, delivery of essential services, restrictive trade practices, unfair trade practices, and other: received during the year, pending resolution at the end of the year, and remarks, for the current and previous financial year.',
   TRUE, 'BRSR-FULL', 'P9', 'ESSENTIAL', 132, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-E04', 'Product Recalls on Safety Issues', 'SOCIAL',
   'Number of voluntary and forced product recalls on account of safety issues and the reasons for recall.',
   'The entity shall disclose details of instances of product recalls on account of safety issues: the number of voluntary recalls and forced recalls, and the reasons for recall.',
   TRUE, 'BRSR-FULL', 'P9', 'ESSENTIAL', 133, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-E05', 'Cyber Security and Data Privacy Framework or Policy', 'SOCIAL',
   'Whether the entity has a framework or policy on cyber security and risks related to data privacy, with a web link.',
   'The entity shall state whether it has a framework/policy on cyber security and risks related to data privacy (Yes/No) and, if available, provide a web-link of the policy.',
   TRUE, 'BRSR-FULL', 'P9', 'ESSENTIAL', 134, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-E06', 'Corrective Actions on Consumer Issues, Cyber Security and Recalls', 'SOCIAL',
   'Corrective actions on advertising, essential services delivery, cyber security and data privacy, recalls and regulatory penalties on product safety.',
   'The entity shall provide details of any corrective actions taken or underway on issues relating to advertising and delivery of essential services; cyber security and data privacy of customers; re-occurrence of instances of product recalls; and penalty/action taken by regulatory authorities on safety of products/services.',
   TRUE, 'BRSR-FULL', 'P9', 'ESSENTIAL', 135, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-L01', 'Channels for Accessing Product and Service Information', 'SOCIAL',
   'Channels and platforms where information on the entity''s products and services can be accessed.',
   'The entity shall disclose the channels/platforms where information on its products and services can be accessed (provide web link, if available).',
   FALSE, 'BRSR-FULL', 'P9', 'LEADERSHIP', 136, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-L02', 'Educating Consumers on Safe and Responsible Usage', 'SOCIAL',
   'Steps taken to inform and educate consumers about safe and responsible usage of products or services.',
   'The entity shall describe the steps taken to inform and educate consumers about safe and responsible usage of products and/or services.',
   FALSE, 'BRSR-FULL', 'P9', 'LEADERSHIP', 137, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-L03', 'Informing Consumers of Service Disruption Risks', 'SOCIAL',
   'Mechanisms to inform consumers of any risk of disruption or discontinuation of essential services.',
   'The entity shall describe the mechanisms in place to inform consumers of any risk of disruption/discontinuation of essential services.',
   FALSE, 'BRSR-FULL', 'P9', 'LEADERSHIP', 138, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-L04', 'Product Information Beyond Legal Requirements and Consumer Surveys', 'SOCIAL',
   'Whether product information is displayed beyond what local law mandates and whether a consumer satisfaction survey was carried out.',
   'The entity shall state whether it displays product information on the product over and above what is mandated as per local laws (Yes/No/Not Applicable) and, if yes, provide details in brief, and whether it carried out any survey with regard to consumer satisfaction relating to the major products/services, significant locations of operation, or the entity as a whole (Yes/No).',
   FALSE, 'BRSR-FULL', 'P9', 'LEADERSHIP', 139, TRUE),

  ((SELECT id FROM framework WHERE code = 'BRSR'), 'P9-L05', 'Data Breaches', 'SOCIAL',
   'Number of data breach instances with their impact and the share involving personally identifiable information of customers.',
   'The entity shall provide the following information relating to data breaches: (a) the number of instances of data breaches along with the impact, and (b) the percentage of data breaches involving personally identifiable information of customers.',
   FALSE, 'BRSR-FULL', 'P9', 'LEADERSHIP', 140, TRUE);
