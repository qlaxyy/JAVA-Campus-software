package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Read-only library category supplied by the server. */
public final class BookCategoryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String categoryId;
    private final String categoryName;

    /**
     * Creates the transfer object.
     * @param categoryId stable category identifier
     * @param categoryName display name
     */
    public BookCategoryDTO(String categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    /** @return stable category identifier */
    public String getCategoryId() { return categoryId; }

    /** @return display name */
    public String getCategoryName() { return categoryName; }
}
