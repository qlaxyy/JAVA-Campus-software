package edu.seu.vcampus.server.security;

import java.util.List;
import java.util.Optional;

/** Read-only directory of active teacher qualifications for server modules. */
public interface TeacherDirectory {
    Optional<TeacherIdentity> findByUserId(String userId);
    List<TeacherIdentity> findActiveTeachers();
}
