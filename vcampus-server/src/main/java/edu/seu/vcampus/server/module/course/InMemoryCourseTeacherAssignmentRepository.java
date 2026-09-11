package edu.seu.vcampus.server.module.course;

import java.util.Map;
import java.util.List;
import java.util.Set;

/** Deterministic assignment relation for isolated tests. */
final class InMemoryCourseTeacherAssignmentRepository implements CourseTeacherAssignmentRepository {
    private static final Map<String, Set<Long>> ASSIGNMENTS = Map.of(
            "U-TEACHER-001", Set.of(1001L),
            "U-TEACHER-002", Set.of(2001L),
            "U-TEACHER-003", Set.of(3001L),
            "U-TEACHER-004", Set.of(9001L),
            "U-TEACHER-005", Set.of(14001L),
            "U-TEACHER-006", Set.of(15002L),
            "U-TEACHER-007", Set.of(14003L),
            "U-TEACHER-008", Set.of(15001L));

    public boolean isAssigned(String teacherUserId, long offeringId) {
        return teacherUserId != null
                && ASSIGNMENTS.getOrDefault(teacherUserId.trim(), Set.of()).contains(offeringId);
    }

    @Override
    public List<String> findTeacherUserIds(long offeringId) {
        return ASSIGNMENTS.entrySet().stream()
                .filter(entry -> entry.getValue().contains(offeringId))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    static Map<String, Set<Long>> seeds() { return ASSIGNMENTS; }
}
