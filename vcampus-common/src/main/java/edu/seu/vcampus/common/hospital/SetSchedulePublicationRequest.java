package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Publishes or closes one future hospital schedule. */
public final class SetSchedulePublicationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String scheduleId;
    private final boolean published;

    public SetSchedulePublicationRequest(String scheduleId, boolean published) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        if (scheduleId.isBlank()) {
            throw new IllegalArgumentException("scheduleId must not be blank");
        }
        this.scheduleId = scheduleId.trim();
        this.published = published;
    }

    public String getScheduleId() { return scheduleId; }

    public boolean isPublished() { return published; }
}
