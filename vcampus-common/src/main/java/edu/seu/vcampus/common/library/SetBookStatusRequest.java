package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Changes whether one catalog record is open for borrowing. */
public final class SetBookStatusRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;
    private final String status;

    public SetBookStatusRequest(String bookId, String status) {
        this.bookId = bookId;
        this.status = status;
    }

    public String getBookId() { return bookId; }

    public String getStatus() { return status; }
}
