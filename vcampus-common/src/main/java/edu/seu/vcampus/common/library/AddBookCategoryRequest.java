package edu.seu.vcampus.common.library;

import java.io.Serial;
import java.io.Serializable;

/** Adds a reusable library classification; the server generates its identifier. */
public final class AddBookCategoryRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String categoryName;

    public AddBookCategoryRequest(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
