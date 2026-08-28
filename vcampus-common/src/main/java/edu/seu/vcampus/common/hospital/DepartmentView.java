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

    public DepartmentView(String departmentId, String departmentName) {
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
