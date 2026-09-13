package edu.seu.vcampus.server.module.course;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 教务修改后的课程基本信息仓库。
 *
 * 课程设置按照 courseId 全局保存，
 * 与选课批次无关。
 */
interface CourseSettingsRepository {

    /**
     * 查询课程设置。
     */
    Optional<CourseSettings> find(
        long courseId);

    /**
     * 保存课程设置。
     */
    void save(
        CourseSettings settings);
}

/**
 * 修改后的课程基本信息。
 */
record CourseSettings(
    long courseId,
    String courseCode,
    String courseName,
    double credits,
    String courseType) {
}

/**
 * 内存课程设置仓库。
 */
final class InMemoryCourseSettingsRepository
    implements CourseSettingsRepository {

    private final ConcurrentMap<Long, CourseSettings>
        settings =
        new ConcurrentHashMap<>();

    @Override
    public Optional<CourseSettings> find(
        long courseId) {

        return Optional.ofNullable(
            settings.get(
                courseId));
    }

    @Override
    public void save(
        CourseSettings courseSettings) {

        if (courseSettings == null) {

            throw new IllegalArgumentException(
                "settings must not be null");
        }

        settings.put(
            courseSettings.courseId(),
            courseSettings);
    }
}
