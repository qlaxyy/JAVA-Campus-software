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
    private final int renewalCount;

    BorrowRecord(
            String recordId,
            String userId,
            String copyId,
            LocalDateTime borrowTime,
            LocalDateTime dueTime,
            BorrowStatus status) {
        this(recordId, userId, copyId, borrowTime, dueTime, status, null, 0);
    }

    BorrowRecord(String recordId, String userId, String copyId,
            LocalDateTime borrowTime, LocalDateTime dueTime, BorrowStatus status,
            LocalDateTime returnTime, int renewalCount) {
        this.recordId = required(recordId, "recordId");
        this.userId = required(userId, "userId");
        this.copyId = required(copyId, "copyId");
        this.borrowTime = Objects.requireNonNull(borrowTime, "borrowTime must not be null");
        this.dueTime = Objects.requireNonNull(dueTime, "dueTime must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.returnTime = returnTime;
        if (renewalCount < 0) {
            throw new IllegalArgumentException("renewalCount must not be negative");
        }
        this.renewalCount = renewalCount;
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

    int renewalCount() {
        return renewalCount;
    }

    BorrowRecord returnedAt(LocalDateTime time) {
        if (status != BorrowStatus.BORROWED) {
            throw new IllegalStateException("Only an active borrow can be returned.");
        }
        return new BorrowRecord(recordId, userId, copyId, borrowTime, dueTime,
                BorrowStatus.RETURNED, Objects.requireNonNull(time), renewalCount);
    }

    BorrowRecord renewedUntil(LocalDateTime renewedDueTime) {
        if (status != BorrowStatus.BORROWED) {
            throw new IllegalStateException("Only an active borrow can be renewed.");
        }
        LocalDateTime requiredDueTime = Objects.requireNonNull(renewedDueTime);
        if (!requiredDueTime.isAfter(dueTime)) {
            throw new IllegalArgumentException("Renewed due time must be later.");
        }
        return new BorrowRecord(recordId, userId, copyId, borrowTime,
                requiredDueTime, status, null, renewalCount + 1);
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
