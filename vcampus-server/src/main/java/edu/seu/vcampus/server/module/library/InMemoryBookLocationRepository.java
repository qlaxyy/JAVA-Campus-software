package edu.seu.vcampus.server.module.library;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** In-memory holding-location dictionary, seeded with the two demonstration rooms. */
final class InMemoryBookLocationRepository implements BookLocationRepository {

    private final Set<String> locations = new LinkedHashSet<>();

    InMemoryBookLocationRepository() {
        this(List.of(InMemoryBookCopyRepository.JIULONGHU, InMemoryBookCopyRepository.SIPAILOU));
    }

    InMemoryBookLocationRepository(List<String> seed) {
        seed.forEach(this::save);
    }

    @Override
    public synchronized List<String> findAll() {
        return List.copyOf(locations);
    }

    @Override
    public synchronized boolean exists(String locationName) {
        return locations.contains(locationName);
    }

    @Override
    public synchronized void save(String locationName) {
        Objects.requireNonNull(locationName, "locationName must not be null");
        if (locationName.isBlank()) {
            throw new IllegalArgumentException("Invalid library location.");
        }
        locations.add(locationName);
    }
}
