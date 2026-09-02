package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Criteria for searching the next seven days of hospital schedules. */
public final class SearchSlotsRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final VisitType visitType;
    private final String departmentId;
    private final String doctorId;
    private final String sourceConsultationId;

    public SearchSlotsRequest(
            VisitType visitType,
            String departmentId,
            String doctorId,
            String sourceConsultationId) {
        this.visitType = Objects.requireNonNull(visitType, "visitType must not be null");
        this.departmentId = normalizeOptional(departmentId, "departmentId");
        this.doctorId = normalizeOptional(doctorId, "doctorId");
        this.sourceConsultationId = normalizeOptional(
                sourceConsultationId, "sourceConsultationId");
        validateCombination();
    }

    public static SearchSlotsRequest firstVisit(String departmentId, String doctorId) {
        return new SearchSlotsRequest(VisitType.FIRST_VISIT, departmentId, doctorId, null);
    }

    public static SearchSlotsRequest followUp(String sourceConsultationId) {
        return new SearchSlotsRequest(VisitType.FOLLOW_UP, null, null, sourceConsultationId);
    }

    public VisitType getVisitType() {
        return visitType;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getSourceConsultationId() {
        return sourceConsultationId;
    }

    private void validateCombination() {
        if (visitType == VisitType.FIRST_VISIT) {
            if (departmentId == null) {
                throw new IllegalArgumentException("departmentId is required for a first visit");
            }
            if (sourceConsultationId != null) {
                throw new IllegalArgumentException(
                        "sourceConsultationId is not allowed for a first visit");
            }
            return;
        }
        if (sourceConsultationId == null) {
            throw new IllegalArgumentException(
                    "sourceConsultationId is required for a follow-up visit");
        }
        if (departmentId != null || doctorId != null) {
            throw new IllegalArgumentException(
                    "departmentId and doctorId are derived by the server for a follow-up visit");
        }
    }

    private static String normalizeOptional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
