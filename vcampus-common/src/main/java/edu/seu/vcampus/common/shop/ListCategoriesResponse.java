package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Category list returned by {@code SHOP.LIST_CATEGORIES}.
 */
public final class ListCategoriesResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<ShopCategoryDto> categories;

    /**
     * Creates a category-list payload.
     *
     * @param categories catalog order
     */
    public ListCategoriesResponse(List<ShopCategoryDto> categories) {
        Objects.requireNonNull(categories, "categories must not be null");
        this.categories = new ArrayList<>(categories);
    }

    public List<ShopCategoryDto> getCategories() {
        return List.copyOf(categories);
    }
}
