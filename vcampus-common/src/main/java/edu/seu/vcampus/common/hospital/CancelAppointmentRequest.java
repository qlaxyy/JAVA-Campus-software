package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Request to cancel one appointment owned by the authenticated patient. */
public final class CancelAppointmentRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;

    public CancelAppointmentRequest(String appointmentId) {
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
