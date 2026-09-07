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
    void listsEightOnSaleProductsAndHidesOffSaleRows() {
        List<ProductSummaryDto> products = catalog.listOnSale(ListProductsRequest.allOnSale());

        assertEquals(8, products.size());
        assertTrue(products.stream().noneMatch(item -> item.getStockQty() < 0));
        assertTrue(products.stream().noneMatch(item -> item.getName().contains("过期")));
        assertTrue(products.stream().allMatch(item -> !item.getPhotos().isEmpty()));
        assertTrue(products.stream().allMatch(item -> !item.getDescription().isBlank()));
    }

    @Test
    void filtersByDescriptionKeyword() {
        List<ProductSummaryDto> matches = catalog.listOnSale(new ListProductsRequest("自习室", null));

        assertEquals(1, matches.size());
        assertEquals("A4 草稿纸 100 张", matches.getFirst().getName());
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
        assertEquals(9, catalog.listOnSale(ListProductsRequest.allOnSale()).size());
    }

    @Test
    void filtersByCategory() {
        List<ProductSummaryDto> food = catalog.listOnSale(new ListProductsRequest(null, 3L));

        assertEquals(2, food.size());
        assertEquals("矿泉水 550ml", food.getFirst().getName());
        assertEquals("面包 1 个", food.get(1).getName());
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
                8, "矿泉水 550ml", "冰柜常温都有。上课带走方便，瓶身轻。", 250, 10, "演示商店管理员");
        assertEquals(250, updated.getPriceFen());
        assertEquals(210, updated.getStockQty());
        assertTrue(local.listListings().stream().anyMatch(row ->
                row.getAction().equals("调整") && row.getProductId() == 8));
    }
}
