package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** One row of the most-borrowed ranking inside {@link LibraryStatisticsDTO}. */
public final class PopularBookDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;
    private final String title;
    private final int borrowCount;

    public PopularBookDTO(String bookId, String title, int borrowCount) {
        this.bookId = bookId;
        this.title = title;
        this.borrowCount = borrowCount;
    }

    public String getBookId() { return bookId; }
    public String getTitle() { return title; }

    /** @return number of borrow records for this title, active and returned alike */
    public int getBorrowCount() { return borrowCount; }
}
