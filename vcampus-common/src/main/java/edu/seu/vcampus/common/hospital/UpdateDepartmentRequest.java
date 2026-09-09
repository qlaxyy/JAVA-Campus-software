package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Updates safe hospital-owned department attributes without deleting history. */
public final class UpdateDepartmentRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String departmentId;
    private final String departmentName;
    private final String parentDepartmentId;
    private final boolean bookable;
    private final boolean active;

    public UpdateDepartmentRequest(
            String departmentId,
            String departmentName,
            String parentDepartmentId,
            boolean bookable,
            boolean active) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.parentDepartmentId = normalize(parentDepartmentId);
        this.bookable = bookable;
        this.active = active;
    }

    public String getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getParentDepartmentId() { return parentDepartmentId; }
    public boolean isBookable() { return bookable; }
    public boolean isActive() { return active; }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
