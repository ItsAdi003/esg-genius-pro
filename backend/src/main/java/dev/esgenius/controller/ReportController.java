package dev.esgenius.controller;

import dev.esgenius.service.GapAssessmentPdfService;
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

    private final GapAssessmentPdfService gapAssessmentPdfService;

    public ReportController(GapAssessmentPdfService gapAssessmentPdfService) {
        this.gapAssessmentPdfService = gapAssessmentPdfService;
    }

    @GetMapping(value = "/analyses/{analysisId}/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadGapAssessmentPdf(@PathVariable Long analysisId) {
        byte[] pdf = gapAssessmentPdfService.generate(analysisId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("gap-assessment-" + analysisId + ".pdf")
                .build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
