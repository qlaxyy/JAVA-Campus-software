package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ProductSaleStatus;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopCategories;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * In-memory shop catalog used until Access DAO replaces this class.
 */
public final class InMemoryShopCatalog {

    private static final String CAMPUS_SELLER = "校园商店";

    private final List<ProductSummaryDto> products = new ArrayList<>(seed());
    private long nextId = 11L;

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
        return product;
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
}
