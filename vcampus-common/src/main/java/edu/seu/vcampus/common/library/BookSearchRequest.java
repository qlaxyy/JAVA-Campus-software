package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Catalog criteria. Empty keyword lists books; a category filter is optional. */
public final class BookSearchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final String keyword;
    private final String categoryId;

    /**
     * Creates keyword criteria for one book search.
     *
     * @param keyword title, author, ISBN or category keyword; null/blank lists all matches
     * @param categoryId optional category filter; null means all categories
     */
    public BookSearchRequest(String keyword, String categoryId) {
        this.keyword = keyword;
        this.categoryId = categoryId;
    }

    /** @return user-entered title, author, ISBN or category keyword */
    public String getKeyword() {
        return keyword;
    }

    /** @return optional category identifier */
    public String getCategoryId() { return categoryId; }
}
