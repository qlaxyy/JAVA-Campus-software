package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Shop-admin request to create a product category.
 */
public final class AddCategoryRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * Creates an add-category request.
     *
     * @param name unique display name, 1–40 characters
     */
    public AddCategoryRequest(String name) {
        Objects.requireNonNull(name, "name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (trimmed.length() > 40) {
            throw new IllegalArgumentException("name is too long");
        }
        this.name = trimmed;
    }

    public String getName() {
        return name;
    }
}
