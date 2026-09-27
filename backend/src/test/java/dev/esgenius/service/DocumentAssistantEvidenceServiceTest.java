package dev.esgenius.service;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentPage;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.DocumentType;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAssistantEvidenceServiceTest {

    private static final String GROUNDWATER =
            "Groundwater withdrawal during the reporting period was 12500 kilolitres across all manufacturing plants and offices.";

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentPageRepository documentPageRepository;

    private RecordingChunkingService chunkingService;
    private RecordingRetrievalService retrievalService;
    private DocumentAssistantEvidenceService service;
    private Document document;

    @BeforeEach
    void setUp() {
        chunkingService = new RecordingChunkingService();
        retrievalService = new RecordingRetrievalService();
        service = new DocumentAssistantEvidenceService(
                documentRepository, documentPageRepository, chunkingService, retrievalService);
        document = readyDocument(GROUNDWATER);
        lenient().when(documentRepository.findById(7L)).thenReturn(java.util.Optional.of(document));
    }

    @Test
    void usesPageChunksWhenPageRowsExistAndBuildsTransientQuery() {
        when(documentPageRepository.findByDocumentOrderByPageNumberAsc(document))
                .thenReturn(List.of(new DocumentPage(document, 3, GROUNDWATER)));

        List<RetrievedChunk> chunks = service.retrieveForQuestion(7L, "groundwater withdrawal kilolitres");

        assertThat(chunkingService.chunkPagesCalls).isEqualTo(1);
        assertThat(chunkingService.chunkCalls).isZero();
        assertThat(chunkingService.lastPages).singleElement().satisfies(page -> {
            assertThat(page.pageNumber()).isEqualTo(3);
            assertThat(page.text()).isEqualTo(GROUNDWATER);
        });
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).pageNumber()).isEqualTo(3);

        FrameworkRequirement query = retrievalService.lastRequirement;
        assertThat(query.getRequirementCode()).isEqualTo(DocumentAssistantEvidenceService.ASSISTANT_QUERY_CODE);
        assertThat(query.getTitle()).isEqualTo("groundwater withdrawal kilolitres");
        assertThat(query.getId()).isNull();
        assertThat(query.getDescription()).isEmpty();
    }

    @Test
    void usesLegacyExtractedTextWhenNoPageRowsExist() {
        when(documentPageRepository.findByDocumentOrderByPageNumberAsc(document)).thenReturn(List.of());

        List<RetrievedChunk> chunks = service.retrieveForQuestion(7L, "groundwater withdrawal kilolitres");

        assertThat(chunkingService.chunkCalls).isEqualTo(1);
        assertThat(chunkingService.chunkPagesCalls).isZero();
        assertThat(chunkingService.lastText).isEqualTo(GROUNDWATER);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).pageNumber()).isNull();
        assertThat(retrievalService.lastRequirement.getRequirementCode())
                .isEqualTo(DocumentAssistantEvidenceService.ASSISTANT_QUERY_CODE);
    }

    @Test
    void unrelatedQuestionRetrievesNoChunks() {
        when(documentPageRepository.findByDocumentOrderByPageNumberAsc(document)).thenReturn(List.of());

        List<RetrievedChunk> unmatched = service.retrieveForQuestion(7L, "whistleblower helpline complaints");

        assertThat(unmatched).isEmpty();
    }

    @Test
    void rejectsDocumentThatIsNotReady() {
        document.setStatus(DocumentStatus.PROCESSING);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.retrieveForQuestion(7L, "groundwater withdrawal"));

        assertThat(ex.getMessage()).contains("READY");
        assertThat(chunkingService.chunkCalls).isZero();
        assertThat(chunkingService.chunkPagesCalls).isZero();
    }

    @Test
    void rejectsDocumentWithoutExtractedText() {
        document.setExtractedText("  ");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.retrieveForQuestion(7L, "groundwater withdrawal"));

        assertThat(ex.getMessage()).contains("extracted text");
        assertThat(chunkingService.chunkCalls).isZero();
    }

    @Test
    void rejectsMissingDocument() {
        when(documentRepository.findById(7L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.retrieveForQuestion(7L, "groundwater withdrawal"));
    }

    @Test
    void lookupTransactionClosesBeforeCallerContinues() throws Exception {
        Transactional transactional = AnnotationUtils.findAnnotation(
                DocumentAssistantEvidenceService.class.getMethod("retrieveForQuestion", Long.class, String.class),
                Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private Document readyDocument(String extractedText) {
        Document ready = new Document(
                null, "report.pdf", "stored.pdf", DocumentType.BRSR, 2025, 100L, "path");
        ready.setStatus(DocumentStatus.READY);
        ready.setExtractedText(extractedText);
        return ready;
    }

    private static final class RecordingChunkingService extends TextChunkingService {
        private int chunkPagesCalls;
        private int chunkCalls;
        private List<ExtractedPdfPage> lastPages;
        private String lastText;

        @Override
        public List<TextChunk> chunkPages(List<ExtractedPdfPage> pages) {
            chunkPagesCalls++;
            lastPages = pages;
            return super.chunkPages(pages);
        }

        @Override
        public List<TextChunk> chunk(String text) {
            chunkCalls++;
            lastText = text;
            return super.chunk(text);
        }
    }

    private static final class RecordingRetrievalService extends LexicalEvidenceRetrievalService {
        private FrameworkRequirement lastRequirement;

        @Override
        public List<RetrievedChunk> retrieve(FrameworkRequirement requirement, List<TextChunk> chunks) {
            lastRequirement = requirement;
            return super.retrieve(requirement, chunks);
        }
    }
}
