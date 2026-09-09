package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.library.BookDTO;

import java.util.Map;
import java.util.Objects;

/** Snapshot transaction for the four mutable in-memory library repositories. */
final class InMemoryLibraryTransactionManager implements LibraryTransactionManager {

    private final InMemoryBookRepository books;
    private final InMemoryBookCopyRepository copies;
    private final InMemoryBorrowRecordRepository borrows;
    private final InMemoryReservationRepository reservations;

    InMemoryLibraryTransactionManager(InMemoryBookRepository books,
            InMemoryBookCopyRepository copies,
            InMemoryBorrowRecordRepository borrows,
            InMemoryReservationRepository reservations) {
        this.books = Objects.requireNonNull(books);
        this.copies = Objects.requireNonNull(copies);
        this.borrows = Objects.requireNonNull(borrows);
        this.reservations = Objects.requireNonNull(reservations);
    }

    @Override
    public synchronized void execute(Runnable operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        Map<String, BookDTO> bookSnapshot = books.snapshot();
        Map<String, BookCopy> copySnapshot = copies.snapshot();
        Map<String, BorrowRecord> borrowSnapshot = borrows.snapshot();
        Map<String, Reservation> reservationSnapshot = reservations.snapshot();
        try {
            operation.run();
        } catch (RuntimeException | Error exception) {
            books.restore(bookSnapshot);
            copies.restore(copySnapshot);
            borrows.restore(borrowSnapshot);
            reservations.restore(reservationSnapshot);
            throw exception;
        }
    }
}
