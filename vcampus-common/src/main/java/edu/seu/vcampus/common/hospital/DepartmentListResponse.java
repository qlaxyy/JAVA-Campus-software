package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Immutable department-list payload returned by the server. */
public final class DepartmentListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<DepartmentView> departments;

    public DepartmentListResponse(List<DepartmentView> departments) {
        this.departments = List.copyOf(Objects.requireNonNull(
                departments, "departments must not be null"));
    }

    public List<DepartmentView> getDepartments() {
        return departments;
    }
}
