package edu.seu.vcampus.server.module;

import edu.seu.vcampus.common.protocol.Actions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.course.CourseServerModule;
import edu.seu.vcampus.server.module.hospital.HospitalServerModule;
import edu.seu.vcampus.server.module.library.LibraryServerModule;
import edu.seu.vcampus.server.module.shop.ShopServerModule;
import edu.seu.vcampus.server.module.student.StudentServerModule;
import edu.seu.vcampus.server.module.user.InMemoryAuthenticationService;
import edu.seu.vcampus.server.module.user.UserAuthenticationBootstrap;
import edu.seu.vcampus.server.module.user.UserServerModule;

import java.nio.file.Path;
import java.util.List;

/**
 * Fixed catalog of the six agreed server-side business modules.
 */
public final class ServerModules {

    private ServerModules() {
    }

    /**
     * Builds the in-memory router and lets every module
     * register its handlers.
     *
     * @return fully initialised router
     */
    public static ActionRouter createRouter() {

        return createRouter(
            new InMemoryAuthenticationService(),
            new HospitalServerModule(),
            new CourseServerModule());
    }

    /**
     * Builds the production router with persistent
     * Access-backed modules.
     */
    public static ActionRouter createPersistentRouter(
        Path databasePath) {

        return createRouter(
            UserAuthenticationBootstrap
                .createAccessBacked(
                    databasePath),
            HospitalServerModule
                .createAccessBacked(
                    databasePath),
            CourseServerModule
                .createAccessBacked(
                    databasePath));
    }

    /**
     * Creates the router using the supplied module
     * implementations.
     */
    private static ActionRouter createRouter(
        InMemoryAuthenticationService authentication,
        HospitalServerModule hospitalModule,
        CourseServerModule courseModule) {

        ActionRouter router =
            new ActionRouter();

        ServerContext context =
            new ServerContext(
                authentication,
                authentication,
                authentication);

        router.register(
            Actions.PING,
            request ->
                Response.success(
                    request,
                    "Server is reachable.",
                    "PONG"));

        modules(
            authentication,
            hospitalModule,
            courseModule)
            .forEach(module ->
                module.registerHandlers(
                    router,
                    context));
        return router;
    }

    /**
     * Returns the fixed module catalogue using
     * in-memory implementations.
     *
     * @return immutable six-module list
     */
    public static List<ServerModule> modules() {

        return modules(
            new InMemoryAuthenticationService(),
            new HospitalServerModule(),
            new CourseServerModule());
    }

    /**
     * Returns all server business modules.
     */
    private static List<ServerModule> modules(
        InMemoryAuthenticationService authentication,
        HospitalServerModule hospitalModule,
        CourseServerModule courseModule) {

        return List.of(
            new UserServerModule(
                authentication),
            new StudentServerModule(),
            courseModule,
            new LibraryServerModule(),
            new ShopServerModule(),
            hospitalModule);
    }
}
