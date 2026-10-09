package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.ComplianceAnalysisSummaryResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.service.Caller;
import dev.esgenius.service.ComplianceAnalysisService;
import dev.esgenius.service.DocumentAccessPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ComplianceAnalysisController {

    private final ComplianceAnalysisService complianceAnalysisService;
    private final DocumentAccessPolicy documentAccessPolicy;

    public ComplianceAnalysisController(
            ComplianceAnalysisService complianceAnalysisService,
            DocumentAccessPolicy documentAccessPolicy) {
        this.complianceAnalysisService = complianceAnalysisService;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @GetMapping("/documents/{documentId}/analyses")
    public ResponseEntity<List<ComplianceAnalysisSummaryResponse>> listAnalysesForDocument(
            @PathVariable Long documentId,
            HttpServletRequest request) {
        return ResponseEntity.ok(complianceAnalysisService.listAnalysesForDocument(documentId, caller(request)));
    }

    @PostMapping("/documents/{documentId}/analyses")
    public ResponseEntity<ComplianceAnalysisResponse> startAnalysis(
            @PathVariable Long documentId,
            @RequestBody StartAnalysisRequest request,
            HttpServletRequest httpRequest) {
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(
                documentId, request, caller(httpRequest));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/analyses/{analysisId}")
    public ResponseEntity<ComplianceAnalysisResponse> getAnalysis(
            @PathVariable Long analysisId,
            HttpServletRequest request) {
        return ResponseEntity.ok(complianceAnalysisService.getAnalysis(analysisId, caller(request)));
    }

    private Caller caller(HttpServletRequest request) {
        return documentAccessPolicy.resolve(AuthenticatedUser.from(request));
    }
}
