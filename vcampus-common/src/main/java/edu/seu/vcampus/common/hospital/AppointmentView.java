package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** One appointment belonging to the currently authenticated patient. */
public final class AppointmentView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;
    private final String scheduleId;
    private final int queueNumber;
    private final AppointmentStatus appointmentStatus;
    private final VisitType visitType;
    private final String departmentName;
    private final String doctorName;
    private final String doctorTitle;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime bookedAt;
    private final String registrationBillId;
    private final PaymentStatus paymentStatus;
    private final long amountCents;

    public AppointmentView(
            String appointmentId,
            String scheduleId,
            int queueNumber,
            AppointmentStatus appointmentStatus,
            VisitType visitType,
            String departmentName,
            String doctorName,
            String doctorTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime bookedAt,
            String registrationBillId,
            PaymentStatus paymentStatus,
            long amountCents) {
        this.appointmentId = requireText(appointmentId, "appointmentId");
        this.scheduleId = requireText(scheduleId, "scheduleId");
        if (queueNumber <= 0) {
            throw new IllegalArgumentException("queueNumber must be positive");
        }
        this.queueNumber = queueNumber;
        this.appointmentStatus = Objects.requireNonNull(
                appointmentStatus, "appointmentStatus must not be null");
        this.visitType = Objects.requireNonNull(visitType, "visitType must not be null");
        this.departmentName = requireText(departmentName, "departmentName");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.bookedAt = Objects.requireNonNull(bookedAt, "bookedAt must not be null");
        this.registrationBillId = requireText(registrationBillId, "registrationBillId");
        this.paymentStatus = Objects.requireNonNull(
                paymentStatus, "paymentStatus must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (amountCents < 0) {
            throw new IllegalArgumentException("amountCents must not be negative");
        }
        this.amountCents = amountCents;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getScheduleId() {
        return scheduleId;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getDoctorTitle() {
        return doctorTitle;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public String getRegistrationBillId() {
        return registrationBillId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public long getAmountCents() {
        return amountCents;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
