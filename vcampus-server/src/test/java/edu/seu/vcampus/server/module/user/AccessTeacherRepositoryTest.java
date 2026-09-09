package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTeacherRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsPersistsAndUpdatesTeacherProfile() {
        Path databasePath = temporaryDirectory.resolve("teachers.accdb");
        Instant created = Instant.parse("2026-09-09T00:00:00Z");
        AccessTeacherRepository repository = repository(databasePath);
        repository.save(new TeacherProfile(
                "U-TEACHER-TEST", "计算机学院", "讲师", true,
                "U-ADMIN-001", created, created));

        Instant updated = created.plusSeconds(60);
        repository(databasePath).save(new TeacherProfile(
                "U-TEACHER-TEST", "软件学院", "副教授", false,
                "U-ADMIN-001", created, updated));

        TeacherProfile profile = repository(databasePath)
                .findByUserId("U-TEACHER-TEST").orElseThrow();
        assertEquals("软件学院", profile.department());
        assertEquals("副教授", profile.title());
        assertFalse(profile.active());
        assertEquals("U-ADMIN-001", profile.createdByUserId());
        assertEquals(1, repository(databasePath).findAll().size());
    }

    @Test
    void persistentBootstrapSeedsOnlyTheDedicatedTeacherAccount() {
        Path databasePath = temporaryDirectory.resolve("seeded-teachers.accdb");
        InMemoryAuthenticationService authentication =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);

        assertTrue(authentication.teacherDirectory()
                .findByUserId("U-COURSE-TEACHER-001").isPresent());
        assertTrue(authentication.teacherDirectory()
                .findByUserId("U-TEACHER-001").isEmpty());
        assertEquals(1, authentication.teacherDirectory().findActiveTeachers().size());
    }

    private AccessTeacherRepository repository(Path path) {
        return new AccessTeacherRepository(new AccessDatabase(path));
    }
}
