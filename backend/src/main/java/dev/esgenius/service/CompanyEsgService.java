package dev.esgenius.service;

import dev.esgenius.dto.*;
import dev.esgenius.entity.*;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private static final double PILLAR_LEAD_THRESHOLD = 0.05;

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
            throw new BadRequestException("Cannot compare a company with itself");
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
        return ratingSnapshotRepository.findFirstByOrganizationOrderByAssessmentDateDesc(company)
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
     * Get rating history for trend charts.
     *
     * Ordering: chronological oldest → newest by {@code assessmentDate}
     * (not quarter-label lexicographic order).
     * Quarter labels (e.g. "Q4 2025") are presentation-only.
     */
    private List<EsgRatingHistoryResponse> getRatingHistory(Organization company) {
        return ratingSnapshotRepository.findByOrganizationOrderByAssessmentDateAsc(company).stream()
                .map(snapshot -> new EsgRatingHistoryResponse(
                        formatDateAsQuarter(snapshot.getAssessmentDate()),
                        snapshot.getOverallScore()))
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
        return eventRepository.findByOrganizationOrderByEventDateDesc(company, PageRequest.of(0, limit)).stream()
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
     * Generate deterministic comparison insight from latest rating snapshots.
     *
     * Pillar statements compare the two companies directly. A company is only
     * described as leading on a pillar when its score is higher than the other's.
     */
    private String generateComparisonInsight(Organization companyA, Organization companyB) {
        EsgRatingSnapshot ratingA = ratingSnapshotRepository.findFirstByOrganizationOrderByAssessmentDateDesc(companyA)
                .orElseThrow(() -> new ResourceNotFoundException("No rating data for: " + companyA.getId()));
        EsgRatingSnapshot ratingB = ratingSnapshotRepository.findFirstByOrganizationOrderByAssessmentDateDesc(companyB)
                .orElseThrow(() -> new ResourceNotFoundException("No rating data for: " + companyB.getId()));

        double overallDiff = ratingA.getOverallScore() - ratingB.getOverallScore();
        if (Math.abs(overallDiff) <= PILLAR_LEAD_THRESHOLD) {
            return String.format(
                    "%s and %s have similar prototype ESGenius overall scores (%.1f vs %.1f).",
                    companyA.getName(),
                    companyB.getName(),
                    ratingA.getOverallScore(),
                    ratingB.getOverallScore());
        }

        boolean aLeadsOverall = overallDiff > 0;
        Organization leader = aLeadsOverall ? companyA : companyB;
        Organization other = aLeadsOverall ? companyB : companyA;
        EsgRatingSnapshot leaderRating = aLeadsOverall ? ratingA : ratingB;
        EsgRatingSnapshot otherRating = aLeadsOverall ? ratingB : ratingA;

        List<String> leaderAdvantages = new ArrayList<>();
        addPillarAdvantage(leaderAdvantages, "Environmental",
                leaderRating.getEnvironmentalScore(), otherRating.getEnvironmentalScore());
        addPillarAdvantage(leaderAdvantages, "Social",
                leaderRating.getSocialScore(), otherRating.getSocialScore());
        addPillarAdvantage(leaderAdvantages, "Governance",
                leaderRating.getGovernanceScore(), otherRating.getGovernanceScore());

        int leaderEvents = eventRepository.findByOrganizationOrderByEventDateDesc(leader).size();
        int otherEvents = eventRepository.findByOrganizationOrderByEventDateDesc(other).size();
        if (leaderEvents < otherEvents) {
            leaderAdvantages.add(String.format("fewer recorded ESG events (%d vs %d)", leaderEvents, otherEvents));
        }

        if (leaderAdvantages.isEmpty()) {
            leaderAdvantages.add(String.format(
                    "a higher overall score (%.1f vs %.1f) with broadly similar pillar scores",
                    leaderRating.getOverallScore(),
                    otherRating.getOverallScore()));
        }

        StringBuilder insight = new StringBuilder();
        insight.append(String.format(
                "%s has a higher prototype ESGenius overall score primarily due to %s.",
                leader.getName(),
                joinNaturalList(leaderAdvantages)));

        List<String> otherPillarLeads = new ArrayList<>();
        addPillarLead(otherPillarLeads, "Environmental",
                otherRating.getEnvironmentalScore(), leaderRating.getEnvironmentalScore());
        addPillarLead(otherPillarLeads, "Social",
                otherRating.getSocialScore(), leaderRating.getSocialScore());
        addPillarLead(otherPillarLeads, "Governance",
                otherRating.getGovernanceScore(), leaderRating.getGovernanceScore());

        if (otherPillarLeads.isEmpty()) {
            insight.append(String.format(
                    " %s does not lead on Environmental, Social or Governance in this comparison.",
                    other.getName()));
        } else {
            insight.append(String.format(
                    " %s leads on %s.",
                    other.getName(),
                    joinNaturalList(otherPillarLeads)));
        }

        return insight.toString();
    }

    private void addPillarAdvantage(
            List<String> advantages,
            String pillar,
            double leaderScore,
            double otherScore) {
        if (leaderScore - otherScore > PILLAR_LEAD_THRESHOLD) {
            advantages.add(String.format(
                    "stronger %s (%.1f vs %.1f)",
                    pillar,
                    leaderScore,
                    otherScore));
        }
    }

    private void addPillarLead(
            List<String> leads,
            String pillar,
            double score,
            double opponentScore) {
        if (score - opponentScore > PILLAR_LEAD_THRESHOLD) {
            leads.add(String.format("%s (%.1f vs %.1f)", pillar, score, opponentScore));
        }
    }

    private String joinNaturalList(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        if (items.size() == 2) {
            return items.get(0) + " and " + items.get(1);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + ", and " + items.get(items.size() - 1);
    }

    /**
     * Format a LocalDate as a quarter string (e.g., "Q4 2025").
     */
    private String formatDateAsQuarter(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return "Q" + quarter + " " + date.getYear();
    }
}
