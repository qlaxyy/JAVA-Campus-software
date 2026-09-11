package edu.seu.vcampus.server.module.course;

import java.util.Map;
import java.util.Set;

/**
 * Temporary course-owned teaching assignments.
 * Teacher qualification itself comes from ServerContext.teachers().
 */
final class TemporaryCourseTeacherAssignmentDirectory {

    private static final Map<String, Set<Long>> OFFERINGS_BY_TEACHER_USER_ID = Map.of(
            "U-COURSE-TEACHER-001", Set.of(1001L));

    private TemporaryCourseTeacherAssignmentDirectory() {
    }

    static boolean isAssignedToOffering(String teacherUserId, long offeringId) {
        if (teacherUserId == null || teacherUserId.isBlank()) {
            return false;
        }
        return OFFERINGS_BY_TEACHER_USER_ID
                .getOrDefault(teacherUserId.trim(), Set.of())
                .contains(offeringId);
    }
}
