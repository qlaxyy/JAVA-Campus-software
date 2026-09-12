package edu.seu.vcampus.common.shop;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Complete shopping cart returned by the server. */
public final class ShoppingCartView implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final ArrayList<CartItemDto> items;

    public ShoppingCartView(List<CartItemDto> items) {
        Objects.requireNonNull(items, "items must not be null");
        this.items = new ArrayList<>(items);
    }

    public List<CartItemDto> getItems() { return List.copyOf(items); }
}
