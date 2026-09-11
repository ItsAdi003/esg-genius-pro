package dev.esgenius.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.EvidenceChunkResponse;
import dev.esgenius.dto.RequirementAssessmentResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.*;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import dev.esgenius.repository.OrganizationRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.support.ComplianceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PageAwareEvidenceProvenanceTest {

    @Autowired
    private LexicalEvidenceRetrievalService retrievalService;

    @Autowired
    private TextChunkingService chunkingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentPageRepository documentPageRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private FrameworkRepository frameworkRepository;

    @Autowired
    private FrameworkRequirementRepository requirementRepository;

    @Autowired
    private ComplianceAnalysisRepository analysisRepository;

    @Autowired
    private RequirementAssessmentRepository assessmentRepository;

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;

    private Organization organization;
    private Framework brsrFramework;

    @BeforeEach
    void setUp() {
        assessmentRepository.deleteAll();
        analysisRepository.deleteAll();
        documentPageRepository.deleteAll();
        documentRepository.deleteAll();
        organization = organizationRepository.findByTicker("INFY").orElseThrow();
        brsrFramework = frameworkRepository.findByCode("BRSR").orElseThrow();
    }

    @Test
    void retrievalRetainsPageNumber() {
        FrameworkRequirement requirement = requirementRepository.findByFramework(brsrFramework).stream()
                .filter(r -> "ENV-003".equals(r.getRequirementCode()))
                .findFirst()
                .orElseThrow();

        List<TextChunk> chunks = chunkingService.chunkPages(List.of(
                new ExtractedPdfPage(1, "Employee health and safety training covered all office locations."),
                new ExtractedPdfPage(27,
                        "The company reduced Scope 1 and Scope 2 greenhouse gas emissions by 18% year-on-year.")));

        List<RetrievedChunk> retrieved = retrievalService.retrieve(requirement, chunks);

        assertThat(retrieved).isNotEmpty();
        assertThat(retrieved.get(0).pageNumber()).isEqualTo(27);
    }

    @Test
    void legacyEvidenceJsonWithoutPageNumberStillParses() throws Exception {
        String legacyJson =
                "[{\"chunkIndex\":14,\"text\":\"Legacy evidence text.\",\"retrievalScore\":92.4}]";

        List<EvidenceChunkResponse> chunks =
                objectMapper.readValue(legacyJson, new TypeReference<List<EvidenceChunkResponse>>() {});

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkIndex()).isEqualTo(14);
        assertThat(chunks.get(0).pageNumber()).isNull();
        assertThat(chunks.get(0).text()).isEqualTo("Legacy evidence text.");
        assertThat(chunks.get(0).retrievalScore()).isEqualTo(92.4);
    }

    @Test
    void pageAwareEvidenceJsonRoundTrips() throws Exception {
        List<EvidenceChunkResponse> payload = List.of(
                new EvidenceChunkResponse(14, 27, "Page-aware evidence text.", 92.4));

        String json = objectMapper.writeValueAsString(payload);
        List<EvidenceChunkResponse> parsed =
                objectMapper.readValue(json, new TypeReference<List<EvidenceChunkResponse>>() {});

        assertThat(parsed.get(0).pageNumber()).isEqualTo(27);
    }

    @Test
    void legacyDocumentChunkingUsesNullPageNumbers() {
        List<TextChunk> chunks = chunkingService.chunk(ComplianceTestFixtures.ESG_SAMPLE_TEXT);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(chunk -> chunk.pageNumber() == null);
    }

    @Test
    void pageAwareAnalysisPersistsPageNumbersInEvidenceResponse() {
        Document document = createReadyDocumentWithPages();

        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                document.getId(), new StartAnalysisRequest(null, "BRSR"));

        RequirementAssessmentResponse scope1 = response.assessments().stream()
                .filter(a -> "ENV-003".equals(a.requirementCode()))
                .findFirst()
                .orElseThrow();

        assertThat(scope1.evidenceChunks()).isNotEmpty();
        assertThat(scope1.evidenceChunks().get(0).pageNumber()).isEqualTo(27);
    }

    @Test
    void documentWithPageRowsUsesPageAwareChunking() {
        Document document = createReadyDocumentWithPages();

        List<DocumentPage> pages = documentPageRepository.findByDocumentOrderByPageNumberAsc(document);
        List<TextChunk> chunks = chunkingService.chunkPages(pages.stream()
                .map(page -> new ExtractedPdfPage(page.getPageNumber(), page.getExtractedText()))
                .toList());

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(chunk -> chunk.pageNumber() != null);
        assertThat(chunks).extracting(TextChunk::pageNumber).contains(1, 27);
    }

    private Document createReadyDocumentWithPages() {
        Document document = new Document(
                organization,
                "page-aware.pdf",
                "page-aware-" + System.nanoTime() + ".pdf",
                DocumentType.BRSR,
                2025,
                1024L,
                "page-aware-" + System.nanoTime() + ".pdf");
        document.setStatus(DocumentStatus.READY);
        document.setExtractedText(ComplianceTestFixtures.ESG_SAMPLE_TEXT);
        document.setPageCount(27);
        document.setProcessedAt(Instant.now());
        document = documentRepository.save(document);

        documentPageRepository.save(new DocumentPage(
                document, 1, "Employee health and safety training covered all office locations."));
        documentPageRepository.save(new DocumentPage(
                document,
                27,
                "The company reduced Scope 1 and Scope 2 greenhouse gas emissions by 18% year-on-year."));
        return document;
    }
}
