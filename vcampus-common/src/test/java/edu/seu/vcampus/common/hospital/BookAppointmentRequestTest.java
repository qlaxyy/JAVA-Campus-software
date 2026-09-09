package edu.seu.vcampus.common.hospital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookAppointmentRequestTest {

    @Test
    void createsAndNormalizesFirstVisit() {
        BookAppointmentRequest request = BookAppointmentRequest.firstVisit(" schedule-1 ");

        assertEquals("schedule-1", request.getScheduleId());
        assertEquals(VisitType.FIRST_VISIT, request.getVisitType());
        assertNull(request.getSourceFirstVisitAppointmentId());
    }

    @Test
    void createsAndNormalizesFollowUp() {
        BookAppointmentRequest request = BookAppointmentRequest.followUp(
                " schedule-2 ", " appointment-1 ");

        assertEquals("schedule-2", request.getScheduleId());
        assertEquals(VisitType.FOLLOW_UP, request.getVisitType());
        assertEquals("appointment-1", request.getSourceFirstVisitAppointmentId());
    }

    @Test
    void rejectsMissingOrBlankScheduleId() {
        assertThrows(NullPointerException.class,
                () -> BookAppointmentRequest.firstVisit(null));
        assertThrows(IllegalArgumentException.class,
                () -> BookAppointmentRequest.firstVisit("   "));
    }

    @Test
    void rejectsInvalidVisitTypeAndSourceCombination() {
        assertThrows(NullPointerException.class,
                () -> new BookAppointmentRequest("schedule-1", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new BookAppointmentRequest(
                        "schedule-1", VisitType.FIRST_VISIT, "appointment-1"));
        assertThrows(IllegalArgumentException.class,
                () -> new BookAppointmentRequest(
                        "schedule-1", VisitType.FOLLOW_UP, null));
    }

    @Test
    void rejectsBlankFollowUpSource() {
        assertThrows(IllegalArgumentException.class,
                () -> BookAppointmentRequest.followUp("schedule-1", "   "));
    }
}
