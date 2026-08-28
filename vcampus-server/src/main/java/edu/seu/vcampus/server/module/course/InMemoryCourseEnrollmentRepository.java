package edu.seu.vcampus.server.module.course;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 开发阶段使用的内存选课记录。
 *
 * Server 关闭后数据会消失。
 */
final class InMemoryCourseEnrollmentRepository
    implements CourseEnrollmentRepository {

    /**
     * userId -> 已选教学班 ID。
     */
    private final Map<String, Set<Long>>
        selectionsByUser =
        new HashMap<>();

    /**
     * offeringId -> 本次运行期间新增人数。
     */
    private final Map<Long, Integer>
        additionalCounts =
        new HashMap<>();

    @Override
    public synchronized boolean isOfferingSelected(
        String userId,
        long offeringId) {

        Set<Long> selections =
            selectionsByUser.get(userId);

        return selections != null
            && selections.contains(offeringId);
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

        Set<Long> selections =
            selectionsByUser.computeIfAbsent(
                userId,
                key -> new HashSet<>());

        /*
         * 防止同一个教学班重复计数。
         */
        if (selections.add(offeringId)) {

            additionalCounts.merge(
                offeringId,
                1,
                Integer::sum);
        }
    }
}
