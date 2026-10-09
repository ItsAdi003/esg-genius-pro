package dev.esgenius.controller;

import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.dto.AssistantAnswerResponse;
import dev.esgenius.dto.AssistantAskRequest;
import dev.esgenius.service.Caller;
import dev.esgenius.service.DocumentAccessPolicy;
import dev.esgenius.service.DocumentAssistantService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final DocumentAccessPolicy documentAccessPolicy;

    public AssistantController(
            DocumentAssistantService documentAssistantService,
            DocumentAccessPolicy documentAccessPolicy) {
        this.documentAssistantService = documentAssistantService;
        this.documentAccessPolicy = documentAccessPolicy;
    }

    /**
     * POST /api/v1/documents/{documentId}/assistant/ask
     * Answer a question from one uploaded document. Stateless; nothing is persisted.
     */
    @PostMapping("/ask")
    public ResponseEntity<AssistantAnswerResponse> ask(
            @PathVariable Long documentId,
            @RequestBody AssistantAskRequest request,
            HttpServletRequest httpRequest) {
        Caller caller = documentAccessPolicy.resolve(AuthenticatedUser.from(httpRequest));
        return ResponseEntity.ok(documentAssistantService.ask(documentId, request, caller));
    }
}
