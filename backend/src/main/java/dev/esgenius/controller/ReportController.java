package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.GapAssessmentPdfService;
import dev.esgenius.service.ReportExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final GapAssessmentPdfService gapAssessmentPdfService;
    private final DocumentAccessPolicy documentAccessPolicy;
    private final ReportExportService reportExportService;

    public ReportController(
            GapAssessmentPdfService gapAssessmentPdfService,
            DocumentAccessPolicy documentAccessPolicy,
            ReportExportService reportExportService) {
        this.gapAssessmentPdfService = gapAssessmentPdfService;
        this.documentAccessPolicy = documentAccessPolicy;
        this.reportExportService = reportExportService;
    }

    @GetMapping(value = "/analyses/{analysisId}/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadGapAssessmentPdf(
            @PathVariable Long analysisId,
            HttpServletRequest request) {
        Caller caller = documentAccessPolicy.resolve(AuthenticatedUser.from(request));
        byte[] pdf = gapAssessmentPdfService.generate(analysisId, caller);
        try {
            reportExportService.recordPdfExport(analysisId, caller, pdf.length);
        } catch (Exception ex) {
            log.warn("Failed to record gap-assessment PDF export analysisId={}", analysisId, ex);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("gap-assessment-" + analysisId + ".pdf")
                .build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
