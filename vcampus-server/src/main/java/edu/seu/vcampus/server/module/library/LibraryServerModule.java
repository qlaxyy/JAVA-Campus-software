package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

/** Server entry point owned by the library module. */
public final class LibraryServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.LIBRARY;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        // The module owner registers LIBRARY.* handlers here after contract review.
    }
}
