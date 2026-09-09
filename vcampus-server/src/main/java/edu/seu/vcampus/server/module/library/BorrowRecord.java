package edu.seu.vcampus.server.module.library;

import java.time.LocalDateTime;
import java.util.Objects;

/** Server-side borrow record kept behind the repository boundary. */
final class BorrowRecord {

    private final String recordId;
    private final String userId;
    private final String copyId;
    private final LocalDateTime borrowTime;
    private final LocalDateTime dueTime;
    private final BorrowStatus status;
    private final LocalDateTime returnTime;

    BorrowRecord(
            String recordId,
            String userId,
            String copyId,
            LocalDateTime borrowTime,
            LocalDateTime dueTime,
            BorrowStatus status) {
        this(recordId, userId, copyId, borrowTime, dueTime, status, null);
    }

    private BorrowRecord(String recordId, String userId, String copyId,
            LocalDateTime borrowTime, LocalDateTime dueTime, BorrowStatus status,
            LocalDateTime returnTime) {
        this.recordId = required(recordId, "recordId");
        this.userId = required(userId, "userId");
        this.copyId = required(copyId, "copyId");
        this.borrowTime = Objects.requireNonNull(borrowTime, "borrowTime must not be null");
        this.dueTime = Objects.requireNonNull(dueTime, "dueTime must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.returnTime = returnTime;
        if (dueTime.isBefore(borrowTime)) {
            throw new IllegalArgumentException("dueTime must not be before borrowTime");
        }
        if ((status == BorrowStatus.BORROWED) != (returnTime == null)) {
            throw new IllegalArgumentException("Borrow status and returnTime are inconsistent.");
        }
        if (returnTime != null && returnTime.isBefore(borrowTime)) {
            throw new IllegalArgumentException("returnTime must not be before borrowTime");
        }
    }

    String recordId() {
        return recordId;
    }

    String userId() {
        return userId;
    }

    String copyId() {
        return copyId;
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
        return new BorrowRecord(recordId, userId, copyId, borrowTime, dueTime,
                BorrowStatus.RETURNED, Objects.requireNonNull(time));
    }

    boolean isOverdueAt(LocalDateTime currentTime) {
        return status == BorrowStatus.BORROWED && currentTime.isAfter(dueTime);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
