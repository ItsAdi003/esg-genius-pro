package dev.esgenius.service.compliance;

public enum ClassificationFailureCategory {
    CONFIGURATION_ERROR,
    HTTP_CLIENT_ERROR,
    HTTP_RATE_LIMIT,
    HTTP_SERVER_ERROR,
    NETWORK_TIMEOUT,
    MALFORMED_RESPONSE,
    BLOCKED_RESPONSE,
    PARSE_FAILURE,
    VALIDATION_FAILURE,
    OTHER
}
