package dev.esgenius.service;

import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentPage;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.EsgCategory;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.DocumentPageRepository;
import dev.esgenius.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads one document, chunks it, and retrieves evidence. The transaction ends
 * when this method returns so callers can call Gemini without holding a database
 * transaction open.
 */
@Service
public class DocumentAssistantEvidenceService {

    static final String ASSISTANT_QUERY_CODE = "ASSISTANT-QUERY";

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final TextChunkingService chunkingService;
    private final LexicalEvidenceRetrievalService retrievalService;

    public DocumentAssistantEvidenceService(
            DocumentRepository documentRepository,
            DocumentPageRepository documentPageRepository,
            TextChunkingService chunkingService,
            LexicalEvidenceRetrievalService retrievalService) {
        this.documentRepository = documentRepository;
        this.documentPageRepository = documentPageRepository;
        this.chunkingService = chunkingService;
        this.retrievalService = retrievalService;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<RetrievedChunk> retrieveForQuestion(Long documentId, String question) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new BadRequestException(
                    "Document must be in READY status before analysis. Current status: " + document.getStatus());
        }

        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            throw new BadRequestException("Document has no extracted text available for analysis");
        }

        List<TextChunk> chunks = buildChunksForDocument(document);
        FrameworkRequirement query = transientQuery(question);
        return retrievalService.retrieve(query, chunks);
    }

    private List<TextChunk> buildChunksForDocument(Document document) {
        List<DocumentPage> pages = documentPageRepository.findByDocumentOrderByPageNumberAsc(document);
        if (!pages.isEmpty()) {
            List<ExtractedPdfPage> pageSources = pages.stream()
                    .map(page -> new ExtractedPdfPage(page.getPageNumber(), page.getExtractedText()))
                    .toList();
            return chunkingService.chunkPages(pageSources);
        }
        return chunkingService.chunk(document.getExtractedText());
    }

    private FrameworkRequirement transientQuery(String question) {
        // Description and framework text stay empty strings. Retrieval alias expansion
        // does not accept null, and this requirement is never persisted.
        return new FrameworkRequirement(
                null,
                ASSISTANT_QUERY_CODE,
                question,
                EsgCategory.GOVERNANCE,
                "",
                "",
                false,
                null);
    }
}
