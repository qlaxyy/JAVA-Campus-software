package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarkAppointmentNoShowRequestTest {

    @Test
    void trimsAndRequiresAppointmentId() {
        assertEquals("appointment-1",
                new MarkAppointmentNoShowRequest("  appointment-1  ")
                        .getAppointmentId());
        assertThrows(NullPointerException.class,
                () -> new MarkAppointmentNoShowRequest(null));
        assertThrows(IllegalArgumentException.class,
                () -> new MarkAppointmentNoShowRequest("  "));
    }
}
