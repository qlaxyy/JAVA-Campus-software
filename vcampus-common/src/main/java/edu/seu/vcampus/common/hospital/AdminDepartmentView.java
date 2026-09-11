package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** One department row with the operational facts needed by hospital administrators. */
public final class AdminDepartmentView implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String departmentId;
    private final String departmentName;
    private final String parentDepartmentId;
    private final String parentDepartmentName;
    private final boolean bookable;
    private final boolean active;
    private final int doctorCount;
    private final int futureScheduleCount;

    public AdminDepartmentView(
            String departmentId,
            String departmentName,
            String parentDepartmentId,
            String parentDepartmentName,
            boolean bookable,
            boolean active,
            int doctorCount,
            int futureScheduleCount) {
        this.departmentId = Objects.requireNonNull(departmentId);
        this.departmentName = Objects.requireNonNull(departmentName);
        this.parentDepartmentId = parentDepartmentId;
        this.parentDepartmentName = parentDepartmentName;
        this.bookable = bookable;
        this.active = active;
        this.doctorCount = doctorCount;
        this.futureScheduleCount = futureScheduleCount;
    }

    public String getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getParentDepartmentId() { return parentDepartmentId; }
    public String getParentDepartmentName() { return parentDepartmentName; }
    public boolean isBookable() { return bookable; }
    public boolean isActive() { return active; }
    public int getDoctorCount() { return doctorCount; }
    public int getFutureScheduleCount() { return futureScheduleCount; }
}
