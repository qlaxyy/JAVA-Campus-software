package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** One pending appointment visible to its assigned doctor. */
public final class DoctorAppointmentView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;
    private final String patientUserId;
    private final int queueNumber;
    private final AppointmentStatus appointmentStatus;
    private final VisitType visitType;
    private final String sourceAppointmentId;
    private final LocalDateTime bookedAt;

    public DoctorAppointmentView(
            String appointmentId,
            String patientUserId,
            int queueNumber,
            AppointmentStatus appointmentStatus,
            VisitType visitType,
            LocalDateTime bookedAt) {
        this(appointmentId, patientUserId, queueNumber, appointmentStatus,
                visitType, null, bookedAt);
    }

    public DoctorAppointmentView(
            String appointmentId,
            String patientUserId,
            int queueNumber,
            AppointmentStatus appointmentStatus,
            VisitType visitType,
            String sourceAppointmentId,
            LocalDateTime bookedAt) {
        this.appointmentId = requireText(appointmentId, "appointmentId");
        this.patientUserId = requireText(patientUserId, "patientUserId");
        if (queueNumber <= 0) {
            throw new IllegalArgumentException("queueNumber must be positive");
        }
        this.queueNumber = queueNumber;
        this.appointmentStatus = Objects.requireNonNull(
                appointmentStatus, "appointmentStatus must not be null");
        this.visitType = Objects.requireNonNull(visitType, "visitType must not be null");
        this.sourceAppointmentId = normalizeOptional(sourceAppointmentId);
        this.bookedAt = Objects.requireNonNull(bookedAt, "bookedAt must not be null");
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientUserId() {
        return patientUserId;
    }

    public int getQueueNumber() {
        return queueNumber;
    }

    public AppointmentStatus getAppointmentStatus() {
        return appointmentStatus;
    }

    public VisitType getVisitType() {
        return visitType;
    }

    public String getSourceAppointmentId() {
        return sourceAppointmentId;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
