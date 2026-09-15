package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Generate and publish 09:00–18:00 half-hour slots for selected days in one week. */
public final class GenerateWeeklySchedulesRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final LocalDate startDate;
    private final List<DayOfWeek> workdays;
    private final int registrationFeeCents;
    private final int capacity;

    public GenerateWeeklySchedulesRequest(
            String doctorId,
            LocalDate startDate,
            List<DayOfWeek> workdays,
            int registrationFeeCents,
            int capacity) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new IllegalArgumentException("doctorId is required");
        }
        this.doctorId = doctorId.trim();
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.workdays = List.copyOf(Objects.requireNonNull(workdays,
                "workdays must not be null"));
        if (this.workdays.isEmpty() || this.workdays.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("at least one workday is required");
        }
        if (registrationFeeCents < 0 || registrationFeeCents > 100_000) {
            throw new IllegalArgumentException("registrationFeeCents is out of range");
        }
        if (capacity <= 0 || capacity > 200) {
            throw new IllegalArgumentException("capacity is out of range");
        }
        this.registrationFeeCents = registrationFeeCents;
        this.capacity = capacity;
    }

    public String getDoctorId() { return doctorId; }

    public LocalDate getStartDate() { return startDate; }

    public List<DayOfWeek> getWorkdays() { return workdays; }

    public int getRegistrationFeeCents() { return registrationFeeCents; }

    public int getCapacity() { return capacity; }
}
