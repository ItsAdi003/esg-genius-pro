package dev.esgenius.controller;

import dev.esgenius.dto.ComplianceAnalysisResponse;
import dev.esgenius.dto.ComplianceAnalysisSummaryResponse;
import dev.esgenius.dto.StartAnalysisRequest;
import dev.esgenius.service.ComplianceAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ComplianceAnalysisController {

    private final ComplianceAnalysisService complianceAnalysisService;

    public ComplianceAnalysisController(ComplianceAnalysisService complianceAnalysisService) {
        this.complianceAnalysisService = complianceAnalysisService;
    }

    @GetMapping("/documents/{documentId}/analyses")
    public ResponseEntity<List<ComplianceAnalysisSummaryResponse>> listAnalysesForDocument(
            @PathVariable Long documentId) {
        return ResponseEntity.ok(complianceAnalysisService.listAnalysesForDocument(documentId));
    }

    @PostMapping("/documents/{documentId}/analyses")
    public ResponseEntity<ComplianceAnalysisResponse> startAnalysis(
            @PathVariable Long documentId,
            @RequestBody StartAnalysisRequest request) {
        ComplianceAnalysisResponse response = complianceAnalysisService.startAnalysis(documentId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/analyses/{analysisId}")
    public ResponseEntity<ComplianceAnalysisResponse> getAnalysis(@PathVariable Long analysisId) {
        return ResponseEntity.ok(complianceAnalysisService.getAnalysis(analysisId));
    }
}
