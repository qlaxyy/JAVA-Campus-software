package edu.seu.vcampus.server.module.course;

import java.util.List;

/** Course-owned relation between a qualified teacher and a teaching offering. */
interface CourseTeacherAssignmentRepository {
    boolean isAssigned(String teacherUserId, long offeringId);

    List<String> findTeacherUserIds(long offeringId);
}
