package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Department catalog returned only to hospital administrators. */
public final class AdminDepartmentWorkspaceView implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<AdminDepartmentView> departments;

    public AdminDepartmentWorkspaceView(List<AdminDepartmentView> departments) {
        this.departments = List.copyOf(departments);
    }

    public List<AdminDepartmentView> getDepartments() { return departments; }
}
