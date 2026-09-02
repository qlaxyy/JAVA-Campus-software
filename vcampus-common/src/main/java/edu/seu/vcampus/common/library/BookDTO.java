package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Serializable summary of one searchable library book. */
public final class BookDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String bookId;
    private final String isbn;
    private final String title;
    private final String author;
    private final String category;
    private final int totalCount;
    private final int availableCount;

    /**
     * Creates an immutable summary of one searchable book.
     *
     * @param bookId stable library book identifier
     * @param isbn international standard book number
     * @param title display title
     * @param author display author
     * @param category library category
     * @param totalCount total number of copies
     * @param availableCount copies currently available to borrow
     */
    public BookDTO(
            String bookId,
            String isbn,
            String title,
            String author,
            String category,
            int totalCount,
            int availableCount) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.totalCount = totalCount;
        this.availableCount = availableCount;
    }

    /** @return stable library book identifier */
    public String getBookId() {
        return bookId;
    }

    /** @return international standard book number */
    public String getIsbn() {
        return isbn;
    }

    /** @return display title */
    public String getTitle() {
        return title;
    }

    /** @return display author */
    public String getAuthor() {
        return author;
    }

    /** @return library category */
    public String getCategory() {
        return category;
    }

    /** @return total number of copies */
    public int getTotalCount() {
        return totalCount;
    }

    /** @return copies currently available to borrow */
    public int getAvailableCount() {
        return availableCount;
    }
}
