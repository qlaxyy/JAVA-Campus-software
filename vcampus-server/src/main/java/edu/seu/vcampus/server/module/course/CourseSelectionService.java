package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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
         * =========================
         * 1. 检查选课批次
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
         * 2. 获取当前可选课程
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
         * =========================
         * 3. 同一门课程不能重复选择
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
         * 4. 检查容量
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
         * 5. 检查教学班基础状态
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
         * 6. 时间冲突检测
         * =========================
         */
        if (hasConflictWithSelectedCourses(
            userId,
            targetOffering,
            courses)) {

            return CourseSelectionResult.failure(
                "时间冲突。");
        }

        /*
         * =========================
         * 7. 保存选课记录
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
     * 判断目标教学班是否与学生当前已选教学班冲突。
     */
    private boolean hasConflictWithSelectedCourses(
        String userId,
        OfferingInfo targetOffering,
        List<CourseInfo> courses) {

        Set<Long> selectedOfferingIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        if (selectedOfferingIds.isEmpty()) {
            return false;
        }

        for (Long selectedOfferingId
            : selectedOfferingIds) {

            OfferingInfo selectedOffering =
                findOffering(
                    courses,
                    selectedOfferingId);

            /*
             * 当前阶段所有选课都来自方案内课程，
             * 因此原则上能够找到。
             */
            if (selectedOffering == null) {
                continue;
            }

            if (offeringsConflict(
                targetOffering,
                selectedOffering)) {

                return true;
            }
        }

        return false;
    }

    /**
     * 根据 offeringId 找教学班。
     */
    private OfferingInfo findOffering(
        List<CourseInfo> courses,
        long offeringId) {

        for (CourseInfo course : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (offering.getOfferingId()
                    == offeringId) {

                    return offering;
                }
            }
        }

        return null;
    }

    /**
     * 判断两个教学班是否存在至少一组冲突时间。
     */
    private boolean offeringsConflict(
        OfferingInfo first,
        OfferingInfo second) {

        for (ScheduleInfo firstSchedule
            : first.getSchedules()) {

            for (ScheduleInfo secondSchedule
                : second.getSchedules()) {

                if (schedulesConflict(
                    firstSchedule,
                    secondSchedule)) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 判断两条上课时间是否冲突。
     *
     * 冲突条件：
     *
     * 1. 星期相同
     * 2. 节次有交集
     * 3. 实际教学周有交集
     */
    private boolean schedulesConflict(
        ScheduleInfo first,
        ScheduleInfo second) {

        /*
         * 星期不同。
         */
        if (first.getDayOfWeek()
            != second.getDayOfWeek()) {

            return false;
        }

        /*
         * 节次没有重叠。
         *
         * 例如：
         * 1-2节 和 3-4节
         */
        if (!periodsOverlap(
            first,
            second)) {

            return false;
        }

        /*
         * 最后判断实际教学周。
         */
        return teachingWeeksOverlap(
            first,
            second);
    }

    /**
     * 节次是否重叠。
     */
    private boolean periodsOverlap(
        ScheduleInfo first,
        ScheduleInfo second) {

        return first.getStartPeriod()
            <= second.getEndPeriod()
            &&
            second.getStartPeriod()
                <= first.getEndPeriod();
    }

    /**
     * 判断两条记录是否存在真正共同上课的一周。
     *
     * 同时处理：
     * EVERY
     * ODD
     * EVEN
     */
    private boolean teachingWeeksOverlap(
        ScheduleInfo first,
        ScheduleInfo second) {

        int startWeek =
            Math.max(
                first.getStartWeek(),
                second.getStartWeek());

        int endWeek =
            Math.min(
                first.getEndWeek(),
                second.getEndWeek());

        /*
         * 周次范围本身不重叠。
         *
         * 例如：
         * 1-8周
         * 9-16周
         */
        if (startWeek > endWeek) {
            return false;
        }

        /*
         * 在共同周次范围中寻找至少一个
         * 两门课都会真正上课的周。
         */
        for (int week = startWeek;
             week <= endWeek;
             week++) {

            if (isTeachingWeek(
                first,
                week)
                &&
                isTeachingWeek(
                    second,
                    week)) {

                return true;
            }
        }

        return false;
    }

    /**
     * 指定周是否实际上课。
     */
    private boolean isTeachingWeek(
        ScheduleInfo schedule,
        int week) {

        return switch (
            schedule.getWeekPattern()) {

            case "ODD" ->
                week % 2 == 1;

            case "EVEN" ->
                week % 2 == 0;

            /*
             * EVERY，以及当前阶段未知值，
             * 按每周处理。
             */
            default ->
                true;
        };
    }

    /**
     * 教学班不可选原因。
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
