package dev.esgenius.service;

import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.entity.FrameworkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for paraphrased BRSR disclosure wording that does not
 * contain the exact contiguous phrases historically used as required anchors.
 */
class ParaphrasedDisclosureRetrievalTest {

    private final LexicalEvidenceRetrievalService retrievalService = new LexicalEvidenceRetrievalService();
    private Framework framework;

    @BeforeEach
    void setUp() {
        framework = new Framework("BRSR", "BRSR", "Business Responsibility and Sustainability Report",
                "India", "MVP-2026", FrameworkStatus.ACTIVE);
    }

    @Test
    void soc002RetrievesAverageHoursOfTrainingParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc002(), List.of(
                new TextChunk(0, "Board composition includes independent directors with sustainability expertise."),
                new TextChunk(1, "Average hours of training per employee stood at 42 hours during the reporting year."),
                new TextChunk(2, "Investor engagement sessions were held with institutional shareholders on ESG priorities.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("hours of training");
    }

    @Test
    void soc002RetrievesSkillUpgradationAndUpskillingParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc002(), List.of(
                new TextChunk(0, "Renewable energy procurement increased to 45% of total electricity consumption."),
                new TextChunk(1, "Skill upgradation and upskilling programmes covered 98% of permanent employees during the year.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("skill upgradation");
    }

    @Test
    void soc002RetrievesPersonHoursOfTrainingParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc002(), List.of(
                new TextChunk(0, "The Company imparted 1.8 million person-hours of training to its workforce across technical and behavioural modules."),
                new TextChunk(1, "The annual general meeting was held in Mumbai during the reporting period.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("person-hours");
    }

    @Test
    void soc002RetrievesCapacityBuildingParaphraseWithoutExactTrainingHoursPhrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc002(), List.of(
                new TextChunk(0, "Capacity building sessions were conducted for all employee categories, including leadership and technical tracks."),
                new TextChunk(1, "Scope 2 emissions from purchased electricity remained stable year-on-year.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("capacity building");
    }

    @Test
    void soc002StillPrefersWorkforceLearningOverBrsrPrinciplesAwarenessTraining() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc002(), List.of(
                new TextChunk(0, "BRSR principles awareness training was provided to all managers on regulatory disclosure requirements."),
                new TextChunk(1, "Employee learning and development programmes delivered 42 average training hours per permanent employee."),
                new TextChunk(2, "Newspaper pamphlets were used for stakeholder communication during the year.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results).noneMatch(chunk -> chunk.text().contains("BRSR principles"));
        assertThat(results).noneMatch(chunk -> chunk.text().contains("Newspaper pamphlets"));
    }

    @Test
    void soc002DoesNotTreatCommunitySkillDevelopmentAsEmployeeTrainingHours() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc002(), List.of(
                new TextChunk(0, "The Foundation delivered skill development programmes for rural youth as part of community initiatives."),
                new TextChunk(1, "Anti-corruption policy training was completed by senior management during onboarding.")));

        assertThat(results).noneMatch(chunk -> chunk.text().contains("rural youth"));
    }

    @Test
    void soc001RetrievesLostTimeInjuryFrequencyParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc001(), List.of(
                new TextChunk(0, "Lost time injury frequency rate (LTIFR) was 0.12 with no fatalities among permanent employees."),
                new TextChunk(1, "Total energy consumption was 1.8 million GJ across offices and data centres.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("LTIFR");
    }

    @Test
    void env007RetrievesWasteRecycledWithStopWordsBetweenAnchorTokens() {
        List<RetrievedChunk> results = retrievalService.retrieve(env007(), List.of(
                new TextChunk(0, "The quantity of waste that was recycled during the year totalled 8,100 metric tonnes."),
                new TextChunk(1, "Investor engagement sessions discussed quarterly financial performance.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("waste that was recycled");
    }

    @Test
    void env007RetrievesWasteRecycledParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env007(), List.of(
                new TextChunk(0, "Quantity of waste recycled during the year was 12,400 metric tonnes across operating locations."),
                new TextChunk(1, "Board composition includes independent directors with relevant sustainability expertise.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("waste recycled");
    }

    @Test
    void gov003RetrievesWhistleBlowingAndEthicsHelplineParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(gov003(), List.of(
                new TextChunk(0, "An ethics helpline and whistle-blowing channel is available to employees and business partners."),
                new TextChunk(1, "Scope 3 emissions from business travel were estimated using distance-based factors.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("whistle-blowing");
    }

    @Test
    void env001RetrievesEnergyUtilisationParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env001(), List.of(
                new TextChunk(0, "The BRSR assessment framework requires the entity to provide information on principle indicators."),
                new TextChunk(1, "Total energy utilisation across operations was 1.8 million GJ with energy intensity of 0.04 GJ per rupee of turnover."),
                new TextChunk(2, "Board composition includes independent directors with sustainability expertise.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("energy utilisation");
    }

    @Test
    void env001DoesNotTreatAnnualGeneralMeetingAsEnergyConsumption() {
        List<RetrievedChunk> results = retrievalService.retrieve(env001(), List.of(
                new TextChunk(0, "The annual general meeting was held in Bangalore with shareholder participation."),
                new TextChunk(1, "Investor engagement sessions discussed quarterly financial performance.")));

        assertThat(results).noneMatch(chunk -> chunk.text().contains("annual general meeting"));
    }

    @Test
    void env002RetrievesCleanEnergyAndRenewableSourcesParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env002(), List.of(
                new TextChunk(0, "Clean energy from solar and wind installations contributed 45% of electricity consumed from renewable sources."),
                new TextChunk(1, "Human rights due diligence assessments were conducted across supplier categories.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("clean energy");
    }

    @Test
    void env003RetrievesDirectGhgEmissionsParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env003(), List.of(
                new TextChunk(0, "Direct GHG emissions from owned facilities and company vehicles totalled 8,200 metric tonnes CO2e."),
                new TextChunk(1, "Scope 2 emissions from purchased electricity remained stable year-on-year.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("Direct GHG");
    }

    @Test
    void env004RetrievesPurchasedEnergyAndGridElectricityParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env004(), List.of(
                new TextChunk(0, "Emissions from purchased energy and grid electricity totalled 12,400 metric tonnes CO2e."),
                new TextChunk(1, "Scope 1 direct emissions from owned facilities were 3,200 metric tonnes CO2e.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("purchased energy");
    }

    @Test
    void env005RetrievesOtherIndirectEmissionsParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env005(), List.of(
                new TextChunk(0, "Other indirect emissions across upstream and downstream value-chain categories were estimated at 48,000 metric tonnes CO2e."),
                new TextChunk(1, "The company reduced Scope 1 and Scope 2 greenhouse gas emissions by 18% year-on-year.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("other indirect emissions");
    }

    @Test
    void env006RetrievesWaterExtractedParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(env006(), List.of(
                new TextChunk(0, "Freshwater extracted from groundwater and municipal water supply sources totalled 1.2 million kilolitres."),
                new TextChunk(1, "CSR expenditure focused on education and healthcare beneficiary areas.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("water extracted");
    }

    @Test
    void env006DoesNotTreatWaterConservationAwarenessAsWithdrawalDisclosure() {
        List<RetrievedChunk> results = retrievalService.retrieve(env006(), List.of(
                new TextChunk(0, "Water conservation awareness campaigns were conducted across offices during World Water Day."),
                new TextChunk(1, "Board composition includes independent directors with relevant sustainability expertise.")));

        assertThat(results).noneMatch(chunk -> chunk.text().contains("conservation awareness"));
    }

    @Test
    void soc003RetrievesHumanRightsImpactAssessmentParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc003(), List.of(
                new TextChunk(0, "A human rights impact assessment was conducted across operations and key supplier categories."),
                new TextChunk(1, "Total energy consumption increased modestly across offices.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("human rights impact assessment");
    }

    @Test
    void soc003DoesNotTreatStakeholderEngagementAsHumanRightsDueDiligence() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc003(), List.of(
                new TextChunk(0, "Stakeholder engagement sessions were held with investors on quarterly financial performance."),
                new TextChunk(1, "Renewable energy procurement increased to 45% of total electricity consumption.")));

        assertThat(results).noneMatch(chunk -> chunk.text().contains("Stakeholder engagement"));
    }

    @Test
    void soc004RetrievesCorporateSocialResponsibilitySpendParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc004(), List.of(
                new TextChunk(0, "The company supports community development through education and healthcare beneficiary programmes."),
                new TextChunk(1, "Corporate social responsibility spend amounted to INR 420 crore for community development initiatives.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("Corporate social responsibility spend");
    }

    @Test
    void soc004DoesNotTreatCsrPolicyNarrativeAsExpenditureDisclosure() {
        List<RetrievedChunk> results = retrievalService.retrieve(soc004(), List.of(
                new TextChunk(0, "The CSR policy exists and the CSR committee charter defines governance responsibilities."),
                new TextChunk(1, "Anti-corruption policy training was completed by senior management.")));

        assertThat(results).noneMatch(chunk -> chunk.text().contains("CSR policy exists"));
    }

    @Test
    void gov001RetrievesPreventionOfCorruptionPolicyParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(gov001(), List.of(
                new TextChunk(0, "A SEBI settlement order related to past regulatory proceedings was disclosed in the annual report."),
                new TextChunk(1, "A prevention of corruption and fraud policy applies to all employees with disciplinary actions for violations.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("prevention of corruption");
    }

    @Test
    void gov002RetrievesNonExecutiveIndependentDirectorsParaphrase() {
        List<RetrievedChunk> results = retrievalService.retrieve(gov002(), List.of(
                new TextChunk(0, "The audit committee and nomination committee met four times during the fiscal year."),
                new TextChunk(1, "The Board comprises 50% non-executive independent directors with relevant sector expertise.")));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("non-executive independent directors");
    }

    @Test
    void scopeGatesStillDisambiguateScope1FromScope2AndScope3() {
        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Scope 1 direct emissions from owned facilities were 3,200 metric tonnes CO2e."),
                new TextChunk(1, "Scope 2 emissions from purchased electricity totalled 12,400 metric tonnes CO2e."),
                new TextChunk(2, "Scope 3 emissions from business travel and purchased goods were 48,000 metric tonnes CO2e."));

        List<RetrievedChunk> scope1 = retrievalService.retrieve(env003(), chunks);
        List<RetrievedChunk> scope2 = retrievalService.retrieve(env004(), chunks);
        List<RetrievedChunk> scope3 = retrievalService.retrieve(env005(), chunks);

        assertThat(scope1).isNotEmpty();
        assertThat(scope1.get(0).chunkIndex()).isEqualTo(0);
        assertThat(scope1.get(0).text()).containsIgnoringCase("Scope 1");
        assertThat(scope1).noneMatch(chunk -> chunk.text().contains("Scope 2 emissions from purchased"));
        assertThat(scope1).noneMatch(chunk -> chunk.text().contains("Scope 3 emissions from business"));

        assertThat(scope2).isNotEmpty();
        assertThat(scope2.get(0).chunkIndex()).isEqualTo(1);
        assertThat(scope2.get(0).text()).containsIgnoringCase("Scope 2");
        assertThat(scope2).noneMatch(chunk -> chunk.text().contains("Scope 1 direct"));
        assertThat(scope2).noneMatch(chunk -> chunk.text().contains("Scope 3 emissions from business"));

        assertThat(scope3).isNotEmpty();
        assertThat(scope3.get(0).chunkIndex()).isEqualTo(2);
        assertThat(scope3.get(0).text()).containsIgnoringCase("Scope 3");
        assertThat(scope3).noneMatch(chunk -> chunk.text().contains("Scope 1 direct"));
        assertThat(scope3).noneMatch(chunk -> chunk.text().contains("Scope 2 emissions from purchased"));
    }

    private FrameworkRequirement soc002() {
        return requirement("SOC-002", "Employee Training & Development Hours", EsgCategory.SOCIAL,
                "Training and awareness programme coverage across the workforce.",
                "The entity shall disclose training and awareness programmes with coverage by employee category and gender.");
    }

    private FrameworkRequirement soc001() {
        return requirement("SOC-001", "Employee Health & Safety", EsgCategory.SOCIAL,
                "Occupational health and safety management system coverage and incidents.",
                "The entity shall describe its occupational health and safety management system.");
    }

    private FrameworkRequirement env007() {
        return requirement("ENV-007", "Waste Recycling Data", EsgCategory.ENVIRONMENTAL,
                "Waste generation and recovery quantities by waste category.",
                "The entity shall disclose total waste generated by category and the quantity recovered through recycling.");
    }

    private FrameworkRequirement gov003() {
        return requirement("GOV-003", "Whistleblower Mechanism", EsgCategory.GOVERNANCE,
                "Vigil mechanism availability, accessibility and complaint handling.",
                "The entity shall disclose the existence of a vigil or whistleblower mechanism, its accessibility to stakeholders and complaints received during the year.");
    }

    private FrameworkRequirement env001() {
        return requirement("ENV-001", "Total Energy Consumption", EsgCategory.ENVIRONMENTAL,
                "Disclosure of total energy consumed from renewable and non-renewable sources with energy intensity ratios.",
                "The entity shall disclose total energy consumed from all sources during the reporting period.");
    }

    private FrameworkRequirement env002() {
        return requirement("ENV-002", "Renewable vs Non-Renewable Energy Breakdown", EsgCategory.ENVIRONMENTAL,
                "Split of energy consumption between renewable and non-renewable sources, reported separately for electricity, fuel and others.",
                "The entity shall separately disclose the total energy consumed from renewable sources and from non-renewable sources.");
    }

    private FrameworkRequirement env006() {
        return requirement("ENV-006", "Water Withdrawal Disclosure", EsgCategory.ENVIRONMENTAL,
                "Total water withdrawal segregated by source across operating locations.",
                "The entity shall disclose total water withdrawal by source.");
    }

    private FrameworkRequirement soc003() {
        return requirement("SOC-003", "Human Rights Due Diligence", EsgCategory.SOCIAL,
                "Human rights due-diligence process, assessments and remediation.",
                "The entity shall disclose the process for human rights due diligence, assessments carried out during the year and remediation actions taken.");
    }

    private FrameworkRequirement soc004() {
        return requirement("SOC-004", "Community Development Spend", EsgCategory.SOCIAL,
                "Community and CSR investment with beneficiary coverage.",
                "The entity shall disclose CSR and community development expenditure along with the beneficiary areas covered.");
    }

    private FrameworkRequirement gov001() {
        return requirement("GOV-001", "Anti-Corruption Policy", EsgCategory.GOVERNANCE,
                "Anti-bribery and anti-corruption policy, coverage and enforcement.",
                "The entity shall disclose whether an anti-corruption or anti-bribery policy exists, its coverage and details of disciplinary actions taken during the year.");
    }

    private FrameworkRequirement gov002() {
        return requirement("GOV-002", "Board Composition & Independence", EsgCategory.GOVERNANCE,
                "Composition, independence and diversity of the board of directors.",
                "The entity shall disclose board composition, including independence and diversity of directors.");
    }

    private FrameworkRequirement env003() {
        return requirement("ENV-003", "Scope 1 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Direct greenhouse gas emissions from owned or controlled sources.",
                "The entity shall disclose total Scope 1 emissions in metric tonnes of CO2 equivalent.");
    }

    private FrameworkRequirement env004() {
        return requirement("ENV-004", "Scope 2 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Indirect emissions from purchased electricity, steam, heating and cooling.",
                "The entity shall disclose total Scope 2 emissions in metric tonnes of CO2 equivalent.");
    }

    private FrameworkRequirement env005() {
        return requirement("ENV-005", "Scope 3 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Other indirect value-chain emissions across upstream and downstream categories.",
                "The entity shall disclose Scope 3 emissions, covering material upstream and downstream value-chain categories.");
    }

    private FrameworkRequirement requirement(
            String code,
            String title,
            EsgCategory category,
            String description,
            String frameworkText) {
        return new FrameworkRequirement(framework, code, title, category, description, frameworkText, true, "MVP-2026");
    }
}
