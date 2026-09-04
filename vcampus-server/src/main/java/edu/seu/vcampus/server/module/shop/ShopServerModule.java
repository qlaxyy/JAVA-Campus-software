package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CancelOrderRequest;
import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.ListOrdersResponse;
import edu.seu.vcampus.common.shop.RechargeCampusCardRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

import java.util.Optional;

/**
 * Server entry point owned by the campus-shop module.
 */
public final class ShopServerModule implements ServerModule {

    private final InMemoryShopCatalog catalog = new InMemoryShopCatalog();
    private final ShopCatalogService catalogService = new ShopCatalogService(catalog);
    private final ShopCheckoutService checkoutService = new ShopCheckoutService(
            catalog, new InMemoryCampusCardStore(), new InMemoryShopOrderStore());

    @Override
    public String id() {
        return ModuleNames.SHOP;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(ShopActions.LIST_PRODUCTS, request -> catalogService.listProducts(request, context));
        router.register(ShopActions.PUBLISH_PRODUCT, request -> catalogService.publishProduct(request, context));
        router.register(ShopActions.GET_CAMPUS_CARD, request -> requireSession(request, context,
                session -> Response.success(request, "已返回校园卡余额。", checkoutService.card(session))));
        router.register(ShopActions.RECHARGE_CAMPUS_CARD, request -> requireSession(request, context, session -> {
            if (!(request.getData() instanceof RechargeCampusCardRequest payload)) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.COMMON_INVALID_REQUEST,
                        "充值金额无效。");
            }
            return Response.success(request, "充值成功。", checkoutService.recharge(session, payload));
        }));
        router.register(ShopActions.CREATE_ORDER, request -> requireSession(request, context, session -> {
            if (!(request.getData() instanceof CreateOrderRequest payload)) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.COMMON_INVALID_REQUEST,
                        "订单信息不完整。");
            }
            return Response.success(request, "付款成功。", checkoutService.createOrder(session, payload));
        }));
        router.register(ShopActions.LIST_ORDERS, request -> requireSession(request, context,
                session -> Response.success(
                        request,
                        "已返回我的订单。",
                        new ListOrdersResponse(checkoutService.listMine(session)))));
        router.register(ShopActions.CANCEL_ORDER, request -> requireSession(request, context, session -> {
            if (!(request.getData() instanceof CancelOrderRequest payload)) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.COMMON_INVALID_REQUEST,
                        "订单号无效。");
            }
            return Response.success(request, "订单已取消，金额已退回校园卡。", checkoutService.cancel(session, payload));
        }));
        router.register(ShopActions.LIST_SALES, request -> requireSession(request, context, session -> {
            if (!session.canAdminister(ModuleNames.SHOP)) {
                return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.AUTH_FORBIDDEN,
                        "只有商店管理员可以查看成交订单。");
            }
            return Response.success(
                    request,
                    "已返回成交订单。",
                    new ListOrdersResponse(checkoutService.listSales()));
        }));
    }

    private Response requireSession(
            Request request,
            ServerContext context,
            java.util.function.Function<SessionInfo, Response> action) {
        Optional<SessionInfo> session = context.sessions().findSession(request.getToken());
        if (session.isEmpty()) {
            return Response.failure(request.getRequestId(), ErrorCodes.AUTH_REQUIRED, "请先登录。");
        }
        try {
            return action.apply(session.get());
        } catch (ShopBusinessException exception) {
            return Response.failure(request.getRequestId(), exception.code(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "请求参数无效。");
        }
    }
}
