package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 方案内课程业务服务。
 */
final class CoursePlanService {

    private final CourseBatchService
        batchService;

    private final CoursePlanRepository
        repository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseHistoryRepository
        historyRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    CoursePlanService(
        CourseBatchService batchService,
        CoursePlanRepository repository,
        CourseEnrollmentRepository enrollmentRepository,
        CourseHistoryRepository historyRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.repository =
            Objects.requireNonNull(
                repository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);

        this.historyRepository =
            Objects.requireNonNull(
                historyRepository);
    }

    /**
     * 查询学生方案内课程。
     */
    List<CourseInfo> listPlanCourses(
        long batchId,
        String userId) {

        if (batchId <= 0) {

            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        Objects.requireNonNull(
            userId,
            "userId must not be null");

        /*
         * =========================
         * 1. 获取当前批次
         * =========================
         */
        SelectionBatchInfo batch =
            batchService.findBatch(
                batchId);

        if (batch == null) {

            throw new IllegalArgumentException(
                "selection batch does not exist");
        }

        /*
         * =========================
         * 2. Repository 原始课程
         * =========================
         */
        List<CourseInfo> originalCourses =
            repository.findPlanCourses(
                batchId);

        /*
         * =========================
         * 3. 根据历史修读情况过滤
         * =========================
         */
        List<CourseInfo> eligibleCourses =
            originalCourses.stream()
                .filter(course ->
                    isCourseVisible(
                        batch,
                        userId,
                        course))
                .toList();

        /*
         * =========================
         * 4. 当前已选教学班
         * =========================
         */
        Set<Long> selectedOfferingIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                originalCourses,
                selectedOfferingIds);

        /*
         * =========================
         * 5. 合并动态状态
         * =========================
         */
        return eligibleCourses.stream()
            .map(course ->
                decorateCourse(
                    course,
                    userId,
                    selectedOfferings))
            .toList();
    }

    /**
     * 当前批次中是否应该显示这门课程。
     */
    private boolean isCourseVisible(
        SelectionBatchInfo batch,
        String userId,
        CourseInfo course) {

        boolean previouslyTaken =
            historyRepository
                .hasTakenCourse(
                    userId,
                    course.getCourseId());

        /*
         * 重修：
         *
         * 只有以前修过的课程才显示。
         */
        if (batch.getBatchType()
            == SelectionBatchType.RETAKE) {

            return previouslyTaken;
        }

        /*
         * 普通预选 / 退改补：
         *
         * 已经历史修过的普通课程，
         * 不允许再次按普通选课方式选择。
         */
        return !previouslyTaken;
    }

    /**
     * 找到学生当前已选择的教学班。
     */
    private List<OfferingInfo> findSelectedOfferings(
        List<CourseInfo> courses,
        Set<Long> selectedOfferingIds) {

        List<OfferingInfo> result =
            new ArrayList<>();

        for (CourseInfo course : courses) {

            for (OfferingInfo offering
                : course.getOfferings()) {

                if (selectedOfferingIds.contains(
                    offering.getOfferingId())) {

                    result.add(
                        offering);
                }
            }
        }

        return result;
    }

    /**
     * 合并课程状态。
     */
    private CourseInfo decorateCourse(
        CourseInfo course,
        String userId,
        List<OfferingInfo> selectedOfferings) {

        boolean courseSelected =
            course.getOfferings()
                .stream()
                .anyMatch(offering ->
                    enrollmentRepository
                        .isOfferingSelected(
                            userId,
                            offering.getOfferingId()));

        List<OfferingInfo> offerings =
            course.getOfferings()
                .stream()
                .map(offering ->
                    decorateOffering(
                        offering,
                        userId,
                        courseSelected,
                        selectedOfferings))
                .toList();

        return new CourseInfo(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            courseSelected,
            offerings);
    }

    /**
     * 合并教学班状态。
     */
    private OfferingInfo decorateOffering(
        OfferingInfo offering,
        String userId,
        boolean courseSelected,
        List<OfferingInfo> selectedOfferings) {

        boolean selected =
            enrollmentRepository
                .isOfferingSelected(
                    userId,
                    offering.getOfferingId());

        int selectedCount =
            offering.getSelectedCount()
                + enrollmentRepository
                .countAdditionalSelections(
                    offering.getOfferingId());

        int remainingCount =
            Math.max(
                offering.getCapacity()
                    - selectedCount,
                0);

        String availabilityStatus =
            calculateAvailabilityStatus(
                offering,
                selected,
                courseSelected,
                remainingCount,
                selectedOfferings);

        return new OfferingInfo(
            offering.getOfferingId(),
            offering.getClassNo(),
            offering.getTeacherNames(),
            offering.getSchedules(),
            offering.getLocationName(),
            offering.getCampusName(),
            offering.getTeachingLanguage(),
            selectedCount,
            offering.getCapacity(),
            remainingCount,
            selected,
            availabilityStatus);
    }

    /**
     * 计算教学班最终状态。
     */
    private String calculateAvailabilityStatus(
        OfferingInfo offering,
        boolean selected,
        boolean courseSelected,
        int remainingCount,
        List<OfferingInfo> selectedOfferings) {

        if (selected) {
            return "SELECTED";
        }

        if (courseSelected) {
            return "COURSE_ALREADY_SELECTED";
        }

        if (remainingCount <= 0) {
            return "FULL";
        }

        if (conflictChecker.hasConflict(
            offering,
            selectedOfferings)) {

            return "TIME_CONFLICT";
        }

        return offering.getAvailabilityStatus();
    }
}
