package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Doctor request to mark one assigned, ended appointment as not attended. */
public final class MarkAppointmentNoShowRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;

    public MarkAppointmentNoShowRequest(String appointmentId) {
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        if (appointmentId.isBlank()) {
            throw new IllegalArgumentException("appointmentId must not be blank");
        }
        this.appointmentId = appointmentId.trim();
    }

    public String getAppointmentId() {
        return appointmentId;
    }
}
