package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Checkout payload paid with the campus-card action.
 */
public final class CreateOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<OrderLineRequest> lines;
    private final String paymentMethod;
    private final String fulfillHint;

    /**
     * Creates a checkout request.
     *
     * @param lines products to buy
     * @param paymentMethod must be {@link ShopPaymentMethods#CAMPUS_CARD}
     * @param fulfillHint pickup or delivery note
     */
    public CreateOrderRequest(List<OrderLineRequest> lines, String paymentMethod, String fulfillHint) {
        Objects.requireNonNull(lines, "lines must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        this.lines = new ArrayList<>(lines);
        this.paymentMethod = requireText(paymentMethod, "paymentMethod");
        this.fulfillHint = requireText(fulfillHint, "fulfillHint");
    }

    public List<OrderLineRequest> getLines() {
        return List.copyOf(lines);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getFulfillHint() {
        return fulfillHint;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
