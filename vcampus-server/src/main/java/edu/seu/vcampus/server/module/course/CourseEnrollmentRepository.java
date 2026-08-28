package edu.seu.vcampus.server.module.course;

/**
 * 学生选课记录数据访问接口。
 *
 * 当前是内存实现，后续由数据库实现替代。
 */
interface CourseEnrollmentRepository {

    /**
     * 判断某学生是否已经选择指定教学班。
     */
    boolean isOfferingSelected(
        String userId,
        long offeringId);

    /**
     * 当前开发运行期间新增到该教学班的人数。
     */
    int countAdditionalSelections(
        long offeringId);

    /**
     * 保存一条选课记录。
     */
    void select(
        String userId,
        long batchId,
        long offeringId);
}
