package edu.seu.vcampus.server.security;

import java.util.Objects;

/** Read-only active teacher identity shared with server-side business modules. */
public record TeacherIdentity(
        String userId,
        String campusCardNumber,
        String displayName,
        String department,
        String title) {
    public TeacherIdentity {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(campusCardNumber, "campusCardNumber must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(department, "department must not be null");
        Objects.requireNonNull(title, "title must not be null");
    }
}
