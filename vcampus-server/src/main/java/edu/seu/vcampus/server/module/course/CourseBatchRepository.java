package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课批次数据访问边界。
 *
 * 后续可以将内存实现替换为 Access/JDBC 实现。
 */
interface CourseBatchRepository {

    /**
     * 查询当前学期的选课批次。
     */
    List<CourseBatchRecord> findCurrentSemesterBatches();
}

/**
 * 服务端内部使用的原始选课批次数据。
 */
record CourseBatchRecord(
    long batchId,
    String semester,
    String batchName,
    SelectionBatchType batchType,
    LocalDateTime startTime,
    LocalDateTime endTime,
    boolean allowSelect,
    boolean allowDrop,
    boolean enabled) {
}
