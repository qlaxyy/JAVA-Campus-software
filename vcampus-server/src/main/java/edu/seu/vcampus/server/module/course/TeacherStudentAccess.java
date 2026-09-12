package edu.seu.vcampus.server.module.course;

/**
 * 教师查看学生档案的选课范围接口。
 */
@FunctionalInterface
public interface TeacherStudentAccess {

    /**
     * 判断学生是否属于该教师负责的教学班。
     */
    boolean canViewStudent(
        String teacherUserId,
        String studentId);
}
