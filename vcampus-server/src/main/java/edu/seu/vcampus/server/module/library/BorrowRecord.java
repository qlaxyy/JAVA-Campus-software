package edu.seu.vcampus.server.module.library;

import java.time.LocalDateTime;
import java.util.Objects;

/** Server-side borrow record kept behind the repository boundary. */
final class BorrowRecord {

    private final String recordId;
    private final String userId;
    private final String bookId;
    private final LocalDateTime borrowTime;
    private final LocalDateTime dueTime;
    private final BorrowStatus status;
    private final LocalDateTime returnTime;

    BorrowRecord(
            String recordId,
            String userId,
            String bookId,
            LocalDateTime borrowTime,
            LocalDateTime dueTime,
            BorrowStatus status) {
        this(recordId, userId, bookId, borrowTime, dueTime, status, null);
    }

    private BorrowRecord(String recordId, String userId, String bookId,
            LocalDateTime borrowTime, LocalDateTime dueTime, BorrowStatus status,
            LocalDateTime returnTime) {
        this.recordId = Objects.requireNonNull(recordId, "recordId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.bookId = Objects.requireNonNull(bookId, "bookId must not be null");
        this.borrowTime = Objects.requireNonNull(borrowTime, "borrowTime must not be null");
        this.dueTime = Objects.requireNonNull(dueTime, "dueTime must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.returnTime = returnTime;
    }

    String recordId() {
        return recordId;
    }

    String userId() {
        return userId;
    }

    String bookId() {
        return bookId;
    }

    LocalDateTime borrowTime() {
        return borrowTime;
    }

    LocalDateTime dueTime() {
        return dueTime;
    }

    BorrowStatus status() {
        return status;
    }

    LocalDateTime returnTime() {
        return returnTime;
    }

    BorrowRecord returnedAt(LocalDateTime time) {
        if (status != BorrowStatus.BORROWED) {
            throw new IllegalStateException("Only an active borrow can be returned.");
        }
        return new BorrowRecord(recordId, userId, bookId, borrowTime, dueTime,
                BorrowStatus.RETURNED, Objects.requireNonNull(time));
    }

    boolean isOverdueAt(LocalDateTime currentTime) {
        return status == BorrowStatus.BORROWED && currentTime.isAfter(dueTime);
    }
}
