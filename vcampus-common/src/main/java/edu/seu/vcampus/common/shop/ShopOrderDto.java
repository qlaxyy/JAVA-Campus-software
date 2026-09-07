package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One customer or merchant order after campus-card payment.
 */
public final class ShopOrderDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String orderId;
    private final String userId;
    private final String buyerName;
    private final ShopOrderStatus status;
    private final String paymentMethod;
    private final String fulfillHint;
    private final int totalFen;
    private final String createdAt;
    private final ArrayList<OrderItemDto> items;

    /**
     * Creates an order snapshot.
     *
     * @param orderId public order number
     * @param userId buyer
     * @param buyerName display name
     * @param status paid or cancelled
     * @param paymentMethod checkout channel
     * @param fulfillHint pickup note
     * @param totalFen payable amount
     * @param createdAt ISO local date-time
     * @param items line snapshots
     */
    public ShopOrderDto(
            String orderId,
            String userId,
            String buyerName,
            ShopOrderStatus status,
            String paymentMethod,
            String fulfillHint,
            int totalFen,
            String createdAt,
            List<OrderItemDto> items) {
        this.orderId = requireText(orderId, "orderId");
        this.userId = requireText(userId, "userId");
        this.buyerName = requireText(buyerName, "buyerName");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.paymentMethod = requireText(paymentMethod, "paymentMethod");
        this.fulfillHint = requireText(fulfillHint, "fulfillHint");
        if (totalFen <= 0) {
            throw new IllegalArgumentException("totalFen must be positive");
        }
        this.totalFen = totalFen;
        this.createdAt = requireText(createdAt, "createdAt");
        Objects.requireNonNull(items, "items must not be null");
        this.items = new ArrayList<>(items);
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public ShopOrderStatus getStatus() {
        return status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getFulfillHint() {
        return fulfillHint;
    }

    public int getTotalFen() {
        return totalFen;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemDto> getItems() {
        return List.copyOf(items);
    }

    public ShopOrderDto withStatus(ShopOrderStatus status) {
        return new ShopOrderDto(
                orderId,
                userId,
                buyerName,
                status,
                paymentMethod,
                fulfillHint,
                totalFen,
                createdAt,
                items);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
