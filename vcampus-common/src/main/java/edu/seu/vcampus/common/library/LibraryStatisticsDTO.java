package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of whole-library statistics for the administrator workbench.
 *
 * <p>Every counter is derived at query time from books, copies, borrow records and reservations;
 * nothing is stored, matching the module's "compute, do not persist" approach to derived values.
 */
public final class LibraryStatisticsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int bookCount;
    private final int copyCount;
    private final int availableCount;
    private final int withdrawnCount;
    private final int activeBorrowCount;
    private final int overdueCount;
    private final int historyBorrowCount;
    private final int waitingReservationCount;
    private final int readyReservationCount;
    private final int unpaidFeeCount;
    private final int unpaidFeeFen;
    private final int settledFeeFen;
    private final List<CategoryStatisticDTO> categories;
    private final List<PopularBookDTO> popularBooks;

    public LibraryStatisticsDTO(int bookCount, int copyCount, int availableCount,
            int withdrawnCount, int activeBorrowCount, int overdueCount, int historyBorrowCount,
            int waitingReservationCount, int readyReservationCount,
            int unpaidFeeCount, int unpaidFeeFen, int settledFeeFen,
            List<CategoryStatisticDTO> categories, List<PopularBookDTO> popularBooks) {
        this.bookCount = bookCount;
        this.copyCount = copyCount;
        this.availableCount = availableCount;
        this.withdrawnCount = withdrawnCount;
        this.activeBorrowCount = activeBorrowCount;
        this.overdueCount = overdueCount;
        this.historyBorrowCount = historyBorrowCount;
        this.waitingReservationCount = waitingReservationCount;
        this.readyReservationCount = readyReservationCount;
        this.unpaidFeeCount = unpaidFeeCount;
        this.unpaidFeeFen = unpaidFeeFen;
        this.settledFeeFen = settledFeeFen;
        this.categories = List.copyOf(categories);
        this.popularBooks = List.copyOf(popularBooks);
    }

    /** @return number of catalog records, active and inactive alike */
    public int getBookCount() { return bookCount; }

    /** @return physical copies still in the collection (withdrawn ones excluded) */
    public int getCopyCount() { return copyCount; }

    /** @return copies currently borrowable, respecting the catalog status */
    public int getAvailableCount() { return availableCount; }

    /** @return logically withdrawn copies kept only for history */
    public int getWithdrawnCount() { return withdrawnCount; }

    /** @return loans that have not been returned */
    public int getActiveBorrowCount() { return activeBorrowCount; }

    /** @return active loans past their due moment */
    public int getOverdueCount() { return overdueCount; }

    /** @return loans that have been returned */
    public int getHistoryBorrowCount() { return historyBorrowCount; }

    /** @return reservations still queueing */
    public int getWaitingReservationCount() { return waitingReservationCount; }

    /** @return reservations whose copy is held for pickup */
    public int getReadyReservationCount() { return readyReservationCount; }

    /** @return number of records with an unsettled fee */
    public int getUnpaidFeeCount() { return unpaidFeeCount; }

    /** @return total unsettled fee in fen */
    public int getUnpaidFeeFen() { return unpaidFeeFen; }

    /** @return total fee already settled in fen */
    public int getSettledFeeFen() { return settledFeeFen; }

    /** @return per-category holdings, largest first */
    public List<CategoryStatisticDTO> getCategories() { return categories; }

    /** @return most-borrowed titles, largest first */
    public List<PopularBookDTO> getPopularBooks() { return popularBooks; }
}
