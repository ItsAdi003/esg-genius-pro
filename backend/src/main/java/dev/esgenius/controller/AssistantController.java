package dev.esgenius.controller;

import dev.esgenius.dto.AssistantAnswerResponse;
import dev.esgenius.dto.AssistantAskRequest;
import dev.esgenius.service.DocumentAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/assistant")
public class AssistantController {

    private final DocumentAssistantService documentAssistantService;

    public AssistantController(DocumentAssistantService documentAssistantService) {
        this.documentAssistantService = documentAssistantService;
    }

    /**
     * POST /api/v1/documents/{documentId}/assistant/ask
     * Answer a question from one uploaded document. Stateless; nothing is persisted.
     */
    @PostMapping("/ask")
    public ResponseEntity<AssistantAnswerResponse> ask(
            @PathVariable Long documentId,
            @RequestBody AssistantAskRequest request) {
        return ResponseEntity.ok(documentAssistantService.ask(documentId, request));
    }
}
