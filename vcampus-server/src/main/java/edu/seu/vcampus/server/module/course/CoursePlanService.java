package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.List;
import java.util.Objects;

/**
 * 方案内课程业务服务。
 */
final class CoursePlanService {

    private final CoursePlanRepository repository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

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
     * 查询某学生的方案内课程。
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

        return repository
            .findPlanCourses(batchId)
            .stream()
            .map(course ->
                decorateCourse(
                    course,
                    userId))
            .toList();
    }

    /**
     * 将学生自己的选课状态合并到课程 DTO。
     */
    private CourseInfo decorateCourse(
        CourseInfo course,
        String userId) {

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
                        courseSelected))
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
     * 合并教学班人数和当前学生选择状态。
     */
    private OfferingInfo decorateOffering(
        OfferingInfo offering,
        String userId,
        boolean courseSelected) {

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
                remainingCount);

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

    private String calculateAvailabilityStatus(
        OfferingInfo offering,
        boolean selected,
        boolean courseSelected,
        int remainingCount) {

        if (selected) {
            return "SELECTED";
        }

        /*
         * 同一门课已经选择其他教学班。
         */
        if (courseSelected) {
            return "COURSE_ALREADY_SELECTED";
        }

        if (remainingCount <= 0) {
            return "FULL";
        }

        return offering.getAvailabilityStatus();
    }
}
