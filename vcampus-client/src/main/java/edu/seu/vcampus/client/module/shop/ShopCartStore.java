package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CartItemDto;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.SetCartItemRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShoppingCartView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local view of the server-side cart. Mutations are stored in the shared Access database.
 */
final class ShopCartStore {

    record Line(ProductSummaryDto product, int quantity) {
        int subtotalFen() {
            return product.getPriceFen() * quantity;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final ClientContext context;

    ShopCartStore(ClientContext context) {
        this.context = context;
    }

    void reload() {
        sync(ShopActions.GET_CART, null);
    }

    void add(ProductSummaryDto product) {
        add(product, 1);
    }

    void add(ProductSummaryDto product, int quantity) {
        int extra = Math.max(1, quantity);
        int stock = Math.max(0, product.getStockQty());
        if (stock == 0) {
            return;
        }
        extra = Math.min(extra, stock);
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (line.product().getProductId() == product.getProductId()) {
                int next = Math.min(stock, line.quantity() + extra);
                sync(ShopActions.SET_CART_ITEM, new SetCartItemRequest(product.getProductId(), next));
                return;
            }
        }
        sync(ShopActions.SET_CART_ITEM, new SetCartItemRequest(product.getProductId(), extra));
    }

    void setQuantity(long productId, int quantity) {
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (line.product().getProductId() != productId) {
                continue;
            }
            int next = quantity < 1 ? 0 : Math.min(line.product().getStockQty(), quantity);
            sync(ShopActions.SET_CART_ITEM, new SetCartItemRequest(productId, next));
            return;
        }
    }

    void remove(long productId) {
        setQuantity(productId, 0);
    }

    void clear() {
        sync(ShopActions.CLEAR_CART, null);
    }

    List<Line> lines() {
        return List.copyOf(lines);
    }

    int totalFen() {
        int total = 0;
        for (Line line : lines) {
            total += line.subtotalFen();
        }
        return total;
    }

    int itemCount() {
        int count = 0;
        for (Line line : lines) {
            count += line.quantity();
        }
        return count;
    }

    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private void sync(String action, java.io.Serializable request) {
        try {
            Response response = context.send(action, request);
            if (response.isSuccess() && response.getData() instanceof ShoppingCartView cart) {
                lines.clear();
                for (CartItemDto item : cart.getItems()) {
                    lines.add(new Line(item.getProduct(), item.getQuantity()));
                }
                notifyListeners();
            }
        } catch (IOException ignored) {
            // The surrounding module already owns connection-error presentation.
        }
    }
}
