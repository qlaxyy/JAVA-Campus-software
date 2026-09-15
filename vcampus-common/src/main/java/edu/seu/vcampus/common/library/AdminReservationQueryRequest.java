package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Selects one administrator reservation-queue view. */
public final class AdminReservationQueryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Reservations still queued for a copy. */
    public static final String WAITING = "WAITING";
    /** Reservations whose copy is currently held for pickup. */
    public static final String READY = "READY";
    /** Every reservation, finished ones included. */
    public static final String ALL = "ALL";

    private final String scope;

    public AdminReservationQueryRequest(String scope) {
        this.scope = scope;
    }

    public String getScope() { return scope; }
}
