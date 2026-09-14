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
    private final LocalDateTime lostReportedAt;
    private final LocalDateTime feeSettledAt;

    BorrowRecord(
            String recordId,
            String userId,
            String copyId,
            LocalDateTime borrowTime,
            LocalDateTime dueTime,
            BorrowStatus status) {
        this(recordId, userId, copyId, borrowTime, dueTime, status, null, 0, null, null);
    }

    BorrowRecord(String recordId, String userId, String copyId,
            LocalDateTime borrowTime, LocalDateTime dueTime, BorrowStatus status,
            LocalDateTime returnTime, int renewalCount) {
        this(recordId, userId, copyId, borrowTime, dueTime, status,
                returnTime, renewalCount, null, null);
    }

    BorrowRecord(String recordId, String userId, String copyId,
            LocalDateTime borrowTime, LocalDateTime dueTime, BorrowStatus status,
            LocalDateTime returnTime, int renewalCount,
            LocalDateTime lostReportedAt, LocalDateTime feeSettledAt) {
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
        if (lostReportedAt != null && returnTime == null) {
            throw new IllegalArgumentException("A lost report requires the loan to be closed.");
        }
        this.lostReportedAt = lostReportedAt;
        this.feeSettledAt = feeSettledAt;
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

    LocalDateTime lostReportedAt() {
        return lostReportedAt;
    }

    LocalDateTime feeSettledAt() {
        return feeSettledAt;
    }

    BorrowRecord returnedAt(LocalDateTime time) {
        if (status != BorrowStatus.BORROWED) {
            throw new IllegalStateException("Only an active borrow can be returned.");
        }
        return new BorrowRecord(recordId, userId, copyId, borrowTime, dueTime,
                BorrowStatus.RETURNED, Objects.requireNonNull(time), renewalCount,
                lostReportedAt, feeSettledAt);
    }

    /**
     * Closes an active borrow as reported lost. The loan ends immediately; the copy itself is
     * withdrawn by the caller in the same transaction.
     */
    BorrowRecord lostAt(LocalDateTime time) {
        if (status != BorrowStatus.BORROWED) {
            throw new IllegalStateException("Only an active borrow can be reported lost.");
        }
        LocalDateTime requiredTime = Objects.requireNonNull(time);
        return new BorrowRecord(recordId, userId, copyId, borrowTime, dueTime,
                BorrowStatus.RETURNED, requiredTime, renewalCount, requiredTime, feeSettledAt);
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
                requiredDueTime, status, null, renewalCount + 1, lostReportedAt, feeSettledAt);
    }

    /** Marks the fee of this record as settled at the given time. */
    BorrowRecord feeSettledAt(LocalDateTime time) {
        return new BorrowRecord(recordId, userId, copyId, borrowTime, dueTime, status,
                returnTime, renewalCount, lostReportedAt, Objects.requireNonNull(time));
    }

    boolean isOverdueAt(LocalDateTime currentTime) {
        return status == BorrowStatus.BORROWED && currentTime.isAfter(dueTime);
    }

    /** @return whether this record was closed by a lost-book report rather than a return */
    boolean isLost() {
        return lostReportedAt != null;
    }

    /**
     * Overdue fine in fen for a loan that was returned late. Zero unless the record was returned
     * after its due time; the due moment itself is not overdue, matching {@link #isOverdueAt}.
     *
     * @param perDayFen fine per started overdue day
     * @param capFen maximum fine
     * @return fine in fen
     */
    int overdueFineFen(int perDayFen, int capFen) {
        if (isLost() || status != BorrowStatus.RETURNED || returnTime == null) {
            return 0;
        }
        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(dueTime, returnTime);
        if (overdueDays <= 0) {
            return 0;
        }
        return (int) Math.min(overdueDays * (long) perDayFen, capFen);
    }

    /**
     * Validates that {@code updated} is a legal successor of {@code original}: a return, a
     * renewal, or settling the fee of an already closed record. Both the Access and the in-memory
     * repository enforce this single rule so the two implementations cannot drift apart.
     *
     * @param original record currently stored
     * @param updated record about to replace it
     * @throws IllegalArgumentException when the transition is not allowed
     */
    static void requireValidTransition(BorrowRecord original, BorrowRecord updated) {
        if (!original.userId().equals(updated.userId())
                || !original.copyId().equals(updated.copyId())
                || !original.borrowTime().equals(updated.borrowTime())) {
            throw new IllegalArgumentException("Invalid borrow-record state transition.");
        }
        if (original.status() == BorrowStatus.BORROWED) {
            boolean returned = updated.status() == BorrowStatus.RETURNED
                    && original.dueTime().equals(updated.dueTime())
                    && original.renewalCount() == updated.renewalCount();
            boolean renewed = updated.status() == BorrowStatus.BORROWED
                    && updated.returnTime() == null
                    && updated.dueTime().isAfter(original.dueTime())
                    && updated.renewalCount() == original.renewalCount() + 1;
            if (!(returned || renewed)) {
                throw new IllegalArgumentException("Invalid borrow-record state transition.");
            }
            return;
        }
        // A closed record is immutable except for settling its fee exactly once.
        boolean settled = updated.status() == BorrowStatus.RETURNED
                && original.feeSettledAt() == null
                && updated.feeSettledAt() != null
                && original.dueTime().equals(updated.dueTime())
                && original.renewalCount() == updated.renewalCount()
                && Objects.equals(original.returnTime(), updated.returnTime())
                && Objects.equals(original.lostReportedAt(), updated.lostReportedAt());
        if (!settled) {
            throw new IllegalArgumentException("Invalid borrow-record state transition.");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
