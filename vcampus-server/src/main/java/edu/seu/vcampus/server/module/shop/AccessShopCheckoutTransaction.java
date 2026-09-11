package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.OrderItemDto;
import edu.seu.vcampus.common.shop.ProductSaleStatus;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopOrderStatus;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Commits payment, stock reservation and order creation in one Access transaction. */
final class AccessShopCheckoutTransaction {
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AccessDatabase database;

    AccessShopCheckoutTransaction(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    synchronized ShopOrderDto createOrder(
            SessionInfo session,
            CreateOrderRequest request,
            Map<Long, Integer> quantities) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                List<OrderItemDto> items = new ArrayList<>();
                int totalFen = 0;
                for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                    ProductRow product = product(connection, entry.getKey());
                    if (product == null
                            || product.status() != ProductSaleStatus.ON_SALE
                            || product.stockQty() < entry.getValue()) {
                        throw new ShopBusinessException(
                                ErrorCodes.SHOP_OUT_OF_STOCK,
                                "库存不足，请减少数量后再试。");
                    }
                    int subtotal = Math.multiplyExact(product.priceFen(), entry.getValue());
                    totalFen = Math.addExact(totalFen, subtotal);
                    items.add(new OrderItemDto(
                            entry.getKey(), product.name(), product.priceFen(),
                            entry.getValue(), subtotal));
                }

                try (PreparedStatement deduct = connection.prepareStatement(
                        "UPDATE tblCampusCard SET balanceFen = balanceFen - ? "
                                + "WHERE userId = ? AND balanceFen >= ?")) {
                    deduct.setInt(1, totalFen);
                    deduct.setString(2, session.getUserId());
                    deduct.setInt(3, totalFen);
                    if (deduct.executeUpdate() != 1) {
                        throw new ShopBusinessException(
                                ErrorCodes.SHOP_INSUFFICIENT_BALANCE,
                                "余额不足，请充值！");
                    }
                }

                for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                    try (PreparedStatement reserve = connection.prepareStatement(
                            "UPDATE tblShopProduct SET stockQty = stockQty - ? "
                                    + "WHERE productId = ? AND stockQty >= ? AND saleStatus = ?")) {
                        reserve.setInt(1, entry.getValue());
                        reserve.setLong(2, entry.getKey());
                        reserve.setInt(3, entry.getValue());
                        reserve.setString(4, ProductSaleStatus.ON_SALE.name());
                        if (reserve.executeUpdate() != 1) {
                            throw new ShopBusinessException(
                                    ErrorCodes.SHOP_OUT_OF_STOCK,
                                    "库存不足，请减少数量后再试。");
                        }
                    }
                }

                long sequence = nextOrderSequence(connection);
                String orderId = "SO-" + sequence;
                String createdAt = LocalDateTime.now().format(CLOCK);
                insertOrder(connection, orderId, sequence, session, request, totalFen, createdAt);
                insertItems(connection, orderId, items);
                connection.commit();
                return new ShopOrderDto(
                        orderId, session.getUserId(), session.getDisplayName(),
                        ShopOrderStatus.PAID, ShopPaymentMethods.CAMPUS_CARD,
                        request.getFulfillHint(), totalFen, createdAt, items);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Cannot complete atomic shop checkout. Database: " + database.path(),
                    exception);
        }
    }

    private static ProductRow product(Connection connection, long productId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT productName, priceFen, stockQty, saleStatus "
                        + "FROM tblShopProduct WHERE productId = ?")) {
            statement.setLong(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new ProductRow(
                                result.getString("productName"),
                                result.getInt("priceFen"),
                                result.getInt("stockQty"),
                                ProductSaleStatus.valueOf(result.getString("saleStatus")))
                        : null;
            }
        }
    }

    private static long nextOrderSequence(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT MAX(orderSequence) FROM tblShopOrder")) {
            result.next();
            long maximum = result.getLong(1);
            return result.wasNull() ? 1001L : maximum + 1L;
        }
    }

    private static void insertOrder(
            Connection connection,
            String orderId,
            long sequence,
            SessionInfo session,
            CreateOrderRequest request,
            int totalFen,
            String createdAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopOrder (orderId, orderSequence, userId, buyerNameSnapshot, "
                        + "orderStatus, paymentMethod, fulfillHint, totalFen, createdAt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, orderId);
            statement.setLong(2, sequence);
            statement.setString(3, session.getUserId());
            statement.setString(4, session.getDisplayName());
            statement.setString(5, ShopOrderStatus.PAID.name());
            statement.setString(6, ShopPaymentMethods.CAMPUS_CARD);
            statement.setString(7, request.getFulfillHint());
            statement.setInt(8, totalFen);
            statement.setString(9, createdAt);
            statement.executeUpdate();
        }
    }

    private static void insertItems(
            Connection connection, String orderId, List<OrderItemDto> items)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopOrderItem (orderId, lineNumber, productId, productNameSnapshot, "
                        + "unitPriceFen, quantity, subtotalFen) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int lineNumber = 1;
            for (OrderItemDto item : items) {
                statement.setString(1, orderId);
                statement.setInt(2, lineNumber++);
                statement.setLong(3, item.getProductId());
                statement.setString(4, item.getName());
                statement.setInt(5, item.getUnitPriceFen());
                statement.setInt(6, item.getQuantity());
                statement.setInt(7, item.getSubtotalFen());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private record ProductRow(
            String name, int priceFen, int stockQty, ProductSaleStatus status) { }
}
