package dev.esgenius.service;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.Organization;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.compliance.ClassificationFailureCategory;
import dev.esgenius.service.compliance.ClassificationFailureHandler;
import dev.esgenius.service.compliance.ComplianceClassificationException;
import dev.esgenius.service.compliance.ComplianceClassificationProvider;
import dev.esgenius.service.compliance.ComplianceClassificationRequest;
import dev.esgenius.service.compliance.ComplianceClassificationResult;
import dev.esgenius.support.FullBrsrEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A full analysis calls the model once per batch (a section, split evenly above the limit), not once per
 * requirement. Runs the real retrieval, batching and persistence with a stubbed provider.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.ai.gemini.max-concurrent-classifications=1",
        "app.ai.gemini.max-requirements-per-call=12"})
class ComplianceAnalysisBatchingTest {

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private ComplianceClassificationProvider classificationProvider;

    private Organization organization;
    private final List<List<ComplianceClassificationRequest>> batches =
            Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        batches.clear();
    }

    private static ComplianceClassificationResult covered(String code) {
        return new ComplianceClassificationResult(AssessmentStatus.COVERED, 0.9, "Covered " + code, null, null);
    }

    private ComplianceAnalysisResponse analyse() {
        Document document = createReadyDocument(FullBrsrEvidence.text());
        ComplianceAnalysisResponse created = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));
        return complianceAnalysisService.getAnalysis(created.id());
    }

    private static List<RequirementAssessmentResponse> withEvidence(ComplianceAnalysisResponse response) {
        return response.assessments().stream()
                .filter(a -> a.retrievalScore() != null && a.retrievalScore() > 0.0)
                .toList();
    }

    private void answerEveryRequirement() {
        when(classificationProvider.classifyBatch(any())).thenAnswer(invocation -> {
            List<ComplianceClassificationRequest> requests = invocation.getArgument(0);
            batches.add(List.copyOf(requests));
            Map<String, ComplianceClassificationResult> results = new LinkedHashMap<>();
            requests.forEach(r -> results.put(r.requirementCode(), covered(r.requirementCode())));
            return results;
        });
    }

    @Test
    void oneCallAnswersAWholeBatchAndEveryRequirementIsPersisted() {
        answerEveryRequirement();

        ComplianceAnalysisResponse response = analyse();

        List<RequirementAssessmentResponse> classified = withEvidence(response);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.assessments()).hasSize(140);
        assertThat(classified).hasSizeGreaterThan(20);
        assertThat(classified).allSatisfy(a -> {
            assertThat(a.assessmentStatus()).isEqualTo("COVERED");
            assertThat(a.explanation()).isEqualTo("Covered " + a.requirementCode());
        });

        int requestsSent = batches.stream().mapToInt(List::size).sum();
        assertThat(requestsSent).isEqualTo(classified.size());
        // Far fewer model calls than requirements, and within what one analysis can spend in a day.
        assertThat(batches.size()).isLessThan(classified.size() / 2).isLessThanOrEqualTo(15);
        verify(classificationProvider, never()).classify(any());
    }

    @Test
    void eachCallStaysInsideOneSectionAndWithinTheBatchLimit() {
        answerEveryRequirement();
        Map<String, String> sectionByCode = analyseAndMapSections();

        assertThat(batches).isNotEmpty();
        for (List<ComplianceClassificationRequest> batch : batches) {
            Set<String> sections = batch.stream()
                    .map(r -> sectionByCode.get(r.requirementCode()))
                    .collect(Collectors.toSet());
            assertThat(sections).as("sections in one call: %s", batch.stream().map(
                    ComplianceClassificationRequest::requirementCode).toList()).hasSize(1);
            assertThat(batch.size()).isBetween(1, 12);
        }
        assertThat(batches.stream()
                .map(batch -> sectionByCode.get(batch.get(0).requirementCode()))
                .distinct().count()).isGreaterThan(2);
    }

    @Test
    void aRequirementTheModelLeavesOutFallsBackToHumanReviewAlone() {
        AtomicInteger omitted = new AtomicInteger();
        String[] omittedCode = new String[1];
        when(classificationProvider.classifyBatch(any())).thenAnswer(invocation -> {
            List<ComplianceClassificationRequest> requests = invocation.getArgument(0);
            Map<String, ComplianceClassificationResult> results = new LinkedHashMap<>();
            for (ComplianceClassificationRequest request : requests) {
                if (requests.size() > 1 && omitted.get() == 0) {
                    omitted.incrementAndGet();
                    omittedCode[0] = request.requirementCode();
                    continue;
                }
                results.put(request.requirementCode(), covered(request.requirementCode()));
            }
            return results;
        });

        ComplianceAnalysisResponse response = analyse();

        assertThat(omittedCode[0]).isNotNull();
        RequirementAssessmentResponse left = response.assessments().stream()
                .filter(a -> a.requirementCode().equals(omittedCode[0])).findFirst().orElseThrow();
        assertThat(left.assessmentStatus()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        assertThat(left.explanation()).isEqualTo(ClassificationFailureHandler.GENERIC_FAILURE_EXPLANATION);
        assertThat(withEvidence(response).stream().filter(a -> !a.requirementCode().equals(omittedCode[0])))
                .allMatch(a -> a.assessmentStatus().equals("COVERED"));
    }

    @Test
    void aFailedCallSendsOnlyItsOwnBatchToHumanReview() {
        AtomicInteger calls = new AtomicInteger();
        List<String> failedCodes = Collections.synchronizedList(new ArrayList<>());
        when(classificationProvider.classifyBatch(any())).thenAnswer(invocation -> {
            List<ComplianceClassificationRequest> requests = invocation.getArgument(0);
            if (calls.incrementAndGet() == 2) {
                requests.forEach(r -> failedCodes.add(r.requirementCode()));
                throw new ComplianceClassificationException(
                        ClassificationFailureCategory.HTTP_SERVER_ERROR, "Gemini API returned HTTP 503", true,
                        503, 3, "Gemini API server error");
            }
            Map<String, ComplianceClassificationResult> results = new LinkedHashMap<>();
            requests.forEach(r -> results.put(r.requirementCode(), covered(r.requirementCode())));
            return results;
        });

        ComplianceAnalysisResponse response = analyse();

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(failedCodes).isNotEmpty();
        for (RequirementAssessmentResponse assessment : withEvidence(response)) {
            if (failedCodes.contains(assessment.requirementCode())) {
                assertThat(assessment.assessmentStatus()).isEqualTo("HUMAN_REVIEW_REQUIRED");
                assertThat(assessment.explanation())
                        .isEqualTo(ClassificationFailureHandler.GENERIC_FAILURE_EXPLANATION);
            } else {
                assertThat(assessment.assessmentStatus()).isEqualTo("COVERED");
            }
        }
    }

    @Test
    void dailyQuotaOnTheFirstCallStopsEveryLaterCall() {
        AtomicInteger calls = new AtomicInteger();
        when(classificationProvider.classifyBatch(any())).thenAnswer(invocation -> {
            List<ComplianceClassificationRequest> requests = invocation.getArgument(0);
            calls.incrementAndGet();
            throw new ComplianceClassificationException(
                    ClassificationFailureCategory.QUOTA_EXHAUSTED, "Gemini API returned HTTP 429", false,
                    429, 1, "Daily quota exhausted");
        });

        ComplianceAnalysisResponse response = analyse();

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(calls.get()).isEqualTo(1);
        List<RequirementAssessmentResponse> classified = withEvidence(response);
        assertThat(classified).isNotEmpty().allSatisfy(a -> {
            assertThat(a.assessmentStatus()).isEqualTo("HUMAN_REVIEW_REQUIRED");
            assertThat(a.explanation()).isEqualTo(ClassificationFailureHandler.QUOTA_EXHAUSTED_EXPLANATION);
        });
    }

    @Test
    void anUnexpectedErrorInOneCallDoesNotLoseItsBatch() {
        AtomicInteger calls = new AtomicInteger();
        List<String> failedCodes = Collections.synchronizedList(new ArrayList<>());
        when(classificationProvider.classifyBatch(any())).thenAnswer(invocation -> {
            List<ComplianceClassificationRequest> requests = invocation.getArgument(0);
            if (calls.incrementAndGet() == 1) {
                requests.forEach(r -> failedCodes.add(r.requirementCode()));
                throw new IllegalStateException("boom");
            }
            Map<String, ComplianceClassificationResult> results = new LinkedHashMap<>();
            requests.forEach(r -> results.put(r.requirementCode(), covered(r.requirementCode())));
            return results;
        });

        ComplianceAnalysisResponse response = analyse();

        assertThat(response.status()).isEqualTo("COMPLETED");
        List<RequirementAssessmentResponse> classified = withEvidence(response);
        assertThat(classified).hasSize(response.assessments().stream()
                .filter(a -> a.retrievalScore() != null && a.retrievalScore() > 0.0).toList().size());
        assertThat(classified.stream().filter(a -> failedCodes.contains(a.requirementCode())))
                .isNotEmpty()
                .allMatch(a -> a.assessmentStatus().equals("HUMAN_REVIEW_REQUIRED"));
        assertThat(classified.stream().filter(a -> !failedCodes.contains(a.requirementCode())))
                .isNotEmpty()
                .allMatch(a -> a.assessmentStatus().equals("COVERED"));
    }

    private Map<String, String> analyseAndMapSections() {
        ComplianceAnalysisResponse response = analyse();
        return response.assessments().stream().collect(Collectors.toMap(
                RequirementAssessmentResponse::requirementCode,
                a -> a.section() == null ? "" : a.section()));
    }

    private Document createReadyDocument(String extractedText) {
        Document document = new Document(
                organization,
                "batching-sample.pdf",
                "batching-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "batching-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(extractedText);
        document.setPageCount(10);
        document.setProcessedAt(Instant.now());
        return documentRepository.save(document);
    }
}
