package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Snapshot of one paid order line.
 */
public final class OrderItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long productId;
    private final String name;
    private final int unitPriceFen;
    private final int quantity;
    private final int subtotalFen;

    /**
     * Creates an order-item snapshot.
     *
     * @param productId catalog key
     * @param name title at checkout
     * @param unitPriceFen unit price at checkout
     * @param quantity units bought
     * @param subtotalFen line total
     */
    public OrderItemDto(long productId, String name, int unitPriceFen, int quantity, int subtotalFen) {
        this.productId = productId;
        this.name = requireText(name, "name");
        if (unitPriceFen <= 0 || quantity < 1 || subtotalFen <= 0) {
            throw new IllegalArgumentException("order item amounts are invalid");
        }
        this.unitPriceFen = unitPriceFen;
        this.quantity = quantity;
        this.subtotalFen = subtotalFen;
    }

    public long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getUnitPriceFen() {
        return unitPriceFen;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getSubtotalFen() {
        return subtotalFen;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
