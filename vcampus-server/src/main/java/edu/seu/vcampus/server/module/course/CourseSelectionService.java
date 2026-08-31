package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 学生选课业务服务。
 */
final class CourseSelectionService {

    private final CourseBatchService
        batchService;

    private final CoursePlanRepository
        planRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseHistoryRepository
        historyRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    CourseSelectionService(
        CourseBatchService batchService,
        CoursePlanRepository planRepository,
        CourseEnrollmentRepository enrollmentRepository,
        CourseHistoryRepository historyRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);

        this.historyRepository =
            Objects.requireNonNull(
                historyRepository);
    }

    /**
     * 选择教学班。
     */
    synchronized CourseSelectionResult selectCourse(
        String userId,
        long batchId,
        long offeringId) {

        /*
         * =========================
         * 1. 批次校验
         * =========================
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
         * =========================
         * 2. 找课程和教学班
         * =========================
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

                    targetCourse =
                        course;

                    targetOffering =
                        offering;

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
         * =========================
         * 3. 历史修读资格
         * =========================
         */
        boolean previouslyTaken =
            historyRepository
                .hasTakenCourse(
                    userId,
                    targetCourse.getCourseId());

        /*
         * 重修批次：
         *
         * 必须以前修过。
         */
        if (batch.getBatchType()
            == SelectionBatchType.RETAKE
            && !previouslyTaken) {

            return CourseSelectionResult.failure(
                "不符合重修选课条件。");
        }

        /*
         * 非重修批次：
         *
         * 以前已经修过的普通课程，
         * 不允许再次普通选课。
         */
        if (batch.getBatchType()
            != SelectionBatchType.RETAKE
            && previouslyTaken) {

            return CourseSelectionResult.failure(
                "该课程已修读，请通过重修选课。");
        }

        /*
         * =========================
         * 4. 同一课程不能重复选择
         * =========================
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
         * =========================
         * 5. 容量
         * =========================
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
         * =========================
         * 6. 教学班基础状态
         * =========================
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
         * =========================
         * 7. 时间冲突
         * =========================
         */
        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                userId,
                courses);

        if (conflictChecker.hasConflict(
            targetOffering,
            selectedOfferings)) {

            return CourseSelectionResult.failure(
                "时间冲突。");
        }

        /*
         * =========================
         * 8. 保存选课
         * =========================
         */
        enrollmentRepository.select(
            userId,
            batchId,
            offeringId);

        return CourseSelectionResult.success(
            "选课成功。");
    }

    /**
     * 当前学生已选教学班。
     */
    private List<OfferingInfo> findSelectedOfferings(
        String userId,
        List<CourseInfo> courses) {

        Set<Long> selectedIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> result =
            new ArrayList<>();

        for (CourseInfo course : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (selectedIds.contains(
                    offering.getOfferingId())) {

                    result.add(
                        offering);
                }
            }
        }

        return result;
    }

    /**
     * 不可选状态文字。
     */
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
 * 服务器内部选课结果。
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
