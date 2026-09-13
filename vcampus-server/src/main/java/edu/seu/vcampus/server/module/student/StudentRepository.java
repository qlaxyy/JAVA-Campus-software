package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.ApplyStatusChangeRequest;
import edu.seu.vcampus.common.student.StatusChangeDto;
import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for student profiles and status-change applications. */
public interface StudentRepository {

    Optional<StudentProfileDto> findByStudentId(String studentId);

    boolean updateProfile(StudentUpdateProfileRequest request);

    StatusChangeDto createStatusChange(ApplyStatusChangeRequest request);

    List<StatusChangeDto> listStatusChanges(String studentId);

    boolean auditStatusChange(Long changeId, boolean approved, String operator);
}
