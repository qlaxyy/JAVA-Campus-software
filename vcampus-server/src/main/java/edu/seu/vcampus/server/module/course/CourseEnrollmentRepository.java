package edu.seu.vcampus.server.module.course;

import java.util.List;
import java.util.Set;

/**
 * 学生选课记录 Repository。
 */
interface CourseEnrollmentRepository {

    /**
     * 是否已经选择某教学班。
     */
    boolean isOfferingSelected(
        String userId,
        long offeringId);

    /**
     * 当前学生所有已选教学班 ID。
     */
    Set<Long> findSelectedOfferingIds(
        String userId);

    /**
     * 当前学生所有有效选课记录。
     */
    List<CourseEnrollmentRecord> findSelectedEnrollments(
        String userId);

    /**
     * 根据 enrollmentId 查询学生自己的选课记录。
     */
    CourseEnrollmentRecord findSelectedEnrollment(
        String userId,
        long enrollmentId);

    /**
     * 当前运行期间新增选择人数。
     */
    int countAdditionalSelections(
        long offeringId);

    /**
     * 选课。
     */
    void select(
        String userId,
        long batchId,
        long offeringId);

    /**
     * 退课。
     */
    boolean drop(
        String userId,
        long enrollmentId);
}

/**
 * 服务器内部使用的选课记录。
 */
record CourseEnrollmentRecord(
    long enrollmentId,
    String userId,
    long selectedBatchId,
    long offeringId) {
}
