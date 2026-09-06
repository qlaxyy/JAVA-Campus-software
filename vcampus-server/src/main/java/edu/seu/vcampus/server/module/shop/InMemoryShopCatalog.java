package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ProductSaleStatus;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopListingRecordDto;
import edu.seu.vcampus.common.shop.ShopCategories;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * In-memory shop catalog used until Access DAO replaces this class.
 */
public final class InMemoryShopCatalog {

    private static final String CAMPUS_SELLER = "校园商店";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<ProductSummaryDto> products = new ArrayList<>(seed());
    private final List<ShopListingRecordDto> listings = new ArrayList<>();
    private long nextId = 11L;
    private long nextListingId = 1L;

    public InMemoryShopCatalog() {
        for (ProductSummaryDto product : products) {
            if (product.getSaleStatus() == ProductSaleStatus.ON_SALE) {
                appendListing(
                        product,
                        "上架",
                        "上架 " + product.getName()
                                + "，单价 "
                                + formatYuan(product.getPriceFen())
                                + "，库存 "
                                + product.getStockQty()
                                + " 件",
                        CAMPUS_SELLER,
                        "2026-08-20 09:00:00");
            }
        }
    }

    /**
     * Lists on-sale products matching an optional name/description and category filter.
     *
     * @param request filter; {@code null} means no extra filter
     * @return matching on-sale rows in catalog order
     */
    public synchronized List<ProductSummaryDto> listOnSale(ListProductsRequest request) {
        String keyword = request == null ? null : request.getKeyword();
        Long categoryId = request == null ? null : request.getCategoryId();
        String needle = keyword == null ? null : keyword.toLowerCase(Locale.ROOT);
        List<ProductSummaryDto> matches = new ArrayList<>();
        for (ProductSummaryDto product : products) {
            if (product.getSaleStatus() != ProductSaleStatus.ON_SALE) {
                continue;
            }
            if (categoryId != null && product.getCategoryId() != categoryId) {
                continue;
            }
            if (needle != null && !matchesKeyword(product, needle)) {
                continue;
            }
            matches.add(product);
        }
        return List.copyOf(matches);
    }

    /**
     * Adds a newly published on-sale product.
     *
     * @param request validated publish payload
     * @param sellerName publisher display name
     * @return the stored catalog row
     */
    public synchronized ProductSummaryDto publish(PublishProductRequest request, String sellerName) {
        Objects.requireNonNull(request, "request must not be null");
        ProductSummaryDto product = new ProductSummaryDto(
                nextId++,
                request.getCategoryId(),
                ShopCategories.nameOf(request.getCategoryId()),
                request.getName(),
                request.getDescription(),
                sellerName,
                request.getPriceFen(),
                request.getStockQty(),
                ProductSaleStatus.ON_SALE,
                request.getPhotos());
        products.add(product);
        appendListing(
                product,
                "上架",
                "上架 " + product.getName()
                        + "，单价 "
                        + formatYuan(product.getPriceFen())
                        + "，库存 "
                        + product.getStockQty()
                        + " 件",
                sellerName,
                LocalDateTime.now().format(CLOCK));
        return product;
    }

    /**
     * Updates title, description, price and optional extra stock for an on-sale product.
     *
     * @param productId catalog key
     * @param name title
     * @param description seller copy
     * @param priceFen unit price
     * @param addStockQty extra units
     * @param operatorName merchant display name
     * @return updated row
     */
    public synchronized ProductSummaryDto update(
            long productId,
            String name,
            String description,
            int priceFen,
            int addStockQty,
            String operatorName) {
        for (int index = 0; index < products.size(); index++) {
            ProductSummaryDto current = products.get(index);
            if (current.getProductId() != productId) {
                continue;
            }
            if (current.getSaleStatus() != ProductSaleStatus.ON_SALE) {
                throw new ShopBusinessException(
                        ErrorCodes.SHOP_PRODUCT_NOT_FOUND,
                        "只能修改在售商品。");
            }
            int stock = current.getStockQty() + Math.max(0, addStockQty);
            ProductSummaryDto updated = current.withCatalog(name, description, priceFen, stock);
            products.set(index, updated);
            appendListing(
                    updated,
                    "调整",
                    changeDetail(current, updated, addStockQty),
                    operatorName,
                    LocalDateTime.now().format(CLOCK));
            return updated;
        }
        throw new ShopBusinessException(
                ErrorCodes.SHOP_PRODUCT_NOT_FOUND,
                "商品不存在。");
    }

    /**
     * Lists merchant listing records, newest first.
     *
     * @return listing log
     */
    public synchronized List<ShopListingRecordDto> listListings() {
        List<ShopListingRecordDto> newestFirst = new ArrayList<>();
        for (int index = listings.size() - 1; index >= 0; index--) {
            newestFirst.add(listings.get(index));
        }
        return List.copyOf(newestFirst);
    }

