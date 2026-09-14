package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryShopCatalogTest {

    private final InMemoryShopCatalog catalog = new InMemoryShopCatalog();

    @Test
    void listsElevenOnSaleProductsAndHidesOffSaleRows() {
        List<ProductSummaryDto> products = catalog.listOnSale(ListProductsRequest.allOnSale());

        assertEquals(11, products.size());
        assertTrue(products.stream().noneMatch(item -> item.getStockQty() < 0));
        assertTrue(products.stream().noneMatch(item -> item.getName().contains("过期")));
        assertTrue(products.stream().allMatch(item -> item.getPhotos().getFirst().length > 8_000));
        assertTrue(products.stream().allMatch(item -> !item.getDescription().isBlank()));
    }

    @Test
    void filtersByDescriptionKeyword() {
        List<ProductSummaryDto> matches = catalog.listOnSale(new ListProductsRequest("自习室", null));

        assertEquals(1, matches.size());
        assertEquals("得力订书机 12号办公装订", matches.getFirst().getName());
    }

    @Test
    void publishAddsOnSaleProduct() {
        ProductSummaryDto published = catalog.publish(
                new edu.seu.vcampus.common.shop.PublishProductRequest(
                        "二手直尺",
                        1L,
                        "九成新，无刻度磨损。",
                        150,
                        3,
                        ShopDemoPhotos.forProduct("文具", "直尺")),
                "商店管理员");

        assertEquals("二手直尺", published.getName());
        assertEquals(12, catalog.listOnSale(ListProductsRequest.allOnSale()).size());
    }

    @Test
    void filtersByCategory() {
        List<ProductSummaryDto> food = catalog.listOnSale(new ListProductsRequest(null, 3L));

        assertEquals(5, food.size());
        assertEquals("农夫山泉饮用天然水 550ml", food.getFirst().getName());
        assertEquals("桃李吐司面包 切片早餐", food.get(1).getName());
    }

    @Test
    void decrementStockReducesQuantity() {
        assertTrue(catalog.decrementStock(8, 3));
        assertEquals(197, catalog.findById(8).orElseThrow().getStockQty());
        catalog.incrementStock(8, 3);
        assertEquals(200, catalog.findById(8).orElseThrow().getStockQty());
    }

    @Test
    void updateChangesPriceAndWritesListing() {
        InMemoryShopCatalog local = new InMemoryShopCatalog();
        ProductSummaryDto updated = local.update(
                8, "农夫山泉饮用天然水 550ml",
                "品牌：农夫山泉\n冰柜常温都有。", 250, 10, "演示商店管理员");
        assertEquals(250, updated.getPriceFen());
        assertEquals(210, updated.getStockQty());
        assertTrue(local.listListings().stream().anyMatch(row ->
                row.getAction().equals("调整") && row.getProductId() == 8));
    }

    @Test
    void addCategoryThenPublishUsesNewCategory() {
        InMemoryShopCatalog local = new InMemoryShopCatalog();
        edu.seu.vcampus.common.shop.ShopCategoryDto camera = local.addCategory("相机");
        ProductSummaryDto published = local.publish(
                new edu.seu.vcampus.common.shop.PublishProductRequest(
                        "二手数码相机",
                        camera.getCategoryId(),
                        "快门正常，适合摄影社借用演示。",
                        19900,
                        1,
                        ShopDemoPhotos.forProduct("相机", "相机")),
                "商店管理员");
        assertEquals("相机", published.getCategoryName());
        assertEquals(4, local.listCategories().size());
    }

    @Test
    void duplicateCategoryNameIsRejected() {
        InMemoryShopCatalog local = new InMemoryShopCatalog();
        try {
            local.addCategory("文具");
            org.junit.jupiter.api.Assertions.fail("expected duplicate category");
        } catch (ShopBusinessException exception) {
            assertEquals(edu.seu.vcampus.common.protocol.ErrorCodes.SHOP_CATEGORY_EXISTS, exception.code());
        }
    }
}
