package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ProductSaleStatus;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopCategories;
import edu.seu.vcampus.common.shop.ShopCategoryDto;
import edu.seu.vcampus.common.shop.ShopListingRecordDto;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Access-backed shop catalog, including product photos and listing history. */
final class AccessShopCatalog implements ShopCatalogRepository {

    private static final String CATEGORY_TABLE = "tblShopCategory";
    private static final String PRODUCT_TABLE = "tblShopProduct";
    private static final String PHOTO_TABLE = "tblShopProductPhoto";
    private static final String LISTING_TABLE = "tblShopListingRecord";
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccessDatabase database;

    AccessShopCatalog(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        initializeSchema();
        seedIfEmpty();
    }

    @Override
    public synchronized List<ProductSummaryDto> listOnSale(ListProductsRequest request) {
        String keyword = request == null ? null : request.getKeyword();
        Long categoryId = request == null ? null : request.getCategoryId();
        String needle = keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
        List<ProductSummaryDto> rows = listProducts();
        return rows.stream()
                .filter(product -> product.getSaleStatus() == ProductSaleStatus.ON_SALE)
                .filter(product -> categoryId == null || product.getCategoryId() == categoryId)
                .filter(product -> needle == null
                        || product.getName().toLowerCase(Locale.ROOT).contains(needle)
                        || product.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    @Override
    public synchronized ProductSummaryDto publish(
            PublishProductRequest request, String sellerName) {
        Objects.requireNonNull(request, "request must not be null");
        String categoryName = categoryName(request.getCategoryId());
        long productId = nextId(PRODUCT_TABLE, "productId");
        ProductSummaryDto product = new ProductSummaryDto(
                productId, request.getCategoryId(), categoryName,
                request.getName(), request.getDescription(), sellerName,
                request.getPriceFen(), request.getStockQty(),
                ProductSaleStatus.ON_SALE, request.getPhotos());
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                insertProduct(connection, product);
                insertPhotos(connection, product);
                insertListing(connection, product, "上架",
                        "上架 " + product.getName() + "，单价 "
                                + formatYuan(product.getPriceFen()) + "，库存 "
                                + product.getStockQty() + " 件",
                        sellerName, LocalDateTime.now().format(CLOCK));
                connection.commit();
                return product;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot publish shop product.", exception);
        }
    }

    @Override
    public synchronized ProductSummaryDto update(
            long productId, String name, String description, int priceFen,
            int addStockQty, String operatorName) {
        ProductSummaryDto current = findById(productId).orElseThrow(() ->
                new ShopBusinessException(ErrorCodes.SHOP_PRODUCT_NOT_FOUND, "商品不存在。"));
        if (current.getSaleStatus() != ProductSaleStatus.ON_SALE) {
            throw new ShopBusinessException(ErrorCodes.SHOP_PRODUCT_NOT_FOUND, "只能修改在售商品。");
        }
        int stock = current.getStockQty() + Math.max(0, addStockQty);
        ProductSummaryDto updated = current.withCatalog(name, description, priceFen, stock);
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tblShopProduct SET productName = ?, description = ?, "
                            + "priceFen = ?, stockQty = ? WHERE productId = ?")) {
                statement.setString(1, updated.getName());
                statement.setString(2, updated.getDescription());
                statement.setInt(3, updated.getPriceFen());
                statement.setInt(4, updated.getStockQty());
                statement.setLong(5, productId);
                statement.executeUpdate();
                insertListing(connection, updated, "调整",
                        changeDetail(current, updated, addStockQty), operatorName,
                        LocalDateTime.now().format(CLOCK));
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot update shop product.", exception);
        }
    }

    @Override
    public synchronized List<ShopListingRecordDto> listListings() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM tblShopListingRecord ORDER BY recordId DESC")) {
            List<ShopListingRecordDto> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new ShopListingRecordDto(
                        result.getLong("recordId"), result.getLong("productId"),
                        result.getString("productNameSnapshot"), result.getString("actionName"),
                        result.getString("detailText"), result.getString("operatorNameSnapshot"),
                        result.getString("createdAt")));
            }
            return List.copyOf(rows);
        } catch (SQLException exception) {
            throw failure("Cannot list shop catalog changes.", exception);
        }
    }

    @Override
    public synchronized List<ShopCategoryDto> listCategories() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT * FROM tblShopCategory ORDER BY categoryId")) {
            List<ShopCategoryDto> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new ShopCategoryDto(
                        result.getLong("categoryId"), result.getString("categoryName")));
            }
            return List.copyOf(rows);
        } catch (SQLException exception) {
            throw failure("Cannot list shop categories.", exception);
        }
    }

    @Override
    public synchronized ShopCategoryDto addCategory(String name) {
        ShopCategoryDto created = new ShopCategoryDto(nextId(CATEGORY_TABLE, "categoryId"), name);
        if (listCategories().stream().anyMatch(
                existing -> existing.getName().equalsIgnoreCase(created.getName()))) {
            throw new ShopBusinessException(ErrorCodes.SHOP_CATEGORY_EXISTS, "该分类已存在。");
        }
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tblShopCategory (categoryId, categoryName) VALUES (?, ?)")) {
            statement.setLong(1, created.getCategoryId());
            statement.setString(2, created.getName());
            statement.executeUpdate();
            return created;
        } catch (SQLException exception) {
            throw failure("Cannot add shop category.", exception);
        }
    }

    @Override
    public synchronized Optional<ProductSummaryDto> findById(long productId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT p.*, c.categoryName FROM tblShopProduct p "
                             + "INNER JOIN tblShopCategory c ON p.categoryId = c.categoryId "
                             + "WHERE p.productId = ?")) {
            statement.setLong(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(readProduct(connection, result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("Cannot read shop product.", exception);
        }
    }

    @Override
    public synchronized boolean decrementStock(long productId, int quantity) {
        if (quantity < 1) {
            return false;
        }
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tblShopProduct SET stockQty = stockQty - ? "
                             + "WHERE productId = ? AND stockQty >= ? AND saleStatus = ?")) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            statement.setString(4, ProductSaleStatus.ON_SALE.name());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw failure("Cannot reserve shop stock.", exception);
        }
    }

    @Override
    public synchronized void incrementStock(long productId, int quantity) {
        if (quantity < 1) {
            return;
        }
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tblShopProduct SET stockQty = stockQty + ? WHERE productId = ?")) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Cannot restore shop stock.", exception);
        }
    }

    private List<ProductSummaryDto> listProducts() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT p.*, c.categoryName FROM tblShopProduct p "
                             + "INNER JOIN tblShopCategory c ON p.categoryId = c.categoryId "
                             + "ORDER BY p.productId")) {
            List<ProductSummaryDto> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(readProduct(connection, result));
            }
            return List.copyOf(rows);
        } catch (SQLException exception) {
            throw failure("Cannot list shop products.", exception);
        }
    }

    private ProductSummaryDto readProduct(Connection connection, ResultSet result)
            throws SQLException {
        long productId = result.getLong("productId");
        List<byte[]> photos = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT photoData FROM tblShopProductPhoto "
                        + "WHERE productId = ? ORDER BY photoIndex")) {
            statement.setLong(1, productId);
            try (ResultSet photoRows = statement.executeQuery()) {
                while (photoRows.next()) {
                    photos.add(photoRows.getBytes("photoData"));
                }
            }
        }
        if (photos.isEmpty()) {
            photos = ShopDemoPhotos.forProduct(
                    result.getString("categoryName"), result.getString("productName"));
        }
        return new ProductSummaryDto(
                productId, result.getLong("categoryId"), result.getString("categoryName"),
                result.getString("productName"), result.getString("description"),
                result.getString("sellerNameSnapshot"), result.getInt("priceFen"),
                result.getInt("stockQty"),
                ProductSaleStatus.valueOf(result.getString("saleStatus")), photos);
    }

    private String categoryName(long categoryId) {
        return listCategories().stream()
                .filter(category -> category.getCategoryId() == categoryId)
                .map(ShopCategoryDto::getName)
                .findFirst()
                .orElseThrow(() -> new ShopBusinessException(
                        ErrorCodes.SHOP_CATEGORY_NOT_FOUND, "分类不存在。"));
    }

    private void initializeSchema() {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            if (!tableExists(connection, CATEGORY_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopCategory ("
                        + "categoryId LONG PRIMARY KEY, categoryName TEXT(40) NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblShopCategory_name "
                        + "ON tblShopCategory (categoryName)");
            }
            if (!tableExists(connection, PRODUCT_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopProduct ("
                        + "productId LONG PRIMARY KEY, categoryId LONG NOT NULL, "
                        + "productName TEXT(100) NOT NULL, description MEMO, "
                        + "sellerNameSnapshot TEXT(100) NOT NULL, priceFen LONG NOT NULL, "
                        + "stockQty LONG NOT NULL, saleStatus TEXT(20) NOT NULL)");
            }
            if (!tableExists(connection, PHOTO_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopProductPhoto ("
                        + "photoId AUTOINCREMENT PRIMARY KEY, productId LONG NOT NULL, "
                        + "photoIndex LONG NOT NULL, photoData OLE NOT NULL)");
                statement.executeUpdate("CREATE UNIQUE INDEX ux_tblShopPhoto_product_index "
                        + "ON tblShopProductPhoto (productId, photoIndex)");
            }
            if (!tableExists(connection, LISTING_TABLE)) {
                statement.executeUpdate("CREATE TABLE tblShopListingRecord ("
                        + "recordId AUTOINCREMENT PRIMARY KEY, productId LONG NOT NULL, "
                        + "productNameSnapshot TEXT(100) NOT NULL, actionName TEXT(20) NOT NULL, "
                        + "detailText MEMO, operatorNameSnapshot TEXT(100) NOT NULL, "
                        + "createdAt TEXT(30) NOT NULL)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize shop catalog schema.", exception);
        }
    }

    private void seedIfEmpty() {
        if (!listCategories().isEmpty()) {
            return;
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement category = connection.prepareStatement(
                        "INSERT INTO tblShopCategory (categoryId, categoryName) VALUES (?, ?)")) {
                    for (ShopCategoryDto seed : ShopCategories.seed()) {
                        category.setLong(1, seed.getCategoryId());
                        category.setString(2, seed.getName());
                        category.addBatch();
                    }
                    category.executeBatch();
                }
                for (ProductSummaryDto product : InMemoryShopCatalog.seedProducts()) {
                    insertProduct(connection, product);
                    insertPhotos(connection, product);
                    if (product.getSaleStatus() == ProductSaleStatus.ON_SALE) {
                        insertListing(connection, product, "上架",
                                "上架 " + product.getName() + "，单价 "
                                        + formatYuan(product.getPriceFen()) + "，库存 "
                                        + product.getStockQty() + " 件",
                                "校园商店", "2026-08-20 09:00:00");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot seed shop catalog.", exception);
        }
    }

    private static void insertProduct(Connection connection, ProductSummaryDto product)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopProduct (productId, categoryId, productName, description, "
                        + "sellerNameSnapshot, priceFen, stockQty, saleStatus) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, product.getProductId());
            statement.setLong(2, product.getCategoryId());
            statement.setString(3, product.getName());
            statement.setString(4, product.getDescription());
            statement.setString(5, product.getSellerName());
            statement.setInt(6, product.getPriceFen());
            statement.setInt(7, product.getStockQty());
            statement.setString(8, product.getSaleStatus().name());
            statement.executeUpdate();
        }
    }

    private static void insertPhotos(Connection connection, ProductSummaryDto product)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopProductPhoto (productId, photoIndex, photoData) "
                        + "VALUES (?, ?, ?)")) {
            List<byte[]> photos = product.getPhotos();
            for (int index = 0; index < photos.size(); index++) {
                statement.setLong(1, product.getProductId());
                statement.setInt(2, index);
                statement.setBytes(3, photos.get(index));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertListing(
            Connection connection, ProductSummaryDto product, String action,
            String detail, String operatorName, String createdAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tblShopListingRecord (productId, productNameSnapshot, "
                        + "actionName, detailText, operatorNameSnapshot, createdAt) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, product.getProductId());
            statement.setString(2, product.getName());
            statement.setString(3, action);
            statement.setString(4, detail);
            statement.setString(5, operatorName);
            statement.setString(6, createdAt);
            statement.executeUpdate();
        }
    }

    private long nextId(String table, String column) {
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT MAX(" + column + ") AS maximumId FROM " + table)) {
            result.next();
            long maximum = result.getLong("maximumId");
            return result.wasNull() ? 1L : maximum + 1L;
        } catch (SQLException exception) {
            throw failure("Cannot allocate shop identifier.", exception);
        }
    }

    private static boolean tableExists(Connection connection, String expected) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (expected.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String changeDetail(
            ProductSummaryDto before, ProductSummaryDto after, int addStockQty) {
        List<String> parts = new ArrayList<>();
        if (before.getPriceFen() != after.getPriceFen()) {
            parts.add("价格 " + formatYuan(before.getPriceFen()) + " → "
                    + formatYuan(after.getPriceFen()));
        }
        if (addStockQty > 0) {
            parts.add("补货 +" + addStockQty + "，库存现为 " + after.getStockQty());
        }
        if (!before.getName().equals(after.getName())) {
            parts.add("标题改为「" + after.getName() + "」");
        }
        if (!before.getDescription().equals(after.getDescription())) {
            parts.add("已更新描述");
        }
        return parts.isEmpty() ? "保存了商品资料，内容未变化" : String.join("；", parts);
    }

    private static String formatYuan(int fen) {
        return "¥" + String.format("%.2f", fen / 100.0);
    }

    private static void rollback(Connection connection, Throwable cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message + " Database: " + database.path(), cause);
    }
}
