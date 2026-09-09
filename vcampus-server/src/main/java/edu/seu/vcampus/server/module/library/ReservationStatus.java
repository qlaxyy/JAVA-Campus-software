package edu.seu.vcampus.server.module.library;

/** Lifecycle states of one title reservation. */
enum ReservationStatus {
    WAITING,
    READY_FOR_PICKUP,
    FULFILLED,
    CANCELED,
    EXPIRED
}
