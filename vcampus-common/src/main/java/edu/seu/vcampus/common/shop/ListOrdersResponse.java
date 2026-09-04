package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orders returned by {@code SHOP.LIST_ORDERS} or {@code SHOP.LIST_SALES}.
 */
public final class ListOrdersResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<ShopOrderDto> orders;

    /**
     * Creates an order list payload.
     *
     * @param orders matching rows
     */
    public ListOrdersResponse(List<ShopOrderDto> orders) {
        Objects.requireNonNull(orders, "orders must not be null");
        this.orders = new ArrayList<>(orders);
    }

    public List<ShopOrderDto> getOrders() {
        return List.copyOf(orders);
    }
}
