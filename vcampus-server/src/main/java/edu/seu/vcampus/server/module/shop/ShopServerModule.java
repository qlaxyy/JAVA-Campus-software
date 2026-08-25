package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

/** Server entry point owned by the campus-shop module. */
public final class ShopServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.SHOP;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        // The module owner registers SHOP.* handlers here after contract review.
    }
}
