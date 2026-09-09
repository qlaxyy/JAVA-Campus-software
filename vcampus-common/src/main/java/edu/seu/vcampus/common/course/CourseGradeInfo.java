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
    private static final long serialVersionUID =
        1L;

    private final Long gradeId;

    private final long enrollmentId;

    private final String studentId;

    private final long offeringId;

    private final String courseCode;

    private final String courseName;

    private final String classNo;

    private final Double usualScore;

    private final Double finalExamScore;

    private final int usualWeightPercent;

    private final int finalExamWeightPercent;

    private final Double totalScore;

    private final LocalDateTime recordedAt;

    /**
     * 新成绩结构使用的构造器。
     */
    public CourseGradeInfo(
        Long gradeId,
        long enrollmentId,
        String studentId,
        long offeringId,
        String courseCode,
        String courseName,
        String classNo,
        Double usualScore,
        Double finalExamScore,
        int usualWeightPercent,
        int finalExamWeightPercent,
        Double totalScore,
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

        this.usualScore =
            usualScore;

        this.finalExamScore =
            finalExamScore;

        this.usualWeightPercent =
            usualWeightPercent;

        this.finalExamWeightPercent =
            finalExamWeightPercent;

        this.totalScore =
            totalScore;

        this.recordedAt =
            recordedAt;
    }

    /**
     * 兼容原有单项成绩代码。
     *
     * 原来的 score 同时作为平时成绩和期末成绩，
     * 因此计算后的总成绩仍然等于原 score。
     */
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

        this(
            gradeId,
            enrollmentId,
            studentId,
            offeringId,
            courseCode,
            courseName,
            classNo,
            score,
            score,
            40,
            60,
            score,
            recordedAt);
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

    public Double getUsualScore() {

        return usualScore;
    }

    public Double getFinalExamScore() {

        return finalExamScore;
    }

    public int getUsualWeightPercent() {

        return usualWeightPercent;
    }

    public int getFinalExamWeightPercent() {

        return finalExamWeightPercent;
    }

    public Double getTotalScore() {

        return totalScore;
    }

    /**
     * 兼容原有页面。
     *
     * 原来的 getScore() 现在返回总成绩。
     */
    public Double getScore() {

        return totalScore;
    }

    public LocalDateTime getRecordedAt() {

        return recordedAt;
    }

    public boolean isRecorded() {

        return usualScore != null
            && finalExamScore != null
            && totalScore != null;
    }

    public boolean isPassed() {

        return totalScore != null
            && totalScore >= 60.0;
    }
}
