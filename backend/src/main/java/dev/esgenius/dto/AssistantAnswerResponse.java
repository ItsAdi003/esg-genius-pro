package dev.esgenius.dto;

import java.util.List;

public record AssistantAnswerResponse(
        String answer,
        List<AssistantCitationResponse> citations,
        boolean grounded) {
}
