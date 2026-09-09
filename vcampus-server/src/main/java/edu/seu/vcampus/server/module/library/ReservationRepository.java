package edu.seu.vcampus.server.module.library;

import java.util.List;
import java.util.Optional;

/** Data boundary for title reservations and their copy assignments. */
interface ReservationRepository {

    List<Reservation> findAll();

    List<Reservation> findByUserId(String userId);

    Optional<Reservation> findById(String reservationId);

    void save(Reservation reservation);

    void update(Reservation reservation);
}
