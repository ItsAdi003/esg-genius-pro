package dev.esgenius.service;

import dev.esgenius.dto.*;
import dev.esgenius.entity.*;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for company ESG comparison and rating data.
 * 
 * Handles retrieval of ESG profiles, ratings, key issues, events, and
 * deterministic comparison insights.
 */
@Service
@Transactional(readOnly = true)
public class CompanyEsgService {

    private final OrganizationRepository organizationRepository;
    private final EsgRatingSnapshotRepository ratingSnapshotRepository;
    private final EsgKeyIssueRepository keyIssueRepository;
    private final CompanyKeyIssueAssessmentRepository assessmentRepository;
    private final EsgEventRepository eventRepository;

    public CompanyEsgService(
            OrganizationRepository organizationRepository,
            EsgRatingSnapshotRepository ratingSnapshotRepository,
            EsgKeyIssueRepository keyIssueRepository,
            CompanyKeyIssueAssessmentRepository assessmentRepository,
            EsgEventRepository eventRepository) {
        this.organizationRepository = organizationRepository;
        this.ratingSnapshotRepository = ratingSnapshotRepository;
        this.keyIssueRepository = keyIssueRepository;
        this.assessmentRepository = assessmentRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Get all listed companies available for comparison.
     */
    public List<CompanySummaryResponse> listCompanies() {
        return organizationRepository.findAllListedCompanies().stream()
                .map(org -> new CompanySummaryResponse(
                        org.getId(),
                        org.getName(),
                        org.getTicker(),
                        org.getIndustry()))
                .collect(Collectors.toList());
    }

    /**
     * Get complete ESG profile for a single company.
     */
    public CompanyEsgProfileResponse getCompanyEsgProfile(Long companyId) {
        Organization company = organizationRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        EsgRatingResponse currentRating = getCurrentRating(company);
        List<EsgRatingHistoryResponse> ratingHistory = getRatingHistory(company);
        List<KeyIssueAssessmentResponse> materialIssues = getMaterialIssues(company);
        List<EsgEventResponse> recentEvents = getRecentEvents(company, 10);

        return new CompanyEsgProfileResponse(
                company.getId(),
                company.getName(),
                company.getTicker(),
                company.getIndustry(),
                currentRating,
                ratingHistory,
                materialIssues,
                recentEvents);
    }

    /**
     * Compare two companies and return detailed comparison data.
     */
    public CompanyComparisonResponse compareCompanies(Long companyAId, Long companyBId) {
        // Validate that both companies exist
        Organization companyA = organizationRepository.findById(companyAId)
                .orElseThrow(() -> new ResourceNotFoundException("Company A not found: " + companyAId));
        Organization companyB = organizationRepository.findById(companyBId)
                .orElseThrow(() -> new ResourceNotFoundException("Company B not found: " + companyBId));

        // Reject if same company
        if (companyAId.equals(companyBId)) {
            throw new IllegalArgumentException("Cannot compare a company with itself");
        }

        // Get profiles for both companies
        CompanyEsgProfileResponse profileA = getCompanyEsgProfile(companyAId);
        CompanyEsgProfileResponse profileB = getCompanyEsgProfile(companyBId);

        // Generate comparison insight
        String insight = generateComparisonInsight(companyA, companyB);

        return new CompanyComparisonResponse(profileA, profileB, insight);
    }

    /**
     * Get current ESG rating for a company.
     */
    private EsgRatingResponse getCurrentRating(Organization company) {
        return ratingSnapshotRepository.findLatestByOrganization(company)
                .map(snapshot -> new EsgRatingResponse(
                        snapshot.getOverallScore(),
                        snapshot.getEnvironmentalScore(),
                        snapshot.getSocialScore(),
                        snapshot.getGovernanceScore(),
                        snapshot.getRatingBand().name(),
                        snapshot.getAssessmentDate().toString()))
                .orElseThrow(
                        () -> new ResourceNotFoundException("No rating data found for company: " + company.getId()));
    }

    /**
     * Get rating history for trend chart.
     * Formats assessment dates as quarters (e.g., "Q4 2025").
     */
    private List<EsgRatingHistoryResponse> getRatingHistory(Organization company) {
        List<EsgRatingSnapshot> snapshots = ratingSnapshotRepository
                .findByOrganizationOrderByAssessmentDateDesc(company);

        return snapshots.stream()
                .map(snapshot -> {
                    String quarter = formatDateAsQuarter(snapshot.getAssessmentDate());
                    return new EsgRatingHistoryResponse(quarter, snapshot.getOverallScore());
                })
                .sorted(Comparator.comparing(EsgRatingHistoryResponse::quarter))
                .collect(Collectors.toList());
    }

    /**
     * Get material ESG issues for a company.
     */
    private List<KeyIssueAssessmentResponse> getMaterialIssues(Organization company) {
        return assessmentRepository.findLatestAssessmentsByOrganization(company).stream()
                .map(assessment -> new KeyIssueAssessmentResponse(
                        assessment.getId(),
                        assessment.getKeyIssue().getCode(),
                        assessment.getKeyIssue().getName(),
                        assessment.getKeyIssue().getPillar().name(),
                        assessment.getScore(),
                        assessment.getRiskLevel().name()))
                .collect(Collectors.toList());
    }

    /**
     * Get recent ESG events for a company.
     */
    private List<EsgEventResponse> getRecentEvents(Organization company, int limit) {
        return eventRepository.findRecentEventsByOrganization(company, limit).stream()
                .map(event -> new EsgEventResponse(
                        event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getPillar().name(),
                        event.getSeverity().name(),
                        event.getEventDate().toString(),
                        event.getScoreImpact(),
                        event.getIsPrototype()))
                .collect(Collectors.toList());
    }

    /**
     * Generate deterministic comparison insight based on mock data.
     * 
     * No LLM. Pure logic based on scores, controversies, etc.
     */
    private String generateComparisonInsight(Organization companyA, Organization companyB) {
        // Get latest ratings
        EsgRatingSnapshot ratingA = ratingSnapshotRepository.findLatestByOrganization(companyA)
                .orElseThrow(() -> new ResourceNotFoundException("No rating data for: " + companyA.getId()));
        EsgRatingSnapshot ratingB = ratingSnapshotRepository.findLatestByOrganization(companyB)
                .orElseThrow(() -> new ResourceNotFoundException("No rating data for: " + companyB.getId()));

        // Determine leader and follower
        boolean aIsLeader = ratingA.getOverallScore() > ratingB.getOverallScore();
        Organization leader = aIsLeader ? companyA : companyB;
        Organization follower = aIsLeader ? companyB : companyA;
        EsgRatingSnapshot leaderRating = aIsLeader ? ratingA : ratingB;
        EsgRatingSnapshot followerRating = aIsLeader ? ratingB : ratingA;

        // Build reasons
        List<String> reasons = new ArrayList<>();

        // Environmental comparison
        double envGap = Math.abs(leaderRating.getEnvironmentalScore() - followerRating.getEnvironmentalScore());
        if (envGap > 0.5) {
            reasons.add("stronger Environmental scores");
        }

        // Social comparison
        double socGap = Math.abs(leaderRating.getSocialScore() - followerRating.getSocialScore());
        if (socGap > 0.5) {
            reasons.add("better Social performance");
        }

        // Governance comparison
        double govGap = Math.abs(leaderRating.getGovernanceScore() - followerRating.getGovernanceScore());
        if (govGap > 0.5) {
            reasons.add("superior Governance");
        }

        // Controversy comparison
        int leaderEvents = eventRepository.findByOrganizationOrderByEventDateDesc(leader).size();
        int followerEvents = eventRepository.findByOrganizationOrderByEventDateDesc(follower).size();
        if (leaderEvents < followerEvents) {
            reasons.add("fewer high-severity ESG controversies (" + leaderEvents + " vs " + followerEvents + ")");
        }

        // If no specific reasons, use generic reason
        if (reasons.isEmpty()) {
            reasons.add("consistent execution across ESG pillars");
        }

        // Build main insight
        String insight = String.format(
                "%s currently has a higher prototype ESGenius score primarily because of %s",
                leader.getName(),
                String.join(", ", reasons));

        // Add follower strength
        double followerEnv = followerRating.getEnvironmentalScore();
        double followerSoc = followerRating.getSocialScore();
        double followerGov = followerRating.getGovernanceScore();

        List<String> strengths = new ArrayList<>();
        if (followerGov > followerEnv && followerGov > followerSoc) {
            strengths.add("Governance");
        }
        if (followerSoc > followerEnv && followerSoc > followerGov) {
            strengths.add("Social");
        }
        if (followerEnv > followerSoc && followerEnv > followerGov) {
            strengths.add("Environmental");
        }

        if (!strengths.isEmpty()) {
            insight += String.format(
                    ". %s remains comparatively strong in %s but has lower scores in selected material issues.",
                    follower.getName(),
                    String.join(" and ", strengths));
        } else {
            insight += String.format(
                    ". %s remains comparatively strong in multiple areas but lags in overall execution.",
                    follower.getName());
        }

        return insight;
    }

    /**
     * Format a LocalDate as a quarter string (e.g., "Q4 2025").
     */
    private String formatDateAsQuarter(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return "Q" + quarter + " " + date.getYear();
    }
}
