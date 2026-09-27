package dev.esgenius.service.assistant;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssistantAnswerPromptBuilder {

    public String buildPrompt(AssistantAnswerRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You answer questions about a single uploaded document.\n\n");
        prompt.append("RULES:\n");
        prompt.append("- Use ONLY the evidence passages below. Do not use outside knowledge, other documents, or assumptions.\n");
        prompt.append("- If the passages do not contain the answer, say clearly that the document does not contain the answer.\n");
        prompt.append("- Do not invent figures, quotations, page numbers, or facts that are not in the passages.\n");
        prompt.append("- Do not present the answer as a compliance determination, legal conclusion, or replacement for human review.\n");
        prompt.append("- answer: a concise response grounded in the passages.\n");
        prompt.append("- passageIndices: the 1-based passage numbers that support the answer.\n");
        prompt.append("- Passage 1 is the first passage. Cite only passage numbers that appear below.\n");
        prompt.append("- Use an empty passageIndices array when none of the passages support an answer.\n\n");

        prompt.append("QUESTION:\n");
        prompt.append(request.question()).append("\n\n");

        prompt.append("EVIDENCE PASSAGES:\n");
        List<String> passages = request.evidencePassages();
        for (int i = 0; i < passages.size(); i++) {
            prompt.append("Passage ").append(i + 1).append(":\n");
            prompt.append(passages.get(i)).append("\n\n");
        }
        return prompt.toString();
    }
}
