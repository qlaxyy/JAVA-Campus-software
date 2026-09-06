package edu.seu.vcampus.server.module.course;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import edu.seu.vcampus.common.course.CourseAdminAuditInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.time.LocalDateTime;


/**
 * 教务强制选课和退课操作日志。
 *
 * 当前开发阶段保存在服务器内存中，
 * 后续可以替换为 Access 数据库实现。
 */
final class CourseAdminAuditService {

    private final Clock clock;

    private final List<CourseAdminAuditRecord>
        records =
        new ArrayList<>();

    private long nextOperationId =
        1L;

    CourseAdminAuditService(
        Clock clock) {

        this.clock =
            Objects.requireNonNull(
                clock,
                "clock must not be null");
    }
    /**
     * 记录选课批次修改。
     */
    synchronized void recordUpdateBatch(
        String operatorUsername,
        long batchId,
        String semester,
        String batchName,
        SelectionBatchType batchType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SelectionBatchStatus status,
        boolean allowSelect,
        boolean allowDrop,
        String reason) {

        String details =
            cleanText(
                reason,
                "reason")
                + "；学期="
                + cleanText(
                semester,
                "未填写")
                + "；批次名称="
                + cleanText(
                batchName,
                "未填写")
                + "；批次类型="
                + batchType
                + "；开始时间="
                + startTime
                + "；结束时间="
                + endTime
                + "；状态="
                + status
                + "；允许选课="
                + (allowSelect
                ? "是"
                : "否")
                + "；允许退课="
                + (allowDrop
                ? "是"
                : "否");

        records.add(
            new CourseAdminAuditRecord(
                nextOperationId++,
                cleanText(
                    operatorUsername,
                    "operatorUsername"),
                "-",
                CourseAdminOperationType
                    .UPDATE_BATCH,
                batchId,
                null,
                null,
                details,
                LocalDateTime.now(
                    clock)));
    }
    /**
     * 记录强制选课。
     */
    synchronized void recordForceSelect(
        String operatorUsername,
        String studentId,
        long batchId,
        long offeringId,
        String reason) {

        records.add(
            new CourseAdminAuditRecord(
                nextOperationId++,
                cleanText(
                    operatorUsername,
                    "operatorUsername"),
                cleanText(
                    studentId,
                    "studentId"),
                CourseAdminOperationType
                    .FORCE_SELECT,
                batchId,
                offeringId,
                null,
                cleanText(
                    reason,
                    "reason"),
                LocalDateTime.now(
                    clock)));
    }

    /**
     * 记录强制退课。
     */
    synchronized void recordForceDrop(
        String operatorUsername,
        String studentId,
        long enrollmentId,
        String reason) {

        records.add(
            new CourseAdminAuditRecord(
                nextOperationId++,
                cleanText(
                    operatorUsername,
                    "operatorUsername"),
                cleanText(
                    studentId,
                    "studentId"),
                CourseAdminOperationType
                    .FORCE_DROP,
                null,
                null,
                enrollmentId,
                cleanText(
                    reason,
                    "reason"),
                LocalDateTime.now(
                    clock)));
    }
    /**
     * 记录教学班设置修改。
     */
    synchronized void recordUpdateOffering(
        String operatorUsername,
        long batchId,
        long offeringId,
        int capacity,
        boolean open,
        String reason) {

        String details =
            cleanText(
                reason,
                "reason")
                + "；修改后容量="
                + capacity
                + "；状态="
                + (open
                ? "开放"
                : "关闭");

        records.add(
            new CourseAdminAuditRecord(
                nextOperationId++,
                cleanText(
                    operatorUsername,
                    "operatorUsername"),
                "-",
                CourseAdminOperationType
                    .UPDATE_OFFERING,
                batchId,
                offeringId,
                null,
                details,
                LocalDateTime.now(
                    clock)));
    }
    /**
     * 记录课程基本信息修改。
     */
    synchronized void recordUpdateCourse(
        String operatorUsername,
        long batchId,
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        String reason) {

        String details =
            cleanText(
                reason,
                "reason")
                + "；课程ID="
                + courseId
                + "；课程代码="
                + cleanText(
                courseCode,
                "未填写")
                + "；课程名称="
                + cleanText(
                courseName,
                "未填写")
                + "；学分="
                + credits
                + "；课程类型="
                + cleanText(
                courseType,
                "未填写");

        records.add(
            new CourseAdminAuditRecord(
                nextOperationId++,
                cleanText(
                    operatorUsername,
                    "operatorUsername"),
                "-",
                CourseAdminOperationType
                    .UPDATE_COURSE,
                batchId,
                null,
                null,
                details,
                LocalDateTime.now(
                    clock)));
    }
    /**
     * 查询全部操作记录。
     */
    /**
     * 转换成可以发送给客户端的日志 DTO。
     */
    synchronized List<CourseAdminAuditInfo>
    listAuditLogs() {

        return records.stream()
            .map(record ->
                new CourseAdminAuditInfo(
                    record.operationId(),
                    record.operatorUsername(),
                    record.studentId(),
                    record.operationType().name(),
                    record.batchId(),
                    record.offeringId(),
                    record.enrollmentId(),
                    record.reason(),
                    record.operatedAt()))
            .toList();
    }

    private String cleanText(
        String value,
        String fieldName) {

        Objects.requireNonNull(
            value,
            fieldName + " must not be null");

        String cleaned =
            value.trim();

        if (cleaned.isBlank()) {

            throw new IllegalArgumentException(
                fieldName + " must not be blank");
        }

        return cleaned;
    }
}

/**
 * 教务操作类型。
 */
enum CourseAdminOperationType {

    FORCE_SELECT,
    FORCE_DROP,
    UPDATE_OFFERING,
    UPDATE_COURSE,
    UPDATE_BATCH
}

/**
 * 一条教务操作日志。
 */
record CourseAdminAuditRecord(
    long operationId,
    String operatorUsername,
    String studentId,
    CourseAdminOperationType operationType,
    Long batchId,
    Long offeringId,
    Long enrollmentId,
    String reason,
    LocalDateTime operatedAt) {
}
