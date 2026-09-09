package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Creates one root category or one child department. */
public final class CreateDepartmentRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String departmentName;
    private final String parentDepartmentId;
    private final boolean bookable;

    public CreateDepartmentRequest(
            String departmentName, String parentDepartmentId, boolean bookable) {
        this.departmentName = departmentName;
        this.parentDepartmentId = normalize(parentDepartmentId);
        this.bookable = bookable;
    }

    public String getDepartmentName() { return departmentName; }
    public String getParentDepartmentId() { return parentDepartmentId; }
    public boolean isBookable() { return bookable; }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
