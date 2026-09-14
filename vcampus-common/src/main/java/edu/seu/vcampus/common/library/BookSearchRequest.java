package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/**
 * Catalog criteria. An empty keyword lists books; category filter and paging are optional.
 *
 * <p>The catalog grows without bound, so the online catalog is the one library list that needs
 * paging. Borrow history and a single book's copies are both bounded by their own rules and are
 * returned whole.
 */
public final class BookSearchRequest implements Serializable {

    /** Results per page when the caller does not page explicitly. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Largest page the server accepts, so one request cannot pull the whole catalog. */
    public static final int MAX_PAGE_SIZE = 100;

    @Serial
    private static final long serialVersionUID = 3L;

    private final String keyword;
    private final String categoryId;
    private final int page;
    private final int pageSize;

    /**
     * Creates criteria for the first page.
     *
     * @param keyword title, author, ISBN, shelf mark or barcode keyword; null/blank lists all
     * @param categoryId optional category filter; null means all categories
     */
    public BookSearchRequest(String keyword, String categoryId) {
        this(keyword, categoryId, 1, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param keyword title, author, ISBN, shelf mark or barcode keyword; null/blank lists all
     * @param categoryId optional category filter; null means all categories
     * @param page 1-based page number
     * @param pageSize results per page, at most {@link #MAX_PAGE_SIZE}
     */
    public BookSearchRequest(String keyword, String categoryId, int page, int pageSize) {
        this.keyword = keyword;
        this.categoryId = categoryId;
        this.page = page;
        this.pageSize = pageSize;
    }

    /** @return user-entered title, author, ISBN, shelf mark or barcode keyword */
    public String getKeyword() {
        return keyword;
    }

    /** @return optional category identifier */
    public String getCategoryId() { return categoryId; }

    /** @return 1-based page number */
    public int getPage() { return page; }

    /** @return requested results per page */
    public int getPageSize() { return pageSize; }

    /** @return the same criteria moved to another page */
    public BookSearchRequest onPage(int newPage) {
        return new BookSearchRequest(keyword, categoryId, newPage, pageSize);
    }
}
