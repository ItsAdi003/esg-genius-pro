package dev.esgenius.service;

import dev.esgenius.dto.AssistantAnswerResponse;
import dev.esgenius.dto.AssistantAskRequest;
import dev.esgenius.dto.AssistantCitationResponse;
import dev.esgenius.exception.AssistantUnavailableException;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.ratelimit.UsageLimiter;
import dev.esgenius.service.assistant.AssistantAnswerException;
import dev.esgenius.service.assistant.AssistantAnswerFailureCategory;
import dev.esgenius.service.assistant.AssistantAnswerProvider;
import dev.esgenius.service.assistant.AssistantAnswerRequest;
import dev.esgenius.service.assistant.AssistantAnswerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stateless, per-request document assistant. Evidence lookup runs in its own
 * transaction and finishes before any Gemini call.
 */
@Service
public class DocumentAssistantService {

    public static final String NOT_FOUND_ANSWER =
            "I couldn't find information about this in the uploaded document.";
    public static final String QUOTA_EXCEEDED_MESSAGE =
            "The AI service has reached its usage limit. Please try again later.";

    static final int MAX_QUESTION_LENGTH = 500;

    private static final Logger log = LoggerFactory.getLogger(DocumentAssistantService.class);

    private final DocumentAssistantEvidenceService evidenceService;
    private final AssistantAnswerProvider answerProvider;
    private final UsageLimiter usageLimiter;

    public DocumentAssistantService(
            DocumentAssistantEvidenceService evidenceService,
            AssistantAnswerProvider answerProvider,
            UsageLimiter usageLimiter) {
        this.evidenceService = evidenceService;
        this.answerProvider = answerProvider;
        this.usageLimiter = usageLimiter;
    }

    public AssistantAnswerResponse ask(Long documentId, AssistantAskRequest request) {
        return ask(documentId, request, null);
    }

    public AssistantAnswerResponse ask(Long documentId, AssistantAskRequest request, Caller caller) {
        String question = validateQuestion(request);
        List<RetrievedChunk> chunks = caller == null
                ? evidenceService.retrieveForQuestion(documentId, question)
                : evidenceService.retrieveForQuestion(documentId, question, caller);
        usageLimiter.consumeAssistantAsk(caller);
        if (chunks.isEmpty()) {
            return new AssistantAnswerResponse(NOT_FOUND_ANSWER, List.of(), false);
        }

        List<String> passages = chunks.stream().map(RetrievedChunk::text).toList();
        try {
            AssistantAnswerResult result = answerProvider.answer(new AssistantAnswerRequest(question, passages));
            return new AssistantAnswerResponse(result.answer(), toCitations(chunks, result.passageIndices()), true);
        } catch (AssistantAnswerException ex) {
            log.warn(
                    "Assistant answer failed documentId={} category={} httpStatus={} attempt={} retryable={} detail={}",
                    documentId,
                    ex.getCategory(),
                    ex.getHttpStatus(),
                    ex.getAttempt(),
                    ex.isRetryable(),
                    ex.getSafeDetail());
            if (ex.getCategory() == AssistantAnswerFailureCategory.QUOTA_EXHAUSTED) {
                throw new AssistantUnavailableException(HttpStatus.SERVICE_UNAVAILABLE, QUOTA_EXCEEDED_MESSAGE);
            }
            throw new AssistantUnavailableException(
                    "The assistant could not generate an answer from the uploaded document. Please try again.");
        }
    }

    private String validateQuestion(AssistantAskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new BadRequestException("Question must not be blank");
        }
        String question = request.question().trim();
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new BadRequestException("Question must be at most " + MAX_QUESTION_LENGTH + " characters");
        }
        return question;
    }

    private List<AssistantCitationResponse> toCitations(List<RetrievedChunk> chunks, List<Integer> passageIndices) {
        if (passageIndices == null || passageIndices.isEmpty()) {
            return List.of();
        }
        List<AssistantCitationResponse> citations = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (Integer index : passageIndices) {
            if (index == null || index < 0 || index >= chunks.size() || !seen.add(index)) {
                continue;
            }
            RetrievedChunk chunk = chunks.get(index);
            citations.add(new AssistantCitationResponse(chunk.pageNumber(), chunk.chunkIndex(), chunk.text()));
        }
        return citations;
    }
}
