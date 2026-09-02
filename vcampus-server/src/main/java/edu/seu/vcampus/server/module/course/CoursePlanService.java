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

    private final CourseSubstitutionRepository
        substitutionRepository;

    /**
     * 体育课 Repository。
     *
     * 用于跨页面时间冲突检测。
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

    CoursePlanService(
        CourseBatchService batchService,
        CoursePlanRepository repository,
        GeneralCourseRepository generalCourseRepository,
        CourseSubstitutionRepository substitutionRepository,
        PeCourseRepository peCourseRepository,
        CourseEnrollmentRepository enrollmentRepository,
        CourseHistoryRepository historyRepository) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.repository =
            Objects.requireNonNull(
                repository);

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
         * 1. 当前选课批次
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
         * 2. 方案内原始课程
         * =========================
         */
        List<CourseInfo> planCourses =
            repository.findPlanCourses(
                batchId);

        /*
         * =========================
         * 3. 方案外课程
         * =========================
         *
         * 用于：
         *
         * - 替代关系状态
         * - 时间冲突
         */
        List<CourseInfo> substituteCourses =
            substitutionRepository
                .findSubstituteCourses(
                    batchId);

        /*
         * =========================
         * 4. 当前所有相关课程
         * =========================
         *
         * 注意：
         *
         * 就是这里加入体育课。
         */
        List<CourseInfo> allCourses =
            new ArrayList<>();

        /*
         * 方案内。
         */
        allCourses.addAll(
            planCourses);

        /*
         * 方案外。
         */
        allCourses.addAll(
            substituteCourses);

        /*
         * 体育课。
         *
         * 学生如果已经选择体育课，
         * 回到方案内课程时，
         * 方案内课程也能检查
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
         * 这样已经选择通选课以后，
         * 方案内课程也能提前显示时间冲突。
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
         * 5. 根据历史修读情况过滤方案内
         * =========================
         */
        List<CourseInfo> eligibleCourses =
            planCourses.stream()
                .filter(course ->
                    isCourseVisible(
                        batch,
                        userId,
                        course))
                .toList();

        /*
         * =========================
         * 6. 当前已选教学班
         * =========================
         */
        Set<Long> selectedOfferingIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                allCourses,
                selectedOfferingIds);

        /*
         * =========================
         * 7. 合并动态状态
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
     * 是否应该在当前批次显示课程。
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
         * 重修批次：
         * 以前修过才显示。
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
     * 找学生已经选择的所有教学班。
     */
    private List<OfferingInfo> findSelectedOfferings(
        List<CourseInfo> courses,
        Set<Long> selectedOfferingIds) {

        List<OfferingInfo> result =
            new ArrayList<>();

        for (CourseInfo course
            : courses) {

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
     * 合并方案内课程动态状态。
     */
    private CourseInfo decorateCourse(
        CourseInfo course,
        String userId,
        List<OfferingInfo> selectedOfferings,
        List<CourseInfo> allCourses) {

        /*
         * 当前课程自己是否已经选了。
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
         * 是否已经通过一个方案外替代课程
         * 满足了同一个培养方案要求。
         */
        boolean equivalentSelected =
            hasSelectedEquivalentCourse(
                course,
                userId,
                allCourses);

        /*
         * 对于方案内课程：
         *
         * 自己被选择
         * 或
         * 已被方案外课程替代
         *
         * 都认为这个培养方案要求已满足。
         */
        boolean requirementSatisfied =
            directlySelected
                || equivalentSelected;

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

        return new CourseInfo(
            course.getCourseId(),
            course.getCourseCode(),
            course.getCourseName(),
            course.getCredits(),
            course.getCourseType(),
            requirementSatisfied,
            offerings);
    }

    /**
     * 合并教学班动态状态。
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

        /*
         * 当前教学班自己已选。
         */
        if (selected) {

            status =
                "SELECTED";

            /*
             * 同一门课程其他教学班已选。
             */
        } else if (directlySelected) {

            status =
                "COURSE_ALREADY_SELECTED";

            /*
             * 已由方案外替代课程满足。
             */
        } else if (equivalentSelected) {

            status =
                "REQUIREMENT_SATISFIED";

            /*
             * 人数已满。
             */
        } else if (remainingCount <= 0) {

            status =
                "FULL";

            /*
             * 与当前任意已选课程冲突。
             *
             * 这里现在也能检测体育课。
             */
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
     * 是否已经选择能够满足相同
     * 培养方案要求的其他课程。
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

            /*
             * 自己不算替代课程。
             */
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
     * 一门课程最终对应哪个
     * 培养方案要求。
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
