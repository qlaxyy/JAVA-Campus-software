package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ListListingsResponse;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ListProductsResponse;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.UpdateProductRequest;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.module.ServerContext;

import java.util.Objects;
import java.util.Optional;

/**
 * Shop catalog use cases. Temporary memory data will later be replaced by DAO.
 */
public final class ShopCatalogService {

    private final InMemoryShopCatalog catalog;

    /**
     * Creates the catalog service.
     *
     * @param catalog in-memory catalog
     */
    public ShopCatalogService(InMemoryShopCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    /**
     * Lists on-sale products for an authenticated user.
     *
     * @param request incoming request
     * @param context shared session lookup
     * @return list result or an authentication/validation error
     */
    public Response listProducts(Request request, ServerContext context) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        if (context.sessions().findSession(request.getToken()).isEmpty()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录。");
        }
        ListProductsRequest filter = readFilter(request);
        if (filter == null && request.getData() != null) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "商品查询条件无效。");
        }
        ListProductsResponse payload = new ListProductsResponse(catalog.listOnSale(filter));
        return Response.success(request, "已返回上架商品。", payload);
    }

    /**
     * Publishes a new on-sale product for a shop administrator.
     *
     * @param request incoming request
     * @param context shared session lookup
     * @return created product or an authentication/validation error
     */
    public Response publishProduct(Request request, ServerContext context) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录。");
        }
        if (!session.get().canAdminister(ModuleNames.SHOP)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "只有商店管理员可以上架商品。");
        }
        if (!(request.getData() instanceof PublishProductRequest payload)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "上架信息不完整。请上传照片并填写标题、分类、价格、描述和数量。");
        }
        try {
            return Response.success(
                    request,
                    "商品已上架。",
                    catalog.publish(payload, session.get().getDisplayName()));
        } catch (IllegalArgumentException exception) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "上架信息不完整。请至少上传一张照片，并填写标题、分类、价格、描述和数量。");
        }
    }

    /**
     * Updates an on-sale product for a shop administrator.
     *
     * @param request incoming request
     * @param context shared session lookup
     * @return updated product or an error
     */
    public Response updateProduct(Request request, ServerContext context) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录。");
        }
        if (!session.get().canAdminister(ModuleNames.SHOP)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "只有商店管理员可以修改商品。");
        }
        if (!(request.getData() instanceof UpdateProductRequest payload)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "修改信息不完整。");
        }
        try {
            return Response.success(
                    request,
                    "商品已更新。",
                    catalog.update(
                            payload.getProductId(),
                            payload.getName(),
                            payload.getDescription(),
                            payload.getPriceFen(),
                            payload.getAddStockQty(),
                            session.get().getDisplayName()));
        } catch (ShopBusinessException exception) {
            return Response.failure(request.getRequestId(), exception.code(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "修改信息不完整。");
        }
    }

    /**
     * Lists listing records for a shop administrator.
     *
     * @param request incoming request
     * @param context shared session lookup
     * @return listing log or an error
     */
    public Response listListings(Request request, ServerContext context) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录。");
        }
        if (!session.get().canAdminister(ModuleNames.SHOP)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "只有商店管理员可以查看上架数据。");
        }
        return Response.success(
                request,
                "已返回上架数据。",
                new ListListingsResponse(catalog.listListings()));
    }

    private static ListProductsRequest readFilter(Request request) {
        if (request.getData() == null) {
            return ListProductsRequest.allOnSale();
        }
        if (request.getData() instanceof ListProductsRequest filter) {
            return filter;
        }
        return null;
    }
}
