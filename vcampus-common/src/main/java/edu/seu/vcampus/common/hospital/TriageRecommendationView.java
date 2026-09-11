package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** One bookable department recommendation produced by the local rule engine. */
public final class TriageRecommendationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String departmentId;
    private final String departmentName;
    private final TriageMatchLevel matchLevel;
    private final List<String> reasons;

    public TriageRecommendationView(
            String departmentId,
            String departmentName,
            TriageMatchLevel matchLevel,
            List<String> reasons) {
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.matchLevel = Objects.requireNonNull(matchLevel, "matchLevel must not be null");
        this.reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons must not be null"));
        if (this.reasons.isEmpty()
                || this.reasons.stream().anyMatch(
                        reason -> reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("reasons must contain non-blank text");
        }
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public TriageMatchLevel getMatchLevel() {
        return matchLevel;
    }

    public List<String> getReasons() {
        return reasons;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
