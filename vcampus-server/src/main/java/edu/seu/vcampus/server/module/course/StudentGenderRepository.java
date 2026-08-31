package edu.seu.vcampus.server.module.course;

/**
 * 选课模块读取学生性别的最小接口。
 */
interface StudentGenderRepository {

    StudentGender findGender(
        String studentId);
}

/**
 * 学生性别。
 */
enum StudentGender {

    MALE,

    FEMALE
}
