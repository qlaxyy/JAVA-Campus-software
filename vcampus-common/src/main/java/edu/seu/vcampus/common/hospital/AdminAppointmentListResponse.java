package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Non-clinical appointment ledger for hospital operations. */
public final class AdminAppointmentListResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<AdminAppointmentView> appointments;

    public AdminAppointmentListResponse(List<AdminAppointmentView> appointments) {
        this.appointments = List.copyOf(appointments);
    }

    public List<AdminAppointmentView> getAppointments() { return appointments; }
}
