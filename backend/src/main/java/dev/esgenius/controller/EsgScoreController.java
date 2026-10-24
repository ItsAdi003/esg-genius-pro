package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.dto.AnalysisIndustryRequest;
import dev.esgenius.dto.ApplicabilityRequest;
import dev.esgenius.dto.ApplicabilityResponse;
import dev.esgenius.dto.EsgScoreResponse;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.esgscore.EsgScoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses/{analysisId}")
public class EsgScoreController {

    private final EsgScoreService esgScoreService;
    private final DocumentAccessPolicy documentAccessPolicy;

    public EsgScoreController(EsgScoreService esgScoreService, DocumentAccessPolicy documentAccessPolicy) {
        this.esgScoreService = esgScoreService;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @GetMapping("/esg-score")
    public ResponseEntity<EsgScoreResponse> getScore(@PathVariable Long analysisId, HttpServletRequest request) {
        return ResponseEntity.ok(esgScoreService.getScore(analysisId, caller(request)));
    }

    @PutMapping("/industry")
    public ResponseEntity<EsgScoreResponse> setIndustry(
            @PathVariable Long analysisId,
            @RequestBody AnalysisIndustryRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(esgScoreService.setIndustry(analysisId, body, caller(request)));
    }

    @PutMapping("/requirements/{requirementId}/applicability")
    public ResponseEntity<ApplicabilityResponse> setApplicability(
            @PathVariable Long analysisId,
            @PathVariable Long requirementId,
            @RequestBody ApplicabilityRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                esgScoreService.setApplicability(analysisId, requirementId, body, caller(request)));
    }

    private Caller caller(HttpServletRequest request) {
        return documentAccessPolicy.resolve(AuthenticatedUser.from(request));
    }
}
