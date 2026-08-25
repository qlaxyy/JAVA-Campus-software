package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;

/** Server entry point owned by the user-management module. */
public final class UserServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.USER;
    }

    @Override
    public void registerHandlers(ActionRouter router) {
        // The module owner registers USER.* handlers here after contract review.
    }
}
