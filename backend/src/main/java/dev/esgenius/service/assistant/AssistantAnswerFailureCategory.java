package dev.esgenius.service.assistant;

public enum AssistantAnswerFailureCategory {
    CONFIGURATION_ERROR,
    HTTP_CLIENT_ERROR,
    HTTP_RATE_LIMIT,
    QUOTA_EXHAUSTED,
    HTTP_SERVER_ERROR,
    NETWORK_TIMEOUT,
    MALFORMED_RESPONSE,
    BLOCKED_RESPONSE,
    PARSE_FAILURE,
    VALIDATION_FAILURE,
    OTHER
}
