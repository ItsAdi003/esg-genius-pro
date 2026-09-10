package dev.esgenius.service;

import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.entity.FrameworkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LexicalEvidenceRetrievalServiceTest {

    private final LexicalEvidenceRetrievalService retrievalService = new LexicalEvidenceRetrievalService();
    private Framework framework;

    @BeforeEach
    void setUp() {
        framework = new Framework("BRSR", "BRSR", "Business Responsibility and Sustainability Report",
                "India", "MVP-2026", FrameworkStatus.ACTIVE);
    }

    @Test
    void emissionsRequirementRanksEmissionsChunkHighest() {
        FrameworkRequirement scope1 = brsrRequirement(
                "ENV-003", "Scope 1 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Direct greenhouse gas emissions from owned or controlled sources.",
                "The entity shall disclose total Scope 1 emissions in metric tonnes of CO2 equivalent.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Employee health and safety training covered all office locations with zero fatalities."),
                new TextChunk(1, "The company reduced Scope 1 and Scope 2 greenhouse gas emissions by 18% year-on-year."),
                new TextChunk(2, "Board composition includes independent directors and a whistleblower mechanism."));

        List<RetrievedChunk> results = retrievalService.retrieve(scope1, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("Scope 1");
        assertThat(results.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void scope2RanksPurchasedElectricityEvidence() {
        FrameworkRequirement scope2 = brsrRequirement(
                "ENV-004", "Scope 2 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Indirect emissions from purchased electricity, steam, heating and cooling.",
                "The entity shall disclose total Scope 2 emissions in metric tonnes of CO2 equivalent.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Scope 2 emissions from purchased electricity totalled 12,400 metric tonnes CO2e."),
                new TextChunk(1, "Scope 1 direct emissions from owned facilities were 3,200 metric tonnes CO2e."),
                new TextChunk(2, "Community development spend focused on digital literacy programmes."));

        List<RetrievedChunk> results = retrievalService.retrieve(scope2, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("Scope 2");
    }

    @Test
    void scope3RanksExplicitScope3AboveScope1And2OnlyPassages() {
        FrameworkRequirement scope3 = brsrRequirement(
                "ENV-005", "Scope 3 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Other indirect value-chain emissions across upstream and downstream categories.",
                "The entity shall disclose Scope 3 emissions, covering material upstream and downstream value-chain categories.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "The company reduced Scope 1 and Scope 2 greenhouse gas emissions by 18% year-on-year."),
                new TextChunk(1, "Scope 3 emissions from business travel, employee commuting and purchased goods were estimated at 48,000 metric tonnes CO2e."),
                new TextChunk(2, "Investor engagement sessions were held with institutional shareholders on ESG priorities."));

        List<RetrievedChunk> results = retrievalService.retrieve(scope3, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("Scope 3");
        assertThat(results).noneMatch(chunk -> chunk.text().contains("Investor engagement"));
    }

    @Test
    void totalEnergyConsumptionRanksEnergyPassageOverUnrelatedAssessmentText() {
        FrameworkRequirement energy = brsrRequirement(
                "ENV-001", "Total Energy Consumption", EsgCategory.ENVIRONMENTAL,
                "Disclosure of total energy consumed from renewable and non-renewable sources with energy intensity ratios.",
                "The entity shall disclose total energy consumed from all sources during the reporting period.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "The BRSR assessment framework requires the entity to provide information on principle indicators."),
                new TextChunk(1, "Total energy consumption was 1.8 million GJ, with energy intensity of 0.04 GJ per rupee of turnover."),
                new TextChunk(2, "Board composition includes independent directors with sustainability expertise."));

        List<RetrievedChunk> results = retrievalService.retrieve(energy, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("energy consumption");
    }

    @Test
    void renewableEnergyRanksSolarAndElectricityEvidence() {
        FrameworkRequirement renewable = brsrRequirement(
                "ENV-002", "Renewable vs Non-Renewable Energy Breakdown", EsgCategory.ENVIRONMENTAL,
                "Split of energy consumption between renewable and non-renewable sources, reported separately for electricity, fuel and others.",
                "The entity shall separately disclose the total energy consumed from renewable sources and from non-renewable sources.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Renewable energy procurement increased to 45% through solar and wind power purchase agreements."),
                new TextChunk(1, "Human rights due diligence assessments were conducted across supplier categories."),
                new TextChunk(2, "The entity provides business responsibility information in the annual report."));

        List<RetrievedChunk> results = retrievalService.retrieve(renewable, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("renewable");
    }

    @Test
    void waterWithdrawalRanksWaterEvidence() {
        FrameworkRequirement water = brsrRequirement(
                "ENV-006", "Water Withdrawal Disclosure", EsgCategory.ENVIRONMENTAL,
                "Total water withdrawal segregated by source across operating locations.",
                "The entity shall disclose total water withdrawal by source.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "The annual general meeting was held in Bangalore."),
                new TextChunk(1, "Total water withdrawal from groundwater and surface water sources was 1.2 million kilolitres."));

        List<RetrievedChunk> results = retrievalService.retrieve(water, chunks);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
    }

    @Test
    void wasteRecyclingRanksRecyclingEvidence() {
        FrameworkRequirement waste = brsrRequirement(
                "ENV-007", "Waste Recycling Data", EsgCategory.ENVIRONMENTAL,
                "Waste generation and recovery quantities by waste category.",
                "The entity shall disclose total waste generated by category and the quantity recovered through recycling.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Waste generated was segregated by category with 62% recovered through recycling, reuse and other recovery operations."),
                new TextChunk(1, "Investor engagement sessions discussed quarterly financial performance."));

        List<RetrievedChunk> results = retrievalService.retrieve(waste, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("recycling");
    }

    @Test
    void employeeHealthAndSafetyRanksOccupationalEvidenceOverInvestorEngagement() {
        FrameworkRequirement safety = brsrRequirement(
                "SOC-001", "Employee Health & Safety", EsgCategory.SOCIAL,
                "Occupational health and safety management system coverage and incidents.",
                "The entity shall describe its occupational health and safety management system.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Investor engagement sessions were held with institutional shareholders on ESG priorities."),
                new TextChunk(1, "Occupational health and safety training covered 100% of permanent employees with zero fatalities."),
                new TextChunk(2, "Renewable energy procurement increased to 45% of total electricity consumption."));

        List<RetrievedChunk> results = retrievalService.retrieve(safety, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("occupational health");
    }

    @Test
    void trainingAndDevelopmentRanksLearningHoursOverGenericBrsrAwarenessTraining() {
        FrameworkRequirement training = brsrRequirement(
                "SOC-002", "Employee Training & Development Hours", EsgCategory.SOCIAL,
                "Training and awareness programme coverage across the workforce.",
                "The entity shall disclose training and awareness programmes with coverage by employee category and gender.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "BRSR principles awareness training was provided to all managers on regulatory disclosure requirements."),
                new TextChunk(1, "Employee learning and development programmes delivered 42 average training hours per permanent employee."),
                new TextChunk(2, "Board composition includes independent directors with sustainability expertise."));

        List<RetrievedChunk> results = retrievalService.retrieve(training, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("training hours");
    }

    @Test
    void humanRightsDueDiligenceRanksDueDiligenceEvidence() {
        FrameworkRequirement humanRights = brsrRequirement(
                "SOC-003", "Human Rights Due Diligence", EsgCategory.SOCIAL,
                "Human rights due-diligence process, assessments and remediation.",
                "The entity shall disclose the process for human rights due diligence, assessments carried out during the year and remediation actions taken.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Human rights due diligence assessments were conducted across key supplier categories with remediation actions tracked."),
                new TextChunk(1, "Total energy consumption increased modestly across offices."));

        List<RetrievedChunk> results = retrievalService.retrieve(humanRights, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("human rights");
    }

    @Test
    void communitySpendRanksExpenditureOverGenericCsrNarrative() {
        FrameworkRequirement community = brsrRequirement(
                "SOC-004", "Community Development Spend", EsgCategory.SOCIAL,
                "Community and CSR investment with beneficiary coverage.",
                "The entity shall disclose CSR and community development expenditure along with the beneficiary areas covered.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "The company supports community development through education and healthcare beneficiary programmes."),
                new TextChunk(1, "CSR expenditure totalled INR 420 crore for community development initiatives across beneficiary areas."),
                new TextChunk(2, "Anti-corruption policy training was completed by senior management."));

        List<RetrievedChunk> results = retrievalService.retrieve(community, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("expenditure");
    }

    @Test
    void antiCorruptionRanksPolicyEvidenceOverRegulatorySettlement() {
        FrameworkRequirement antiCorruption = brsrRequirement(
                "GOV-001", "Anti-Corruption Policy", EsgCategory.GOVERNANCE,
                "Anti-bribery and anti-corruption policy, coverage and enforcement.",
                "The entity shall disclose whether an anti-corruption or anti-bribery policy exists, its coverage and details of disciplinary actions taken during the year.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "A SEBI settlement order related to past regulatory proceedings was disclosed in the annual report."),
                new TextChunk(1, "An anti-bribery and anti-corruption policy applies to all employees with disciplinary actions for violations."),
                new TextChunk(2, "Whistleblower mechanism complaints were reviewed by the audit committee."));

        List<RetrievedChunk> results = retrievalService.retrieve(antiCorruption, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("anti-corruption");
    }

    @Test
    void boardIndependenceRanksIndependentDirectorEvidenceOverCommitteeReferences() {
        FrameworkRequirement board = brsrRequirement(
                "GOV-002", "Board Composition & Independence", EsgCategory.GOVERNANCE,
                "Composition, independence and diversity of the board of directors.",
                "The entity shall disclose board composition, including independence and diversity of directors.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "The audit committee and nomination committee met four times during the fiscal year."),
                new TextChunk(1, "Board composition includes 50% independent directors with relevant sustainability expertise."),
                new TextChunk(2, "CSR expenditure focused on education and healthcare beneficiary areas."));

        List<RetrievedChunk> results = retrievalService.retrieve(board, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results.get(0).text()).containsIgnoringCase("independent director");
    }

    @Test
    void whistleblowerRanksVigilMechanismEvidence() {
        FrameworkRequirement whistleblower = brsrRequirement(
                "GOV-003", "Whistleblower Mechanism", EsgCategory.GOVERNANCE,
                "Vigil mechanism availability, accessibility and complaint handling.",
                "The entity shall disclose the existence of a vigil or whistleblower mechanism, its accessibility to stakeholders and complaints received during the year.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "A whistleblower mechanism and vigil mechanism are accessible to employees and business partners."),
                new TextChunk(1, "Scope 2 emissions from purchased electricity remained stable year-on-year."));

        List<RetrievedChunk> results = retrievalService.retrieve(whistleblower, chunks);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkIndex()).isEqualTo(0);
        assertThat(results.get(0).text()).containsIgnoringCase("whistleblower");
    }

    @Test
    void zeroRelevanceChunksAreExcluded() {
        FrameworkRequirement water = brsrRequirement(
                "ENV-006", "Water Withdrawal Disclosure", EsgCategory.ENVIRONMENTAL,
                "Total water withdrawal segregated by source across operating locations.",
                "The entity shall disclose total water withdrawal by source.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "The annual general meeting was held in Bangalore."),
                new TextChunk(1, "Total water withdrawal from groundwater and surface water sources was 1.2 million kilolitres."));

        List<RetrievedChunk> results = retrievalService.retrieve(water, chunks);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkIndex()).isEqualTo(1);
        assertThat(results).noneMatch(chunk -> chunk.text().contains("annual general meeting"));
    }

    @Test
    void retrievalIsDeterministic() {
        FrameworkRequirement safety = brsrRequirement(
                "SOC-001", "Employee Health & Safety", EsgCategory.SOCIAL,
                "Occupational health and safety management system coverage and incidents.",
                "The entity shall describe its occupational health and safety management system.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Occupational health and safety training covered 100% of permanent employees."),
                new TextChunk(1, "Renewable energy procurement increased to 45% of total electricity consumption."));

        List<RetrievedChunk> first = retrievalService.retrieve(safety, chunks);
        List<RetrievedChunk> second = retrievalService.retrieve(safety, chunks);

        assertThat(first).hasSize(second.size());
        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).chunkIndex()).isEqualTo(second.get(i).chunkIndex());
            assertThat(first.get(i).score()).isEqualTo(second.get(i).score());
        }
    }

    @Test
    void returnsUpToThreeHighQualityChunks() {
        FrameworkRequirement emissions = brsrRequirement(
                "ENV-004", "Scope 2 GHG Emissions", EsgCategory.ENVIRONMENTAL,
                "Indirect emissions from purchased electricity.",
                "The entity shall disclose total Scope 2 emissions in metric tonnes of CO2 equivalent.");

        List<TextChunk> chunks = List.of(
                new TextChunk(0, "Scope 2 emissions from purchased electricity totalled 12,400 metric tonnes CO2e."),
                new TextChunk(1, "Market-based Scope 2 emissions were lower due to renewable energy certificates."),
                new TextChunk(2, "Location-based Scope 2 emissions remained stable year-on-year."),
                new TextChunk(3, "Purchased electricity emissions intensity improved by 6%."),
                new TextChunk(4, "Grid electricity consumption increased modestly in data centres."),
                new TextChunk(5, "Community development spend focused on digital literacy programmes."),
                new TextChunk(6, "Anti-corruption policy training was completed by all senior managers."));

        List<RetrievedChunk> results = retrievalService.retrieve(emissions, chunks);

        assertThat(results).hasSizeLessThanOrEqualTo(LexicalEvidenceRetrievalService.MAX_RESULTS);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(chunk -> chunk.score() >= LexicalEvidenceRetrievalService.MIN_SCORE_THRESHOLD);
        assertThat(results.get(0).text()).containsIgnoringCase("Scope 2");
    }

    private FrameworkRequirement brsrRequirement(
            String code,
            String title,
            EsgCategory category,
            String description,
            String frameworkText) {
        return new FrameworkRequirement(framework, code, title, category, description, frameworkText, true, "MVP-2026");
    }
}
