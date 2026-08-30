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

    public String getBookId() {
        return bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getAvailableCount() {
        return availableCount;
    }
}
