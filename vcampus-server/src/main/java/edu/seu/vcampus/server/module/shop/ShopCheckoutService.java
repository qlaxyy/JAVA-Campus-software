package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.CancelOrderRequest;
import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.OrderItemDto;
import edu.seu.vcampus.common.shop.OrderLineRequest;
import edu.seu.vcampus.common.shop.ProductSaleStatus;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.RechargeCampusCardRequest;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopOrderStatus;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;
import edu.seu.vcampus.common.user.SessionInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Campus-card wallet, checkout, cancel and refund. Memory-backed until Access DAOs exist.
 */
final class ShopCheckoutService {

    private final InMemoryShopCatalog catalog;
    private final InMemoryCampusCardStore cards;
    private final InMemoryShopOrderStore orders;
    private final Object lock = new Object();

    ShopCheckoutService(
            InMemoryShopCatalog catalog,
            InMemoryCampusCardStore cards,
            InMemoryShopOrderStore orders) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.cards = Objects.requireNonNull(cards, "cards must not be null");
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
    }

    CampusCardView card(SessionInfo session) {
        return cards.view(session);
    }

    CampusCardView recharge(SessionInfo session, RechargeCampusCardRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        synchronized (lock) {
            return cards.recharge(session, request.getAmountFen());
        }
    }

    ShopOrderDto createOrder(SessionInfo session, CreateOrderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!ShopPaymentMethods.CAMPUS_CARD.equals(request.getPaymentMethod())) {
            throw new ShopBusinessException(
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "当前仅支持校园卡支付。");
        }
        Map<Long, Integer> quantities = merge(request.getLines());
        synchronized (lock) {
            List<OrderItemDto> items = new ArrayList<>();
            int totalFen = 0;
            for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                ProductSummaryDto product = catalog.findById(entry.getKey()).orElseThrow(() ->
                        new ShopBusinessException(ErrorCodes.SHOP_OUT_OF_STOCK, "商品不存在或已下架。"));
                if (product.getSaleStatus() != ProductSaleStatus.ON_SALE
                        || product.getStockQty() < entry.getValue()) {
                    throw new ShopBusinessException(
                            ErrorCodes.SHOP_OUT_OF_STOCK,
                            "库存不足，请减少数量后再试。");
                }
                int subtotal = product.getPriceFen() * entry.getValue();
                items.add(new OrderItemDto(
                        product.getProductId(),
                        product.getName(),
                        product.getPriceFen(),
                        entry.getValue(),
                        subtotal));
                totalFen += subtotal;
            }
            cards.deduct(session, totalFen);
            List<Long> reserved = new ArrayList<>();
            try {
                for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                    if (!catalog.decrementStock(entry.getKey(), entry.getValue())) {
                        throw new ShopBusinessException(
                                ErrorCodes.SHOP_OUT_OF_STOCK,
                                "库存不足，请减少数量后再试。");
                    }
                    reserved.add(entry.getKey());
                }
            } catch (RuntimeException exception) {
                cards.refund(session.getUserId(), totalFen);
                for (int index = 0; index < reserved.size(); index++) {
                    Long productId = reserved.get(index);
                    catalog.incrementStock(productId, quantities.get(productId));
                }
                throw exception;
            }
            ShopOrderDto order = new ShopOrderDto(
                    orders.nextOrderId(),
                    session.getUserId(),
                    session.getDisplayName(),
                    ShopOrderStatus.PAID,
                    ShopPaymentMethods.CAMPUS_CARD,
                    request.getFulfillHint(),
                    totalFen,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    items);
            orders.save(order);
            return order;
        }
    }

    ShopOrderDto cancel(SessionInfo session, CancelOrderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        synchronized (lock) {
            ShopOrderDto order = orders.findById(request.getOrderId()).orElseThrow(() ->
                    new ShopBusinessException(ErrorCodes.SHOP_ORDER_NOT_FOUND, "订单不存在。"));
            if (!order.getUserId().equals(session.getUserId())) {
                throw new ShopBusinessException(ErrorCodes.AUTH_FORBIDDEN, "不能取消他人的订单。");
            }
            if (order.getStatus() != ShopOrderStatus.PAID) {
                throw new ShopBusinessException(
                        ErrorCodes.SHOP_ORDER_NOT_CANCELLABLE,
                        "该订单已取消，不能重复退款。");
            }
            ShopOrderDto cancelled = order.withStatus(ShopOrderStatus.CANCELLED);
            orders.save(cancelled);
            cards.refund(session.getUserId(), order.getTotalFen());
            for (OrderItemDto item : order.getItems()) {
                catalog.incrementStock(item.getProductId(), item.getQuantity());
            }
            return cancelled;
        }
    }

    List<ShopOrderDto> listMine(SessionInfo session) {
        return orders.listByUser(session.getUserId());
    }

    List<ShopOrderDto> listSales() {
        return orders.listAllNewestFirst();
    }

    private static Map<Long, Integer> merge(List<OrderLineRequest> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest line : lines) {
            quantities.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }
        return quantities;
    }
}
