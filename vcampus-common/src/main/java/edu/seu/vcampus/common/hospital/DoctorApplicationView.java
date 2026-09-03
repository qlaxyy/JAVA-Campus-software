package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Safe doctor-registration request shown to hospital and super administrators. */
public final class DoctorApplicationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final String requestId;
    private final DoctorApplicationType applicationType;
    private final String username;
    private final String displayName;
    private final String departmentId;
    private final String departmentName;
    private final String doctorTitle;
    private final String requestedByUserId;
    private final DoctorApplicationStatus status;
    private final String targetUserId;
    private final String reviewedByUserId;
    private final LocalDateTime createdAt;

    public DoctorApplicationView(
            String requestId,
            DoctorApplicationType applicationType,
            String username,
            String displayName,
            String departmentId,
            String departmentName,
            String doctorTitle,
            String requestedByUserId,
            DoctorApplicationStatus status,
            String targetUserId,
            String reviewedByUserId,
            LocalDateTime createdAt) {
        this.requestId = Objects.requireNonNull(requestId);
        this.applicationType = Objects.requireNonNull(applicationType);
        this.username = username;
        this.displayName = Objects.requireNonNull(displayName);
        this.departmentId = Objects.requireNonNull(departmentId);
        this.departmentName = Objects.requireNonNull(departmentName);
        this.doctorTitle = Objects.requireNonNull(doctorTitle);
        this.requestedByUserId = Objects.requireNonNull(requestedByUserId);
        this.status = Objects.requireNonNull(status);
        this.targetUserId = targetUserId;
        this.reviewedByUserId = reviewedByUserId;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getRequestId() { return requestId; }
    public DoctorApplicationType getApplicationType() { return applicationType; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getDoctorTitle() { return doctorTitle; }
    public String getRequestedByUserId() { return requestedByUserId; }
    public DoctorApplicationStatus getStatus() { return status; }
    public String getTargetUserId() { return targetUserId; }
    public String getReviewedByUserId() { return reviewedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
