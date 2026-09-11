package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.CourseSearchItem;
import edu.seu.vcampus.common.course.CourseSearchResult;
import edu.seu.vcampus.common.course.EnrollmentInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.TeacherStudentInfo;
import edu.seu.vcampus.server.security.TeacherDirectory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 教师端课程业务。
 */
final class CourseTeacherService {

    private final CourseOfferingAdministrationService
        offeringAdministrationService;

    private final CourseEnrollmentRepository
        enrollmentRepository;
    private final CourseTeacherAssignmentRepository assignmentRepository;
    private final TeacherDirectory teacherDirectory;

    CourseTeacherService(
        CourseOfferingAdministrationService
            offeringAdministrationService,
        CourseEnrollmentRepository
            enrollmentRepository,
        CourseTeacherAssignmentRepository assignmentRepository,
        TeacherDirectory teacherDirectory) {

        this.offeringAdministrationService =
            Objects.requireNonNull(
                offeringAdministrationService);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);
        this.assignmentRepository = Objects.requireNonNull(assignmentRepository);
        this.teacherDirectory = teacherDirectory;
    }

    /**
     * 判断教师是否负责指定教学班。
     */
    boolean canManageOffering(
        String userId,
        long offeringId) {

        return assignmentRepository.isAssigned(userId, offeringId);
    }

    /**
     * 查询教师在指定批次负责的课程。
     */
    List<CourseInfo> listTeacherCourses(
        String userId,
        long batchId) {

        List<CourseInfo> result =
            new ArrayList<>();

        for (CourseInfo course : withCurrentTeacherNames(
                offeringAdministrationService.listCourses(batchId))) {

            List<OfferingInfo>
                assignedOfferings =
                course.getOfferings()
                    .stream()
                    .filter(offering ->
                        canManageOffering(
                            userId,
                            offering
                                .getOfferingId()))
                    .toList();

            if (assignedOfferings.isEmpty()) {

                continue;
            }

            result.add(
                new CourseInfo(
                    course.getCourseId(),
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredits(),
                    course.getCourseType(),
                    course.isSelected(),
                    assignedOfferings));
        }

        return List.copyOf(
            result);
    }

    /** Replaces stored display snapshots with names from the public teacher directory. */
    List<CourseInfo> withCurrentTeacherNames(List<CourseInfo> courses) {
        return courses.stream().map(course -> new CourseInfo(
                course.getCourseId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getCredits(),
                course.getCourseType(),
                course.isSelected(),
                course.getOfferings().stream().map(this::withCurrentTeacherNames).toList()))
                .toList();
    }

    CourseSearchResult withCurrentSearchTeacherNames(CourseSearchResult result) {
        List<CourseSearchItem> items = result.getItems().stream().map(item -> new CourseSearchItem(
                item.getCourseId(), item.getCourseCode(), item.getCourseName(),
                item.getCredits(), item.getCourseType(), item.getDepartmentName(),
                item.getOfferingId(), item.getClassNo(), currentTeacherNames(
                        item.getOfferingId(), item.getTeacherNames()),
                item.getSchedules(), item.getLocationName(), item.getCampusName(),
                item.getTeachingLanguage(), item.getSelectedCount(), item.getCapacity(),
                item.getRemainingCount())).toList();
        return new CourseSearchResult(items, result.getPage(), result.getPageSize(),
                result.getTotalCount(), result.getTotalPages());
    }

    List<EnrollmentInfo> withCurrentEnrollmentTeacherNames(List<EnrollmentInfo> items) {
        return items.stream().map(item -> new EnrollmentInfo(
                item.getEnrollmentId(), item.getOfferingId(), item.getCourseCode(),
                item.getCourseName(), item.getClassNo(), currentTeacherNames(
                        item.getOfferingId(), item.getTeacherNames()),
                item.getSchedules(), item.getLocationName(), item.getCredits(),
                item.getCourseType(), item.isCanDrop(), item.getDropUnavailableReason()))
                .toList();
    }

    private OfferingInfo withCurrentTeacherNames(OfferingInfo offering) {
        return new OfferingInfo(
                offering.getOfferingId(), offering.getClassNo(),
                currentTeacherNames(offering.getOfferingId(), offering.getTeacherNames()),
                offering.getSchedules(), offering.getLocationName(), offering.getCampusName(),
                offering.getTeachingLanguage(), offering.getSelectedCount(),
                offering.getCapacity(), offering.getRemainingCount(), offering.isSelected(),
                offering.getAvailabilityStatus());
    }

    private List<String> currentTeacherNames(long offeringId, List<String> fallback) {
        if (teacherDirectory == null) return fallback;
        List<String> names = assignmentRepository.findTeacherUserIds(offeringId).stream()
                .map(teacherDirectory::findByUserId)
                .flatMap(java.util.Optional::stream)
                .map(identity -> identity.displayName())
                .toList();
        return names.isEmpty() ? fallback : names;
    }

    /**
     * 查询教师负责教学班中的学生名单。
     */
    List<TeacherStudentInfo> listStudents(
        String userId,
        long batchId,
        long offeringId) {

        if (!canManageOffering(
            userId,
            offeringId)) {

            return List.of();
        }

        return enrollmentRepository
            .findSelectedEnrollmentsByOffering(
                offeringId)
            .stream()
            .filter(record ->
                record.selectedBatchId()
                    == batchId)
            .map(record ->
                new TeacherStudentInfo(
                    record.enrollmentId(),
                    record.studentId(),
                    record.selectedBatchId(),
                    record.offeringId()))
            .sorted(
                Comparator.comparing(
                    TeacherStudentInfo
                        ::getStudentId))
            .toList();
    }
    /**
     * 判断选课记录是否属于该教师负责的教学班。
     */
    boolean canManageEnrollment(
        String userId,
        String studentId,
        long enrollmentId) {

        if (studentId == null
            || studentId.isBlank()
            || enrollmentId <= 0) {

            return false;
        }

        CourseEnrollmentRecord record =
            enrollmentRepository
                .findSelectedEnrollment(
                    enrollmentId);

        if (record == null) {

            return false;
        }

        if (!record.studentId()
            .equals(
                studentId.trim())) {

            return false;
        }

        return canManageOffering(
            userId,
            record.offeringId());
    }

}
