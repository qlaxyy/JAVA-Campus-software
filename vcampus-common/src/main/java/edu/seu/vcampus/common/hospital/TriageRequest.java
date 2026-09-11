package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Patient input for department guidance; it must not be treated as a diagnosis. */
public final class TriageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String symptomDescription;
    private final boolean urgentConcern;

    public TriageRequest(String symptomDescription, boolean urgentConcern) {
        String normalized = symptomDescription == null ? "" : symptomDescription.trim();
        if (!urgentConcern && normalized.length() < 2) {
            throw new IllegalArgumentException(
                    "symptomDescription must contain at least 2 characters");
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException(
                    "symptomDescription must not exceed 500 characters");
        }
        this.symptomDescription = normalized;
        this.urgentConcern = urgentConcern;
    }

    public String getSymptomDescription() {
        return symptomDescription;
    }

    public boolean hasUrgentConcern() {
        return urgentConcern;
    }
}
