package edu.seu.vcampus.server.module.course;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 教师任课关系的内存实现。
 */
final class InMemoryCourseTeacherAssignmentRepository
    implements CourseTeacherAssignmentRepository {

    private final Set<Assignment> assignments =
        ConcurrentHashMap.newKeySet();

    InMemoryCourseTeacherAssignmentRepository() {

        long[] offeringIds = {
            1001L, 2001L, 3001L, 9001L,
            14001L, 15002L, 14003L, 15001L
        };
        for (int index = 0; index < offeringIds.length; index++) {
            assignments.add(new Assignment(
                offeringIds[index],
                "U-TEACHER-" + String.format("%03d", index + 1)));
        }
    }

    @Override
    public boolean isAssigned(
        String teacherUserId,
        long offeringId) {

        String normalizedUserId =
            normalizeUserId(
                teacherUserId);

        if (normalizedUserId == null
            || offeringId <= 0) {

            return false;
        }

        return assignments.contains(
            new Assignment(
                offeringId,
                normalizedUserId));
    }

    @Override
    public List<Long> findOfferingIds(
        String teacherUserId) {

        String normalizedUserId =
            normalizeUserId(
                teacherUserId);

        if (normalizedUserId == null) {

            return List.of();
        }

        return assignments.stream()
            .filter(assignment ->
                assignment.teacherUserId()
                    .equals(
                        normalizedUserId))
            .map(
                Assignment::offeringId)
            .sorted()
            .toList();
    }

    @Override
    public List<String> findTeacherUserIds(
        long offeringId) {

        if (offeringId <= 0) {

            return List.of();
        }

        return assignments.stream()
            .filter(assignment ->
                assignment.offeringId()
                    == offeringId)
            .map(
                Assignment::teacherUserId)
            .sorted()
            .toList();
    }

    @Override
    public boolean assign(
        long offeringId,
        String teacherUserId,
        String teacherName) {

        String normalizedUserId =
            normalizeUserId(
                teacherUserId);

        if (normalizedUserId == null
            || offeringId <= 0) {

            return false;
        }

        return assignments.add(
            new Assignment(
                offeringId,
                normalizedUserId));
    }

    @Override
    public boolean remove(
        long offeringId,
        String teacherUserId) {

        String normalizedUserId =
            normalizeUserId(
                teacherUserId);

        if (normalizedUserId == null
            || offeringId <= 0) {

            return false;
        }

        return assignments.remove(
            new Assignment(
                offeringId,
                normalizedUserId));
    }

    private String normalizeUserId(
        String teacherUserId) {

        if (teacherUserId == null
            || teacherUserId.isBlank()) {

            return null;
        }

        return teacherUserId.trim();
    }

    private record Assignment(
        long offeringId,
        String teacherUserId) {
    }
}
