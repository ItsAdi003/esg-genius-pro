package dev.esgenius.entity;

/**
 * Final disclosure-classification statuses are written by Phase 3C-2 and later analyses.
 * Legacy retrieval-stage constants remain so historical Phase 3C-1 rows can still be loaded.
 */
public enum AssessmentStatus {
    COVERED,
    PARTIALLY_COVERED,
    NOT_COVERED,
    HUMAN_REVIEW_REQUIRED,

    /**
     * Legacy Phase 3C-1 retrieval status. Retained for reading historical persisted rows only.
     * New analyses must not persist this value.
     */
    @Deprecated
    EVIDENCE_RETRIEVED,

    /**
     * Legacy Phase 3C-1 retrieval status. Retained for reading historical persisted rows only.
     * New analyses must not persist this value.
     */
    @Deprecated
    NO_EVIDENCE_FOUND;

    public boolean isLegacyRetrievalStatus() {
        return this == EVIDENCE_RETRIEVED || this == NO_EVIDENCE_FOUND;
    }

    public boolean isClassificationStatus() {
        return !isLegacyRetrievalStatus();
    }
}
