package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 选课批次业务服务。
 */
final class CourseBatchService {

    private final CourseBatchRepository
        repository;

    private final Clock clock;

    private final CourseBatchSettingsRepository
        settingsRepository =
        new InMemoryCourseBatchSettingsRepository();

    CourseBatchService(
        CourseBatchRepository repository,
        Clock clock) {

        this.repository =
            Objects.requireNonNull(
                repository,
                "repository must not be null");

        this.clock =
            Objects.requireNonNull(
                clock,
                "clock must not be null");
    }

    /**
     * 返回当前学期全部启用的选课批次。
     */
    List<SelectionBatchInfo> listBatches() {

        LocalDateTime now =
            LocalDateTime.now(
                clock);

        return repository
            .findCurrentSemesterBatches()
            .stream()
            .filter(
                CourseBatchRecord::enabled)
            .map(batch ->
                toInfo(
                    batch,
                    now))
            .toList();
    }

    /**
     * 根据 ID 查找当前学期批次。
     */
    SelectionBatchInfo findBatch(
        long batchId) {

        return listBatches()
            .stream()
            .filter(batch ->
                batch.getBatchId()
                    == batchId)
            .findFirst()
            .orElse(null);
    }

    /**
     * 教务修改选课批次。
     */
    synchronized BatchUpdateResult updateBatch(
        long batchId,
        String semester,
        String batchName,
        SelectionBatchType batchType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SelectionBatchStatus status,
        boolean allowSelect,
        boolean allowDrop) {

        CourseBatchRecord original =
            repository
                .findCurrentSemesterBatches()
                .stream()
                .filter(batch ->
                    batch.batchId()
                        == batchId)
                .findFirst()
                .orElse(null);

        if (original == null
            || !original.enabled()) {

            return BatchUpdateResult.failure(
                "选课批次不存在。");
        }

        if (semester == null
            || semester.isBlank()) {

            return BatchUpdateResult.failure(
                "学期不能为空。");
        }

        if (batchName == null
            || batchName.isBlank()) {

            return BatchUpdateResult.failure(
                "批次名称不能为空。");
        }

        if (batchType == null) {

            return BatchUpdateResult.failure(
                "批次类型不能为空。");
        }

        if (startTime == null
            || endTime == null) {

            return BatchUpdateResult.failure(
                "批次开始时间和结束时间不能为空。");
        }

        if (!startTime.isBefore(
            endTime)) {

            return BatchUpdateResult.failure(
                "批次开始时间必须早于结束时间。");
        }

        if (status == null) {

            return BatchUpdateResult.failure(
                "批次状态不能为空。");
        }

        CourseBatchSettings settings =
            new CourseBatchSettings(
                batchId,
                semester.trim(),
                batchName.trim(),
                batchType,
                startTime,
                endTime,
                status,
                allowSelect,
                allowDrop);

        settingsRepository.save(
            settings);

        SelectionBatchInfo updatedBatch =
            new SelectionBatchInfo(
                settings.batchId(),
                settings.semester(),
                settings.batchName(),
                settings.batchType(),
                settings.startTime(),
                settings.endTime(),
                settings.status(),
                settings.allowSelect(),
                settings.allowDrop());

        return BatchUpdateResult.success(
            "选课批次修改成功。",
            updatedBatch);
    }

    /**
     * 把原始批次和教务设置转换为客户端对象。
     */
    private SelectionBatchInfo toInfo(
        CourseBatchRecord batch,
        LocalDateTime now) {

        CourseBatchSettings settings =
            settingsRepository
                .find(
                    batch.batchId())
                .orElse(null);

        if (settings != null) {

            return new SelectionBatchInfo(
                settings.batchId(),
                settings.semester(),
                settings.batchName(),
                settings.batchType(),
                settings.startTime(),
                settings.endTime(),
                settings.status(),
                settings.allowSelect(),
                settings.allowDrop());
        }

        return new SelectionBatchInfo(
            batch.batchId(),
            batch.semester(),
            batch.batchName(),
            batch.batchType(),
            batch.startTime(),
            batch.endTime(),
            calculateStatus(
                batch,
                now),
            batch.allowSelect(),
            batch.allowDrop());
    }

    /**
     * 没有教务设置时根据时间计算批次状态。
     */
    private SelectionBatchStatus calculateStatus(
        CourseBatchRecord batch,
        LocalDateTime now) {

        if (now.isBefore(
            batch.startTime())) {

            return SelectionBatchStatus
                .NOT_STARTED;
        }

        if (now.isAfter(
            batch.endTime())) {

            return SelectionBatchStatus
                .ENDED;
        }

        return SelectionBatchStatus
            .OPEN;
    }
}

/**
 * 修改选课批次结果。
 */
record BatchUpdateResult(
    boolean success,
    String message,
    SelectionBatchInfo batch) {

    static BatchUpdateResult success(
        String message,
        SelectionBatchInfo batch) {

        return new BatchUpdateResult(
            true,
            message,
            batch);
    }

    static BatchUpdateResult failure(
        String message) {

        return new BatchUpdateResult(
            false,
            message,
            null);
    }
}
