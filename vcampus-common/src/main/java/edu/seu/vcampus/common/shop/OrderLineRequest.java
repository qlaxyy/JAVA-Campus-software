package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;

/**
 * One product quantity in a checkout request.
 */
public final class OrderLineRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long productId;
    private final int quantity;

    /**
     * Creates an order line.
     *
     * @param productId catalog key
     * @param quantity units to buy
     */
    public OrderLineRequest(long productId, int quantity) {
        if (productId <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
