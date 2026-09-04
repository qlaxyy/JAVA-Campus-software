package edu.seu.vcampus.common.shop;

/**
 * Fixed campus-shop categories used by listing and the publish form.
 */
public final class ShopCategories {

    public static final long STATIONERY = 1L;
    public static final long DAILY = 2L;
    public static final long FOOD = 3L;

    private ShopCategories() {
    }

    /**
     * Resolves a category key to the storefront label.
     *
     * @param categoryId catalog category key
     * @return display name
     */
    public static String nameOf(long categoryId) {
        if (categoryId == STATIONERY) {
            return "文具";
        }
        if (categoryId == DAILY) {
            return "日常用品";
        }
        if (categoryId == FOOD) {
            return "食品";
        }
        throw new IllegalArgumentException("unknown categoryId");
    }

    /**
     * Reports whether a category can be used when publishing.
     *
     * @param categoryId catalog category key
     * @return {@code true} for stationery, daily goods or food
     */
    public static boolean isSupported(long categoryId) {
        return categoryId == STATIONERY || categoryId == DAILY || categoryId == FOOD;
    }
}
