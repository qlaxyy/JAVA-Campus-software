package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 选课批次业务服务。
 */
final class CourseBatchService {

    private final CourseBatchRepository repository;
    private final Clock clock;

    CourseBatchService(
        CourseBatchRepository repository,
        Clock clock) {

        this.repository = Objects.requireNonNull(
            repository,
            "repository must not be null");

        this.clock = Objects.requireNonNull(
            clock,
            "clock must not be null");
    }

    /**
     * 返回当前学期全部启用的选课批次。
     */
    List<SelectionBatchInfo> listBatches() {
        LocalDateTime now = LocalDateTime.now(clock);

        return repository.findCurrentSemesterBatches()
            .stream()
            .filter(CourseBatchRecord::enabled)
            .map(batch -> toInfo(batch, now))
            .toList();
    }

    private SelectionBatchInfo toInfo(
        CourseBatchRecord batch,
        LocalDateTime now) {

        return new SelectionBatchInfo(
            batch.batchId(),
            batch.semester(),
            batch.batchName(),
            batch.batchType(),
            batch.startTime(),
            batch.endTime(),
            calculateStatus(batch, now),
            batch.allowSelect(),
            batch.allowDrop()
        );
    }

    private SelectionBatchStatus calculateStatus(
        CourseBatchRecord batch,
        LocalDateTime now) {

        if (now.isBefore(batch.startTime())) {
            return SelectionBatchStatus.NOT_STARTED;
        }

        if (now.isAfter(batch.endTime())) {
            return SelectionBatchStatus.ENDED;
        }

        return SelectionBatchStatus.OPEN;
    }
}
