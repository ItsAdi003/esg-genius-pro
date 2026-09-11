package dev.esgenius.service.compliance;

import org.springframework.stereotype.Component;

@Component
public class ComplianceClassificationPromptBuilder {

    public String buildPrompt(ComplianceClassificationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant performing disclosure coverage analysis for ESG reporting requirements.\n\n");
        prompt.append("TASK: Determine whether the supplied document evidence contains the disclosure requested by the requirement rubric below.\n");
        prompt.append("This is disclosure/readiness coverage analysis only — NOT ESG performance scoring.\n\n");

        prompt.append("CLASSIFICATION STATUSES (use exactly one):\n");
        prompt.append("- COVERED: Every material component explicitly requested in the evaluation rubric is clearly present in the evidence.\n");
        prompt.append("- PARTIALLY_COVERED: The evidence addresses the rubric but one or more material components explicitly requested in the rubric are incomplete, unclear, or missing.\n");
        prompt.append("- NOT_COVERED: The disclosure explicitly requested in the rubric is not found in the supplied document evidence.\n");
        prompt.append("- HUMAN_REVIEW_REQUIRED: The evidence is ambiguous, conflicting, too weak to classify reliably, or requires human interpretation.\n\n");

        prompt.append("STRICT RUBRIC RULES (mandatory):\n");
        prompt.append("- The evaluation rubric below is the COMPLETE and ONLY list of disclosure components to evaluate.\n");
        prompt.append("- Treat the provided requirement title, description, and framework text together as the authoritative evaluation rubric.\n");
        prompt.append("- Do NOT add, infer, or import additional disclosure criteria from general BRSR knowledge, SEBI expectations, other ESG standards, best practice, or remembered regulatory guidance.\n");
        prompt.append("- Do NOT require methodology, emission factors, assurance, breakdowns, ratios, disciplinary-action counts, location-based vs market-based methods, or any other field unless it is explicitly written in the evaluation rubric below.\n");
        prompt.append("- Do NOT infer requirements from the requirement code alone.\n");
        prompt.append("- Outside knowledge of BRSR, SEBI, ESG frameworks, MSCI, or the company must not affect the classification.\n");
        prompt.append("- If every material component explicitly requested in the evaluation rubric is present in the evidence, return COVERED even if additional information that could exist in a broader real-world framework is absent.\n");
        prompt.append("- Use PARTIALLY_COVERED only when a component explicitly requested in the rubric is missing or incomplete — never because broader framework practice would normally expect more detail.\n\n");

        prompt.append("OTHER RULES:\n");
        prompt.append("- Evaluate ONLY whether the rubric-requested information is disclosed — never whether the disclosed metric is environmentally/socially good or bad.\n");
        prompt.append("- Use ONLY the evaluation rubric and evidence passages below. Do not use outside knowledge, web information, assumptions, or invented disclosures.\n");
        prompt.append("- Do not invent quotations, page numbers, or facts not present in the evidence.\n");
        prompt.append("- Do not claim official SEBI certification, legal compliance, ESG ratings, or MSCI assessments.\n");
        prompt.append("- explanation: concise user-facing justification referencing only rubric components (not chain-of-thought).\n");
        prompt.append("- gap: rubric components that appear missing from the evidence (null if fully covered).\n");
        prompt.append("- recommendation: what should be disclosed to satisfy the rubric (null if fully covered).\n");
        prompt.append("- confidence: number from 0.0 to 1.0 reflecting classification certainty.\n\n");

        prompt.append("EVALUATION RUBRIC (complete — evaluate only these explicitly stated components):\n");
        prompt.append("Title: ").append(request.requirementTitle()).append('\n');
        if (request.requirementDescription() != null && !request.requirementDescription().isBlank()) {
            prompt.append("Description: ").append(request.requirementDescription()).append('\n');
        }
        if (request.requirementFrameworkText() != null && !request.requirementFrameworkText().isBlank()) {
            prompt.append("Framework text: ").append(request.requirementFrameworkText()).append('\n');
        }

        prompt.append("\nDOCUMENT EVIDENCE PASSAGES:\n");
        for (int i = 0; i < request.evidencePassages().size(); i++) {
            prompt.append("--- Passage ").append(i + 1).append(" ---\n");
            prompt.append(request.evidencePassages().get(i)).append('\n');
        }

        return prompt.toString();
    }
}
