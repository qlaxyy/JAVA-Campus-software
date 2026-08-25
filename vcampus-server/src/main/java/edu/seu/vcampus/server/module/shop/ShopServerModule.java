package edu.seu.vcampus.server.module.shop;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;

/** Server entry point owned by the campus-shop module. */
public final class ShopServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.SHOP;
    }

    @Override
    public void registerHandlers(ActionRouter router) {
        // The module owner registers SHOP.* handlers here after contract review.
    }
}
