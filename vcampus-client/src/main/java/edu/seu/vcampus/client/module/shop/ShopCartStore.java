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
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (line.product().getProductId() == product.getProductId()) {
                lines.set(index, new Line(product, line.quantity() + 1));
                notifyListeners();
                return;
            }
        }
        lines.add(new Line(product, 1));
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
