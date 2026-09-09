package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Requests all physical copies registered under one catalog record. */
public final class ListBookCopiesRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;

    public ListBookCopiesRequest(String bookId) {
        this.bookId = bookId;
    }

    public String getBookId() { return bookId; }
}
