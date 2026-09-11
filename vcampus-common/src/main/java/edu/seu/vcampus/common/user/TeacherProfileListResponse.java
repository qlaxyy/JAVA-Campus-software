package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Teacher-profile list returned to the super-administrator client. */
public final class TeacherProfileListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<TeacherProfileView> teachers;

    public TeacherProfileListResponse(List<TeacherProfileView> teachers) {
        this.teachers = List.copyOf(Objects.requireNonNull(
                teachers, "teachers must not be null"));
    }

    public List<TeacherProfileView> getTeachers() {
        return teachers;
    }
}
