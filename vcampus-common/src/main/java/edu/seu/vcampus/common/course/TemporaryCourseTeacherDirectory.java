package edu.seu.vcampus.common.course;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Frozen compatibility facade for course branches created before the shared teacher table.
 * New server code must query {@code ServerContext.teachers()} and the persistent
 * course assignment repository; this class is not the authoritative directory.
 */
@Deprecated(forRemoval = true)
public final class TemporaryCourseTeacherDirectory {

    private static final Map<String, TeacherAssignment> TEACHERS = Map.of(
            "20260021",
            new TeacherAssignment("20260021", "王建国", Set.of(1001L)));

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
