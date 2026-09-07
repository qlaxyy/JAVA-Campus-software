package edu.seu.vcampus.server.module.course;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 教务操作日志仓库。
 */
interface CourseAdminAuditRepository {

    /**
     * 保存一条操作日志。
     */
    void append(
        String operatorUsername,
        String studentId,
        CourseAdminOperationType operationType,
        Long batchId,
        Long offeringId,
        Long enrollmentId,
        String details,
        LocalDateTime operatedAt);

    /**
     * 查询全部操作日志。
     */
    List<CourseAdminAuditRecord> findAll();
}

/**
 * 内存教务操作日志仓库。
 */
final class InMemoryCourseAdminAuditRepository
    implements CourseAdminAuditRepository {

    private final List<CourseAdminAuditRecord>
        records =
        new ArrayList<>();

    private long nextOperationId =
        1L;

    @Override
    public synchronized void append(
        String operatorUsername,
        String studentId,
        CourseAdminOperationType operationType,
        Long batchId,
        Long offeringId,
        Long enrollmentId,
        String details,
        LocalDateTime operatedAt) {

        records.add(
            new CourseAdminAuditRecord(
                nextOperationId++,
                operatorUsername,
                studentId,
                operationType,
                batchId,
                offeringId,
                enrollmentId,
                details,
                operatedAt));
    }

    @Override
    public synchronized List<CourseAdminAuditRecord>
    findAll() {

        return List.copyOf(
            records);
    }
}
