package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One catalog row returned to the client, including photos and seller copy.
 */
public final class ProductSummaryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    public static final int MIN_PHOTOS = 1;
    public static final int MAX_PHOTOS = 9;
    public static final int MAX_PHOTO_BYTES = 400_000;

    private final long productId;
    private final long categoryId;
    private final String categoryName;
    private final String name;
    private final String description;
    private final String sellerName;
    private final int priceFen;
    private final int stockQty;
    private final ProductSaleStatus saleStatus;
    private final ArrayList<byte[]> photos;

    /**
     * Creates an immutable product summary.
     *
     * @param productId catalog key
     * @param categoryId owning category
     * @param categoryName display name of the category
     * @param name product title
     * @param description seller description
     * @param sellerName publisher display name
     * @param priceFen unit price in fen
     * @param stockQty remaining quantity
     * @param saleStatus sale flag copied from the catalog
     * @param photos at least one product photo
     */
    public ProductSummaryDto(
            long productId,
            long categoryId,
            String categoryName,
            String name,
            String description,
            String sellerName,
            int priceFen,
            int stockQty,
            ProductSaleStatus saleStatus,
            List<byte[]> photos) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = requireText(categoryName, "categoryName");
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        this.sellerName = requireText(sellerName, "sellerName");
        if (priceFen <= 0) {
            throw new IllegalArgumentException("priceFen must be positive");
        }
        if (stockQty < 0) {
            throw new IllegalArgumentException("stockQty must not be negative");
        }
        this.priceFen = priceFen;
        this.stockQty = stockQty;
        this.saleStatus = Objects.requireNonNull(saleStatus, "saleStatus must not be null");
        this.photos = copyPhotos(photos);
    }

    /** @return catalog key */
    public long getProductId() {
        return productId;
    }

    /** @return owning category */
    public long getCategoryId() {
        return categoryId;
    }

    /** @return category display name */
    public String getCategoryName() {
        return categoryName;
    }

    /** @return product title */
    public String getName() {
        return name;
    }

    /** @return seller description */
    public String getDescription() {
        return description;
    }

    /** @return publisher display name */
    public String getSellerName() {
        return sellerName;
    }

    /** @return unit price in fen */
    public int getPriceFen() {
        return priceFen;
    }

    /** @return remaining quantity */
    public int getStockQty() {
        return stockQty;
    }

    /** @return sale flag */
    public ProductSaleStatus getSaleStatus() {
        return saleStatus;
    }

    /**
     * Returns a copy with a different remaining quantity.
     *
     * @param stockQty remaining quantity
     * @return updated catalog row
     */
    public ProductSummaryDto withStockQty(int stockQty) {
        return withCatalog(name, description, priceFen, stockQty);
    }

    /**
     * Returns a copy with edited merchant fields.
     *
     * @param name product title
     * @param description seller copy
     * @param priceFen unit price
     * @param stockQty remaining quantity
     * @return updated catalog row
     */
    public ProductSummaryDto withCatalog(String name, String description, int priceFen, int stockQty) {
        return new ProductSummaryDto(
                productId,
                categoryId,
                categoryName,
                name,
                description,
                sellerName,
                priceFen,
                stockQty,
                saleStatus,
                photos);
    }

    /** @return product photos in display order */
    public List<byte[]> getPhotos() {
        return List.copyOf(photos);
    }

    /** @return first photo used as the feed cover */
    public byte[] getCoverPhoto() {
        return photos.getFirst().clone();
    }

    static ArrayList<byte[]> copyPhotos(List<byte[]> photos) {
        if (photos == null || photos.size() < MIN_PHOTOS || photos.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("photos must contain 1 to 9 images");
        }
        ArrayList<byte[]> copy = new ArrayList<>(photos.size());
        for (byte[] photo : photos) {
            if (photo == null || photo.length == 0) {
                throw new IllegalArgumentException("photo must not be empty");
            }
            if (photo.length > MAX_PHOTO_BYTES) {
                throw new IllegalArgumentException("photo is too large");
            }
            copy.add(photo.clone());
        }
        return copy;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
