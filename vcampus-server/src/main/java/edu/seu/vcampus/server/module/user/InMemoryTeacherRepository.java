package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.server.demo.FinalDemoRoster;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory teacher repository used by tests and local non-persistent runs. */
final class InMemoryTeacherRepository implements TeacherRepository {

    private final Map<String, TeacherProfile> profiles = new LinkedHashMap<>();

    InMemoryTeacherRepository() {
        Instant createdAt = Instant.parse("2026-09-01T00:00:00Z");
        FinalDemoRoster.teachers().forEach(seed -> profiles.put(
                seed.userId(),
                new TeacherProfile(
                        seed.userId(), seed.department(), seed.title(), true,
                        FinalDemoRoster.SUPER_ADMIN_USER_ID, createdAt, createdAt)));
    }

    @Override
    public synchronized Optional<TeacherProfile> findByUserId(String userId) {
        return Optional.ofNullable(profiles.get(userId));
    }

    @Override
    public synchronized List<TeacherProfile> findAll() {
        return List.copyOf(new ArrayList<>(profiles.values()));
    }

    @Override
    public synchronized void save(TeacherProfile profile) {
        profiles.put(profile.userId(), profile);
    }
}
