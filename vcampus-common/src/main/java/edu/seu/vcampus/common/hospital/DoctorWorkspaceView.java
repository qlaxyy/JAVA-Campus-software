package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Read-only doctor identity, schedules and pending-patient queue. */
public final class DoctorWorkspaceView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final String doctorName;
    private final String doctorTitle;
    private final String departmentId;
    private final String departmentName;
    private final List<DoctorScheduleView> schedules;
    private final List<DoctorFollowUpView> followUps;
    private final List<DoctorClinicalRecordView> signedRecords;

    public DoctorWorkspaceView(
            String doctorId,
            String doctorName,
            String doctorTitle,
            String departmentId,
            String departmentName,
            List<DoctorScheduleView> schedules,
            List<DoctorFollowUpView> followUps) {
        this(doctorId, doctorName, doctorTitle, departmentId, departmentName,
                schedules, followUps, List.of());
    }

    public DoctorWorkspaceView(
            String doctorId,
            String doctorName,
            String doctorTitle,
            String departmentId,
            String departmentName,
            List<DoctorScheduleView> schedules,
            List<DoctorFollowUpView> followUps,
            List<DoctorClinicalRecordView> signedRecords) {
        this.doctorId = requireText(doctorId, "doctorId");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.schedules = List.copyOf(Objects.requireNonNull(
                schedules, "schedules must not be null"));
        this.followUps = List.copyOf(Objects.requireNonNull(
                followUps, "followUps must not be null"));
        this.signedRecords = List.copyOf(Objects.requireNonNull(
                signedRecords, "signedRecords must not be null"));
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getDoctorTitle() {
        return doctorTitle;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public List<DoctorScheduleView> getSchedules() {
        return schedules;
    }

    public List<DoctorFollowUpView> getFollowUps() {
        return followUps;
    }

    public List<DoctorClinicalRecordView> getSignedRecords() {
        return signedRecords == null ? List.of() : signedRecords;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
