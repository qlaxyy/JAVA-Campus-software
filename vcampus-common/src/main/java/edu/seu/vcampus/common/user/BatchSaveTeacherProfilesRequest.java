package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Super-administrator request for importing several teacher profiles at once. */
public final class BatchSaveTeacherProfilesRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<SaveTeacherProfileRequest> teachers;

    public BatchSaveTeacherProfilesRequest(List<SaveTeacherProfileRequest> teachers) {
        this.teachers = List.copyOf(Objects.requireNonNull(
                teachers, "teachers must not be null"));
        if (this.teachers.isEmpty() || this.teachers.size() > 1000) {
            throw new IllegalArgumentException("teacher batch size must be between 1 and 1000");
        }
        Set<String> userIds = new HashSet<>();
        for (SaveTeacherProfileRequest teacher : this.teachers) {
            if (!userIds.add(teacher.getUserId())) {
                throw new IllegalArgumentException("teacher userId must not be duplicated");
            }
        }
    }

    public List<SaveTeacherProfileRequest> getTeachers() {
        return teachers;
    }
}
