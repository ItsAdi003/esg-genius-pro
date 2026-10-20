package dev.esgenius.service.compliance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ComplianceClassificationProvider {

    ComplianceClassificationResult classify(ComplianceClassificationRequest request);

    /**
     * Classifies several requirements, ideally with a single model call. Results are keyed by requirement
     * code. A requirement missing from the returned map could not be classified and is the caller's to
     * handle. A failure of the whole call is thrown as {@link ComplianceClassificationException}.
     *
     * <p>The default classifies one requirement at a time, so providers without batching keep working.
     */
    default Map<String, ComplianceClassificationResult> classifyBatch(List<ComplianceClassificationRequest> requests) {
        Map<String, ComplianceClassificationResult> results = new LinkedHashMap<>();
        for (ComplianceClassificationRequest request : requests) {
            results.put(request.requirementCode(), classify(request));
        }
        return results;
    }
}
