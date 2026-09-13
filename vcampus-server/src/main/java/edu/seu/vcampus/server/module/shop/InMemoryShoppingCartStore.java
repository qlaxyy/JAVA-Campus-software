package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.CartItemDto;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShoppingCartView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory cart used only by isolated tests. */
final class InMemoryShoppingCartStore implements ShoppingCartRepository {
    private final ShopCatalogRepository catalog;
    private final Map<String, Map<Long, Integer>> carts = new LinkedHashMap<>();

    InMemoryShoppingCartStore(ShopCatalogRepository catalog) { this.catalog = catalog; }

    public synchronized ShoppingCartView view(String userId) {
        List<CartItemDto> lines = carts.getOrDefault(userId, Map.of()).entrySet().stream()
                .map(entry -> new CartItemDto(catalog.findById(entry.getKey()).orElseThrow(), entry.getValue()))
                .toList();
        return new ShoppingCartView(lines);
    }

    public synchronized ShoppingCartView setQuantity(String userId, long productId, int quantity) {
        ProductSummaryDto product = catalog.findById(productId).orElseThrow();
        Map<Long, Integer> cart = carts.computeIfAbsent(userId, ignored -> new LinkedHashMap<>());
        if (quantity == 0) cart.remove(productId);
        else cart.put(productId, Math.min(quantity, product.getStockQty()));
        return view(userId);
    }

    public synchronized ShoppingCartView clear(String userId) {
        carts.remove(userId);
        return new ShoppingCartView(List.of());
    }
}
