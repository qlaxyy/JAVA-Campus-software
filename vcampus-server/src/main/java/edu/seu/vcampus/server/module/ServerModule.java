package edu.seu.vcampus.server.module;

import edu.seu.vcampus.server.infrastructure.ActionRouter;

/**
 * Extension point owned by one server-side business module.
 */
public interface ServerModule {

    /** @return stable module identifier */
    String id();

    /**
     * Registers this module's public request handlers.
     *
     * @param router shared server action router
     */
    void registerHandlers(ActionRouter router);
}
