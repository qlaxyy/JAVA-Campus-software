package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** Internal department data owned by the hospital server. */
record HospitalDepartment(String departmentId, String departmentName, boolean active) {

    HospitalDepartment {
        Objects.requireNonNull(departmentId, "departmentId must not be null");
        Objects.requireNonNull(departmentName, "departmentName must not be null");
    }
}
