package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;

import java.util.List;
import java.util.Objects;

/**
 * 学生选课业务服务。
 */
final class CourseSelectionService {

    private final CourseBatchService batchService;

    private final CoursePlanRepository
        planRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    CourseSelectionService(
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
     * 选择教学班。
     */
    synchronized CourseSelectionResult selectCourse(
        String userId,
        long batchId,
        long offeringId) {

        /*
         * 1. 检查批次。
         */
        SelectionBatchInfo batch =
            batchService.findBatch(
                batchId);

        if (batch == null) {

            return CourseSelectionResult.failure(
                "选课批次不存在。");
        }

        if (batch.getStatus()
            != SelectionBatchStatus.OPEN) {

            return CourseSelectionResult.failure(
                "当前批次不在选课时间内。");
        }

        if (!batch.isAllowSelect()) {

            return CourseSelectionResult.failure(
                "当前批次不可选课。");
        }

        /*
         * 2. 找到目标课程和教学班。
         */
        List<CourseInfo> courses =
            planRepository.findPlanCourses(
                batchId);

        CourseInfo targetCourse = null;
        OfferingInfo targetOffering = null;

        for (CourseInfo course : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    targetCourse = course;
                    targetOffering = offering;

                    break;
                }
            }

            if (targetOffering != null) {
                break;
            }
        }

        if (targetCourse == null
            || targetOffering == null) {

            return CourseSelectionResult.failure(
                "该教学班不在当前可选课程范围内。");
        }

        /*
         * 3. 同一门课程只能选一个教学班。
         */
        for (OfferingInfo offering
            : targetCourse.getOfferings()) {

            if (enrollmentRepository
                .isOfferingSelected(
                    userId,
                    offering.getOfferingId())) {

                return CourseSelectionResult.failure(
                    "已经选择该课程。");
            }
        }

        /*
         * 4. 重新计算当前人数。
         */
        int currentSelectedCount =
            targetOffering.getSelectedCount()
                + enrollmentRepository
                .countAdditionalSelections(
                    offeringId);

        if (currentSelectedCount
            >= targetOffering.getCapacity()) {

            return CourseSelectionResult.failure(
                "人数已满。");
        }

        /*
         * 5. 当前版本只允许 AVAILABLE。
         *
         * TIME_CONFLICT 等规则后面继续完善。
         */
        if (!"AVAILABLE".equals(
            targetOffering
                .getAvailabilityStatus())) {

            return CourseSelectionResult.failure(
                availabilityMessage(
                    targetOffering
                        .getAvailabilityStatus()));
        }

        /*
         * 6. 保存选课记录。
         */
        enrollmentRepository.select(
            userId,
            batchId,
            offeringId);

        return CourseSelectionResult.success(
            "选课成功。");
    }

    private String availabilityMessage(
        String status) {

        return switch (status) {

            case "FULL" ->
                "人数已满。";

            case "TIME_CONFLICT" ->
                "时间冲突。";

            case "NOT_ELIGIBLE" ->
                "不符合选课条件。";

            case "COURSE_ALREADY_SELECTED",
                 "SELECTED" ->
                "已经选择该课程。";

            case "OFFERING_CLOSED" ->
                "当前教学班不可选。";

            default ->
                "当前教学班不可选。";
        };
    }
}

/**
 * 服务器内部使用的选课结果。
 */
record CourseSelectionResult(
    boolean success,
    String message) {

    static CourseSelectionResult success(
        String message) {

        return new CourseSelectionResult(
            true,
            message);
    }

    static CourseSelectionResult failure(
        String message) {

        return new CourseSelectionResult(
            false,
            message);
    }
}
