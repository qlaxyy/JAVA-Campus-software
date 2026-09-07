package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 教务修改后的选课批次设置仓库。
 */
interface CourseBatchSettingsRepository {

    /**
     * 根据批次 ID 查询设置。
     */
    Optional<CourseBatchSettings> find(
        long batchId);

    /**
     * 保存批次设置。
     */
    void save(
        CourseBatchSettings settings);
}

/**
 * 教务修改后的批次信息。
 */
record CourseBatchSettings(
    long batchId,
    String semester,
    String batchName,
    SelectionBatchType batchType,
    LocalDateTime startTime,
    LocalDateTime endTime,
    SelectionBatchStatus status,
    boolean allowSelect,
    boolean allowDrop) {
}

/**
 * 内存批次设置仓库。
 *
 * 服务器重启后设置会清空，
 * 后续可以替换成数据库实现。
 */
final class InMemoryCourseBatchSettingsRepository
    implements CourseBatchSettingsRepository {

    private final ConcurrentMap<Long, CourseBatchSettings>
        settings =
        new ConcurrentHashMap<>();

    @Override
    public Optional<CourseBatchSettings> find(
        long batchId) {

        return Optional.ofNullable(
            settings.get(
                batchId));
    }

    @Override
    public void save(
        CourseBatchSettings batchSettings) {

        settings.put(
            batchSettings.batchId(),
            batchSettings);
    }
}
