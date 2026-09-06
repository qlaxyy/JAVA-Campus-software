package edu.seu.vcampus.server.module.course;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 教务修改后的课程基本信息仓库。
 */
interface CourseSettingsRepository {

    /**
     * 查询课程设置。
     *
     * batchId 暂时保留在接口中，
     * 但课程基本信息实际按 courseId 全局存储。
     */
    Optional<CourseSettings> find(
        long batchId,
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
    long batchId,
    long courseId,
    String courseCode,
    String courseName,
    double credits,
    String courseType) {
}

/**
 * 内存课程设置仓库。
 *
 * 课程基本信息按 courseId 存储，
 * 不再被选课批次隔离。
 */
final class InMemoryCourseSettingsRepository
    implements CourseSettingsRepository {

    private final ConcurrentMap<Long, CourseSettings>
        settings =
        new ConcurrentHashMap<>();

    @Override
    public Optional<CourseSettings> find(
        long batchId,
        long courseId) {

        return Optional.ofNullable(
            settings.get(
                courseId));
    }

    @Override
    public void save(
        CourseSettings courseSettings) {

        settings.put(
            courseSettings.courseId(),
            courseSettings);
    }
}
