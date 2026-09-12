package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教务添加或移除任课教师的请求。
 */
public final class AdminTeacherAssignmentRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long offeringId;
    private final String teacherUserId;

    public AdminTeacherAssignmentRequest(
        long offeringId,
        String teacherUserId) {

        if (offeringId <= 0) {

            throw new IllegalArgumentException(
                "offeringId must be positive");
        }

        this.offeringId =
            offeringId;

        this.teacherUserId =
            requireText(
                teacherUserId,
                "teacherUserId");
    }

    public long getOfferingId() {

        return offeringId;
    }

    public String getTeacherUserId() {

        return teacherUserId;
    }

    private String requireText(
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
