package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.CartItemDto;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShoppingCartView;
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

/** Access-backed cart keyed by userId and productId. */
final class AccessShoppingCartStore implements ShoppingCartRepository {
    private static final String TABLE = "tblShopCartItem";
    private final AccessDatabase database;
    private final ShopCatalogRepository catalog;

    AccessShoppingCartStore(AccessDatabase database, ShopCatalogRepository catalog) {
        this.database = Objects.requireNonNull(database);
        this.catalog = Objects.requireNonNull(catalog);
        initializeSchema();
    }

    public synchronized ShoppingCartView view(String userId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT productId, quantity FROM tblShopCartItem WHERE userId = ? ORDER BY productId")) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<CartItemDto> rows = new ArrayList<>();
                while (result.next()) {
                    catalog.findById(result.getLong("productId")).ifPresent(product ->
                            rows.add(new CartItemDto(product, Math.min(
                                    resultQuantity(result), Math.max(1, product.getStockQty())))));
                }
                return new ShoppingCartView(rows);
            }
        } catch (SQLException exception) { throw failure("Cannot read cart.", exception); }
    }

    private static int resultQuantity(ResultSet result) {
        try { return result.getInt("quantity"); }
        catch (SQLException exception) { throw new IllegalStateException(exception); }
    }

    public synchronized ShoppingCartView setQuantity(String userId, long productId, int quantity) {
        ProductSummaryDto product = catalog.findById(productId).orElseThrow(() ->
                new ShopBusinessException(ErrorCodes.SHOP_PRODUCT_NOT_FOUND, "商品不存在。"));
        int normalized = Math.min(quantity, Math.max(0, product.getStockQty()));
        try (Connection connection = database.openConnection()) {
            delete(connection, userId, productId);
            if (normalized > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tblShopCartItem (userId, productId, quantity) VALUES (?, ?, ?)")) {
                    statement.setString(1, userId); statement.setLong(2, productId);
                    statement.setInt(3, normalized); statement.executeUpdate();
                }
            }
            return view(userId);
        } catch (SQLException exception) { throw failure("Cannot update cart.", exception); }
    }

    public synchronized ShoppingCartView clear(String userId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM tblShopCartItem WHERE userId = ?")) {
            statement.setString(1, userId); statement.executeUpdate();
            return new ShoppingCartView(List.of());
        } catch (SQLException exception) { throw failure("Cannot clear cart.", exception); }
    }

    private static void delete(Connection connection, String userId, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM tblShopCartItem WHERE userId = ? AND productId = ?")) {
            statement.setString(1, userId); statement.setLong(2, productId); statement.executeUpdate();
        }
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            if (!tableExists(connection, TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopCartItem (cartItemId AUTOINCREMENT PRIMARY KEY, "
                        + "userId TEXT(64) NOT NULL, productId LONG NOT NULL, quantity LONG NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblShopCart_user_product "
                        + "ON tblShopCartItem (userId, productId)");
            }
        } catch (SQLException exception) { throw failure("Cannot initialize cart schema.", exception); }
    }

    private static boolean tableExists(Connection connection, String expected) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
        }
        return false;
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message + " Database: " + database.path(), cause);
    }
}
