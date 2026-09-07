package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Immutable administrator view of one borrow record. */
public final class AdminBorrowRecordDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String recordId;
    private final String userId;
    private final String bookId;
    private final String bookTitle;
    private final String copyId;
    private final String barcode;
    private final LocalDateTime borrowTime;
    private final LocalDateTime dueTime;
    private final LocalDateTime returnTime;
    private final String status;
    private final boolean overdue;

    public AdminBorrowRecordDTO(String recordId, String userId, String bookId,
            String bookTitle, String copyId, String barcode,
            LocalDateTime borrowTime, LocalDateTime dueTime, LocalDateTime returnTime,
            String status, boolean overdue) {
        this.recordId = recordId;
        this.userId = userId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.copyId = copyId;
        this.barcode = barcode;
        this.borrowTime = borrowTime;
        this.dueTime = dueTime;
        this.returnTime = returnTime;
        this.status = status;
        this.overdue = overdue;
    }

    public String getRecordId() { return recordId; }
    public String getUserId() { return userId; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getCopyId() { return copyId; }
    public String getBarcode() { return barcode; }
    public LocalDateTime getBorrowTime() { return borrowTime; }
    public LocalDateTime getDueTime() { return dueTime; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public String getStatus() { return status; }
    public boolean isOverdue() { return overdue; }
}
