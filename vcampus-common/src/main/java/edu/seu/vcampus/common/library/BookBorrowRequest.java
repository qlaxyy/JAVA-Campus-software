package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Request to borrow one library book for the authenticated user. */
public final class BookBorrowRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;

    /**
     * Creates a borrow request.
     *
     * @param bookId stable identifier of the book to borrow
     */
    public BookBorrowRequest(String bookId) {
        this.bookId = bookId;
    }

    /** @return stable identifier of the book to borrow */
    public String getBookId() {
        return bookId;
    }
}
