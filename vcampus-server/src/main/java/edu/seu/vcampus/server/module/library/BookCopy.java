package edu.seu.vcampus.server.module.library;

import java.util.Objects;

/** One physical library item identified by an immutable barcode. */
final class BookCopy {

    private final String copyId;
    private final String barcode;
    private final String bookId;
    private final String location;
    private final String callNumber;
    private final BookCopyStatus status;

    BookCopy(String copyId, String barcode, String bookId, String location,
            String callNumber, BookCopyStatus status) {
        this.copyId = required(copyId, "copyId");
        this.barcode = required(barcode, "barcode");
        this.bookId = required(bookId, "bookId");
        this.location = required(location, "location");
        this.callNumber = required(callNumber, "callNumber");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    String copyId() { return copyId; }

    String barcode() { return barcode; }

    String bookId() { return bookId; }

    String location() { return location; }

    String callNumber() { return callNumber; }

    BookCopyStatus status() { return status; }

    BookCopy withStatus(BookCopyStatus newStatus) {
        return new BookCopy(copyId, barcode, bookId, location, callNumber, newStatus);
    }

    BookCopy withLocation(String newLocation, String newCallNumber) {
        return new BookCopy(copyId, barcode, bookId, newLocation, newCallNumber, status);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
