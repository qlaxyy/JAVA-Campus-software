package edu.seu.vcampus.server.module.library;

import java.time.LocalDateTime;
import java.util.Objects;

/** One user's reservation for a title at a specific pickup location. */
final class Reservation {

    private final String reservationId;
    private final String userId;
    private final String bookId;
    private final String pickupLocation;
    private final String assignedCopyId;
    private final LocalDateTime createdAt;
    private final LocalDateTime readyAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime closedAt;
    private final ReservationStatus status;

    Reservation(String reservationId, String userId, String bookId,
            String pickupLocation, String assignedCopyId, LocalDateTime createdAt,
            LocalDateTime readyAt, LocalDateTime expiresAt, LocalDateTime closedAt,
            ReservationStatus status) {
        this.reservationId = required(reservationId, "reservationId");
        this.userId = required(userId, "userId");
        this.bookId = required(bookId, "bookId");
        this.pickupLocation = required(pickupLocation, "pickupLocation");
        this.assignedCopyId = optional(assignedCopyId);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.readyAt = readyAt;
        this.expiresAt = expiresAt;
        this.closedAt = closedAt;
        this.status = Objects.requireNonNull(status, "status must not be null");
        validateState();
    }

    static Reservation waiting(String reservationId, String userId, String bookId,
            String pickupLocation, LocalDateTime createdAt) {
        return new Reservation(reservationId, userId, bookId, pickupLocation,
                null, createdAt, null, null, null, ReservationStatus.WAITING);
    }

    String reservationId() { return reservationId; }

    String userId() { return userId; }

    String bookId() { return bookId; }

    String pickupLocation() { return pickupLocation; }

    String assignedCopyId() { return assignedCopyId; }

    LocalDateTime createdAt() { return createdAt; }

    LocalDateTime readyAt() { return readyAt; }

    LocalDateTime expiresAt() { return expiresAt; }

    LocalDateTime closedAt() { return closedAt; }

    ReservationStatus status() { return status; }

    boolean isActive() {
        return status == ReservationStatus.WAITING
                || status == ReservationStatus.READY_FOR_PICKUP;
    }

    Reservation readyForPickup(String copyId, LocalDateTime readyTime,
            LocalDateTime expiryTime) {
        if (status != ReservationStatus.WAITING) {
            throw new IllegalStateException("Only a waiting reservation can receive a copy.");
        }
        return new Reservation(reservationId, userId, bookId, pickupLocation,
                copyId, createdAt, readyTime, expiryTime, null,
                ReservationStatus.READY_FOR_PICKUP);
    }

    Reservation fulfilledAt(LocalDateTime time) {
        if (!isActive()) {
            throw new IllegalStateException("Only an active reservation can be fulfilled.");
        }
        return new Reservation(reservationId, userId, bookId, pickupLocation,
                assignedCopyId, createdAt, readyAt, expiresAt,
                Objects.requireNonNull(time), ReservationStatus.FULFILLED);
    }

    Reservation canceledAt(LocalDateTime time) {
        if (!isActive()) {
            throw new IllegalStateException("Only an active reservation can be canceled.");
        }
        return new Reservation(reservationId, userId, bookId, pickupLocation,
                assignedCopyId, createdAt, readyAt, expiresAt,
                Objects.requireNonNull(time), ReservationStatus.CANCELED);
    }

    Reservation expiredAt(LocalDateTime time) {
        if (status != ReservationStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Only a ready reservation can expire.");
        }
        return new Reservation(reservationId, userId, bookId, pickupLocation,
                assignedCopyId, createdAt, readyAt, expiresAt,
                Objects.requireNonNull(time), ReservationStatus.EXPIRED);
    }

    private void validateState() {
        boolean assignmentEmpty = assignedCopyId == null && readyAt == null && expiresAt == null;
        boolean assignmentComplete = assignedCopyId != null && readyAt != null && expiresAt != null
                && expiresAt.isAfter(readyAt);
        if (!(assignmentEmpty || assignmentComplete)) {
            throw new IllegalArgumentException("Copy assignment fields must be all empty or complete.");
        }
        switch (status) {
            case WAITING -> {
                if (!assignmentEmpty || closedAt != null) {
                    throw new IllegalArgumentException("Waiting reservation has invalid state.");
                }
            }
            case READY_FOR_PICKUP -> {
                if (!assignmentComplete || closedAt != null) {
                    throw new IllegalArgumentException("Ready reservation has invalid state.");
                }
            }
            case FULFILLED, CANCELED -> {
                if (closedAt == null) {
                    throw new IllegalArgumentException("Closed reservation needs a close time.");
                }
            }
            case EXPIRED -> {
                if (!assignmentComplete || closedAt == null) {
                    throw new IllegalArgumentException("Expired reservation has invalid state.");
                }
            }
            default -> throw new IllegalStateException("Unknown reservation status.");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
