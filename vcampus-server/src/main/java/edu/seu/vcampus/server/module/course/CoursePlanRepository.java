package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;

import java.util.List;

/**
 * 方案内课程数据访问接口。
 */
interface CoursePlanRepository {

    /**
     * 查询指定选课批次中的方案内课程。
     */
    List<CourseInfo> findPlanCourses(long batchId);
}
