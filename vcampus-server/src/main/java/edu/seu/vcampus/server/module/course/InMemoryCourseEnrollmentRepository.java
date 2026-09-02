package edu.seu.vcampus.server.module.course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 开发阶段内存选课记录。
 */
final class InMemoryCourseEnrollmentRepository
    implements CourseEnrollmentRepository {

    private final Map<
        String,
        Map<Long, CourseEnrollmentRecord>>
        recordsByUser =
        new HashMap<>();

    private final Map<Long, Integer>
        additionalCounts =
        new HashMap<>();

    private long nextEnrollmentId =
        1L;

    @Override
    public synchronized boolean isOfferingSelected(
        String userId,
        long offeringId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(
                userId);

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
    public synchronized Set<Long>
    findSelectedOfferingIds(
        String userId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(
                userId);

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

        return Set.copyOf(
            result);
    }

    @Override
    public synchronized List<CourseEnrollmentRecord>
    findSelectedEnrollments(
        String userId) {

        Map<Long, CourseEnrollmentRecord> records =
            recordsByUser.get(
                userId);

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
            recordsByUser.get(
                userId);

        if (records == null) {
            return null;
        }

        return records.get(
            enrollmentId);
    }

    @Override
    public synchronized List<CourseEnrollmentRecord>
    findSelectedEnrollmentsByOffering(
        long offeringId) {

        List<CourseEnrollmentRecord> result =
            new ArrayList<>();

        for (Map<Long, CourseEnrollmentRecord> records
            : recordsByUser.values()) {

            for (CourseEnrollmentRecord record
                : records.values()) {

                if (record.offeringId()
                    == offeringId) {

                    result.add(
                        record);
                }
            }
        }

        return result;
    }

    @Override
    public synchronized int countAdditionalSelections(
        long offeringId) {

        return additionalCounts
            .getOrDefault(
                offeringId,
                0);
    }

    @Override
    public synchronized void select(
        String userId,
        String studentId,
        long batchId,
        long offeringId) {

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
                studentId,
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
            recordsByUser.get(
                userId);

        if (records == null) {
            return false;
        }

        CourseEnrollmentRecord removed =
            records.remove(
                enrollmentId);

        if (removed == null) {
            return false;
        }

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
