package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;

/** Sets the absolute quantity of one product; zero removes it. */
public final class SetCartItemRequest implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final long productId;
    private final int quantity;

    public SetCartItemRequest(long productId, int quantity) {
        if (productId < 1 || quantity < 0) throw new IllegalArgumentException("invalid cart item");
        this.productId = productId;
        this.quantity = quantity;
    }

    public long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
