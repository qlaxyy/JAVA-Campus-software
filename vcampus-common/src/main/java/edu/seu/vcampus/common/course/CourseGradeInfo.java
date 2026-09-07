package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生课程成绩信息。
 */
public final class CourseGradeInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long gradeId;
    private final long enrollmentId;
    private final String studentId;
    private final long offeringId;
    private final String courseCode;
    private final String courseName;
    private final String classNo;
    private final Double score;
    private final LocalDateTime recordedAt;

    public CourseGradeInfo(
        Long gradeId,
        long enrollmentId,
        String studentId,
        long offeringId,
        String courseCode,
        String courseName,
        String classNo,
        Double score,
        LocalDateTime recordedAt) {

        this.gradeId =
            gradeId;

        this.enrollmentId =
            enrollmentId;

        this.studentId =
            studentId;

        this.offeringId =
            offeringId;

        this.courseCode =
            courseCode;

        this.courseName =
            courseName;

        this.classNo =
            classNo;

        this.score =
            score;

        this.recordedAt =
            recordedAt;
    }

    public Long getGradeId() {

        return gradeId;
    }

    public long getEnrollmentId() {

        return enrollmentId;
    }

    public String getStudentId() {

        return studentId;
    }

    public long getOfferingId() {

        return offeringId;
    }

    public String getCourseCode() {

        return courseCode;
    }

    public String getCourseName() {

        return courseName;
    }

    public String getClassNo() {

        return classNo;
    }

    public Double getScore() {

        return score;
    }

    public LocalDateTime getRecordedAt() {

        return recordedAt;
    }

    public boolean isRecorded() {

        return score != null;
    }

    public boolean isPassed() {

        return score != null
            && score >= 60.0;
    }
}
