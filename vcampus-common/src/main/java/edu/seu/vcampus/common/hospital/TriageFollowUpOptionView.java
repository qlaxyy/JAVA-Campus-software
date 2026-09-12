package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** One patient-readable choice for a server-owned triage question. */
public final class TriageFollowUpOptionView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String optionId;
    private final String label;

    public TriageFollowUpOptionView(String optionId, String label) {
        this.optionId = requireText(optionId, 64, "optionId");
        this.label = requireText(label, 100, "label");
    }

    public String getOptionId() {
        return optionId;
    }

    public String getLabel() {
        return label;
    }

    private static String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        return normalized;
    }
}
