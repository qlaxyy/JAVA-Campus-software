package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Patient input for department guidance; it must not be treated as a diagnosis. */
public final class TriageRequest implements Serializable {

    /** Maximum number of free-text questions that may be asked by the AI layer. */
    public static final int MAX_FOLLOW_UPS = 4;
    /** Maximum number of fixed server-owned safety questions. */
    public static final int MAX_SAFETY_FOLLOW_UPS = 7;
    /** Protocol ceiling when both safety and ordinary follow-ups are present. */
    public static final int MAX_FOLLOW_UP_ANSWERS =
            MAX_FOLLOW_UPS + MAX_SAFETY_FOLLOW_UPS;

    @Serial
    private static final long serialVersionUID = 1L;

    private final String symptomDescription;
    private final boolean urgentConcern;
    private final List<TriageFollowUpAnswer> followUpAnswers;

    public TriageRequest(String symptomDescription, boolean urgentConcern) {
        this(symptomDescription, urgentConcern, List.of());
    }

    public TriageRequest(
            String symptomDescription,
            boolean urgentConcern,
            List<TriageFollowUpAnswer> followUpAnswers) {
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
        this.followUpAnswers = List.copyOf(Objects.requireNonNull(
                followUpAnswers, "followUpAnswers must not be null"));
        if (this.followUpAnswers.size() > MAX_FOLLOW_UP_ANSWERS) {
            throw new IllegalArgumentException(
                    "followUpAnswers must not contain more than "
                            + MAX_FOLLOW_UP_ANSWERS + " items");
        }
    }

    public String getSymptomDescription() {
        return symptomDescription;
    }

    public boolean hasUrgentConcern() {
        return urgentConcern;
    }

    /** Returns an empty list when reading a request created by an older client. */
    public List<TriageFollowUpAnswer> getFollowUpAnswers() {
        return followUpAnswers == null ? List.of() : followUpAnswers;
    }
}
