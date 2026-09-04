package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Immutable personal borrow record; overdue is computed by the server at query time. */
public final class BorrowRecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String recordId;
    private final String bookId;
    private final String bookTitle;
    private final LocalDateTime borrowTime;
    private final LocalDateTime dueTime;
    private final LocalDateTime returnTime;
    private final String status;
    private final boolean overdue;

    /**
     * @param recordId borrow record identifier
     * @param bookId book identifier
     * @param bookTitle display title
     * @param borrowTime server borrowing time
     * @param dueTime server due time
     * @param returnTime actual return time, or null while borrowed
     * @param status BORROWED or RETURNED
     * @param overdue whether the active record is overdue at query time
     */
    public BorrowRecordDTO(String recordId, String bookId, String bookTitle,
            LocalDateTime borrowTime, LocalDateTime dueTime, LocalDateTime returnTime,
            String status, boolean overdue) {
        this.recordId = recordId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowTime = borrowTime;
        this.dueTime = dueTime;
        this.returnTime = returnTime;
        this.status = status;
        this.overdue = overdue;
    }

    /** @return borrow record identifier */
    public String getRecordId() { return recordId; }

    /** @return book identifier */
    public String getBookId() { return bookId; }

    /** @return display title */
    public String getBookTitle() { return bookTitle; }

    /** @return borrowing time */
    public LocalDateTime getBorrowTime() { return borrowTime; }

    /** @return due time */
    public LocalDateTime getDueTime() { return dueTime; }

    /** @return actual return time, or null while borrowed */
    public LocalDateTime getReturnTime() { return returnTime; }

    /** @return BORROWED or RETURNED */
    public String getStatus() { return status; }

    /** @return server-computed overdue flag */
    public boolean isOverdue() { return overdue; }
}
