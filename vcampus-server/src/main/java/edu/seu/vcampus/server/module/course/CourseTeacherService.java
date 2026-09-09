package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.TeacherStudentInfo;

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

    CourseTeacherService(
        CourseOfferingAdministrationService
            offeringAdministrationService,
        CourseEnrollmentRepository
            enrollmentRepository) {

        this.offeringAdministrationService =
            Objects.requireNonNull(
                offeringAdministrationService);

        this.enrollmentRepository =
            Objects.requireNonNull(
                enrollmentRepository);
    }

    /**
     * 判断教师是否负责指定教学班。
     */
    boolean canManageOffering(
        String userId,
        long offeringId) {

        return TemporaryCourseTeacherAssignmentDirectory
            .isAssignedToOffering(
                userId,
                offeringId);
    }

    /**
     * 查询教师在指定批次负责的课程。
     */
    List<CourseInfo> listTeacherCourses(
        String userId,
        long batchId) {

        List<CourseInfo> result =
            new ArrayList<>();

        for (CourseInfo course
            : offeringAdministrationService
            .listCourses(
                batchId)) {

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
