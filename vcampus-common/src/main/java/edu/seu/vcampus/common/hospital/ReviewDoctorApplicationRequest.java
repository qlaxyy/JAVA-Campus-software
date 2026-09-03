package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Super-administrator decision for one doctor registration request. */
public final class ReviewDoctorApplicationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final boolean approved;

    public ReviewDoctorApplicationRequest(String requestId, boolean approved) {
        Objects.requireNonNull(requestId, "requestId must not be null");
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        this.requestId = requestId.trim();
        this.approved = approved;
    }

    public String getRequestId() { return requestId; }
    public boolean isApproved() { return approved; }
}
