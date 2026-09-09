package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Requests one appointment's consultation background for its assigned doctor. */
public final class DoctorConsultationContextRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;

    public DoctorConsultationContextRequest(String appointmentId) {
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
