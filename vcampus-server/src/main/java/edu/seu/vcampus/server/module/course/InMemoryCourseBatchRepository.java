package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.SelectionBatchType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 开发阶段使用的内存选课批次数据。
 */
final class InMemoryCourseBatchRepository
    implements CourseBatchRepository {

    private final List<CourseBatchRecord> batches;

    InMemoryCourseBatchRepository(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");

        LocalDateTime now = LocalDateTime.now(clock);

        batches = List.of(
            // 已结束，用来测试 ENDED
            new CourseBatchRecord(
                1L,
                "2026-2027-1",
                "2026-2027秋季学期预选课",
                SelectionBatchType.PRE_SELECTION,
                now.minusDays(10),
                now.minusDays(5),
                true,
                true,
                true
            ),

            // 正在进行，用来测试 OPEN
            new CourseBatchRecord(
                2L,
                "2026-2027-1",
                "2026-2027秋季学期重修选课",
                SelectionBatchType.RETAKE,
                now.minusDays(1),
                now.plusDays(1),
                true,
                true,
                true
            ),

            // 尚未开始，用来测试 NOT_STARTED
            new CourseBatchRecord(
                3L,
                "2026-2027-1",
                "2026-2027秋季学期退改补",
                SelectionBatchType.ADD_DROP,
                now.plusDays(3),
                now.plusDays(7),
                true,
                true,
                true
            )
        );
    }

    @Override
    public List<CourseBatchRecord> findCurrentSemesterBatches() {
        return batches;
    }
}
