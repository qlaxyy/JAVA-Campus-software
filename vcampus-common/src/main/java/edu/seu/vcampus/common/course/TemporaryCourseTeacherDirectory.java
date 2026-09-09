package edu.seu.vcampus.common.course;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 课程模块临时教师名单。
 *
 * 当前用户模块还没有正式教师角色，
 * 因此客户端和服务端暂时通过该名单识别教师。
 *
 * 正式教师角色和教师任课关系完成后，
 * 删除这个临时类。
 */
public final class TemporaryCourseTeacherDirectory {

    /**
     * 临时教师名单。
     *
     * 20260002：
     * 现有演示教师账号。
     *
     * 1001：
     * 当前分配给该教师的演示教学班。
     */
    private static final Map<String, TeacherAssignment>
        TEACHERS =
        Map.of(
            "20260002",
            new TeacherAssignment(
                "20260002",
                "张老师",
                Set.of(
                    1001L)));

    private TemporaryCourseTeacherDirectory() {
    }

    /**
     * 判断账号是否属于临时教师。
     */
    public static boolean isTeacher(
        String username) {

        if (username == null
            || username.isBlank()) {

            return false;
        }

        return TEACHERS.containsKey(
            username.trim());
    }

    /**
     * 查询教师信息及任课关系。
     */
    public static Optional<TeacherAssignment>
    findTeacher(
        String username) {

        if (username == null
            || username.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
            TEACHERS.get(
                username.trim()));
    }

    /**
     * 判断教师是否负责指定教学班。
     */
    public static boolean isAssignedToOffering(
        String username,
        long offeringId) {

        return findTeacher(
            username)
            .map(teacher ->
                teacher.offeringIds()
                    .contains(
                        offeringId))
            .orElse(
                false);
    }

    /**
     * 临时教师及其任课信息。
     */
    public record TeacherAssignment(
        String username,
        String teacherName,
        Set<Long> offeringIds) {

        public TeacherAssignment {

            username =
                Objects.requireNonNull(
                    username,
                    "username must not be null");

            teacherName =
                Objects.requireNonNull(
                    teacherName,
                    "teacherName must not be null");

            offeringIds =
                Set.copyOf(
                    Objects.requireNonNull(
                        offeringIds,
                        "offeringIds must not be null"));
        }
    }
}
