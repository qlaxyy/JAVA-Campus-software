package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** Internal hierarchical department data owned by the hospital server. */
record HospitalDepartment(
        String departmentId,
        String departmentName,
        String parentDepartmentId,
        boolean bookable,
        boolean active) {

    HospitalDepartment {
        Objects.requireNonNull(departmentId, "departmentId must not be null");
        Objects.requireNonNull(departmentName, "departmentName must not be null");
        if (departmentId.isBlank() || departmentName.isBlank()) {
            throw new IllegalArgumentException("department identity must not be blank");
        }
        if (parentDepartmentId != null && parentDepartmentId.isBlank()) {
            throw new IllegalArgumentException("parentDepartmentId must not be blank");
        }
        if (departmentId.equals(parentDepartmentId)) {
            throw new IllegalArgumentException("department cannot be its own parent");
        }
    }
}
