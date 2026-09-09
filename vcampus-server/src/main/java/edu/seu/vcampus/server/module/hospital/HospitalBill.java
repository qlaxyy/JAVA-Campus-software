package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.PaymentStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** Internal registration bill created together with an appointment. */
record HospitalBill(
        String billId,
        String appointmentId,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        PaymentStatus paymentStatus) {

    HospitalBill {
        Objects.requireNonNull(billId, "billId must not be null");
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");
        if (paidAt != null && paidAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("paidAt must not be before createdAt");
        }
        if (paymentStatus == PaymentStatus.UNPAID
                && (paidAt != null || refundedAt != null)) {
            throw new IllegalArgumentException(
                    "an unpaid bill cannot have payment or refund time");
        }
        if (paymentStatus == PaymentStatus.PAID
                && (paidAt == null || refundedAt != null)) {
            throw new IllegalArgumentException(
                    "a paid bill requires paidAt and cannot have refundedAt");
        }
        if (paymentStatus == PaymentStatus.REFUNDED
                && (paidAt == null || refundedAt == null)) {
            throw new IllegalArgumentException(
                    "a refunded bill requires payment and refund time");
        }
        if (refundedAt != null && refundedAt.isBefore(paidAt)) {
            throw new IllegalArgumentException("refundedAt must not be before paidAt");
        }
    }
}
