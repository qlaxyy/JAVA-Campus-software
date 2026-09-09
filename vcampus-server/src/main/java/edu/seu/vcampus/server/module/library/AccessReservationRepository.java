package edu.seu.vcampus.server.module.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Access-backed repository for title reservations. */
final class AccessReservationRepository implements ReservationRepository {

    private static final String SELECT_RESERVATION = "SELECT reservationId, userId, bookId, "
            + "pickupLocation, assignedCopyId, createdAt, readyAt, expiresAt, closedAt, "
            + "[status] FROM tblReservation";

    private final AccessLibraryStore store;

    AccessReservationRepository(AccessLibraryStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    @Override
    public List<Reservation> findAll() {
        return store.read("Cannot list library reservations.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_RESERVATION + " ORDER BY createdAt, reservationId");
                 ResultSet result = statement.executeQuery()) {
                return readReservations(result);
            }
        });
    }

    @Override
    public List<Reservation> findByUserId(String userId) {
        return store.read("Cannot list user library reservations.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_RESERVATION
                            + " WHERE userId = ? ORDER BY createdAt DESC, reservationId")) {
                statement.setString(1, userId);
                try (ResultSet result = statement.executeQuery()) {
                    return readReservations(result);
                }
            }
        });
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return store.read("Cannot read library reservation.", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_RESERVATION + " WHERE reservationId = ?")) {
                statement.setString(1, reservationId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(readReservation(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void save(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation must not be null");
        store.execute(() -> {
            requireUniqueAssignment(reservation);
            store.write("Cannot insert library reservation.", connection -> {
                String sql = "INSERT INTO tblReservation (reservationId, userId, bookId, "
                        + "pickupLocation, assignedCopyId, createdAt, readyAt, expiresAt, "
                        + "closedAt, [status]) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindReservation(statement, reservation);
                    requireOne(statement.executeUpdate(), "Reservation was not inserted.");
                }
            });
        });
    }

    @Override
    public void update(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation must not be null");
        store.execute(() -> {
            Reservation original = findById(reservation.reservationId())
                    .orElseThrow(() -> new IllegalStateException("Reservation does not exist."));
            requireIdentity(original, reservation);
            requireTransition(original.status(), reservation.status());
            requireUniqueAssignment(reservation);
            store.write("Cannot update library reservation.", connection -> {
                String sql = "UPDATE tblReservation SET assignedCopyId = ?, readyAt = ?, "
                        + "expiresAt = ?, closedAt = ?, [status] = ? WHERE reservationId = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    nullableString(statement, 1, reservation.assignedCopyId());
                    nullableTime(statement, 2, reservation.readyAt());
                    nullableTime(statement, 3, reservation.expiresAt());
                    nullableTime(statement, 4, reservation.closedAt());
                    statement.setString(5, reservation.status().name());
                    statement.setString(6, reservation.reservationId());
                    requireOne(statement.executeUpdate(), "Reservation does not exist.");
                }
            });
        });
    }

    private void requireUniqueAssignment(Reservation candidate) {
        if (candidate.status() != ReservationStatus.READY_FOR_PICKUP) {
            return;
        }
        boolean duplicate = findAll().stream()
                .filter(existing -> !existing.reservationId().equals(candidate.reservationId()))
                .filter(existing -> existing.status() == ReservationStatus.READY_FOR_PICKUP)
                .anyMatch(existing -> existing.assignedCopyId().equals(candidate.assignedCopyId()));
        if (duplicate) {
            throw new IllegalStateException("Physical copy already has an active reservation.");
        }
    }

    private List<Reservation> readReservations(ResultSet result) throws java.sql.SQLException {
        List<Reservation> reservations = new ArrayList<>();
        while (result.next()) {
            reservations.add(readReservation(result));
        }
        return reservations;
    }

    private Reservation readReservation(ResultSet result) throws java.sql.SQLException {
        return new Reservation(
                result.getString("reservationId"),
                result.getString("userId"),
                result.getString("bookId"),
                result.getString("pickupLocation"),
                result.getString("assignedCopyId"),
                result.getTimestamp("createdAt").toLocalDateTime(),
                localTime(result, "readyAt"),
                localTime(result, "expiresAt"),
                localTime(result, "closedAt"),
                ReservationStatus.valueOf(result.getString("status")));
    }

    private void bindReservation(PreparedStatement statement, Reservation reservation)
            throws java.sql.SQLException {
        statement.setString(1, reservation.reservationId());
        statement.setString(2, reservation.userId());
        statement.setString(3, reservation.bookId());
        statement.setString(4, reservation.pickupLocation());
        nullableString(statement, 5, reservation.assignedCopyId());
        statement.setTimestamp(6, Timestamp.valueOf(reservation.createdAt()));
        nullableTime(statement, 7, reservation.readyAt());
        nullableTime(statement, 8, reservation.expiresAt());
        nullableTime(statement, 9, reservation.closedAt());
        statement.setString(10, reservation.status().name());
    }

    private static LocalDateTime localTime(ResultSet result, String column)
            throws java.sql.SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static void nullableString(PreparedStatement statement, int index, String value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void nullableTime(PreparedStatement statement, int index, LocalDateTime value)
            throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static void requireIdentity(Reservation original, Reservation updated) {
        if (!original.userId().equals(updated.userId())
                || !original.bookId().equals(updated.bookId())
                || !original.pickupLocation().equals(updated.pickupLocation())
                || !original.createdAt().equals(updated.createdAt())) {
            throw new IllegalArgumentException("Reservation identity is immutable.");
        }
    }

    private static void requireTransition(ReservationStatus before, ReservationStatus after) {
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

    private static void requireOne(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
