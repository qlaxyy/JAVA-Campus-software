package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseGradeInfo;
import edu.seu.vcampus.common.course.CourseGradePolicyInfo;
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

    private final CourseGradePolicyService
        gradePolicyService;

    private final Clock clock;

    /**
     * 新版构造器。
     */
    CourseGradeService(
        CourseEnrollmentService enrollmentService,
        CourseGradeRepository gradeRepository,
        CourseGradePolicyService gradePolicyService,
        Clock clock) {

        this.enrollmentService =
            Objects.requireNonNull(
                enrollmentService);

        this.gradeRepository =
            Objects.requireNonNull(
                gradeRepository);

        this.gradePolicyService =
            Objects.requireNonNull(
                gradePolicyService);

        this.clock =
            Objects.requireNonNull(
                clock);
    }

    /**
     * 兼容原有测试代码的构造器。
     */
    CourseGradeService(
        CourseEnrollmentService enrollmentService,
        CourseGradeRepository gradeRepository,
        Clock clock) {

        this(
            enrollmentService,
            gradeRepository,
            new CourseGradePolicyService(
                new InMemoryCourseGradePolicyRepository(),
                clock),
            clock);
    }

    /**
     * 查询指定学生全部已选课程及成绩。
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
     * 保存平时成绩和期末成绩。
     */
    synchronized GradeUpdateResult updateGrade(
        String studentId,
        long enrollmentId,
        double usualScore,
        double finalExamScore) {

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

        if (!validScore(
            usualScore)) {

            return GradeUpdateResult.failure(
                "平时成绩必须在 0 到 100 之间。");
        }

        if (!validScore(
            finalExamScore)) {

            return GradeUpdateResult.failure(
                "期末成绩必须在 0 到 100 之间。");
        }

        EnrollmentInfo targetEnrollment =
            findEnrollment(
                normalizedStudentId,
                enrollmentId);

        if (targetEnrollment == null) {

            return GradeUpdateResult.failure(
                "未找到该学生对应的选课记录。");
        }

        CourseGradeRecord grade =
            gradeRepository.save(
                enrollmentId,
                usualScore,
                finalExamScore,
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
     * 兼容原有单项成绩修改代码。
     *
     * 旧 score 同时作为平时成绩和期末成绩。
     */
    synchronized GradeUpdateResult updateGrade(
        String studentId,
        long enrollmentId,
        double score) {

        return updateGrade(
            studentId,
            enrollmentId,
            score,
            score);
    }

    /**
     * 查找指定选课记录。
     */
    private EnrollmentInfo findEnrollment(
        String studentId,
        long enrollmentId) {

        for (EnrollmentInfo enrollment
            : enrollmentService
            .listAdminEnrollments(
                studentId)) {

            if (enrollment.getEnrollmentId()
                == enrollmentId) {

                return enrollment;
            }
        }

        return null;
    }

    /**
     * 转换为客户端成绩信息。
     */
    private CourseGradeInfo toInfo(
        String studentId,
        EnrollmentInfo enrollment,
        CourseGradeRecord grade) {

        CourseGradePolicyInfo policy =
            gradePolicyService.getPolicy(
                enrollment.getOfferingId());

        Double usualScore =
            grade == null
                ? null
                : grade.usualScore();

        Double finalExamScore =
            grade == null
                ? null
                : grade.finalExamScore();

        Double totalScore =
            grade == null
                ? null
                : gradePolicyService
                .calculateTotalScore(
                    enrollment.getOfferingId(),
                    grade.usualScore(),
                    grade.finalExamScore());

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
            usualScore,
            finalExamScore,
            policy.getUsualWeightPercent(),
            policy.getFinalExamWeightPercent(),
            totalScore,
            grade == null
                ? null
                : grade.recordedAt());
    }

    /**
     * 判断成绩是否合法。
     */
    private boolean validScore(
        double score) {

        return !Double.isNaN(
            score)
            && !Double.isInfinite(
            score)
            && score >= 0.0
            && score <= 100.0;
    }

    /**
     * 规范化学生学号。
     */
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
