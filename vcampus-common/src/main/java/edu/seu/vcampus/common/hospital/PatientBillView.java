package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** One patient-owned hospital bill with its single v1 charge line. */
public final class PatientBillView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String billId;
    private final String appointmentId;
    private final HospitalBillType billType;
    private final String itemName;
    private final long amountCents;
    private final PaymentStatus paymentStatus;
    private final String departmentName;
    private final String doctorName;
    private final LocalDateTime visitTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime paidAt;
    private final LocalDateTime refundedAt;

    public PatientBillView(
            String billId,
            String appointmentId,
            HospitalBillType billType,
            String itemName,
            long amountCents,
            PaymentStatus paymentStatus,
            String departmentName,
            String doctorName,
            LocalDateTime visitTime,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime refundedAt) {
        this.billId = requireText(billId, "billId");
        this.appointmentId = requireText(appointmentId, "appointmentId");
        this.billType = Objects.requireNonNull(billType, "billType must not be null");
        this.itemName = requireText(itemName, "itemName");
        if (amountCents < 0) {
            throw new IllegalArgumentException("amountCents must not be negative");
        }
        this.amountCents = amountCents;
        this.paymentStatus = Objects.requireNonNull(
                paymentStatus, "paymentStatus must not be null");
        this.departmentName = requireText(departmentName, "departmentName");
        this.doctorName = requireText(doctorName, "doctorName");
        this.visitTime = Objects.requireNonNull(visitTime, "visitTime must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.paidAt = paidAt;
        this.refundedAt = refundedAt;
    }

    public String getBillId() { return billId; }

    public String getAppointmentId() { return appointmentId; }

    public HospitalBillType getBillType() { return billType; }

    public String getItemName() { return itemName; }

    public long getAmountCents() { return amountCents; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    public String getDepartmentName() { return departmentName; }

    public String getDoctorName() { return doctorName; }

    public LocalDateTime getVisitTime() { return visitTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
