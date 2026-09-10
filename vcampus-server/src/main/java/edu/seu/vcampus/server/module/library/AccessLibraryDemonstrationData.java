package edu.seu.vcampus.server.module.library;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Initializes a coherent circulation scenario for a brand-new Access library schema. */
final class AccessLibraryDemonstrationData {

    static final String DEMO_USER_ID = "U-STUDENT-001";
    static final String CURRENT_RECORD_ID = "DEMO-BORROW-CURRENT-001";
    static final String HISTORY_RECORD_ID = "DEMO-BORROW-HISTORY-001";
    static final String READY_RESERVATION_ID = "DEMO-RESERVATION-READY-001";
    static final String LOANED_BARCODE = "SEU-B001-001";
    static final String RETURNED_BARCODE = "SEU-B002-001";
    static final String RESERVED_BARCODE = "SEU-B003-001";

    private AccessLibraryDemonstrationData() {
    }

    static void seedIfEligible(
            AccessLibraryStore store,
            BookCopyRepository copies,
            BorrowRecordRepository records,
            ReservationRepository reservations,
            Clock clock) {
        Objects.requireNonNull(store, "store must not be null");
        Objects.requireNonNull(copies, "copies must not be null");
        Objects.requireNonNull(records, "records must not be null");
        Objects.requireNonNull(reservations, "reservations must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        if (!store.isNewlyCreatedLibrarySchema()) {
            return;
        }

        store.execute(() -> {
            if (!records.findAll().isEmpty() || !reservations.findAll().isEmpty()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
            seedCurrentBorrow(copies, records, now);
            seedBorrowHistory(copies, records, now);
            seedReadyReservation(copies, reservations, now);
        });
    }

    private static void seedCurrentBorrow(
            BookCopyRepository copies,
            BorrowRecordRepository records,
            LocalDateTime now) {
        BookCopy copy = requireAvailableCopy(copies, LOANED_BARCODE);
        LocalDateTime borrowedAt = now.minusDays(3);
        copies.update(copy.withStatus(BookCopyStatus.LOANED));
        records.save(new BorrowRecord(
                CURRENT_RECORD_ID,
                DEMO_USER_ID,
                copy.copyId(),
                borrowedAt,
                borrowedAt.plusDays(30),
                BorrowStatus.BORROWED));
    }

    private static void seedBorrowHistory(
            BookCopyRepository copies,
            BorrowRecordRepository records,
            LocalDateTime now) {
        BookCopy copy = requireAvailableCopy(copies, RETURNED_BARCODE);
        LocalDateTime borrowedAt = now.minusDays(60);
        BorrowRecord returned = new BorrowRecord(
                HISTORY_RECORD_ID,
                DEMO_USER_ID,
                copy.copyId(),
                borrowedAt,
                borrowedAt.plusDays(30),
                BorrowStatus.BORROWED)
                .returnedAt(now.minusDays(35));
        records.save(returned);
    }

    private static void seedReadyReservation(
            BookCopyRepository copies,
            ReservationRepository reservations,
            LocalDateTime now) {
        BookCopy copy = requireAvailableCopy(copies, RESERVED_BARCODE);
        LocalDateTime createdAt = now.minusHours(1);
        Reservation waiting = Reservation.waiting(
                READY_RESERVATION_ID,
                DEMO_USER_ID,
                copy.bookId(),
                copy.location(),
                createdAt);
        reservations.save(waiting);
        copies.update(copy.withStatus(BookCopyStatus.RESERVED));
        reservations.update(waiting.readyForPickup(
                copy.copyId(), createdAt, now.plusHours(23)));
    }

    private static BookCopy requireAvailableCopy(
            BookCopyRepository copies, String barcode) {
        BookCopy copy = copies.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalStateException(
                        "Demonstration copy is missing: " + barcode));
        if (copy.status() != BookCopyStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Demonstration copy is not available: " + barcode);
        }
        return copy;
    }
}
