package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.OrderItemDto;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopOrderStatus;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Access-backed orders and immutable order-item snapshots. */
final class AccessShopOrderStore implements ShopOrderRepository {

    private static final String ORDER_TABLE = "tblShopOrder";
    private static final String ITEM_TABLE = "tblShopOrderItem";
    private final AccessDatabase database;

    AccessShopOrderStore(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        initializeSchema();
    }

    @Override
    public synchronized String nextOrderId() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT MAX(orderSequence) AS maximumId FROM tblShopOrder")) {
            result.next();
            long maximum = result.getLong("maximumId");
            return "SO-" + (result.wasNull() ? 1001L : maximum + 1L);
        } catch (SQLException exception) {
            throw failure("Cannot allocate order number.", exception);
        }
    }

    @Override
    public synchronized void save(ShopOrderDto order) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, order.getOrderId())) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE tblShopOrder SET orderStatus = ? WHERE orderId = ?")) {
                        update.setString(1, order.getStatus().name());
                        update.setString(2, order.getOrderId());
                        update.executeUpdate();
                    }
                } else {
                    insertOrder(connection, order);
                    insertItems(connection, order);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot save order.", exception);
        }
    }

    @Override
    public synchronized Optional<ShopOrderDto> findById(String orderId) {
        List<ShopOrderDto> rows = query("WHERE orderId = ? ORDER BY orderSequence DESC", orderId);
        return rows.stream().findFirst();
    }

    @Override
    public synchronized List<ShopOrderDto> listByUser(String userId) {
        return query("WHERE userId = ? ORDER BY orderSequence DESC", userId);
    }

    @Override
    public synchronized List<ShopOrderDto> listAllNewestFirst() {
        return query("ORDER BY orderSequence DESC", null);
    }

    private List<ShopOrderDto> query(String suffix, String parameter) {
        String sql = "SELECT * FROM tblShopOrder " + suffix;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<ShopOrderDto> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new ShopOrderDto(
                            result.getString("orderId"), result.getString("userId"),
                            result.getString("buyerNameSnapshot"),
                            ShopOrderStatus.valueOf(result.getString("orderStatus")),
                            result.getString("paymentMethod"), result.getString("fulfillHint"),
                            result.getInt("totalFen"), result.getString("createdAt"),
                            readItems(connection, result.getString("orderId"))));
                }
                return List.copyOf(rows);
            }
        } catch (SQLException exception) {
            throw failure("Cannot read orders.", exception);
        }
    }

    private List<OrderItemDto> readItems(Connection connection, String orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tblShopOrderItem WHERE orderId = ? ORDER BY lineNumber")) {
            statement.setString(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderItemDto> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new OrderItemDto(
                            result.getLong("productId"), result.getString("productNameSnapshot"),
                            result.getInt("unitPriceFen"), result.getInt("quantity"),
                            result.getInt("subtotalFen")));
                }
                return rows;
            }
        }
    }

    private void insertOrder(Connection connection, ShopOrderDto order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopOrder (orderId, orderSequence, userId, buyerNameSnapshot, "
                        + "orderStatus, paymentMethod, fulfillHint, totalFen, createdAt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, order.getOrderId());
            statement.setLong(2, Long.parseLong(order.getOrderId().substring(3)));
            statement.setString(3, order.getUserId());
            statement.setString(4, order.getBuyerName());
            statement.setString(5, order.getStatus().name());
            statement.setString(6, order.getPaymentMethod());
            statement.setString(7, order.getFulfillHint());
            statement.setInt(8, order.getTotalFen());
            statement.setString(9, order.getCreatedAt());
            statement.executeUpdate();
        }
    }

    private void insertItems(Connection connection, ShopOrderDto order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopOrderItem (orderId, lineNumber, productId, productNameSnapshot, "
                        + "unitPriceFen, quantity, subtotalFen) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            int line = 1;
            for (OrderItemDto item : order.getItems()) {
                statement.setString(1, order.getOrderId());
                statement.setInt(2, line++);
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

    private boolean exists(Connection connection, String orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT orderId FROM tblShopOrder WHERE orderId = ?")) {
            statement.setString(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, ORDER_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopOrder ("
                        + "orderId TEXT(32) PRIMARY KEY, orderSequence LONG NOT NULL, userId TEXT(64) NOT NULL, "
                        + "buyerNameSnapshot TEXT(100) NOT NULL, orderStatus TEXT(20) NOT NULL, "
                        + "paymentMethod TEXT(30) NOT NULL, fulfillHint TEXT(255) NOT NULL, "
                        + "totalFen LONG NOT NULL, createdAt TEXT(30) NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblShopOrder_sequence ON tblShopOrder (orderSequence)");
                statement.executeUpdate("CREATE INDEX ix_tblShopOrder_userId ON tblShopOrder (userId)");
            }
            if (!tableExists(connection, ITEM_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopOrderItem ("
                        + "itemId AUTOINCREMENT PRIMARY KEY, orderId TEXT(32) NOT NULL, lineNumber LONG NOT NULL, "
                        + "productId LONG NOT NULL, productNameSnapshot TEXT(100) NOT NULL, "
                        + "unitPriceFen LONG NOT NULL, quantity LONG NOT NULL, subtotalFen LONG NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblShopOrderItem_line "
                        + "ON tblShopOrderItem (orderId, lineNumber)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize order schema.", exception);
        }
    }

    private static boolean tableExists(Connection connection, String expected) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message + " Database: " + database.path(), cause);
    }
}
