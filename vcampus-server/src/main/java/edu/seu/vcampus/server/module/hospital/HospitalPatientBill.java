package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** A patient-owned hospital bill and its single charge line in the v1 workflow. */
record HospitalPatientBill(
        String billId,
        String appointmentId,
        String patientUserId,
        HospitalBillType billType,
        PaymentStatus paymentStatus,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        HospitalBillItem item) {

    HospitalPatientBill {
        Objects.requireNonNull(billId, "billId must not be null");
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        Objects.requireNonNull(billType, "billType must not be null");
        Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(item, "item must not be null");
        if (!item.billId().equals(billId)) {
            throw new IllegalArgumentException("bill item does not belong to bill");
        }
        if (paymentStatus == PaymentStatus.UNPAID
                && (paidAt != null || refundedAt != null)) {
            throw new IllegalArgumentException("unpaid bill cannot have payment timestamps");
        }
        if (paymentStatus == PaymentStatus.PAID
                && (paidAt == null || refundedAt != null)) {
            throw new IllegalArgumentException("paid bill timestamps are invalid");
        }
        if (paymentStatus == PaymentStatus.REFUNDED
                && (paidAt == null || refundedAt == null)) {
            throw new IllegalArgumentException("refunded bill timestamps are invalid");
        }
    }

    long amountCents() {
        return item.amountCents();
    }
}
