package dev.esgenius.service.esgscore;

import dev.esgenius.dto.AnalysisIndustryRequest;
import dev.esgenius.dto.ApplicabilityRequest;
import dev.esgenius.dto.ApplicabilityResponse;
import dev.esgenius.dto.EsgScoreResponse;
import dev.esgenius.entity.AnalysisStatus;
import dev.esgenius.entity.AssessmentStatus;
import dev.esgenius.entity.ComplianceAnalysis;
import dev.esgenius.entity.EsgIndustry;
import dev.esgenius.entity.EsgPillar;
import dev.esgenius.entity.RequirementAssessment;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.exception.ResourceNotFoundException;
import dev.esgenius.repository.ComplianceAnalysisRepository;
import dev.esgenius.repository.RequirementAssessmentRepository;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.esgscore.EsgScoreCalculator.ScoreInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MSCI-style ESG score for a completed BRSR analysis, plus the two reviewer inputs it depends on:
 * the sector, and which requirements do not apply to the company.
 */
@Service
public class EsgScoreService {

    static final String FRAMEWORK_CODE = "BRSR";
    static final int MAX_REASON_LENGTH = 500;

    static final String NOTE = "MSCI-style score based on the quality of BRSR disclosure. It is not an official "
            + "MSCI rating and does not measure real-world ESG performance.";

    private final ComplianceAnalysisRepository analysisRepository;
    private final RequirementAssessmentRepository assessmentRepository;
    private final DocumentAccessPolicy documentAccessPolicy;

    public EsgScoreService(
            ComplianceAnalysisRepository analysisRepository,
            RequirementAssessmentRepository assessmentRepository,
            DocumentAccessPolicy documentAccessPolicy) {
        this.analysisRepository = analysisRepository;
        this.assessmentRepository = assessmentRepository;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    @Transactional(readOnly = true)
    public EsgScoreResponse getScore(Long analysisId, Caller caller) {
        ComplianceAnalysis analysis = loadAnalysis(analysisId);
        documentAccessPolicy.requireView(analysis.getDocument(), caller, "Analysis not found: " + analysisId);
        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new BadRequestException("The ESG score is available once the analysis has completed.");
        }

        EsgIndustry industry = analysis.getIndustry();
        EsgScoreCalculator.Result result = EsgScoreCalculator.calculate(
                industry, inputsFor(List.of(analysisId)).getOrDefault(analysisId, List.of()));

        Double wakis = result.wakis();
        Double adjusted = wakis;
        boolean industryAdjusted = false;
        int peerCount = 0;
        Double minimum = null;
        Double maximum = null;

        if (wakis != null && industry != null) {
            List<Double> peerScores = peerWakisScores(analysis, wakis);
            peerCount = peerScores.size();
            minimum = peerScores.stream().mapToDouble(Double::doubleValue).min().orElse(wakis);
            maximum = peerScores.stream().mapToDouble(Double::doubleValue).max().orElse(wakis);
            if (peerCount >= EsgScoreCalculator.MIN_PEERS_FOR_ADJUSTMENT && maximum > minimum) {
                adjusted = EsgScoreCalculator.industryAdjusted(wakis, minimum, maximum);
                industryAdjusted = true;
            }
        }

        return new EsgScoreResponse(
                analysisId,
                industry != null ? industry.name() : null,
                industry != null ? industry.getLabel() : null,
                result.scorable(),
                wakis,
                adjusted,
                industryAdjusted,
                peerCount,
                minimum,
                maximum,
                adjusted != null ? EsgScoreCalculator.band(adjusted) : null,
                result.pillarScores().get(EsgPillar.ENVIRONMENTAL),
                result.pillarScores().get(EsgPillar.SOCIAL),
                result.pillarScores().get(EsgPillar.GOVERNANCE),
                result.pillarWeights().get(EsgPillar.ENVIRONMENTAL),
                result.pillarWeights().get(EsgPillar.SOCIAL),
                result.pillarWeights().get(EsgPillar.GOVERNANCE),
                result.scored(),
                result.notApplicable(),
                result.humanReview(),
                result.total(),
                result.keyIssues(),
                NOTE);
    }

