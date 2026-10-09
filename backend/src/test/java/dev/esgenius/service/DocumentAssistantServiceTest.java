package dev.esgenius.service;

import dev.esgenius.dto.AssistantAnswerResponse;
import dev.esgenius.dto.AssistantAskRequest;
import dev.esgenius.exception.AssistantUnavailableException;
import dev.esgenius.exception.BadRequestException;
import dev.esgenius.service.assistant.AssistantAnswerException;
import dev.esgenius.service.assistant.AssistantAnswerFailureCategory;
import dev.esgenius.service.assistant.AssistantAnswerProvider;
import dev.esgenius.service.assistant.AssistantAnswerRequest;
import dev.esgenius.service.assistant.AssistantAnswerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAssistantServiceTest {

    @Mock
    private AssistantAnswerProvider answerProvider;

    private RecordingEvidence evidence;
    private DocumentAssistantService service;

    @BeforeEach
    void setUp() {
        evidence = new RecordingEvidence();
        service = new DocumentAssistantService(evidence, answerProvider);
    }

    @Test
    void returnsNotFoundWithoutCallingGeminiWhenRetrievalIsEmpty() {
        evidence.result = List.of();

        AssistantAnswerResponse response = service.ask(5L, new AssistantAskRequest("whistleblower helpline"));

        assertThat(response.grounded()).isFalse();
        assertThat(response.answer()).isEqualTo(DocumentAssistantService.NOT_FOUND_ANSWER);
        assertThat(response.citations()).isEmpty();
        assertThat(evidence.calls).isEqualTo(1);
        assertThat(evidence.lastQuestion).isEqualTo("whistleblower helpline");
        verifyNoInteractions(answerProvider);
        verify(answerProvider, never()).answer(any());
    }

    @Test
    void mapsCitedPassagesAfterRetrieval() {
        RetrievedChunk chunk = new RetrievedChunk(
                2, "Groundwater withdrawal was 12500 kilolitres.", 1.4, 4);
        evidence.result = List.of(chunk);
        when(answerProvider.answer(any())).thenReturn(
                new AssistantAnswerResult("Withdrawal was 12500 kilolitres.", List.of(0)));

        AssistantAnswerResponse response = service.ask(5L, new AssistantAskRequest("  groundwater withdrawal  "));

        assertThat(response.grounded()).isTrue();
        assertThat(response.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.pageNumber()).isEqualTo(4);
            assertThat(citation.chunkIndex()).isEqualTo(2);
            assertThat(citation.snippet()).contains("12500");
        });

        ArgumentCaptor<AssistantAnswerRequest> captor = ArgumentCaptor.forClass(AssistantAnswerRequest.class);
        verify(answerProvider).answer(captor.capture());
        assertThat(captor.getValue().question()).isEqualTo("groundwater withdrawal");
        assertThat(captor.getValue().evidencePassages()).containsExactly(chunk.text());
    }

    @Test
    void keepsNullPageNumberForLegacyChunks() {
        RetrievedChunk chunk = new RetrievedChunk(
                8, "Board independence is described for the directors.", 0.4, null);
        evidence.result = List.of(chunk);
        when(answerProvider.answer(any())).thenReturn(
                new AssistantAnswerResult("Independence is discussed.", List.of(0)));

        AssistantAnswerResponse response = service.ask(1L, new AssistantAskRequest("board independence"));

        assertThat(response.citations().get(0).pageNumber()).isNull();
        assertThat(response.citations().get(0).chunkIndex()).isEqualTo(8);
    }

    @Test
    void rejectsBlankQuestionWithoutRetrievalOrGemini() {
        assertThrows(BadRequestException.class, () -> service.ask(1L, new AssistantAskRequest("   ")));
        assertThat(evidence.calls).isZero();
        verifyNoInteractions(answerProvider);
    }

    @Test
    void rejectsQuestionOverMaxLengthWithoutRetrievalOrGemini() {
        String question = "a".repeat(DocumentAssistantService.MAX_QUESTION_LENGTH + 1);
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.ask(1L, new AssistantAskRequest(question)));

        assertThat(ex.getMessage()).contains("500");
        assertThat(evidence.calls).isZero();
        verifyNoInteractions(answerProvider);
    }

    @Test
    void geminiFailureDoesNotFabricateAnAnswer() {
        evidence.result = List.of(new RetrievedChunk(0, "Groundwater withdrawal was 12500 kilolitres.", 1.0, 1));
        when(answerProvider.answer(any())).thenThrow(new AssistantAnswerException(
                AssistantAnswerFailureCategory.HTTP_SERVER_ERROR,
                "Gemini API returned HTTP 503",
                true,
                503,
                3,
                "Gemini API server error"));

        AssistantUnavailableException ex = assertThrows(AssistantUnavailableException.class, () ->
                service.ask(1L, new AssistantAskRequest("groundwater withdrawal")));

        assertThat(ex.getMessage()).doesNotContain("test-api-key");
        assertThat(ex.getMessage()).contains("could not generate an answer");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void quotaExhaustionReturnsClearLimitMessage() {
        evidence.result = List.of(new RetrievedChunk(0, "Groundwater withdrawal was 12500 kilolitres.", 1.0, 1));
        when(answerProvider.answer(any())).thenThrow(new AssistantAnswerException(
                AssistantAnswerFailureCategory.QUOTA_EXHAUSTED,
                "Gemini API returned HTTP 429",
                false,
                429,
                1,
                "Daily quota exhausted"));

        AssistantUnavailableException ex = assertThrows(AssistantUnavailableException.class, () ->
                service.ask(1L, new AssistantAskRequest("groundwater withdrawal")));

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ex.getMessage()).isEqualTo(DocumentAssistantService.QUOTA_EXCEEDED_MESSAGE);
        assertThat(ex.getMessage()).doesNotContain("quotaId");
        assertThat(ex.getMessage()).doesNotContain("test-api-key");
        assertThat(ex.getMessage()).doesNotContain("RESOURCE_EXHAUSTED");
    }

    @Test
    void askIsNotTransactionalSoGeminiRunsAfterTheLookupTransaction() throws Exception {
        assertThat(AnnotationUtils.findAnnotation(DocumentAssistantService.class, Transactional.class)).isNull();
        assertThat(AnnotationUtils.findAnnotation(
                DocumentAssistantService.class.getMethod("ask", Long.class, AssistantAskRequest.class),
                Transactional.class)).isNull();
    }

    private static final class RecordingEvidence extends DocumentAssistantEvidenceService {
        private List<RetrievedChunk> result = List.of();
        private int calls;
        private String lastQuestion;

        private RecordingEvidence() {
            super(null, null, new TextChunkingService(), new LexicalEvidenceRetrievalService());
        }

        @Override
        public List<RetrievedChunk> retrieveForQuestion(Long documentId, String question) {
            calls++;
            lastQuestion = question;
            return result;
        }
    }
}
