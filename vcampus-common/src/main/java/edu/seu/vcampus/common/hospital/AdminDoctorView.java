package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Active doctor option available to a hospital schedule administrator. */
public final class AdminDoctorView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final String doctorName;
    private final String doctorTitle;
    private final String departmentId;
    private final String departmentName;

    public AdminDoctorView(
            String doctorId,
            String doctorName,
            String doctorTitle,
            String departmentId,
            String departmentName) {
        this.doctorId = requireText(doctorId, "doctorId");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
    }

    public String getDoctorId() { return doctorId; }

    public String getDoctorName() { return doctorName; }

    public String getDoctorTitle() { return doctorTitle; }

    public String getDepartmentId() { return departmentId; }

    public String getDepartmentName() { return departmentName; }

    @Override
    public String toString() {
        return doctorName + " " + doctorTitle + " · " + departmentName;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
