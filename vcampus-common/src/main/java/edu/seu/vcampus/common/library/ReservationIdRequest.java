package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Identifies one reservation without accepting a user identifier. */
public final class ReservationIdRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String reservationId;

    public ReservationIdRequest(String reservationId) {
        this.reservationId = reservationId;
    }

    /** @return reservation identifier */
    public String getReservationId() { return reservationId; }
}