    /**
     * Finds a catalog row by id.
     *
     * @param productId catalog key
     * @return matching product, if any
     */
    public synchronized java.util.Optional<ProductSummaryDto> findById(long productId) {
        for (ProductSummaryDto product : products) {
            if (product.getProductId() == productId) {
                return java.util.Optional.of(product);
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Decrements remaining stock for an on-sale product.
     *
     * @param productId catalog key
     * @param quantity units to reserve
     * @return {@code true} when stock was reduced
     */
    public synchronized boolean decrementStock(long productId, int quantity) {
        if (quantity < 1) {
            return false;
        }
        for (int index = 0; index < products.size(); index++) {
            ProductSummaryDto product = products.get(index);
            if (product.getProductId() != productId) {
                continue;
            }
            if (product.getSaleStatus() != ProductSaleStatus.ON_SALE
                    || product.getStockQty() < quantity) {
                return false;
            }
            products.set(index, product.withStockQty(product.getStockQty() - quantity));
            return true;
        }
        return false;
    }

    /**
     * Restores stock after a cancelled order.
     *
     * @param productId catalog key
     * @param quantity units to return
     */
    public synchronized void incrementStock(long productId, int quantity) {
        if (quantity < 1) {
            return;
        }
        for (int index = 0; index < products.size(); index++) {
            ProductSummaryDto product = products.get(index);
            if (product.getProductId() != productId) {
                continue;
            }
            products.set(index, product.withStockQty(product.getStockQty() + quantity));
            return;
        }
    }

    private static boolean matchesKeyword(ProductSummaryDto product, String needle) {
        return product.getName().toLowerCase(Locale.ROOT).contains(needle)
                || product.getDescription().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static List<ProductSummaryDto> seed() {
        List<ProductSummaryDto> rows = new ArrayList<>();
        rows.add(onSale(1, ShopCategories.STATIONERY, "中性笔 0.5mm", 350, 120,
                "顺滑好写，适合课堂笔记。笔帽不易丢，整盒分装后单支出售。"));
        rows.add(onSale(2, ShopCategories.STATIONERY, "A4 草稿纸 100 张", 600, 80,
                "作业演算够用。纸面平整，双面都能写，适合带去自习室。"));
        rows.add(onSale(3, ShopCategories.STATIONERY, "荧光笔套装", 1290, 40,
                "四个常用色，划重点不洇纸。开学整理笔记够用一学期。"));
        rows.add(offSale(4, ShopCategories.STATIONERY, "停售纪念本", 1990, 0,
                "已停售样品，仅保留库存记录，不在首页展示。"));
        rows.add(onSale(5, ShopCategories.DAILY, "抽纸 3 包", 990, 60,
                "宿舍常备抽纸，三包一组。纸张偏厚，抽取得出。"));
        rows.add(onSale(6, ShopCategories.DAILY, "牙刷两支装", 850, 45,
                "软毛两支装，换新更方便。包装未拆，校内自提。"));
        rows.add(onSale(7, ShopCategories.DAILY, "洗衣凝珠 20 粒", 1590, 30,
                "一粒一次，味道清淡。适合宿舍洗衣机小件衣物。"));
        rows.add(onSale(8, ShopCategories.FOOD, "矿泉水 550ml", 200, 200,
                "冰柜常温都有。上课带走方便，瓶身轻。"));
        rows.add(onSale(9, ShopCategories.FOOD, "面包 1 个", 450, 25,
                "当天现货，早餐或加餐。建议当日食用。"));
        rows.add(offSale(10, ShopCategories.FOOD, "过期试吃饼干", 100, 3,
                "过期试吃品，不得上架售卖。"));
        return rows;
    }

    private static ProductSummaryDto onSale(
            long id, long categoryId, String name, int priceFen, int stock, String description) {
        String category = ShopCategories.nameOf(categoryId);
        return new ProductSummaryDto(
                id,
                categoryId,
                category,
                name,
                description,
                CAMPUS_SELLER,
                priceFen,
                stock,
                ProductSaleStatus.ON_SALE,
                ShopDemoPhotos.forProduct(category, name));
    }

    private static ProductSummaryDto offSale(
            long id, long categoryId, String name, int priceFen, int stock, String description) {
        String category = ShopCategories.nameOf(categoryId);
        return new ProductSummaryDto(
                id,
                categoryId,
                category,
                name,
                description,
                CAMPUS_SELLER,
                priceFen,
                stock,
                ProductSaleStatus.OFF_SALE,
                ShopDemoPhotos.forProduct(category, name));
    }

    private void appendListing(
            ProductSummaryDto product,
            String action,
            String detail,
            String operatorName,
            String createdAt) {
        listings.add(new ShopListingRecordDto(
                nextListingId++,
                product.getProductId(),
                product.getName(),
                action,
                detail,
                operatorName,
                createdAt));
    }

    private static String changeDetail(
            ProductSummaryDto before,
            ProductSummaryDto after,
            int addStockQty) {
        List<String> parts = new ArrayList<>();
        if (before.getPriceFen() != after.getPriceFen()) {
            parts.add("价格 " + formatYuan(before.getPriceFen()) + " → " + formatYuan(after.getPriceFen()));
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
        if (parts.isEmpty()) {
            return "保存了商品资料，内容未变化";
        }
        return String.join("；", parts);
    }

    private static String formatYuan(int fen) {
        return "¥" + String.format("%.2f", fen / 100.0);
    }
}
