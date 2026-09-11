package edu.seu.vcampus.server.module.course;

import java.util.List;

/**
 * 教师与教学班任课关系仓库。
 */
interface CourseTeacherAssignmentRepository {

    /**
     * 判断教师是否负责指定教学班。
     */
    boolean isAssigned(
        String teacherUserId,
        long offeringId);

    /**
     * 查询教师负责的全部教学班。
     */
    List<Long> findOfferingIds(
        String teacherUserId);

    /**
     * 查询教学班的全部任课教师。
     */
    List<String> findTeacherUserIds(
        long offeringId);

    /**
     * 添加任课关系。
     */
    boolean assign(
        long offeringId,
        String teacherUserId,
        String teacherName);
    /**
     * 删除任课关系。
     */
    boolean remove(
        long offeringId,
        String teacherUserId);
}
