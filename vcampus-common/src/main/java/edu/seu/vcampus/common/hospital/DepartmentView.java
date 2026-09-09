package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Public department option shown by the hospital client. */
public final class DepartmentView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String departmentId;
    private final String departmentName;
    private final String parentDepartmentId;
    private final boolean bookable;

    public DepartmentView(String departmentId, String departmentName) {
        this(departmentId, departmentName, null, true);
    }

    public DepartmentView(
            String departmentId,
            String departmentName,
            String parentDepartmentId,
            boolean bookable) {
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.parentDepartmentId = normalizeOptional(parentDepartmentId);
        if (this.departmentId.equals(this.parentDepartmentId)) {
            throw new IllegalArgumentException("department cannot be its own parent");
        }
        this.bookable = bookable;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getParentDepartmentId() {
        return parentDepartmentId;
    }

    public boolean isBookable() {
        return bookable;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("parentDepartmentId must not be blank");
        }
        return value.trim();
    }
}
