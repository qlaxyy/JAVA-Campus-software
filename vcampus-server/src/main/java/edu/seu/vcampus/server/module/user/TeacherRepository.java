package edu.seu.vcampus.server.module.user;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for school-wide teacher qualifications. */
interface TeacherRepository {
    Optional<TeacherProfile> findByUserId(String userId);
    List<TeacherProfile> findAll();
    void save(TeacherProfile profile);
}
