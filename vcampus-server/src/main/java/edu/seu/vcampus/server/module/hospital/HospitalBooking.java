package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** Appointment, paid registration bill and its line item saved as one aggregate. */
record HospitalBooking(
        HospitalAppointment appointment,
        HospitalBill bill,
        HospitalBillItem billItem) {

    HospitalBooking {
        Objects.requireNonNull(appointment, "appointment must not be null");
        Objects.requireNonNull(bill, "bill must not be null");
        Objects.requireNonNull(billItem, "billItem must not be null");
        if (!bill.appointmentId().equals(appointment.appointmentId())) {
            throw new IllegalArgumentException("bill does not belong to appointment");
        }
        if (!billItem.billId().equals(bill.billId())) {
            throw new IllegalArgumentException("bill item does not belong to bill");
        }
    }
}
