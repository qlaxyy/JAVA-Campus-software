package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseInfo;

import java.util.List;

/**
 * 全校当前学期课程目录 Repository。
 */
interface CourseCatalogRepository {

    /**
     * 查询当前学期全部开课课程。
     */
    List<CourseCatalogRecord> findCurrentSemesterCourses();
}

/**
 * 全校课程目录内部记录。
 *
 * CourseInfo 保存课程和教学班信息，
 * departmentName 保存开课院系。
 */
record CourseCatalogRecord(
    CourseInfo course,
    String departmentName) {
}
