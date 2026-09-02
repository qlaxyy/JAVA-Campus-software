package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.EnrollmentInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 已选课程及退课业务。
 *
 * 当前支持：
 *
 * - 方案内
 * - 方案外
 * - 体育
 * - 通选
 */
final class CourseEnrollmentService {

    private final CourseBatchService
        batchService;

    private final CoursePlanRepository
        planRepository;

    private final CourseSubstitutionRepository
        substitutionRepository;

    private final PeCourseRepository
        peCourseRepository;

    private final GeneralCourseRepository
        generalCourseRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    CourseEnrollmentService(
        CourseBatchService batchService,
        CoursePlanRepository planRepository,
        CourseSubstitutionRepository substitutionRepository,
        PeCourseRepository peCourseRepository,
        GeneralCourseRepository generalCourseRepository,
        CourseEnrollmentRepository enrollmentRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);

        this.peCourseRepository =
            Objects.requireNonNull(
                peCourseRepository);

        this.generalCourseRepository =
            Objects.requireNonNull(
                generalCourseRepository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);
    }

    /**
     * 查询当前学生已选课程。
     */
    List<EnrollmentInfo> listEnrollments(
        String userId,
        long currentBatchId) {

        SelectionBatchInfo currentBatch =
            batchService.findBatch(
                currentBatchId);

        boolean canDrop =
            currentBatch != null
                && currentBatch.getStatus()
                == SelectionBatchStatus.OPEN
                && currentBatch.isAllowDrop();

        String dropUnavailableReason =
            canDrop
                ? null
                : "当前批次不可退课";

        List<CourseEnrollmentRecord> records =
            enrollmentRepository
                .findSelectedEnrollments(
                    userId);

        List<EnrollmentInfo> result =
            new ArrayList<>();

        for (CourseEnrollmentRecord record
            : records) {

            CourseAndOffering resolved =
                resolveCourseAndOffering(
                    record);

            if (resolved == null) {

                continue;
            }

            CourseInfo course =
                resolved.course();

            OfferingInfo offering =
                resolved.offering();

            result.add(
                new EnrollmentInfo(
                    record.enrollmentId(),
                    offering.getOfferingId(),
                    course.getCourseCode(),
                    course.getCourseName(),
                    offering.getClassNo(),
                    offering.getTeacherNames(),
                    offering.getSchedules(),
                    offering.getLocationName(),
                    course.getCredits(),
                    course.getCourseType(),
                    canDrop,
                    dropUnavailableReason));
        }

        return result;
    }

    /**
     * 退课。
     */
    synchronized CourseDropResult dropCourse(
        String userId,
        long currentBatchId,
        long enrollmentId) {

        SelectionBatchInfo batch =
            batchService.findBatch(
                currentBatchId);

        if (batch == null
            || batch.getStatus()
            != SelectionBatchStatus.OPEN
            || !batch.isAllowDrop()) {

            return CourseDropResult.failure(
                "当前批次不可退课");
        }

        CourseEnrollmentRecord record =
            enrollmentRepository
                .findSelectedEnrollment(
                    userId,
                    enrollmentId);

        if (record == null) {

            return CourseDropResult.failure(
                "选课记录不存在。");
        }

        boolean dropped =
            enrollmentRepository.drop(
                userId,
                enrollmentId);

        if (!dropped) {

            return CourseDropResult.failure(
                "退课失败。");
        }

        return CourseDropResult.success(
            "退课成功。");
    }

    /**
     * 根据选课记录解析课程。
     *
     * 顺序：
     *
     * 方案内
     * → 方案外
     * → 体育
     * → 通选
     */
    private CourseAndOffering
    resolveCourseAndOffering(
        CourseEnrollmentRecord record) {

        /*
         * 方案内。
         */
        CourseAndOffering resolved =
            findCourseAndOffering(
                planRepository
                    .findPlanCourses(
                        record
                            .selectedBatchId()),
                record.offeringId());

        if (resolved != null) {

            return resolved;
        }

        /*
         * 方案外。
         */
        resolved =
            findCourseAndOffering(
                substitutionRepository
                    .findSubstituteCourses(
                        record
                            .selectedBatchId()),
                record.offeringId());

        if (resolved != null) {

            return resolved;
        }

        /*
         * 体育。
         */
        for (PeCourseRecord peRecord
            : peCourseRepository
            .findPeCourses(
                record.selectedBatchId())) {

            resolved =
                findCourseAndOffering(
                    List.of(
                        peRecord.course()),
                    record.offeringId());

            if (resolved != null) {

                return resolved;
            }
        }

        /*
         * 通选。
         */
        for (GeneralCourseRecord generalRecord
            : generalCourseRepository
            .findGeneralCourses(
                record.selectedBatchId())) {

            resolved =
                findCourseAndOffering(
                    List.of(
                        generalRecord.course()),
                    record.offeringId());

            if (resolved != null) {

                return resolved;
            }
        }

        return null;
    }

    private CourseAndOffering
    findCourseAndOffering(
        List<CourseInfo> courses,
        long offeringId) {

        for (CourseInfo course
            : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    return new CourseAndOffering(
                        course,
                        offering);
                }
            }
        }

        return null;
    }

    private record CourseAndOffering(
        CourseInfo course,
        OfferingInfo offering) {
    }
}

/**
 * Server 内部退课结果。
 */
record CourseDropResult(
    boolean success,
    String message) {

    static CourseDropResult success(
        String message) {

        return new CourseDropResult(
            true,
            message);
    }

    static CourseDropResult failure(
        String message) {

        return new CourseDropResult(
            false,
            message);
    }
}
