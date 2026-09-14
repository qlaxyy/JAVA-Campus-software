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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Access-backed shop catalog, including product photos and listing history. */
final class AccessShopCatalog implements ShopCatalogRepository {

    private static final String CATEGORY_TABLE = "tblShopCategory";
    private static final String PRODUCT_TABLE = "tblShopProduct";
    private static final String PHOTO_TABLE = "tblShopProductPhoto";
    private static final String LISTING_TABLE = "tblShopListingRecord";
    private static final String SEED_VERSION = "shop-demo-catalog-2026-09-v1";
    private static final Object SEED_LOCK = new Object();
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccessDatabase database;

    AccessShopCatalog(AccessDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        initializeSchema();
        synchronized (SEED_LOCK) {
            boolean freshlySeeded = seedIfEmpty();
            migrateOfficialDemoSeed(freshlySeeded);
        }
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
                    byte[] payload = imagePayload(readPhoto(photoRows));
                    if (payload.length > 0) {
                        photos.add(payload);
                    }
                }
            }
        }
        if (photos.isEmpty()) {
            photos = ShopDemoPhotos.forProduct(
                    productId, result.getString("categoryName"), result.getString("productName"));
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
            if (!tableExists(connection, "tblShopSeedVersion")) {
                statement.executeUpdate("CREATE TABLE tblShopSeedVersion ("
                        + "seedKey TEXT(100) PRIMARY KEY, appliedAt TEXT(30) NOT NULL)");
            }
        } catch (SQLException exception) {
            throw failure("Cannot initialize shop catalog schema.", exception);
        }
    }

    private boolean seedIfEmpty() {
        if (!listCategories().isEmpty()) {
            return false;
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
        return true;
    }

    /**
     * Applies a catalog upgrade once, not on every server restart. The migration
     * marker and old-file changes share a transaction; administrator edits survive later starts.
     */
    private void migrateOfficialDemoSeed(boolean freshlySeeded) {
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement version = connection.prepareStatement(
                        "SELECT seedKey FROM tblShopSeedVersion WHERE seedKey = ?")) {
                    version.setString(1, SEED_VERSION);
                    try (ResultSet rows = version.executeQuery()) {
                        if (rows.next()) { return; }
                    }
                }
                if (!freshlySeeded) {
                    for (ProductSummaryDto product : InMemoryShopCatalog.seedProducts()) {
                        if (!productExists(connection, product.getProductId())) {
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
                    }
                    // Never overwrite an existing product/photo or retire it during normal startup.
                    // A clean official catalogue is generated only by explicit database rebuilding.
                }
                try (PreparedStatement version = connection.prepareStatement(
                        "INSERT INTO tblShopSeedVersion (seedKey, appliedAt) VALUES (?, ?)")) {
                    version.setString(1, SEED_VERSION);
                    version.setString(2, java.time.LocalDateTime.now().format(CLOCK));
                    version.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw failure("Cannot refresh official shop demo seed.", exception);
        }
    }

    /**
     * Drops retired official demo SKUs (ids 1–15 no longer in the seed list)
     * so existing Access files do not keep leftover catalog cards.
     */
    private void retireRemovedOfficialDemoProducts(Connection connection) throws SQLException {
        Set<Long> keep = new HashSet<>();
        for (ProductSummaryDto product : InMemoryShopCatalog.seedProducts()) {
            keep.add(product.getProductId());
        }
        for (long productId = 1L; productId <= 15L; productId++) {
            if (keep.contains(productId) || !productExists(connection, productId)) {
                continue;
            }
            deleteRetiredProduct(connection, productId);
        }
    }

    private void deleteRetiredProduct(Connection connection, long productId) throws SQLException {
        if (tableExists(connection, "tblShopCartItem")) {
            try (PreparedStatement cart = connection.prepareStatement(
                    "DELETE FROM tblShopCartItem WHERE productId = ?")) {
                cart.setLong(1, productId);
                cart.executeUpdate();
            }
        }
        try (PreparedStatement listings = connection.prepareStatement(
                "DELETE FROM tblShopListingRecord WHERE productId = ?")) {
            listings.setLong(1, productId);
            listings.executeUpdate();
        }
        deletePhotos(connection, productId);
        try (PreparedStatement product = connection.prepareStatement(
                "DELETE FROM tblShopProduct WHERE productId = ?")) {
            product.setLong(1, productId);
            product.executeUpdate();
        }
    }

    private static boolean productExists(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tblShopProduct WHERE productId = ?")) {
            statement.setLong(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void updateOfficialDemoProduct(Connection connection, ProductSummaryDto product)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tblShopProduct SET categoryId = ?, productName = ?, description = ?, "
                        + "sellerNameSnapshot = ?, priceFen = ?, saleStatus = ? "
                        + "WHERE productId = ?")) {
            statement.setLong(1, product.getCategoryId());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setString(4, product.getSellerName());
            statement.setInt(5, product.getPriceFen());
            statement.setString(6, product.getSaleStatus().name());
            statement.setLong(7, product.getProductId());
            statement.executeUpdate();
        }
    }

    private static void deletePhotos(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM tblShopProductPhoto WHERE productId = ?")) {
            statement.setLong(1, productId);
            statement.executeUpdate();
        }
    }

    private static byte[] readPhoto(ResultSet rows) throws SQLException {
        try (InputStream stream = rows.getBinaryStream("photoData")) {
            if (stream != null) {
                return stream.readAllBytes();
            }
        } catch (IOException exception) {
            throw new SQLException("Cannot read shop photo bytes.", exception);
        }
        byte[] stored = rows.getBytes("photoData");
        return stored == null ? new byte[0] : stored;
    }

    private static byte[] imagePayload(byte[] stored) {
        if (stored == null || stored.length < 8) {
            return new byte[0];
        }
        int jpeg = indexOf(stored, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        if (jpeg >= 0) {
            return Arrays.copyOfRange(stored, jpeg, stored.length);
        }
        int png = indexOf(stored, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        if (png >= 0) {
            return Arrays.copyOfRange(stored, png, stored.length);
        }
        return stored;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int index = 0; index <= haystack.length - needle.length; index++) {
            for (int offset = 0; offset < needle.length; offset++) {
                if (haystack[index + offset] != needle[offset]) {
                    continue outer;
                }
            }
            return index;
        }
        return -1;
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
                byte[] photo = photos.get(index);
                statement.setLong(1, product.getProductId());
                statement.setInt(2, index);
                statement.setBinaryStream(3, new ByteArrayInputStream(photo), photo.length);
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
