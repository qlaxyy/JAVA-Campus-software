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
 * 方案外课程业务服务。
 */
final class CourseSubstitutionService {

    private final CourseBatchService
        batchService;

    private final CourseSubstitutionRepository
        substitutionRepository;

    private final CoursePlanRepository
        planRepository;

    /**
     * 体育课 Repository。
     *
     * 用于跨页面时间冲突。
     */
    private final PeCourseRepository
        peCourseRepository;
    private final GeneralCourseRepository
        generalCourseRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseHistoryRepository
        historyRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    CourseSubstitutionService(
        CourseBatchService batchService,
        CourseSubstitutionRepository substitutionRepository,
        CoursePlanRepository planRepository,
        PeCourseRepository peCourseRepository,
        GeneralCourseRepository generalCourseRepository,
        CourseEnrollmentRepository enrollmentRepository,
        CourseHistoryRepository historyRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.peCourseRepository =
            Objects.requireNonNull(
                peCourseRepository);
        this.generalCourseRepository =
            Objects.requireNonNull(
                generalCourseRepository);
        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);

        this.historyRepository =
            Objects.requireNonNull(
                historyRepository);
    }

    /**
     * 查询方案外课程。
     */
    List<CourseInfo> listSubstituteCourses(
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
         * 1. 当前批次
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
         * 2. 方案外原始课程
         * =========================
         */
        List<CourseInfo> substituteCourses =
            substitutionRepository
                .findSubstituteCourses(
                    batchId);

        /*
         * =========================
         * 3. 根据历史修读资格过滤
         * =========================
         */
        List<CourseInfo> eligibleCourses =
            substituteCourses.stream()
                .filter(course ->
                    isCourseVisible(
                        batch,
                        userId,
                        course))
                .toList();

        /*
         * =========================
         * 4. 组装所有相关课程
         * =========================
         */
        List<CourseInfo> allCourses =
            new ArrayList<>();

        /*
         * 方案内。
         */
        allCourses.addAll(
            planRepository
                .findPlanCourses(
                    batchId));

        /*
         * 方案外。
         */
        allCourses.addAll(
            substituteCourses);

        /*
         * 体育课。
         *
         * 就是在这里加入。
         *
         * 学生已经选体育课以后，
         * 方案外页面也能检测
         * 与体育课之间的时间冲突。
         */
        for (PeCourseRecord record
            : peCourseRepository
            .findPeCourses(
                batchId)) {

            allCourses.add(
                record.course());
        }
        /*
         * 通选课。
         *
         * 已选通选课以后，
         * 方案外课程也能显示时间冲突。
         */
        for (GeneralCourseRecord record
            : generalCourseRepository
            .findGeneralCourses(
                batchId)) {

            allCourses.add(
                record.course());
        }
        /*
         * =========================
         * 5. 当前已选课程
         * =========================
         */
        Set<Long> selectedIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                allCourses,
                selectedIds);

        /*
         * =========================
         * 6. 合并动态状态
         * =========================
         */
        return eligibleCourses.stream()
            .map(course ->
                decorateCourse(
                    course,
                    userId,
                    selectedOfferings,
                    allCourses))
            .toList();
    }

    /**
     * 当前批次是否显示这门方案外课程。
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
         * 必须以前修过。
         */
        if (batch.getBatchType()
            == SelectionBatchType.RETAKE) {

            return previouslyTaken;
        }

        /*
         * 普通批次：
         * 已经修过的不再次普通选择。
         */
        return !previouslyTaken;
    }

    /**
     * 找当前学生所有已选教学班。
     */
    private List<OfferingInfo> findSelectedOfferings(
        List<CourseInfo> courses,
        Set<Long> selectedIds) {

        List<OfferingInfo> result =
            new ArrayList<>();

        for (CourseInfo course
            : courses) {

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
     * 合并方案外课程动态状态。
     */
    private CourseInfo decorateCourse(
        CourseInfo course,
        String userId,
        List<OfferingInfo> selectedOfferings,
        List<CourseInfo> allCourses) {

        /*
         * 当前方案外课程本身
         * 是否已经选择。
         */
        boolean directlySelected =
            course.getOfferings()
                .stream()
                .anyMatch(offering ->
                    enrollmentRepository
                        .isOfferingSelected(
                            userId,
                            offering
                                .getOfferingId()));

        /*
         * 是否已经选择了能够满足
         * 同一培养方案要求的其他课程。
         *
         * 例如已经选了原始高等数学。
         */
        boolean equivalentSelected =
            hasSelectedEquivalentCourse(
                course,
                userId,
                allCourses);

        List<OfferingInfo> offerings =
            course.getOfferings()
                .stream()
                .map(offering ->
                    decorateOffering(
                        offering,
                        userId,
                        directlySelected,
                        equivalentSelected,
                        selectedOfferings))
                .toList();

        /*
         * 方案外页面的外层“已选”
         * 只代表这门方案外课程本身
         * 是否真的被选择。
         *
         * 原课程已选择时，
         * 这里仍然显示未选，
         * 但教学班显示：
         *
         * 培养方案要求已满足。
         */
        return new CourseInfo(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            directlySelected,
            offerings);
    }

    /**
     * 合并教学班状态。
     */
    private OfferingInfo decorateOffering(
        OfferingInfo offering,
        String userId,
        boolean directlySelected,
        boolean equivalentSelected,
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

        String status;

        if (selected) {

            status =
                "SELECTED";

        } else if (directlySelected) {

            status =
                "COURSE_ALREADY_SELECTED";

        } else if (equivalentSelected) {

            status =
                "REQUIREMENT_SATISFIED";

        } else if (remainingCount <= 0) {

            status =
                "FULL";

        } else if (conflictChecker.hasConflict(
            offering,
            selectedOfferings)) {

            status =
                "TIME_CONFLICT";

        } else {

            status =
                offering.getAvailabilityStatus();
        }

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
            status);
    }

    /**
     * 是否已经选择其他满足同一
     * 培养方案要求的课程。
     */
    private boolean hasSelectedEquivalentCourse(
        CourseInfo targetCourse,
        String userId,
        List<CourseInfo> allCourses) {

        long targetRequirementId =
            requirementCourseId(
                targetCourse
                    .getCourseId());

        for (CourseInfo candidate
            : allCourses) {

            if (candidate.getCourseId()
                == targetCourse.getCourseId()) {

                continue;
            }

            if (requirementCourseId(
                candidate.getCourseId())
                != targetRequirementId) {

                continue;
            }

            for (OfferingInfo offering
                : candidate.getOfferings()) {

                if (enrollmentRepository
                    .isOfferingSelected(
                        userId,
                        offering.getOfferingId())) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取最终对应的培养方案课程 ID。
     */
    private long requirementCourseId(
        long courseId) {

        Long replacedCourseId =
            substitutionRepository
                .findReplacedCourseId(
                    courseId);

        return replacedCourseId == null
            ? courseId
            : replacedCourseId;
    }
}
