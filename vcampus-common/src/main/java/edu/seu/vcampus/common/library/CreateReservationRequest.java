package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Requests a title reservation at one existing pickup location. */
public final class CreateReservationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;
    private final String pickupLocation;

    public CreateReservationRequest(String bookId, String pickupLocation) {
        this.bookId = bookId;
        this.pickupLocation = pickupLocation;
    }

    /** @return stable catalog identifier */
    public String getBookId() { return bookId; }

    /** @return selected existing holding location */
    public String getPickupLocation() { return pickupLocation; }
}
