package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

/**
 * Server entry point owned by the campus-shop module.
 */
public final class ShopServerModule implements ServerModule {

    private final ShopCatalogService catalogService = new ShopCatalogService(new InMemoryShopCatalog());

    @Override
    public String id() {
        return ModuleNames.SHOP;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(ShopActions.LIST_PRODUCTS, request -> catalogService.listProducts(request, context));
        router.register(ShopActions.PUBLISH_PRODUCT, request -> catalogService.publishProduct(request, context));
    }
}
