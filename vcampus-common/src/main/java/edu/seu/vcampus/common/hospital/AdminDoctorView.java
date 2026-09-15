package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Doctor directory entry available to a hospital administrator. */
public final class AdminDoctorView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final String doctorName;
    private final String doctorTitle;
    private final String departmentId;
    private final String departmentName;
    private final String accountName;
    private final boolean active;

    public AdminDoctorView(
            String doctorId,
            String doctorName,
            String doctorTitle,
            String departmentId,
            String departmentName) {
        this(doctorId, doctorName, doctorTitle, departmentId, departmentName, true);
    }

    public AdminDoctorView(
            String doctorId,
            String doctorName,
            String doctorTitle,
            String departmentId,
            String departmentName,
            boolean active) {
        this(doctorId, doctorName, doctorTitle, departmentId, departmentName, "", active);
    }

    public AdminDoctorView(
            String doctorId,
            String doctorName,
            String doctorTitle,
            String departmentId,
            String departmentName,
            String accountName,
            boolean active) {
        this.doctorId = requireText(doctorId, "doctorId");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.accountName = accountName == null ? "" : accountName.trim();
        this.active = active;
    }

    public String getDoctorId() { return doctorId; }

    public String getDoctorName() { return doctorName; }

    public String getDoctorTitle() { return doctorTitle; }

    public String getDepartmentId() { return departmentId; }

    public String getDepartmentName() { return departmentName; }

    /** Campus-card login account when the shared user directory can resolve it. */
    public String getAccountName() { return accountName; }

    public boolean isActive() { return active; }

    @Override
    public String toString() {
        return doctorName + " " + doctorTitle + " · " + departmentName
                + (active ? "" : "（已停用）");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
