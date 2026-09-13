package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** One server-side shopping-cart line with the current product view. */
public final class CartItemDto implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final ProductSummaryDto product;
    private final int quantity;

    public CartItemDto(ProductSummaryDto product, int quantity) {
        this.product = Objects.requireNonNull(product, "product must not be null");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
        this.quantity = quantity;
    }

    public ProductSummaryDto getProduct() { return product; }
    public int getQuantity() { return quantity; }
}
