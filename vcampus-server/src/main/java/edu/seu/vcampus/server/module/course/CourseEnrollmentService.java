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
 */
final class CourseEnrollmentService {

    private final CourseBatchService
        batchService;

    private final CoursePlanRepository
        planRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    CourseEnrollmentService(
        CourseBatchService batchService,
        CoursePlanRepository planRepository,
        CourseEnrollmentRepository enrollmentRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);
    }

    /**
     * 查询学生当前已选课程。
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

                /*
                 * 当前 demo 阶段理论上不会发生。
                 * 后面接数据库以后会改成独立 Offering Repository。
                 */
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

        /*
         * =========================
         * 1. 再次检查当前批次
         * =========================
         */
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

        /*
         * =========================
         * 2. 必须是自己的有效选课记录
         * =========================
         */
        CourseEnrollmentRecord record =
            enrollmentRepository
                .findSelectedEnrollment(
                    userId,
                    enrollmentId);

        if (record == null) {

            return CourseDropResult.failure(
                "选课记录不存在。");
        }

        /*
         * =========================
         * 3. 执行退课
         * =========================
         */
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
     * 通过选课记录找到课程和教学班。
     *
     * 当前 demo 的 CoursePlanRepository
     * 暂时承担教学班数据来源。
     */
    private CourseAndOffering resolveCourseAndOffering(
        CourseEnrollmentRecord record) {

        List<CourseInfo> courses =
            planRepository.findPlanCourses(
                record.selectedBatchId());

        for (CourseInfo course : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == record.offeringId()) {

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
 * 服务器内部退课结果。
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
