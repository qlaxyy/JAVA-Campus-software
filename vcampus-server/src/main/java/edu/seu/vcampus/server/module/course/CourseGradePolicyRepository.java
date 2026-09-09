package edu.seu.vcampus.server.module.course;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 教学班成绩比例仓库。
 */
interface CourseGradePolicyRepository {

    /**
     * 查询教学班的成绩比例。
     */
    Optional<CourseGradePolicyRecord>
    findByOfferingId(
        long offeringId);

    /**
     * 保存教学班成绩比例。
     */
    CourseGradePolicyRecord save(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent,
        LocalDateTime updatedAt);
}

/**
 * 教学班成绩比例记录。
 */
record CourseGradePolicyRecord(
    long offeringId,
    int usualWeightPercent,
    int finalExamWeightPercent,
    LocalDateTime updatedAt) {
}

/**
 * 内存成绩比例仓库。
 */
final class InMemoryCourseGradePolicyRepository
    implements CourseGradePolicyRepository {

    private final ConcurrentMap<
        Long,
        CourseGradePolicyRecord> records =
        new ConcurrentHashMap<>();

    @Override
    public Optional<CourseGradePolicyRecord>
    findByOfferingId(
        long offeringId) {

        return Optional.ofNullable(
            records.get(
                offeringId));
    }

    @Override
    public CourseGradePolicyRecord save(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent,
        LocalDateTime updatedAt) {

        CourseGradePolicyRecord record =
            new CourseGradePolicyRecord(
                offeringId,
                usualWeightPercent,
                finalExamWeightPercent,
                updatedAt);

        records.put(
            offeringId,
            record);

        return record;
    }
}
