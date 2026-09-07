package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseAdminAuditInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 教务操作日志业务服务。
 */
final class CourseAdminAuditService {

    private final Clock clock;

    private final CourseAdminAuditRepository
        repository;

    /**
     * 默认使用内存日志仓库。
     */
    CourseAdminAuditService(
        Clock clock) {

        this(
            clock,
            new InMemoryCourseAdminAuditRepository());
    }

    /**
     * 使用指定日志仓库。
     */
    CourseAdminAuditService(
        Clock clock,
        CourseAdminAuditRepository repository) {

        this.clock =
            Objects.requireNonNull(
                clock,
                "clock must not be null");

        this.repository =
            Objects.requireNonNull(
                repository,
                "repository must not be null");
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
            optionalText(
                reason)
                + "；学期="
                + requiredText(
                semester,
                "semester")
                + "；批次名称="
                + requiredText(
                batchName,
                "batchName")
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

        append(
            operatorUsername,
            "-",
            CourseAdminOperationType.UPDATE_BATCH,
            batchId,
            null,
            null,
            details);
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

        append(
            operatorUsername,
            studentId,
            CourseAdminOperationType.FORCE_SELECT,
            batchId,
            offeringId,
            null,
            optionalText(
                reason));
    }

    /**
     * 记录成绩录入或修改。
     */
    synchronized void recordUpdateGrade(
        String operatorUsername,
        String studentId,
        long enrollmentId,
        double score,
        String reason) {

        String details =
            optionalText(
                reason)
                + "；修改后成绩="
                + score;

        append(
            operatorUsername,
            studentId,
            CourseAdminOperationType.UPDATE_GRADE,
            null,
            null,
            enrollmentId,
            details);
    }
    /**
     * 记录强制退课。
     */
    synchronized void recordForceDrop(
        String operatorUsername,
        String studentId,
        long enrollmentId,
        String reason) {

        append(
            operatorUsername,
            studentId,
            CourseAdminOperationType.FORCE_DROP,
            null,
            null,
            enrollmentId,
            optionalText(
                reason));
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
            optionalText(
                reason)
                + "；修改后容量="
                + capacity
                + "；状态="
                + (open
                ? "开放"
                : "关闭");

        append(
            operatorUsername,
            "-",
            CourseAdminOperationType.UPDATE_OFFERING,
            batchId,
            offeringId,
            null,
            details);
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
            optionalText(
                reason)
                + "；课程ID="
                + courseId
                + "；课程代码="
                + requiredText(
                courseCode,
                "courseCode")
                + "；课程名称="
                + requiredText(
                courseName,
                "courseName")
                + "；学分="
                + credits
                + "；课程类型="
                + requiredText(
                courseType,
                "courseType");

        append(
            operatorUsername,
            "-",
            CourseAdminOperationType.UPDATE_COURSE,
            batchId,
            null,
            null,
            details);
    }

    /**
     * 保存一条日志。
     */
    private void append(
        String operatorUsername,
        String studentId,
        CourseAdminOperationType operationType,
        Long batchId,
        Long offeringId,
        Long enrollmentId,
        String details) {

        repository.append(
            requiredText(
                operatorUsername,
                "operatorUsername"),
            requiredText(
                studentId,
                "studentId"),
            Objects.requireNonNull(
                operationType),
            batchId,
            offeringId,
            enrollmentId,
            requiredText(
                details,
                "details"),
            LocalDateTime.now(
                clock));
    }

    /**
     * 查询全部操作日志。
     */
    synchronized List<CourseAdminAuditInfo>
    listAuditLogs() {

        return repository.findAll()
            .stream()
            .map(record ->
                new CourseAdminAuditInfo(
                    record.operationId(),
                    record.operatorUsername(),
                    record.studentId(),
                    record.operationType()
                        .name(),
                    record.batchId(),
                    record.offeringId(),
                    record.enrollmentId(),
                    record.reason(),
                    record.operatedAt()))
            .toList();
    }

    /**
     * 必填文本处理。
     */
    private String requiredText(
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

    /**
     * 选填原因处理。
     */
    private String optionalText(
        String value) {

        if (value == null
            || value.isBlank()) {

            return "未填写原因";
        }

        return value.trim();
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
    UPDATE_BATCH,
    UPDATE_GRADE
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
