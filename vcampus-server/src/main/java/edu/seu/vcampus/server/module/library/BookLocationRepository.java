package edu.seu.vcampus.server.module.library;

import java.util.List;

/**
 * Holding-location dictionary: the authoritative list of places a physical copy may be filed.
 *
 * <p>Names are the key. Copies and reservations already store a location as text, so keying the
 * dictionary by name keeps them readable and needs no data migration; the price is that locations,
 * like categories, are add-only — renaming would orphan every copy that references the old name.
 */
interface BookLocationRepository {

    /** Lists every known location in a stable order (seed order, then insertion order). */
    List<String> findAll();

    /** @return whether this exact name is already in the dictionary */
    boolean exists(String locationName);

    /** Adds one location; callers check {@link #exists} first for a friendly duplicate error. */
    void save(String locationName);
}
