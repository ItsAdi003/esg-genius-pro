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
