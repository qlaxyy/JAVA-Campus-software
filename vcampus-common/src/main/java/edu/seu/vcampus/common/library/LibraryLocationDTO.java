package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * One entry of the library's holding-location dictionary.
 *
 * <p>Unlike {@link BookLocationDTO}, which summarises how many copies of <em>one</em> book sit at
 * a location, this is the library-wide list of places a book can be kept. It exists so that a
 * location is a known value rather than free text typed into the copy editor: two spellings of the
 * same shelf would otherwise split one room's inventory in two.
 */
public final class LibraryLocationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String locationName;
    private final int copyCount;

    /**
     * @param locationName display name; also the value stored on every physical copy
     * @param copyCount how many copies are filed there, excluding withdrawn ones
     */
    public LibraryLocationDTO(String locationName, int copyCount) {
        Objects.requireNonNull(locationName, "locationName must not be null");
        if (locationName.isBlank() || copyCount < 0) {
            throw new IllegalArgumentException("Invalid library location.");
        }
        this.locationName = locationName;
        this.copyCount = copyCount;
    }

    /** @return display name, also the value stored on every physical copy */
    public String getLocationName() { return locationName; }

    /** @return copies currently filed at this location */
    public int getCopyCount() { return copyCount; }

    /** @return the name alone, so a plain combo box renders locations readably */
    @Override
    public String toString() { return locationName; }
}
