package edu.seu.vcampus.server.module.user;

import java.time.Instant;
import java.util.Objects;

/** Server-owned teacher qualification record. Basic identity remains in tblUser. */
record TeacherProfile(
        String userId,
        String department,
        String title,
        boolean active,
        String createdByUserId,
        Instant createdAt,
        Instant updatedAt) {
    TeacherProfile {
        userId = requireText(userId, "userId");
        department = requireText(department, "department");
        title = requireText(title, "title");
        createdByUserId = requireText(createdByUserId, "createdByUserId");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
