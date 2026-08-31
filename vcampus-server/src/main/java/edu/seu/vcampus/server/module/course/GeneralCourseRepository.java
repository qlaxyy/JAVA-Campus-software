package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;

import java.util.List;

/**
 * 通选课程 Repository。
 */
interface GeneralCourseRepository {

    /**
     * 查询当前批次的全部通选课程。
     */
    List<GeneralCourseRecord> findGeneralCourses(
        long batchId);
}

/**
 * 通选课程内部记录。
 *
 * CourseInfo：
 * 课程及教学班公共信息。
 *
 * generalCategory：
 * 通选课类别。
 */
record GeneralCourseRecord(
    CourseInfo course,
    String generalCategory) {
}
