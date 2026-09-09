package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;

/**
 * Filter for listing on-sale products. Null or blank fields mean "no filter".
 */
public final class ListProductsRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String keyword;
    private final Long categoryId;

    /**
     * Creates a list-products filter.
     *
     * @param keyword optional title or description substring, may be {@code null}
     * @param categoryId optional category key, may be {@code null}
     */
    public ListProductsRequest(String keyword, Long categoryId) {
        this.keyword = normalizeKeyword(keyword);
        this.categoryId = categoryId;
    }

    /** @return a request that lists every on-sale product */
    public static ListProductsRequest allOnSale() {
        return new ListProductsRequest(null, null);
    }

    /** @return trimmed keyword, or {@code null} when unused */
    public String getKeyword() {
        return keyword;
    }

    /** @return category filter, or {@code null} when unused */
    public Long getCategoryId() {
        return categoryId;
    }

    private static String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
