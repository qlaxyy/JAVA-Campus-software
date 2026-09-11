package edu.seu.vcampus.server.module.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory teacher repository used by tests and local non-persistent runs. */
final class InMemoryTeacherRepository implements TeacherRepository {

    private final Map<String, TeacherProfile> profiles = new LinkedHashMap<>();

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
