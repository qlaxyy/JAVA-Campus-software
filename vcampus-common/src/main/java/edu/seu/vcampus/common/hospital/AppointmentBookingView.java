package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Successful result of atomically booking a schedule and simulating payment. */
public final class AppointmentBookingView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;
    private final int queueNumber;
    private final AppointmentStatus appointmentStatus;
    private final String registrationBillId;
    private final PaymentStatus paymentStatus;
    private final long amountCents;
    private final String doctorName;
    private final String departmentName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime paidAt;

    public AppointmentBookingView(
            String appointmentId,
            int queueNumber,
            AppointmentStatus appointmentStatus,
            String registrationBillId,
            PaymentStatus paymentStatus,
            long amountCents,
            String doctorName,
            String departmentName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime createdAt,
            LocalDateTime paidAt) {
        this.appointmentId = requireText(appointmentId, "appointmentId");
        if (queueNumber <= 0) {
            throw new IllegalArgumentException("queueNumber must be positive");
        }
        this.queueNumber = queueNumber;
        this.appointmentStatus = Objects.requireNonNull(
                appointmentStatus, "appointmentStatus must not be null");
        this.registrationBillId = requireText(registrationBillId, "registrationBillId");
        this.paymentStatus = Objects.requireNonNull(
                paymentStatus, "paymentStatus must not be null");
        if (appointmentStatus != AppointmentStatus.BOOKED) {
            throw new IllegalArgumentException("a booking result must have BOOKED status");
        }
        if (paymentStatus != PaymentStatus.PAID) {
            throw new IllegalArgumentException("a registration booking must be PAID");
        }
        if (amountCents < 0) {
            throw new IllegalArgumentException("amountCents must not be negative");
        }
        this.amountCents = amountCents;
        this.doctorName = requireText(doctorName, "doctorName");
        this.departmentName = requireText(departmentName, "departmentName");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (paidAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("paidAt must not be before createdAt");
        }
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public int getQueueNumber() {
        return queueNumber;
    }

    public AppointmentStatus getAppointmentStatus() {
        return appointmentStatus;
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

    public String getDoctorName() {
        return doctorName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
