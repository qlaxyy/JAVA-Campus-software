package edu.seu.vcampus.server.module.course;

import java.util.List;
import java.util.Set;

/**
 * 学生选课记录 Repository。
 */
interface CourseEnrollmentRepository {

    boolean isOfferingSelected(
        String userId,
        long offeringId);

    Set<Long> findSelectedOfferingIds(
        String userId);

    List<CourseEnrollmentRecord>
    findSelectedEnrollments(
        String userId);

    CourseEnrollmentRecord
    findSelectedEnrollment(
        String userId,
        long enrollmentId);

    /**
     * 查询一个教学班在当前运行期间
     * 所有新增选课记录。
     *
     * 体育课男女容量计算需要使用。
     */
    List<CourseEnrollmentRecord>
    findSelectedEnrollmentsByOffering(
        long offeringId);

    int countAdditionalSelections(
        long offeringId);

    /**
     * 选课。
     *
     * userId：
     * 登录系统稳定用户 ID。
     *
     * studentId：
     * 当前学籍模块使用的学生 ID。
     */
    void select(
        String userId,
        String studentId,
        long batchId,
        long offeringId);

    boolean drop(
        String userId,
        long enrollmentId);
}

/**
 * 服务器内部选课记录。
 */
record CourseEnrollmentRecord(
    long enrollmentId,
    String userId,
    String studentId,
    long selectedBatchId,
    long offeringId) {
}
