package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教师端教学班学生信息。
 */
public final class TeacherStudentInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    /**
     * 选课记录 ID。
     */
    private final long enrollmentId;

    /**
     * 学号。
     */
    private final String studentId;

    /**
     * 选课批次 ID。
     */
    private final long batchId;

    /**
     * 教学班 ID。
     */
    private final long offeringId;

    public TeacherStudentInfo(
        long enrollmentId,
        String studentId,
        long batchId,
        long offeringId) {

        this.enrollmentId =
            enrollmentId;

        this.studentId =
            Objects.requireNonNull(
                studentId);

        this.batchId =
            batchId;

        this.offeringId =
            offeringId;
    }

    public long getEnrollmentId() {

        return enrollmentId;
    }

    public String getStudentId() {

        return studentId;
    }

    public long getBatchId() {

        return batchId;
    }

    public long getOfferingId() {

        return offeringId;
    }
}
