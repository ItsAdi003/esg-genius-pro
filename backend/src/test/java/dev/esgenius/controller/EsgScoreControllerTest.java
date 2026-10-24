package dev.esgenius.controller;

import com.jayway.jsonpath.JsonPath;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.entity.RequirementAssessment;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EsgScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
    }

    private Long newDocument() {
        Organization organization = organizationRepository.findByTicker("INFY").orElseThrow();
        String stored = "esg-score-test-" + System.nanoTime() + ".pdf";
        Document document = new Document(
                organization, "esg-sample.pdf", stored, DocumentType.BRSR, 2025, 1024L, stored);
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document).getId();
    }

    private Long runAnalysis(Long documentId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/documents/{id}/analyses", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"frameworkCode\":\"BRSR\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private Long firstRequirementId(Long analysisId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.assessments[?(@.requirementCode=='P6-E06')].requirementId");
        return ids.get(0).longValue();
    }

    private void forceAllAssessments(Long analysisId, AssessmentStatus status, int firstN) {
        List<RequirementAssessment> all = assessmentRepository.findAll().stream()
                .filter(a -> a.getAnalysis().getId().equals(analysisId))
                .limit(firstN)
                .toList();
        all.forEach(a -> a.setAssessmentStatus(status));
        assessmentRepository.saveAll(all);
    }

    @Test
    void scoreIsReturnedForACompletedAnalysis() throws Exception {
        Long analysisId = runAnalysis(newDocument());

        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId", is(analysisId.intValue())))
                .andExpect(jsonPath("$.scorable", is(true)))
                .andExpect(jsonPath("$.weightedAverageKeyIssueScore", notNullValue()))
                .andExpect(jsonPath("$.industry", nullValue()))
                .andExpect(jsonPath("$.industryAdjusted", is(false)))
                .andExpect(jsonPath("$.keyIssues", hasSize(10)))
                .andExpect(jsonPath("$.totalRequirements", is(140)))
                .andExpect(jsonPath("$.note", notNullValue()));
    }

    @Test
    void settingTheIndustryChangesTheWeightProfile() throws Exception {
        Long analysisId = runAnalysis(newDocument());

        mockMvc.perform(put("/api/v1/analyses/{id}/industry", analysisId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"industry\":\"ENERGY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industry", is("ENERGY")))
                .andExpect(jsonPath("$.industryLabel", is("Energy")))
                .andExpect(jsonPath("$.environmentalWeight", is(0.45)));

        mockMvc.perform(put("/api/v1/analyses/{id}/industry", analysisId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"industry\":\"NOT_A_SECTOR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notApplicableRequiresAReasonAndCanBeUndone() throws Exception {
        Long analysisId = runAnalysis(newDocument());
        Long requirementId = firstRequirementId(analysisId);
        String path = "/api/v1/analyses/{id}/requirements/{rid}/applicability";

        mockMvc.perform(put(path, analysisId, requirementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicable\":false,\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(path, analysisId, requirementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicable\":false,\"reason\":\"The entity does not use process water.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentStatus", is("NOT_APPLICABLE")))
                .andExpect(jsonPath("$.notApplicableReason", is("The entity does not use process water.")));

        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", analysisId))
                .andExpect(jsonPath("$.notApplicableRequirements", is(1)));
        mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
                .andExpect(jsonPath("$.assessments[?(@.requirementCode=='P6-E06')].assessmentStatus",
                        hasItem("NOT_APPLICABLE")))
                .andExpect(jsonPath("$.assessments[?(@.requirementCode=='P6-E06')].notApplicableReason",
                        hasItem("The entity does not use process water.")));

        mockMvc.perform(put(path, analysisId, requirementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicable\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentStatus", is("COVERED")))
                .andExpect(jsonPath("$.notApplicableReason", nullValue()));

        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", analysisId))
                .andExpect(jsonPath("$.notApplicableRequirements", is(0)));
    }

    @Test
    void scoreIsAdjustedAgainstSameIndustryPeers() throws Exception {
        Long low = runAnalysis(newDocument());
        Long middle = runAnalysis(newDocument());
        Long high = runAnalysis(newDocument());
        for (Long id : List.of(low, middle, high)) {
            mockMvc.perform(put("/api/v1/analyses/{id}/industry", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"industry\":\"INFORMATION_TECHNOLOGY\"}"))
                    .andExpect(status().isOk());
        }
        forceAllAssessments(low, AssessmentStatus.NOT_COVERED, 140);
        forceAllAssessments(middle, AssessmentStatus.PARTIALLY_COVERED, 140);
        forceAllAssessments(high, AssessmentStatus.COVERED, 140);

        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", high))
                .andExpect(jsonPath("$.industryAdjusted", is(true)))
                .andExpect(jsonPath("$.peerCount", is(3)))
                .andExpect(jsonPath("$.industryAdjustedScore", is(10.0)))
                .andExpect(jsonPath("$.ratingBand", is("AAA")));
        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", middle))
                .andExpect(jsonPath("$.industryAdjustedScore", is(5.0)))
                .andExpect(jsonPath("$.ratingBand", is("BBB")));
        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", low))
                .andExpect(jsonPath("$.industryAdjustedScore", is(0.0)))
                .andExpect(jsonPath("$.ratingBand", is("CCC")));
    }

    @Test
    void scoreIsNotAdjustedWithTooFewPeers() throws Exception {
        Long only = runAnalysis(newDocument());
        mockMvc.perform(put("/api/v1/analyses/{id}/industry", only)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"industry\":\"ENERGY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industryAdjusted", is(false)))
                .andExpect(jsonPath("$.peerCount", is(1)));
    }

    @Test
    void unknownAnalysisIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/analyses/{id}/esg-score", 987654L)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/analyses/{id}/requirements/{rid}/applicability", 987654L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicable\":true}"))
                .andExpect(status().isNotFound());
    }
}
