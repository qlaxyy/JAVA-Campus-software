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
        assignments.addAll(List.of(
            new Assignment(1001L, "U-TEACHER-003"),
            new Assignment(2001L, "U-TEACHER-007"),
            new Assignment(3001L, "U-TEACHER-001"),
            new Assignment(4001L, "U-TEACHER-002"),
            new Assignment(6001L, "U-TEACHER-008"),
            new Assignment(14001L, "U-TEACHER-005"),
            new Assignment(15001L, "U-TEACHER-006"),
            new Assignment(15002L, "U-TEACHER-004")));
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
