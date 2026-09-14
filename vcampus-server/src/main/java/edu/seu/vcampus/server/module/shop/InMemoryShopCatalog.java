package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ProductSaleStatus;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopCategoryDto;
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
public final class InMemoryShopCatalog implements ShopCatalogRepository {

    private static final String CAMPUS_SELLER = "校园商店";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<ProductSummaryDto> products = new ArrayList<>(seedProducts());
    private final List<ShopListingRecordDto> listings = new ArrayList<>();
    private final List<ShopCategoryDto> categories = new ArrayList<>(ShopCategories.seed());
    private long nextId = 16L;
    private long nextListingId = 1L;
    private long nextCategoryId = 4L;

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
                requireCategoryName(request.getCategoryId()),
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
     * Lists active product categories in catalog order.
     *
     * @return seed categories plus any admin-created rows
     */
    public synchronized List<ShopCategoryDto> listCategories() {
        return List.copyOf(categories);
    }

    /**
     * Adds a unique product category.
     *
     * @param name display name
     * @return stored category
     */
    public synchronized ShopCategoryDto addCategory(String name) {
        ShopCategoryDto created = new ShopCategoryDto(nextCategoryId, name);
        for (ShopCategoryDto existing : categories) {
            if (existing.getName().equalsIgnoreCase(created.getName())) {
                throw new ShopBusinessException(ErrorCodes.SHOP_CATEGORY_EXISTS, "该分类已存在。");
            }
        }
        nextCategoryId++;
        categories.add(created);
        return created;
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

    private String requireCategoryName(long categoryId) {
        for (ShopCategoryDto category : categories) {
            if (category.getCategoryId() == categoryId) {
                return category.getName();
            }
        }
        throw new ShopBusinessException(ErrorCodes.SHOP_CATEGORY_NOT_FOUND, "分类不存在。");
    }

    private static boolean matchesKeyword(ProductSummaryDto product, String needle) {
        return product.getName().toLowerCase(Locale.ROOT).contains(needle)
                || product.getDescription().toLowerCase(Locale.ROOT).contains(needle);
    }

    static List<ProductSummaryDto> seedProducts() {
        List<ProductSummaryDto> rows = new ArrayList<>();
        rows.add(onSale(1, ShopCategories.STATIONERY, "晨光中性笔 0.5mm 黑色按动", 350, 120,
                spec("品牌：晨光",
                        "型号：K-35 按动中性笔",
                        "笔尖：0.5mm 子弹头",
                        "墨色：黑色",
                        "包装：单支出售",
                        "适用：课堂笔记 / 考试填涂",
                        "发货：校内自提，拆盒分装")));
        rows.add(offSale(4, ShopCategories.STATIONERY, "停售纪念本（已下架）", 1990, 0,
                spec("状态：已停售",
                        "说明：样品仅保留库存记录，不在首页展示。")));
        rows.add(onSale(5, ShopCategories.DAILY, "清风抽纸 3包 130抽", 990, 60,
                spec("品牌：清风",
                        "规格：3 层 130 抽 × 3 包",
                        "材质：原生木浆",
                        "适用：宿舍/教室常备",
                        "发货：校内自提")));
        rows.add(onSale(6, ShopCategories.DAILY, "舒客软毛牙刷 两支装", 850, 45,
                spec("品牌：舒客",
                        "毛型：软毛",
                        "数量：2 支/组",
                        "适用：成人日常清洁",
                        "发货：校内自提，包装未拆")));
        rows.add(onSale(7, ShopCategories.DAILY, "蓝月亮洗衣凝珠 20粒", 1590, 30,
                spec("品牌：蓝月亮",
                        "净含量：20 粒",
                        "用法：一粒一次，勿拆开食用",
                        "香型：留香",
                        "适用：宿舍洗衣机小件",
                        "发货：校内自提")));
        rows.add(onSale(8, ShopCategories.FOOD, "农夫山泉饮用天然水 550ml", 200, 200,
                spec("品牌：农夫山泉",
                        "品名：饮用天然水",
                        "净含量：550ml",
                        "包装：PET 瓶装",
                        "保质期：24 个月",
                        "储存：阴凉处，开瓶请尽快饮用",
                        "发货：冰柜/常温都有，校内自提")));
        rows.add(onSale(9, ShopCategories.FOOD, "桃李吐司面包 切片早餐", 450, 25,
                spec("品牌：桃李",
                        "品名：全麦/白吐司（按到货）",
                        "规格：切片装 1 袋",
                        "保质期：请见包装",
                        "食用：建议当日早餐",
                        "发货：当天现货，校内自提")));
        rows.add(offSale(10, ShopCategories.FOOD, "过期试吃饼干（不得上架）", 100, 3,
                spec("状态：过期试吃品",
                        "说明：不得上架售卖。")));
        rows.add(onSale(11, ShopCategories.FOOD, "卫龙大面筋辣条 106g", 590, 80,
                spec("品牌：卫龙",
                        "品名：大面筋",
                        "净含量：106g",
                        "口味：麻辣",
                        "保质期：180 天",
                        "储存：阴凉干燥",
                        "发货：校内自提")));
        rows.add(onSale(12, ShopCategories.FOOD, "可口可乐汽水 330ml罐装", 300, 90,
                spec("品牌：可口可乐",
                        "品名：碳酸饮料",
                        "净含量：330ml",
                        "包装：铝罐",
                        "保质期：12 个月",
                        "储存：阴凉，冰镇口感更佳",
                        "发货：校内自提")));
        rows.add(onSale(13, ShopCategories.STATIONERY, "得力订书机 12号办公装订", 1680, 35,
                spec("品牌：得力",
                        "型号：12 号钉",
                        "装订：约 20 张 70g 纸",
                        "材质：金属机身",
                        "适用：作业装订、社团材料、自习室",
                        "发货：校内自提")));
        rows.add(onSale(14, ShopCategories.DAILY, "维达湿巾 80抽 杀菌清洁", 1280, 50,
                spec("品牌：维达",
                        "规格：80 抽/包",
                        "用途：日常清洁、外出擦手",
                        "特点：加盖保湿",
                        "发货：校内自提")));
        rows.add(onSale(15, ShopCategories.FOOD, "乐事原味薯片 70g", 650, 70,
                spec("品牌：乐事",
                        "口味：原味",
                        "净含量：70g",
                        "类型：膨化食品",
                        "保质期：9 个月",
                        "储存：避免阳光直射",
                        "发货：校内自提")));
        return rows;
    }

    private static String spec(String... lines) {
        return String.join("\n", lines);
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
                ShopDemoPhotos.forProduct(id, category, name));
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
                ShopDemoPhotos.forProduct(id, category, name));
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
