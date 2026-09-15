package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** One atomic manual scheduling request, containing multiple 30-minute draft slots. */
public final class BatchCreateSchedulesRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<CreateScheduleRequest> schedules;

    public BatchCreateSchedulesRequest(List<CreateScheduleRequest> schedules) {
        this.schedules = List.copyOf(Objects.requireNonNull(
                schedules, "schedules must not be null"));
        if (this.schedules.isEmpty()) {
            throw new IllegalArgumentException("at least one schedule is required");
        }
        if (this.schedules.size() > 500) {
            throw new IllegalArgumentException("too many schedules in one request");
        }
        if (this.schedules.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("schedules must not contain null");
        }
    }

    public List<CreateScheduleRequest> getSchedules() {
        return schedules;
    }
}
