package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Admin request to put a new product on sale.
 */
public final class PublishProductRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final long categoryId;
    private final String description;
    private final int priceFen;
    private final int stockQty;
    private final ArrayList<byte[]> photos;

    /**
     * Creates a publish request.
     *
     * @param name product title
     * @param categoryId stationery, daily goods or food
     * @param description seller copy
     * @param priceFen unit price in fen
     * @param stockQty quantity to sell
     * @param photos at least one photo
     */
    public PublishProductRequest(
            String name,
            long categoryId,
            String description,
            int priceFen,
            int stockQty,
            List<byte[]> photos) {
        this.name = requireText(name, "name");
        if (!ShopCategories.isSupported(categoryId)) {
            throw new IllegalArgumentException("categoryId is not supported");
        }
        this.categoryId = categoryId;
        this.description = requireText(description, "description");
        if (priceFen <= 0) {
            throw new IllegalArgumentException("priceFen must be positive");
        }
        if (stockQty < 1) {
            throw new IllegalArgumentException("stockQty must be at least 1");
        }
        this.priceFen = priceFen;
        this.stockQty = stockQty;
        this.photos = ProductSummaryDto.copyPhotos(photos);
    }

    /** @return product title */
    public String getName() {
        return name;
    }

    /** @return category key */
    public long getCategoryId() {
        return categoryId;
    }

    /** @return seller copy */
    public String getDescription() {
        return description;
    }

    /** @return unit price in fen */
    public int getPriceFen() {
        return priceFen;
    }

    /** @return quantity to sell */
    public int getStockQty() {
        return stockQty;
    }

    /** @return product photos */
    public List<byte[]> getPhotos() {
        return List.copyOf(photos);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
