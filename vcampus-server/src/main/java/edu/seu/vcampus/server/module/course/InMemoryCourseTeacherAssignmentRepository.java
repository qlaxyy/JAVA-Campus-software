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

        /*
         * 保留原有演示任课关系。
         */
        assignments.add(
            new Assignment(
                1001L,
                "U-COURSE-TEACHER-001"));
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
