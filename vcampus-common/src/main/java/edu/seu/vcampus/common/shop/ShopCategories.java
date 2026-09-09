package edu.seu.vcampus.common.shop;

import java.util.List;

/**
 * Seed campus-shop categories. Extra categories are created with {@code SHOP.ADD_CATEGORY}.
 */
public final class ShopCategories {

    public static final long STATIONERY = 1L;
    public static final long DAILY = 2L;
    public static final long FOOD = 3L;

    private ShopCategories() {
    }

    /**
     * Built-in categories used by the demo catalog.
     *
     * @return stationery, daily goods and food
     */
    public static List<ShopCategoryDto> seed() {
        return List.of(
                new ShopCategoryDto(STATIONERY, "文具"),
                new ShopCategoryDto(DAILY, "日常用品"),
                new ShopCategoryDto(FOOD, "食品"));
    }

    /**
     * Resolves a seed category key to the storefront label.
     *
     * @param categoryId catalog category key
     * @return display name
     */
    public static String nameOf(long categoryId) {
        for (ShopCategoryDto category : seed()) {
            if (category.getCategoryId() == categoryId) {
                return category.getName();
            }
        }
        throw new IllegalArgumentException("unknown categoryId");
    }
}
