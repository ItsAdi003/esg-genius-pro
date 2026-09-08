package dev.esgenius.dto;

/**
 * Comparison response combining two companies' ESG profiles.
 */
public record CompanyComparisonResponse(
        CompanyEsgProfileResponse companyA,
        CompanyEsgProfileResponse companyB,
        String comparisonInsight) {
}
