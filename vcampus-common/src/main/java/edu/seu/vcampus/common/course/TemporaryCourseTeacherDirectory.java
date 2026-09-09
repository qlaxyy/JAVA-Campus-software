package edu.seu.vcampus.common.course;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compatibility facade for course branches created before the shared teacher table.
 * New code must query USER.CURRENT_TEACHER_PROFILE or ServerContext.teachers().
 */
@Deprecated(forRemoval = true)
public final class TemporaryCourseTeacherDirectory {

    private static final Map<String, TeacherAssignment> TEACHERS = Map.of(
            "20260008",
            new TeacherAssignment("20260008", "演示教师", Set.of(1001L)));

    private TemporaryCourseTeacherDirectory() {
    }

    public static boolean isTeacher(String username) {
        return username != null && TEACHERS.containsKey(username.trim());
    }

    public static Optional<TeacherAssignment> findTeacher(String username) {
        return username == null
                ? Optional.empty()
                : Optional.ofNullable(TEACHERS.get(username.trim()));
    }

    public static boolean isAssignedToOffering(String username, long offeringId) {
        return findTeacher(username)
                .map(teacher -> teacher.offeringIds().contains(offeringId))
                .orElse(false);
    }

    public record TeacherAssignment(
            String username,
            String teacherName,
            Set<Long> offeringIds) {
        public TeacherAssignment {
            Objects.requireNonNull(username, "username must not be null");
            Objects.requireNonNull(teacherName, "teacherName must not be null");
            offeringIds = Set.copyOf(Objects.requireNonNull(
                    offeringIds, "offeringIds must not be null"));
        }
    }
}
