package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Shop-admin request to change an on-sale product's price, copy or stock.
 */
public final class UpdateProductRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long productId;
    private final String name;
    private final String description;
    private final int priceFen;
    private final int addStockQty;

    /**
     * Creates an update request.
     *
     * @param productId catalog key
     * @param name title
     * @param description seller copy
     * @param priceFen unit price in fen
     * @param addStockQty extra units to add; may be zero
     */
    public UpdateProductRequest(
            long productId,
            String name,
            String description,
            int priceFen,
            int addStockQty) {
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
        this.productId = productId;
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        if (priceFen <= 0) {
            throw new IllegalArgumentException("priceFen must be positive");
        }
        if (addStockQty < 0) {
            throw new IllegalArgumentException("addStockQty must not be negative");
        }
        this.priceFen = priceFen;
        this.addStockQty = addStockQty;
    }

    public long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriceFen() {
        return priceFen;
    }

    public int getAddStockQty() {
        return addStockQty;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
