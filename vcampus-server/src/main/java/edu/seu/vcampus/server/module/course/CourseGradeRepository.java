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
    Optional<CourseGradeRecord>
    findByEnrollmentId(
        long enrollmentId);

    /**
     * 查询全部成绩记录。
     */
    List<CourseGradeRecord> findAll();

    /**
     * 保存平时成绩和期末成绩。
     */
    CourseGradeRecord save(
        long enrollmentId,
        double usualScore,
        double finalExamScore,
        LocalDateTime recordedAt);

    /**
     * 兼容原有单项成绩保存代码。
     *
     * 旧成绩同时保存为平时成绩和期末成绩，
     * 保证迁移前后的总成绩不变。
     */
    default CourseGradeRecord save(
        long enrollmentId,
        double score,
        LocalDateTime recordedAt) {

        return save(
            enrollmentId,
            score,
            score,
            recordedAt);
    }
}

/**
 * 服务端内部成绩记录。
 */
record CourseGradeRecord(
    long gradeId,
    long enrollmentId,
    double usualScore,
    double finalExamScore,
    LocalDateTime recordedAt) {
    /**
     * 兼容原有单项成绩记录。
     */
    CourseGradeRecord(
        long gradeId,
        long enrollmentId,
        double score,
        LocalDateTime recordedAt) {

        this(
            gradeId,
            enrollmentId,
            score,
            score,
            recordedAt);
    }
    /**
     * 兼容原有代码。
     *
     * 新业务代码应当使用
     * CourseGradePolicyService 计算总成绩。
     */
    double score() {

        return usualScore * 0.4
            + finalExamScore * 0.6;
    }
}

/**
 * 内存成绩仓库。
 */
final class InMemoryCourseGradeRepository
    implements CourseGradeRepository {

    private final ConcurrentMap<
        Long,
        CourseGradeRecord>
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
        double usualScore,
        double finalExamScore,
        LocalDateTime recordedAt) {

        CourseGradeRecord existing =
            recordsByEnrollmentId.get(
                enrollmentId);

        long gradeId =
            existing == null
                ? nextGradeId
                .getAndIncrement()
                : existing.gradeId();

        CourseGradeRecord updated =
            new CourseGradeRecord(
                gradeId,
                enrollmentId,
                usualScore,
                finalExamScore,
                recordedAt);

        recordsByEnrollmentId.put(
            enrollmentId,
            updated);

        return updated;
    }
}
