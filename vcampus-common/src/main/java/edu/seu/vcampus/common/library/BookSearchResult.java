package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Type-safe response data returned by a successful book search. */
public final class BookSearchResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<BookDTO> books;

    /**
     * Creates an immutable book-search result.
     *
     * @param books matching books
     */
    public BookSearchResult(List<BookDTO> books) {
        this.books = List.copyOf(books);
    }

    /** @return immutable matching-book list */
    public List<BookDTO> getBooks() {
        return books;
    }
}
