package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentListResponseTest {

    @Test
    void defensivelyCopiesAppointments() {
        ArrayList<AppointmentView> source = new ArrayList<>();
        source.add(appointment());

        AppointmentListResponse response = new AppointmentListResponse(source);
        source.clear();

        assertEquals(1, response.getAppointments().size());
        assertThrows(UnsupportedOperationException.class,
                () -> response.getAppointments().clear());
    }

    @Test
    void validatesAppointmentFields() {
        AppointmentView appointment = appointment();

        assertEquals("appointment-1", appointment.getAppointmentId());
        assertEquals("slot-general-1", appointment.getScheduleId());
        assertEquals(5, appointment.getQueueNumber());
        assertEquals(1_200, appointment.getAmountCents());
    }

    private static AppointmentView appointment() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 3, 8, 30);
        return new AppointmentView(
                "appointment-1",
                "slot-general-1",
                5,
                AppointmentStatus.BOOKED,
                VisitType.FIRST_VISIT,
                "全科门诊",
                "陈医生",
                "主治医师",
                start,
                start.plusMinutes(30),
                start.minusDays(1),
                "bill-1",
                PaymentStatus.PAID,
                1_200);
    }
}
