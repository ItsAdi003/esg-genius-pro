package dev.esgenius.controller;

import dev.esgenius.dto.*;
import dev.esgenius.service.CompanyEsgService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for company ESG comparison and rating data.
 * 
 * PROTOTYPE DATA: All scores and ratings returned by these endpoints are
 * illustrative prototype values used to validate the ESGenius architecture.
 * They are NOT official MSCI or third-party ESG ratings.
 */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyEsgService companyEsgService;

    public CompanyController(CompanyEsgService companyEsgService) {
        this.companyEsgService = companyEsgService;
    }

    /**
     * GET /api/v1/companies
     * List all companies available for comparison.
     */
    @GetMapping
    public ResponseEntity<List<CompanySummaryResponse>> listCompanies() {
        return ResponseEntity.ok(companyEsgService.listCompanies());
    }

    /**
     * GET /api/v1/companies/{companyId}/esg
     * Get complete ESG profile for a single company.
     */
    @GetMapping("/{companyId}/esg")
    public ResponseEntity<CompanyEsgProfileResponse> getCompanyEsgProfile(
            @PathVariable Long companyId) {
        return ResponseEntity.ok(companyEsgService.getCompanyEsgProfile(companyId));
    }

    /**
     * GET /api/v1/companies/compare?companyA={id}&companyB={id}
     * Compare two companies and get detailed comparison data.
     * 
     * Rejects: companyA == companyB
     */
    @GetMapping("/compare")
    public ResponseEntity<CompanyComparisonResponse> compareCompanies(
            @RequestParam Long companyA,
            @RequestParam Long companyB) {

        if (companyA.equals(companyB)) {
            throw new IllegalArgumentException("Cannot compare a company with itself");
        }

        return ResponseEntity.ok(companyEsgService.compareCompanies(companyA, companyB));
    }
}
