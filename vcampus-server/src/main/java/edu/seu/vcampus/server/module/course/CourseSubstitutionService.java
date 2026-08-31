package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 方案外课程业务服务。
 */
final class CourseSubstitutionService {

    private final CourseSubstitutionRepository
        substitutionRepository;

    private final CoursePlanRepository
        planRepository;

    private final CourseEnrollmentRepository
        enrollmentRepository;

    private final CourseScheduleConflictChecker
        conflictChecker =
        new CourseScheduleConflictChecker();

    CourseSubstitutionService(
        CourseSubstitutionRepository substitutionRepository,
        CoursePlanRepository planRepository,
        CourseEnrollmentRepository enrollmentRepository) {

        this.substitutionRepository =
            Objects.requireNonNull(
                substitutionRepository);

        this.planRepository =
            Objects.requireNonNull(
                planRepository);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);
    }

    /**
     * 查询方案外课程。
     */
    List<CourseInfo> listSubstituteCourses(
        long batchId,
        String userId) {

        List<CourseInfo> courses =
            substitutionRepository
                .findSubstituteCourses(
                    batchId);

        List<OfferingInfo> selectedOfferings =
            findSelectedOfferings(
                batchId,
                userId,
                courses);

        return courses.stream()
            .map(course ->
                decorateCourse(
                    course,
                    userId,
                    selectedOfferings))
            .toList();
    }

    /**
     * 找到当前学生已经选择的教学班。
     *
     * 目前同时检查方案内和方案外课程。
     */
    private List<OfferingInfo> findSelectedOfferings(
        long batchId,
        String userId,
        List<CourseInfo> substituteCourses) {

        Set<Long> selectedIds =
            enrollmentRepository
                .findSelectedOfferingIds(
                    userId);

        List<OfferingInfo> result =
            new ArrayList<>();

        List<CourseInfo> allCourses =
            new ArrayList<>();

        allCourses.addAll(
            planRepository.findPlanCourses(
                batchId));

        allCourses.addAll(
            substituteCourses);

        for (CourseInfo course : allCourses) {

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

        String status;

        if (selected) {

            status = "SELECTED";

        } else if (courseSelected) {

            status =
                "COURSE_ALREADY_SELECTED";

        } else if (remainingCount <= 0) {

            status = "FULL";

        } else if (conflictChecker.hasConflict(
            offering,
            selectedOfferings)) {

            status = "TIME_CONFLICT";

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
}
