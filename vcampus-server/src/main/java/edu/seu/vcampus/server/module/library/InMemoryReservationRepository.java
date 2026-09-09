package edu.seu.vcampus.server.module.library;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thread-safe in-memory reservation repository. */
final class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> reservations = new LinkedHashMap<>();

    @Override
    public synchronized List<Reservation> findAll() {
        return List.copyOf(reservations.values());
    }

    @Override
    public synchronized List<Reservation> findByUserId(String userId) {
        return reservations.values().stream()
                .filter(reservation -> reservation.userId().equals(userId))
                .toList();
    }

    @Override
    public synchronized Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public synchronized void save(Reservation reservation) {
        requireUniqueAssignment(reservation);
        if (reservations.putIfAbsent(reservation.reservationId(), reservation) != null) {
            throw new IllegalStateException("Reservation identifier already exists.");
        }
    }

    @Override
    public synchronized void update(Reservation reservation) {
        Reservation original = reservations.get(reservation.reservationId());
        if (original == null) {
            throw new IllegalStateException("Reservation does not exist.");
        }
        requireIdentity(original, reservation);
        requireTransition(original.status(), reservation.status());
        requireUniqueAssignment(reservation);
        reservations.put(reservation.reservationId(), reservation);
    }

    synchronized Map<String, Reservation> snapshot() {
        return new LinkedHashMap<>(reservations);
    }

    synchronized void restore(Map<String, Reservation> snapshot) {
        reservations.clear();
        reservations.putAll(snapshot);
    }

    private void requireUniqueAssignment(Reservation candidate) {
        if (candidate.status() != ReservationStatus.READY_FOR_PICKUP) {
            return;
        }
        boolean duplicate = reservations.values().stream()
                .filter(existing -> !existing.reservationId().equals(candidate.reservationId()))
                .filter(existing -> existing.status() == ReservationStatus.READY_FOR_PICKUP)
                .anyMatch(existing -> existing.assignedCopyId().equals(candidate.assignedCopyId()));
        if (duplicate) {
            throw new IllegalStateException("Physical copy already has an active reservation.");
        }
    }

    private void requireIdentity(Reservation original, Reservation updated) {
        if (!original.userId().equals(updated.userId())
                || !original.bookId().equals(updated.bookId())
                || !original.pickupLocation().equals(updated.pickupLocation())
                || !original.createdAt().equals(updated.createdAt())) {
            throw new IllegalArgumentException("Reservation identity is immutable.");
        }
    }

    private void requireTransition(ReservationStatus before, ReservationStatus after) {
        boolean valid = switch (before) {
            case WAITING -> after == ReservationStatus.READY_FOR_PICKUP
                    || after == ReservationStatus.FULFILLED
                    || after == ReservationStatus.CANCELED;
            case READY_FOR_PICKUP -> after == ReservationStatus.FULFILLED
                    || after == ReservationStatus.CANCELED
                    || after == ReservationStatus.EXPIRED;
            case FULFILLED, CANCELED, EXPIRED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid reservation state transition.");
        }
    }
}
