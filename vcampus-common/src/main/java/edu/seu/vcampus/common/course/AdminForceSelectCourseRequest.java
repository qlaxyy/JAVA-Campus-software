package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 管理员为指定学生强制选课的请求。
 *
 * 教务只需要指定学生和教学班，
 * 不需要选择选课批次。
 */
public final class AdminForceSelectCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String studentId;
    private final long offeringId;
    private final String reason;

    public AdminForceSelectCourseRequest(
        String studentId,
        long offeringId,
        String reason) {

        this.studentId =
            requireText(
                studentId,
                "studentId");

        if (offeringId <= 0) {

            throw new IllegalArgumentException(
                "offeringId must be positive");
        }

        this.offeringId =
            offeringId;

        this.reason =
            requireText(
                reason,
                "reason");
    }

    public String getStudentId() {

        return studentId;
    }

    public long getOfferingId() {

        return offeringId;
    }

    public String getReason() {

        return reason;
    }

    private static String requireText(
        String value,
        String fieldName) {

        Objects.requireNonNull(
            value,
            fieldName + " must not be null");

        String normalized =
            value.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                fieldName + " must not be blank");
        }

        return normalized;
    }
}
