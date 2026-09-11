package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ShopOrderDto;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for shop orders and immutable order-line snapshots. */
interface ShopOrderRepository {
    String nextOrderId();
    void save(ShopOrderDto order);
    Optional<ShopOrderDto> findById(String orderId);
    List<ShopOrderDto> listByUser(String userId);
    List<ShopOrderDto> listAllNewestFirst();
}
