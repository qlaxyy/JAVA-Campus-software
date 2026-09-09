package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelAppointmentRequestTest {

    @Test
    void keepsOnlyTheAppointmentIdentifier() {
        CancelAppointmentRequest request = new CancelAppointmentRequest(" appointment-1 ");

        assertEquals("appointment-1", request.getAppointmentId());
    }

    @Test
    void rejectsMissingAppointmentIdentifier() {
        assertThrows(NullPointerException.class,
                () -> new CancelAppointmentRequest(null));
        assertThrows(IllegalArgumentException.class,
                () -> new CancelAppointmentRequest("  "));
    }
}
