package dev.esgenius.service;

import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.DocumentStatus;
import dev.esgenius.entity.Framework;
import dev.esgenius.entity.FrameworkRequirement;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.DocumentRepository;
import dev.esgenius.repository.FrameworkRepository;
import dev.esgenius.repository.FrameworkRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComplianceAnalysisCreationService {

    private final ComplianceAnalysisRepository analysisRepository;
    private final DocumentRepository documentRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkRequirementRepository requirementRepository;

    public ComplianceAnalysisCreationService(
            ComplianceAnalysisRepository analysisRepository,
            DocumentRepository documentRepository,
            FrameworkRepository frameworkRepository,
            FrameworkRequirementRepository requirementRepository) {
        this.analysisRepository = analysisRepository;
        this.documentRepository = documentRepository;
        this.frameworkRepository = frameworkRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createInProgressAnalysis(Long documentId, StartAnalysisRequest request) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new BadRequestException(
                    "Document must be in READY status before analysis. Current status: " + document.getStatus());
        }

        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            throw new BadRequestException("Document has no extracted text available for analysis");
        }

        Framework framework = resolveFramework(request);
        List<FrameworkRequirement> requirements = requirementRepository.findByFramework(framework);
        if (requirements.isEmpty()) {
            throw new BadRequestException("Framework has no requirements: " + framework.getCode());
        }

        if (analysisRepository.existsByDocumentIdAndFrameworkIdAndStatus(
                documentId, framework.getId(), AnalysisStatus.IN_PROGRESS)) {
            throw new BadRequestException(
                    "An analysis is already in progress for this document and framework.");
        }

        ComplianceAnalysis analysis = new ComplianceAnalysis(document, framework);
        analysisRepository.save(analysis);
        return analysis.getId();
    }

    private Framework resolveFramework(StartAnalysisRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required with frameworkId or frameworkCode");
        }

        if (request.frameworkId() != null) {
            return frameworkRepository.findById(request.frameworkId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Framework not found: " + request.frameworkId()));
        }

        if (request.frameworkCode() != null && !request.frameworkCode().isBlank()) {
            return frameworkRepository.findByCode(request.frameworkCode().trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Framework not found: " + request.frameworkCode()));
        }

        throw new BadRequestException("frameworkId or frameworkCode is required");
    }
}
