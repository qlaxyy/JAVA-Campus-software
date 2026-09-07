package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Derived inventory summary for one catalog book at one holding location. */
public final class BookLocationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String location;
    private final int totalCount;
    private final int availableCount;

    /** Creates one immutable location summary. */
    public BookLocationDTO(String location, int totalCount, int availableCount) {
        this.location = Objects.requireNonNull(location, "location must not be null");
        if (location.isBlank() || totalCount < 0 || availableCount < 0
                || availableCount > totalCount) {
            throw new IllegalArgumentException("Invalid book-location summary.");
        }
        this.totalCount = totalCount;
        this.availableCount = availableCount;
    }

    public String getLocation() {
        return location;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getAvailableCount() {
        return availableCount;
    }
}
