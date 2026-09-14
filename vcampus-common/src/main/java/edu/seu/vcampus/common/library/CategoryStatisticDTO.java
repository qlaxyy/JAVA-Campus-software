package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** One row of the per-category holdings breakdown inside {@link LibraryStatisticsDTO}. */
public final class CategoryStatisticDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String categoryName;
    private final int bookCount;
    private final int copyCount;

    public CategoryStatisticDTO(String categoryName, int bookCount, int copyCount) {
        this.categoryName = categoryName;
        this.bookCount = bookCount;
        this.copyCount = copyCount;
    }

    public String getCategoryName() { return categoryName; }

    /** @return number of catalog records in this category */
    public int getBookCount() { return bookCount; }

    /** @return number of copies in this category, withdrawn ones excluded */
    public int getCopyCount() { return copyCount; }
}
