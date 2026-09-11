package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Doctors and future schedules needed by the hospital schedule workspace. */
public final class AdminScheduleWorkspaceView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<AdminDoctorView> doctors;
    private final List<SlotView> schedules;

    public AdminScheduleWorkspaceView(
            List<AdminDoctorView> doctors,
            List<SlotView> schedules) {
        this.doctors = List.copyOf(Objects.requireNonNull(
                doctors, "doctors must not be null"));
        this.schedules = List.copyOf(Objects.requireNonNull(
                schedules, "schedules must not be null"));
    }

    public List<AdminDoctorView> getDoctors() { return doctors; }

    public List<SlotView> getSchedules() { return schedules; }
}
