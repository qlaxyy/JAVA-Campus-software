package edu.seu.vcampus.server.module;

import edu.seu.vcampus.common.protocol.Actions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.course.CourseServerModule;
import edu.seu.vcampus.server.module.hospital.HospitalServerModule;
import edu.seu.vcampus.server.module.library.LibraryServerModule;
import edu.seu.vcampus.server.module.shop.ShopServerModule;
import edu.seu.vcampus.server.module.student.StudentServerModule;
import edu.seu.vcampus.server.module.user.UserServerModule;
import edu.seu.vcampus.server.module.user.InMemoryAuthenticationService;
import edu.seu.vcampus.server.module.user.UserAuthenticationBootstrap;

import java.nio.file.Path;
import java.util.List;

/**
 * Fixed catalog of the six agreed server-side business modules.
 */
public final class ServerModules {

    private ServerModules() {
    }

    /**
     * Builds the production router and lets every module register its handlers.
     *
     * @return fully initialized router
     */
    public static ActionRouter createRouter() {
        return createRouter(new InMemoryAuthenticationService());
    }

    /** Builds the production router with accounts persisted in Access. */
    public static ActionRouter createPersistentRouter(Path databasePath) {
        return createRouter(UserAuthenticationBootstrap.createAccessBacked(databasePath));
    }

    private static ActionRouter createRouter(InMemoryAuthenticationService authentication) {
        ActionRouter router = new ActionRouter();
        ServerContext context = new ServerContext(authentication);
        router.register(Actions.PING, request ->
                Response.success(request, "Server is reachable.", "PONG"));
        modules(authentication).forEach(module -> module.registerHandlers(router, context));
        return router;
    }

    /**
     * Returns the fixed module catalog. New optional modules require team review.
     *
     * @return immutable six-module list
     */
    public static List<ServerModule> modules() {
        return modules(new InMemoryAuthenticationService());
    }

    private static List<ServerModule> modules(InMemoryAuthenticationService authentication) {
        return List.of(
                new UserServerModule(authentication),
                new StudentServerModule(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new ShopServerModule(),
                new HospitalServerModule());
    }
}
