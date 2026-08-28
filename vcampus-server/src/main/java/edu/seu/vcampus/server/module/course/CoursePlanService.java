package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 方案内课程业务服务。
 */
final class CoursePlanService {

    private final CoursePlanRepository repository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    CoursePlanService(
        CoursePlanRepository repository,
        CourseEnrollmentRepository enrollmentRepository) {

        this.repository =
            Objects.requireNonNull(
                repository,
                "repository must not be null");

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository,
                "enrollmentRepository must not be null");
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
         * Repository 中的原始课程数据。
         */
        List<CourseInfo> courses =
            repository.findPlanCourses(
                batchId);

        /*
         * 当前学生已经选择的教学班。
         */
        Set<Long> selectedOfferingIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                courses,
                selectedOfferingIds);

        /*
         * 将选课状态、人数、时间冲突状态
         * 合并到返回 DTO 中。
         */
        return courses.stream()
            .map(course ->
                decorateCourse(
                    course,
                    userId,
                    selectedOfferings))
            .toList();
    }

    /**
     * 找到当前学生已经选择的教学班完整信息。
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

                    result.add(offering);
                }
            }
        }

        return result;
    }

    /**
     * 合并课程级状态。
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
                            offering
                                .getOfferingId()));

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

        /*
         * 当前学生是否选择了这个教学班。
         */
        boolean selected =
            enrollmentRepository
                .isOfferingSelected(
                    userId,
                    offering.getOfferingId());

        /*
         * 动态人数。
         */
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

        /*
         * 当前教学班最终显示状态。
         */
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
     * 计算教学班可选状态。
     */
    private String calculateAvailabilityStatus(
        OfferingInfo offering,
        boolean selected,
        boolean courseSelected,
        int remainingCount,
        List<OfferingInfo> selectedOfferings) {

        /*
         * 自己已经选择。
         */
        if (selected) {
            return "SELECTED";
        }

        /*
         * 同一课程已经选择其他教学班。
         */
        if (courseSelected) {
            return "COURSE_ALREADY_SELECTED";
        }

        /*
         * 已满。
         */
        if (remainingCount <= 0) {
            return "FULL";
        }

        /*
         * 与已选教学班存在时间冲突。
         *
         * 现在 LIST_PLAN_COURSES 阶段
         * 就会提前算出来。
         */
        if (conflictChecker.hasConflict(
            offering,
            selectedOfferings)) {

            return "TIME_CONFLICT";
        }

        /*
         * 保留 Repository 原始状态。
         */
        return offering.getAvailabilityStatus();
    }
}
