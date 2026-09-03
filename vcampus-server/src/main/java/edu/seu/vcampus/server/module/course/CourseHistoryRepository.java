package edu.seu.vcampus.server.module.course;

/**
 * 学生历史修读记录。
 *
 * 这里只关心“以前是否修过某门课程”，
 * 暂时不判断成绩是否及格。
 */
interface CourseHistoryRepository {

    /**
     * 学生历史上是否修读过指定课程。
     */
    boolean hasTakenCourse(
        String userId,
        long courseId);
}
