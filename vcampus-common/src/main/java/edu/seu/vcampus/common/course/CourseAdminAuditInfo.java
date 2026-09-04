package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 教务强制选课和退课日志。
 */
public final class CourseAdminAuditInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    private final long operationId;
    private final String operatorUsername;
    private final String studentId;
    private final String operationType;
    private final Long batchId;
    private final Long offeringId;
    private final Long enrollmentId;
    private final String reason;
    private final LocalDateTime operatedAt;

    public CourseAdminAuditInfo(
        long operationId,
        String operatorUsername,
        String studentId,
        String operationType,
        Long batchId,
        Long offeringId,
        Long enrollmentId,
        String reason,
        LocalDateTime operatedAt) {

        this.operationId =
            operationId;

        this.operatorUsername =
            requireText(
                operatorUsername,
                "operatorUsername");

        this.studentId =
            requireText(
                studentId,
                "studentId");

        this.operationType =
            requireText(
                operationType,
                "operationType");

        this.batchId =
            batchId;

        this.offeringId =
            offeringId;

        this.enrollmentId =
            enrollmentId;

        this.reason =
            requireText(
                reason,
                "reason");

        this.operatedAt =
            Objects.requireNonNull(
                operatedAt,
                "operatedAt must not be null");
    }

    public long getOperationId() {

        return operationId;
    }

    public String getOperatorUsername() {

        return operatorUsername;
    }

    public String getStudentId() {

        return studentId;
    }

    public String getOperationType() {

        return operationType;
    }

    public Long getBatchId() {

        return batchId;
    }

    public Long getOfferingId() {

        return offeringId;
    }

    public Long getEnrollmentId() {

        return enrollmentId;
    }

    public String getReason() {

        return reason;
    }

    public LocalDateTime getOperatedAt() {

        return operatedAt;
    }

    private static String requireText(
        String value,
        String fieldName) {

        Objects.requireNonNull(
            value,
            fieldName + " must not be null");

        String cleaned =
            value.trim();

        if (cleaned.isBlank()) {

            throw new IllegalArgumentException(
                fieldName + " must not be blank");
        }

        return cleaned;
    }
}
