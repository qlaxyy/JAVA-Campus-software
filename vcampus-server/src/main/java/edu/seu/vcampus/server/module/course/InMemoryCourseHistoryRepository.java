package edu.seu.vcampus.server.module.course;

import java.util.Set;

/**
 * 开发阶段使用的历史修读数据。
 */
final class InMemoryCourseHistoryRepository
    implements CourseHistoryRepository {

    /**
     * 当前测试数据：
     *
     * 101 = 高等数学
     * 201 = 数学分析基础（方案外课程）
     *
     * 当前测试学生历史上修过这两门课程。
     */
    private static final Set<Long>
        DEFAULT_TAKEN_COURSE_IDS =
        Set.of(
            101L,
            201L
        );

    @Override
    public boolean hasTakenCourse(
        String userId,
        long courseId) {

        return DEFAULT_TAKEN_COURSE_IDS.contains(
            courseId);
    }
}
