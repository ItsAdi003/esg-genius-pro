package dev.esgenius.service;

import dev.esgenius.dto.ReportExportResponse;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.Document;
import dev.esgenius.entity.ReportExport;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.ReportExportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReportExportService {

    public static final int LIST_LIMIT = 50;

    private final ReportExportRepository reportExportRepository;
    private final ComplianceAnalysisRepository analysisRepository;
    private final DocumentAccessPolicy documentAccessPolicy;

    public ReportExportService(
            ReportExportRepository reportExportRepository,
            ComplianceAnalysisRepository analysisRepository,
            DocumentAccessPolicy documentAccessPolicy) {
        this.reportExportRepository = reportExportRepository;
        this.analysisRepository = analysisRepository;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @Transactional
    public void recordPdfExport(Long analysisId, Caller caller, long sizeBytes) {
        ComplianceAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
        UUID userId = caller == null ? null : caller.userId();
        reportExportRepository.save(new ReportExport(
                analysis, userId, ReportExport.FORMAT_PDF, sizeBytes));
    }

    @Transactional(readOnly = true)
    public List<ReportExportResponse> listRecent(Caller caller) {
        PageRequest pageable = PageRequest.of(0, LIST_LIMIT);
        List<ReportExport> exports;
        if (caller.admin()) {
            exports = reportExportRepository.findRecentAll(pageable);
        } else if (caller.userId() == null) {
            return List.of();
        } else {
            exports = reportExportRepository.findRecentByUserId(caller.userId(), pageable);
        }
        return exports.stream()
                .filter(export -> documentAccessPolicy.canView(documentOf(export), caller))
                .limit(LIST_LIMIT)
                .map(this::toResponse)
                .toList();
    }

    private static Document documentOf(ReportExport export) {
        return export.getAnalysis().getDocument();
    }

    private ReportExportResponse toResponse(ReportExport export) {
        ComplianceAnalysis analysis = export.getAnalysis();
        Document document = analysis.getDocument();
        return new ReportExportResponse(
                export.getId(),
                analysis.getId(),
                document.getId(),
                document.getOriginalFilename(),
                analysis.getFramework().getCode(),
                export.getFormat(),
                export.getSizeBytes(),
                export.getGeneratedAt());
    }
}
