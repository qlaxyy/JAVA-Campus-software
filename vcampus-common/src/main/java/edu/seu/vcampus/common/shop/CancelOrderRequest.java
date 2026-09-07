package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Buyer request to cancel a paid order and refund the campus card.
 */
public final class CancelOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String orderId;

    /**
     * Creates a cancel request.
     *
     * @param orderId public order number
     */
    public CancelOrderRequest(String orderId) {
        this.orderId = requireText(orderId, "orderId");
    }

    public String getOrderId() {
        return orderId;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
