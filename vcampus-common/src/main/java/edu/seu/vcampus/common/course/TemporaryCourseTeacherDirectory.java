package edu.seu.vcampus.common.course;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 课程模块临时教师名单。
 *
 * 后续可替换为数据库中的教师档案和授课关系。
 */
public final class TemporaryCourseTeacherDirectory {

    /**
     * 使用稳定 userId 绑定教师身份。
     */
    private static final Map<
        String,
        TeacherAssignment> TEACHERS =
        Map.of(
            "U-COURSE-TEACHER-001",
            new TeacherAssignment(
                "U-COURSE-TEACHER-001",
                "演示教师",
                Set.of(
                    1001L)));

    private TemporaryCourseTeacherDirectory() {
    }

    /**
     * 判断用户是否为课程教师。
     */
    public static boolean isTeacher(
        String userId) {

        return findTeacher(
            userId).isPresent();
    }

    /**
     * 查询教师信息。
     */
    public static Optional<TeacherAssignment>
    findTeacher(
        String userId) {

        if (userId == null
            || userId.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
            TEACHERS.get(
                userId.trim()));
    }

    /**
     * 判断教师是否负责指定教学班。
     */
    public static boolean isAssignedToOffering(
        String userId,
        long offeringId) {

        return findTeacher(
            userId)
            .map(teacher ->
                teacher.offeringIds()
                    .contains(
                        offeringId))
            .orElse(false);
    }

    /**
     * 临时教师及授课关系。
     */
    public record TeacherAssignment(
        String userId,
        String teacherName,
        Set<Long> offeringIds) {

        public TeacherAssignment {

            Objects.requireNonNull(
                userId);

            Objects.requireNonNull(
                teacherName);

            Objects.requireNonNull(
                offeringIds);

            offeringIds =
                Set.copyOf(
                    offeringIds);
        }
    }
}
