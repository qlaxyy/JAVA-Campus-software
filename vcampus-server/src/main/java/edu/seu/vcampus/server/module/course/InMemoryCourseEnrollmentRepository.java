package edu.seu.vcampus.server.module.course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 开发阶段内存选课记录 Repository。
 *
 * Server 重启后数据会清空。
 */
final class InMemoryCourseEnrollmentRepository
    implements CourseEnrollmentRepository {

    /**
     * userId
     * ->
     * enrollmentId -> EnrollmentRecord
     */
    private final Map<
        String,
        Map<Long, CourseEnrollmentRecord>>
        recordsByUser =
        new HashMap<>();

    /**
     * 本次服务器运行期间，
     * 每个教学班新增了多少学生。
     */
    private final Map<Long, Integer>
        additionalCounts =
        new HashMap<>();

    /**
     * 临时选课记录 ID。
     */
    private long nextEnrollmentId = 1L;

    @Override
    public synchronized boolean isOfferingSelected(
        String userId,
        long offeringId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(userId);

        if (records == null) {
            return false;
        }

        return records.values()
            .stream()
            .anyMatch(record ->
                record.offeringId()
                    == offeringId);
    }

    @Override
    public synchronized Set<Long> findSelectedOfferingIds(
        String userId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(userId);

        if (records == null) {
            return Set.of();
        }

        Set<Long> result =
            new HashSet<>();

        for (CourseEnrollmentRecord record
            : records.values()) {

            result.add(
                record.offeringId());
        }

        return Set.copyOf(result);
    }

    @Override
    public synchronized List<CourseEnrollmentRecord>
    findSelectedEnrollments(
        String userId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(userId);

        if (records == null) {
            return List.of();
        }

        return new ArrayList<>(
            records.values());
    }

    @Override
    public synchronized CourseEnrollmentRecord
    findSelectedEnrollment(
        String userId,
        long enrollmentId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(userId);

        if (records == null) {
            return null;
        }

        return records.get(
            enrollmentId);
    }

    @Override
    public synchronized int countAdditionalSelections(
        long offeringId) {

        return additionalCounts.getOrDefault(
            offeringId,
            0);
    }

    @Override
    public synchronized void select(
        String userId,
        long batchId,
        long offeringId) {

        /*
         * 防止重复选择同一个教学班。
         */
        if (isOfferingSelected(
            userId,
            offeringId)) {

            return;
        }

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.computeIfAbsent(
                userId,
                key ->
                    new LinkedHashMap<>());

        long enrollmentId =
            nextEnrollmentId++;

        CourseEnrollmentRecord record =
            new CourseEnrollmentRecord(
                enrollmentId,
                userId,
                batchId,
                offeringId);

        records.put(
            enrollmentId,
            record);

        additionalCounts.merge(
            offeringId,
            1,
            Integer::sum);
    }

    @Override
    public synchronized boolean drop(
        String userId,
        long enrollmentId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(userId);

        if (records == null) {
            return false;
        }

        CourseEnrollmentRecord removed =
            records.remove(
                enrollmentId);

        if (removed == null) {
            return false;
        }

        /*
         * 退课之后恢复教学班剩余名额。
         */
        additionalCounts.computeIfPresent(
            removed.offeringId(),
            (id, count) -> {

                if (count <= 1) {
                    return null;
                }

                return count - 1;
            });

        return true;
    }
}
