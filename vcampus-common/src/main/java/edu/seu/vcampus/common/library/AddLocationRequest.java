package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/**
 * Adds one holding location to the dictionary; library administrators only.
 *
 * <p>Locations are add-only, matching categories: copies and reservations reference a location by
 * name, so renaming or deleting one would orphan existing records.
 */
public final class AddLocationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String locationName;

    /** @param locationName display name, at most 100 characters */
    public AddLocationRequest(String locationName) {
        this.locationName = locationName;
    }

    /** @return display name of the new location */
    public String getLocationName() { return locationName; }
}
