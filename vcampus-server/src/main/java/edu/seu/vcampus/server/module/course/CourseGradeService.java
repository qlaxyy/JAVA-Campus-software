package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseGradeInfo;
import edu.seu.vcampus.common.course.EnrollmentInfo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 成绩管理业务服务。
 */
final class CourseGradeService {

    private final CourseEnrollmentService
        enrollmentService;

    private final CourseGradeRepository
        gradeRepository;

    private final Clock clock;

    CourseGradeService(
        CourseEnrollmentService enrollmentService,
        CourseGradeRepository gradeRepository,
        Clock clock) {

        this.enrollmentService =
            Objects.requireNonNull(
                enrollmentService);

        this.gradeRepository =
            Objects.requireNonNull(
                gradeRepository);

        this.clock =
            Objects.requireNonNull(
                clock);
    }

    /**
     * 查询指定学生全部已选课程及成绩。
     *
     * 尚未录入成绩的课程也会返回，
     * 此时 score 和 recordedAt 为 null。
     */
    List<CourseGradeInfo> listGrades(
        String studentId) {

        String normalizedStudentId =
            normalizeStudentId(
                studentId);

        if (normalizedStudentId == null) {

            return List.of();
        }

        List<EnrollmentInfo> enrollments =
            enrollmentService
                .listAdminEnrollments(
                    normalizedStudentId);

        List<CourseGradeInfo> result =
            new ArrayList<>();

        for (EnrollmentInfo enrollment
            : enrollments) {

            CourseGradeRecord grade =
                gradeRepository
                    .findByEnrollmentId(
                        enrollment
                            .getEnrollmentId())
                    .orElse(null);

            result.add(
                toInfo(
                    normalizedStudentId,
                    enrollment,
                    grade));
        }

        return result;
    }

    /**
     * 超级管理员新增或修改成绩。
     */
    synchronized GradeUpdateResult updateGrade(
        String studentId,
        long enrollmentId,
        double score) {

        String normalizedStudentId =
            normalizeStudentId(
                studentId);

        if (normalizedStudentId == null) {

            return GradeUpdateResult.failure(
                "学生学号不能为空。");
        }

        if (enrollmentId <= 0) {

            return GradeUpdateResult.failure(
                "选课记录 ID 不正确。");
        }

        if (Double.isNaN(score)
            || Double.isInfinite(score)
            || score < 0.0
            || score > 100.0) {

            return GradeUpdateResult.failure(
                "成绩必须在 0 到 100 之间。");
        }

        EnrollmentInfo targetEnrollment =
            null;

        for (EnrollmentInfo enrollment
            : enrollmentService
            .listAdminEnrollments(
                normalizedStudentId)) {

            if (enrollment.getEnrollmentId()
                == enrollmentId) {

                targetEnrollment =
                    enrollment;

                break;
            }
        }

        if (targetEnrollment == null) {

            return GradeUpdateResult.failure(
                "未找到该学生对应的选课记录。");
        }

        CourseGradeRecord grade =
            gradeRepository.save(
                enrollmentId,
                score,
                LocalDateTime.now(
                    clock));

        return GradeUpdateResult.success(
            "成绩保存成功。",
            toInfo(
                normalizedStudentId,
                targetEnrollment,
                grade));
    }

    /**
     * 转换为客户端成绩信息。
     */
    private CourseGradeInfo toInfo(
        String studentId,
        EnrollmentInfo enrollment,
        CourseGradeRecord grade) {

        return new CourseGradeInfo(
            grade == null
                ? null
                : grade.gradeId(),
            enrollment.getEnrollmentId(),
            studentId,
            enrollment.getOfferingId(),
            enrollment.getCourseCode(),
            enrollment.getCourseName(),
            enrollment.getClassNo(),
            grade == null
                ? null
                : grade.score(),
            grade == null
                ? null
                : grade.recordedAt());
    }

    private String normalizeStudentId(
        String studentId) {

        if (studentId == null
            || studentId.isBlank()) {

            return null;
        }

        return studentId.trim();
    }
}

/**
 * 成绩修改结果。
 */
record GradeUpdateResult(
    boolean success,
    String message,
    CourseGradeInfo grade) {

    static GradeUpdateResult success(
        String message,
        CourseGradeInfo grade) {

        return new GradeUpdateResult(
            true,
            message,
            grade);
    }

    static GradeUpdateResult failure(
        String message) {

        return new GradeUpdateResult(
            false,
            message,
            null);
    }
}
