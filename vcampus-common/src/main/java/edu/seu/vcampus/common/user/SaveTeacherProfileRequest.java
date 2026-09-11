package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Super-administrator request for creating or updating a teacher profile. */
public final class SaveTeacherProfileRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String department;
    private final String title;
    private final boolean active;

    public SaveTeacherProfileRequest(
            String userId,
            String department,
            String title,
            boolean active) {
        this.userId = requireText(userId, "userId", 36);
        this.department = requireText(department, "department", 100);
        this.title = requireText(title, "title", 50);
        this.active = active;
    }

    public String getUserId() { return userId; }
    public String getDepartment() { return department; }
    public String getTitle() { return title; }
    public boolean isActive() { return active; }

    private static String requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }
}