    @Transactional
    public EsgScoreResponse setIndustry(Long analysisId, AnalysisIndustryRequest request, Caller caller) {
        ComplianceAnalysis analysis = loadAnalysis(analysisId);
        documentAccessPolicy.requireModify(analysis.getDocument(), caller, "Analysis not found: " + analysisId);
        analysis.setIndustry(parseIndustry(request == null ? null : request.industry()));
        analysisRepository.save(analysis);
        return getScore(analysisId, caller);
    }

    @Transactional
    public ApplicabilityResponse setApplicability(
            Long analysisId, Long requirementId, ApplicabilityRequest request, Caller caller) {
        ComplianceAnalysis analysis = loadAnalysis(analysisId);
        documentAccessPolicy.requireModify(analysis.getDocument(), caller, "Analysis not found: " + analysisId);
        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new BadRequestException("Applicability can be set once the analysis has completed.");
        }
        if (request == null) {
            throw new BadRequestException("A request body is required.");
        }
        RequirementAssessment assessment = assessmentRepository
                .findByAnalysis_IdAndFrameworkRequirement_Id(analysisId, requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found in analysis: " + requirementId));

        if (request.applicable()) {
            assessment.markApplicable();
        } else {
            String reason = request.reason() == null ? "" : request.reason().trim();
            if (reason.isEmpty()) {
                throw new BadRequestException("A reason is required to mark a requirement as not applicable.");
            }
            if (reason.length() > MAX_REASON_LENGTH) {
                throw new BadRequestException("The reason must be at most " + MAX_REASON_LENGTH + " characters.");
            }
            assessment.markNotApplicable(reason);
        }
        assessmentRepository.save(assessment);
        return new ApplicabilityResponse(
                requirementId, assessment.getAssessmentStatus().name(), assessment.getNotApplicableReason());
    }

    /**
     * WAKIS of the latest completed analysis of every other document in the same industry, plus this one.
     * Only numbers leave this method, so no peer document is exposed.
     */
    private List<Double> peerWakisScores(ComplianceAnalysis current, Double currentWakis) {
        Map<Long, ComplianceAnalysis> latestPerDocument = new HashMap<>();
        for (ComplianceAnalysis candidate :
                analysisRepository.findCompletedByIndustry(current.getIndustry(), FRAMEWORK_CODE)) {
            latestPerDocument.putIfAbsent(candidate.getDocument().getId(), candidate);
        }
        latestPerDocument.remove(current.getDocument().getId());

        List<Long> peerIds = latestPerDocument.values().stream().map(ComplianceAnalysis::getId).toList();
        Map<Long, List<ScoreInput>> peerInputs = peerIds.isEmpty() ? Map.of() : inputsFor(peerIds);

        List<Double> scores = new ArrayList<>();
        scores.add(currentWakis);
        for (Long peerId : peerIds) {
            Double peerWakis = EsgScoreCalculator.calculate(
                    current.getIndustry(), peerInputs.getOrDefault(peerId, List.of())).wakis();
            if (peerWakis != null) {
                scores.add(peerWakis);
            }
        }
        return scores;
    }

    private Map<Long, List<ScoreInput>> inputsFor(List<Long> analysisIds) {
        Map<Long, List<ScoreInput>> byAnalysis = new HashMap<>();
        for (Object[] row : assessmentRepository.countBySectionForAnalysisIds(analysisIds)) {
            byAnalysis.computeIfAbsent((Long) row[0], ignored -> new ArrayList<>())
                    .add(new ScoreInput(
                            (String) row[1], (String) row[2], (AssessmentStatus) row[3], ((Number) row[4]).intValue()));
        }
        return byAnalysis;
    }

    private ComplianceAnalysis loadAnalysis(Long analysisId) {
        return analysisRepository.findByIdWithFrameworkAndDocument(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + analysisId));
    }

    private static EsgIndustry parseIndustry(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return EsgIndustry.valueOf(code.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown industry: " + code);
        }
    }
}
