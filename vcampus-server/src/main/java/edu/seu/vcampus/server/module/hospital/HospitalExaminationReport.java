package edu.seu.vcampus.server.module.hospital;

import java.time.LocalDateTime;
import java.util.Objects;

/** Text-only fictional report used to demonstrate the result-review workflow. */
record HospitalExaminationReport(
        String reportId,
        String orderId,
        String resultSummary,
        LocalDateTime reportedAt) {

    HospitalExaminationReport {
        reportId = requireText(reportId, "reportId");
        orderId = requireText(orderId, "orderId");
        resultSummary = requireText(resultSummary, "resultSummary");
        Objects.requireNonNull(reportedAt, "reportedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
