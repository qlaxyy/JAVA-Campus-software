package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.shop.ProductSummaryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side cart used by the first UI slice. Server persistence comes later.
 */
final class ShopCartStore {

    record Line(ProductSummaryDto product, int quantity) {
        int subtotalFen() {
            return product.getPriceFen() * quantity;
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

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
                lines.set(index, new Line(product, next));
                notifyListeners();
                return;
            }
        }
        lines.add(new Line(product, extra));
        notifyListeners();
    }

    void setQuantity(long productId, int quantity) {
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (line.product().getProductId() != productId) {
                continue;
            }
            if (quantity < 1) {
                lines.remove(index);
            } else {
                int next = Math.min(line.product().getStockQty(), quantity);
                lines.set(index, new Line(line.product(), next));
            }
            notifyListeners();
            return;
        }
    }

    void remove(long productId) {
        setQuantity(productId, 0);
    }

    void clear() {
        lines.clear();
        notifyListeners();
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
}
