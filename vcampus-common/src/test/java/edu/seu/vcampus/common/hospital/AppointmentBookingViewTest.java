package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentBookingViewTest {

    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 8, 31, 10, 0);
    private static final LocalDateTime START_TIME =
            LocalDateTime.of(2026, 9, 1, 8, 0);
    private static final LocalDateTime END_TIME =
            LocalDateTime.of(2026, 9, 1, 12, 0);

    @Test
    void createsSuccessfulBookingView() {
        AppointmentBookingView view = validView(2500);

        assertEquals("appointment-1", view.getAppointmentId());
        assertEquals(3, view.getQueueNumber());
        assertEquals(AppointmentStatus.BOOKED, view.getAppointmentStatus());
        assertEquals("bill-1", view.getRegistrationBillId());
        assertEquals(PaymentStatus.PAID, view.getPaymentStatus());
        assertEquals(2500, view.getAmountCents());
        assertEquals("张医生", view.getDoctorName());
        assertEquals("内科", view.getDepartmentName());
        assertEquals(START_TIME, view.getStartTime());
        assertEquals(END_TIME, view.getEndTime());
        assertEquals(CREATED_AT, view.getCreatedAt());
        assertEquals(CREATED_AT, view.getPaidAt());
    }

    @Test
    void rejectsInvalidBookingResultStateAndAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> view(AppointmentStatus.CANCELLED, PaymentStatus.PAID, 2500));
        assertThrows(IllegalArgumentException.class,
                () -> view(AppointmentStatus.BOOKED, PaymentStatus.UNPAID, 2500));
        assertThrows(IllegalArgumentException.class,
                () -> view(AppointmentStatus.BOOKED, PaymentStatus.PAID, -1));
        assertThrows(IllegalArgumentException.class, () -> new AppointmentBookingView(
                "appointment-1",
                0,
                AppointmentStatus.BOOKED,
                "bill-1",
                PaymentStatus.PAID,
                2500,
                "张医生",
                "内科",
                START_TIME,
                END_TIME,
                CREATED_AT,
                CREATED_AT));
    }

    @Test
    void rejectsInvalidTimeRange() {
        assertThrows(IllegalArgumentException.class, () -> new AppointmentBookingView(
                "appointment-1",
                3,
                AppointmentStatus.BOOKED,
                "bill-1",
                PaymentStatus.PAID,
                2500,
                "张医生",
                "内科",
                END_TIME,
                START_TIME,
                CREATED_AT,
                CREATED_AT));
    }

    private static AppointmentBookingView validView(long amountCents) {
        return view(AppointmentStatus.BOOKED, PaymentStatus.PAID, amountCents);
    }

    private static AppointmentBookingView view(
            AppointmentStatus appointmentStatus,
            PaymentStatus paymentStatus,
            long amountCents) {
        return new AppointmentBookingView(
                "appointment-1",
                3,
                appointmentStatus,
                "bill-1",
                paymentStatus,
                amountCents,
                "张医生",
                "内科",
                START_TIME,
                END_TIME,
                CREATED_AT,
                CREATED_AT);
    }
}
