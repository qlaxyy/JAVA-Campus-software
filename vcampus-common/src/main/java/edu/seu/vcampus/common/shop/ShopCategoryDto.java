package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * One shop category row.
 */
public final class ShopCategoryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long categoryId;
    private final String name;

    /**
     * Creates a category snapshot.
     *
     * @param categoryId catalog key
     * @param name display name
     */
    public ShopCategoryDto(long categoryId, String name) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be positive");
        }
        this.categoryId = categoryId;
        this.name = requireText(name);
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "name must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (trimmed.length() > 40) {
            throw new IllegalArgumentException("name is too long");
        }
        return trimmed;
    }
}
