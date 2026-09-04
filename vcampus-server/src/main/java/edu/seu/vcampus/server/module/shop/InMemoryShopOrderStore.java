package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ShopOrderDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-memory shop orders until Access persistence exists. */
final class InMemoryShopOrderStore {

    private final List<ShopOrderDto> orders = new ArrayList<>();
    private long nextId = 1001L;

    synchronized String nextOrderId() {
        return "SO-" + nextId++;
    }

    synchronized void save(ShopOrderDto order) {
        for (int index = 0; index < orders.size(); index++) {
            if (orders.get(index).getOrderId().equals(order.getOrderId())) {
                orders.set(index, order);
                return;
            }
        }
        orders.add(order);
    }

    synchronized Optional<ShopOrderDto> findById(String orderId) {
        for (ShopOrderDto order : orders) {
            if (order.getOrderId().equals(orderId)) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    synchronized List<ShopOrderDto> listByUser(String userId) {
        List<ShopOrderDto> matches = new ArrayList<>();
        for (int index = orders.size() - 1; index >= 0; index--) {
            ShopOrderDto order = orders.get(index);
            if (order.getUserId().equals(userId)) {
                matches.add(order);
            }
        }
        return List.copyOf(matches);
    }

    synchronized List<ShopOrderDto> listAllNewestFirst() {
        List<ShopOrderDto> copy = new ArrayList<>();
        for (int index = orders.size() - 1; index >= 0; index--) {
            copy.add(orders.get(index));
        }
        return List.copyOf(copy);
    }
}
