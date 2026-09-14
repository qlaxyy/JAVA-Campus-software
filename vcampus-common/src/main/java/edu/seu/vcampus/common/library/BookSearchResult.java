package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Type-safe response data returned by a successful book search. */
public final class BookSearchResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final List<BookDTO> books;
    private final int totalCount;
    private final int page;
    private final int pageSize;

    /**
     * Creates an immutable page of a book-search result.
     *
     * @param books matching books on this page
     * @param totalCount matches in the whole result, across every page
     * @param page 1-based number of this page
     * @param pageSize results requested per page
     */
    public BookSearchResult(List<BookDTO> books, int totalCount, int page, int pageSize) {
        this.books = List.copyOf(books);
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    /** @return immutable matching-book list for this page */
    public List<BookDTO> getBooks() {
        return books;
    }

    /** @return total matches across every page, not just this one */
    public int getTotalCount() {
        return totalCount;
    }

    /** @return 1-based number of this page */
    public int getPage() { return page; }

    /** @return results requested per page */
    public int getPageSize() { return pageSize; }

    /** @return page count needed to show every match; an empty result still reports one page */
    public int getTotalPages() {
        return pageSize <= 0 ? 1 : Math.max(1, (totalCount + pageSize - 1) / pageSize);
    }

    /** @return whether more matches exist after this page */
    public boolean hasNextPage() {
        return page < getTotalPages();
    }
}
