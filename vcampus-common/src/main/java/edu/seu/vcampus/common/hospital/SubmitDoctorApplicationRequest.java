package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Locale;

/** Hospital-administrator request for adding or binding one doctor. */
public final class SubmitDoctorApplicationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final DoctorApplicationType applicationType;
    private final String existingUsername;
    private final String displayName;
    private final String departmentId;
    private final String doctorTitle;

    private SubmitDoctorApplicationRequest(
            DoctorApplicationType applicationType,
            String existingUsername,
            String displayName,
            String departmentId,
            String doctorTitle) {
        this.applicationType = Objects.requireNonNull(
                applicationType, "applicationType must not be null");
        this.existingUsername = applicationType == DoctorApplicationType.EXISTING_ACCOUNT
                ? normalizeUsername(existingUsername) : null;
        this.displayName = applicationType == DoctorApplicationType.EXTERNAL_DOCTOR
                ? requireText(displayName, "displayName", 100) : null;
        this.departmentId = requireText(departmentId, "departmentId", 36);
        this.doctorTitle = requireText(doctorTitle, "doctorTitle", 50);
    }

    /** Creates a request that explicitly binds an account already in the campus directory. */
    public static SubmitDoctorApplicationRequest forExistingAccount(
            String username,
            String departmentId,
            String doctorTitle) {
        return new SubmitDoctorApplicationRequest(
                DoctorApplicationType.EXISTING_ACCOUNT,
                username, null, departmentId, doctorTitle);
    }

    /** Creates a request whose login account will be generated after approval. */
    public static SubmitDoctorApplicationRequest forExternalDoctor(
            String displayName,
            String departmentId,
            String doctorTitle) {
        return new SubmitDoctorApplicationRequest(
                DoctorApplicationType.EXTERNAL_DOCTOR,
                null, displayName, departmentId, doctorTitle);
    }

    public DoctorApplicationType getApplicationType() { return applicationType; }
    public String getExistingUsername() { return existingUsername; }
    public String getDisplayName() { return displayName; }
    public String getDepartmentId() { return departmentId; }
    public String getDoctorTitle() { return doctorTitle; }

    private static String normalizeUsername(String value) {
        String normalized = requireText(value, "username", 32)
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{3,32}")) {
            throw new IllegalArgumentException("username format is invalid");
        }
        return normalized;
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " format is invalid");
        }
        return normalized;
    }
}
