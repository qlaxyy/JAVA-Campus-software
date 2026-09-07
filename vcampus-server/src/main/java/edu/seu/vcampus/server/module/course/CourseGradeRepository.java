package edu.seu.vcampus.server.module.course;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 成绩数据访问接口。
 */
interface CourseGradeRepository {

    /**
     * 根据选课记录查询成绩。
     */
    Optional<CourseGradeRecord> findByEnrollmentId(
        long enrollmentId);

    /**
     * 查询全部成绩记录。
     */
    List<CourseGradeRecord> findAll();

    /**
     * 新增或更新成绩。
     */
    CourseGradeRecord save(
        long enrollmentId,
        double score,
        LocalDateTime recordedAt);
}

/**
 * 服务端内部成绩记录。
 */
record CourseGradeRecord(
    long gradeId,
    long enrollmentId,
    double score,
    LocalDateTime recordedAt) {
}

/**
 * 内存成绩仓库。
 *
 * 每条选课记录最多对应一条成绩。
 */
final class InMemoryCourseGradeRepository
    implements CourseGradeRepository {

    private final ConcurrentMap<Long, CourseGradeRecord>
        recordsByEnrollmentId =
        new ConcurrentHashMap<>();

    private final AtomicLong nextGradeId =
        new AtomicLong(
            1L);

    @Override
    public Optional<CourseGradeRecord>
    findByEnrollmentId(
        long enrollmentId) {

        return Optional.ofNullable(
            recordsByEnrollmentId.get(
                enrollmentId));
    }

    @Override
    public List<CourseGradeRecord> findAll() {

        List<CourseGradeRecord> result =
            new ArrayList<>(
                recordsByEnrollmentId.values());

        result.sort(
            Comparator.comparingLong(
                CourseGradeRecord::gradeId));

        return result;
    }

    @Override
    public synchronized CourseGradeRecord save(
        long enrollmentId,
        double score,
        LocalDateTime recordedAt) {

        CourseGradeRecord existing =
            recordsByEnrollmentId.get(
                enrollmentId);

        long gradeId =
            existing == null
                ? nextGradeId.getAndIncrement()
                : existing.gradeId();

        CourseGradeRecord updated =
            new CourseGradeRecord(
                gradeId,
                enrollmentId,
                score,
                recordedAt);

        recordsByEnrollmentId.put(
            enrollmentId,
            updated);

        return updated;
    }
}
