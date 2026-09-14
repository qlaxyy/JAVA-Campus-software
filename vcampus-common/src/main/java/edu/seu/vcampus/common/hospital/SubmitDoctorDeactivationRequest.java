package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Hospital-administrator request to stop a doctor without deleting history. */
public final class SubmitDoctorDeactivationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final String reason;

    public SubmitDoctorDeactivationRequest(String doctorId, String reason) {
        this.doctorId = requireText(doctorId, "doctorId", 36);
        this.reason = requireText(reason, "reason", 240);
    }

    public String getDoctorId() { return doctorId; }

    public String getReason() { return reason; }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " format is invalid");
        }
        return normalized;
    }
}
