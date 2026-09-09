package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Safe teacher qualification and basic identity returned to clients. */
public final class TeacherProfileView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String campusCardNumber;
    private final String displayName;
    private final String department;
    private final String title;
    private final boolean active;

    public TeacherProfileView(
            String userId,
            String campusCardNumber,
            String displayName,
            String department,
            String title,
            boolean active) {
        this.userId = requireText(userId, "userId");
        this.campusCardNumber = requireText(campusCardNumber, "campusCardNumber");
        this.displayName = requireText(displayName, "displayName");
        this.department = requireText(department, "department");
        this.title = requireText(title, "title");
        this.active = active;
    }

    public String getUserId() { return userId; }
    public String getCampusCardNumber() { return campusCardNumber; }
    public String getDisplayName() { return displayName; }
    public String getDepartment() { return department; }
    public String getTitle() { return title; }
    public boolean isActive() { return active; }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
