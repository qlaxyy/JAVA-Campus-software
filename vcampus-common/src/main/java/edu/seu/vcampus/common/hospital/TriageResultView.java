package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Safe triage outcome containing either an urgent warning or department guidance. */
public final class TriageResultView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean urgent;
    private final String safetyMessage;
    private final List<TriageRecommendationView> recommendations;

    public TriageResultView(
            boolean urgent,
            String safetyMessage,
            List<TriageRecommendationView> recommendations) {
        if (safetyMessage == null || safetyMessage.isBlank()) {
            throw new IllegalArgumentException("safetyMessage must not be blank");
        }
        this.urgent = urgent;
        this.safetyMessage = safetyMessage.trim();
        this.recommendations = List.copyOf(Objects.requireNonNull(
                recommendations, "recommendations must not be null"));
        if (urgent && !this.recommendations.isEmpty()) {
            throw new IllegalArgumentException(
                    "urgent triage result must not include appointment recommendations");
        }
    }

    public boolean isUrgent() {
        return urgent;
    }

    public String getSafetyMessage() {
        return safetyMessage;
    }

    public List<TriageRecommendationView> getRecommendations() {
        return recommendations;
    }
}
