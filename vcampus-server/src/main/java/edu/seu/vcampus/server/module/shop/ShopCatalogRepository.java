package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopCategoryDto;
import edu.seu.vcampus.common.shop.ShopListingRecordDto;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for shop categories, products, photos and listing logs. */
interface ShopCatalogRepository {
    List<ProductSummaryDto> listOnSale(ListProductsRequest request);
    ProductSummaryDto publish(PublishProductRequest request, String sellerName);
    ProductSummaryDto update(
            long productId, String name, String description, int priceFen,
            int addStockQty, String operatorName);
    List<ShopListingRecordDto> listListings();
    List<ShopCategoryDto> listCategories();
    ShopCategoryDto addCategory(String name);
    Optional<ProductSummaryDto> findById(long productId);
    boolean decrementStock(long productId, int quantity);
    void incrementStock(long productId, int quantity);
}
