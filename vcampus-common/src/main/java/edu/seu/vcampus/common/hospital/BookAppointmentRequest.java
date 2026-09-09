package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Request for atomically booking a hospital schedule and simulating payment. */
public final class BookAppointmentRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String scheduleId;
    private final VisitType visitType;
    private final String sourceFirstVisitAppointmentId;

    public BookAppointmentRequest(
            String scheduleId,
            VisitType visitType,
            String sourceFirstVisitAppointmentId) {
        this.scheduleId = requireText(scheduleId, "scheduleId");
        this.visitType = Objects.requireNonNull(visitType, "visitType must not be null");
        this.sourceFirstVisitAppointmentId = normalizeOptional(
                sourceFirstVisitAppointmentId, "sourceFirstVisitAppointmentId");
        validateCombination();
    }

    /** Creates a first-visit booking request. */
    public static BookAppointmentRequest firstVisit(String scheduleId) {
        return new BookAppointmentRequest(scheduleId, VisitType.FIRST_VISIT, null);
    }

    /** Creates a follow-up booking request linked to its source first visit. */
    public static BookAppointmentRequest followUp(
            String scheduleId,
            String sourceFirstVisitAppointmentId) {
        return new BookAppointmentRequest(
                scheduleId,
                VisitType.FOLLOW_UP,
                sourceFirstVisitAppointmentId);
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public VisitType getVisitType() {
        return visitType;
    }

    public String getSourceFirstVisitAppointmentId() {
        return sourceFirstVisitAppointmentId;
    }

    private void validateCombination() {
        if (visitType == VisitType.FIRST_VISIT) {
            if (sourceFirstVisitAppointmentId != null) {
                throw new IllegalArgumentException(
                        "sourceFirstVisitAppointmentId is not allowed for a first visit");
            }
            return;
        }
        if (sourceFirstVisitAppointmentId == null) {
            throw new IllegalArgumentException(
                    "sourceFirstVisitAppointmentId is required for a follow-up visit");
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
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
