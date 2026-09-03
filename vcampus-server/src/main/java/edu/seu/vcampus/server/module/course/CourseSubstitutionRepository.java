package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;

import java.util.List;

/**
 * 方案外替代课程 Repository。
 */
interface CourseSubstitutionRepository {

    /**
     * 查询当前批次可供学生选择的方案外课程。
     */
    List<CourseInfo> findSubstituteCourses(
        long batchId);

    /**
     * 查询方案外课程所替代的方案内课程 ID。
     *
     * 当前版本一门方案外课程只对应
     * 一门方案内课程。
     */
    Long findReplacedCourseId(
        long substituteCourseId);
}
