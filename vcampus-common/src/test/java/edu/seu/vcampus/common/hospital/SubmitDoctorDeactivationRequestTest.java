package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmitDoctorDeactivationRequestTest {

    @Test
    void trimsValidInput() {
        SubmitDoctorDeactivationRequest request =
                new SubmitDoctorDeactivationRequest(" doctor-liu ", " 离岗 ");

        assertEquals("doctor-liu", request.getDoctorId());
        assertEquals("离岗", request.getReason());
    }

    @Test
    void rejectsMissingOrOversizedFields() {
        assertThrows(NullPointerException.class,
                () -> new SubmitDoctorDeactivationRequest(null, "离岗"));
        assertThrows(IllegalArgumentException.class,
                () -> new SubmitDoctorDeactivationRequest(" ", "离岗"));
        assertThrows(IllegalArgumentException.class,
                () -> new SubmitDoctorDeactivationRequest("doctor-liu", " "));
        assertThrows(IllegalArgumentException.class,
                () -> new SubmitDoctorDeactivationRequest("d".repeat(37), "离岗"));
        assertThrows(IllegalArgumentException.class,
                () -> new SubmitDoctorDeactivationRequest("doctor-liu", "r".repeat(241)));
    }
}
