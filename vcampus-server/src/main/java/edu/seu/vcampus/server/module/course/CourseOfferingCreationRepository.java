package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.ScheduleInfo;

/**
 * 新建教学班的持久化入口。
 */
interface CourseOfferingCreationRepository {

    long create(
        long courseId,
        String classNo,
        String locationName,
        String campusName,
        String teachingLanguage,
        int capacity,
        ScheduleInfo schedule);
}
