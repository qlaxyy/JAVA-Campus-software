package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Immutable list of appointments owned by the authenticated patient. */
public final class AppointmentListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<AppointmentView> appointments;

    public AppointmentListResponse(List<AppointmentView> appointments) {
        this.appointments = List.copyOf(Objects.requireNonNull(
                appointments, "appointments must not be null"));
    }

    public List<AppointmentView> getAppointments() {
        return appointments;
    }
}
