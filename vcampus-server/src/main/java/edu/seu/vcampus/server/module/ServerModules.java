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
     * Builds the in-memory router and lets every module register its handlers.
     *
     * @return fully initialized router
     */
    public static ActionRouter createRouter() {
        InMemoryAuthenticationService authentication = new InMemoryAuthenticationService();
        return createRouter(
                authentication,
                new StudentServerModule(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new ShopServerModule(),
                new HospitalServerModule());
    }

    /** Builds the production router with supported module data persisted in Access. */
    public static ActionRouter createPersistentRouter(Path databasePath) {
        InMemoryAuthenticationService authentication =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        return createRouter(
                authentication,
                StudentServerModule.createAccessBacked(databasePath, authentication),
                CourseServerModule.createAccessBacked(
                        databasePath, authentication, authentication.teacherDirectory()),
                LibraryServerModule.createAccessBacked(databasePath),
                ShopServerModule.createAccessBacked(databasePath),
                HospitalServerModule.createAccessBacked(databasePath, authentication));
    }

    private static ActionRouter createRouter(
            InMemoryAuthenticationService authentication,
            StudentServerModule studentModule,
            CourseServerModule courseModule,
            LibraryServerModule libraryModule,
            ShopServerModule shopModule,
            HospitalServerModule hospitalModule) {
        ActionRouter router = new ActionRouter();
        ServerContext context =
            new ServerContext(
                authentication,
                authentication,
                authentication,
                authentication.teacherDirectory(),
                courseModule.teacherStudentAccess());
        router.register(Actions.PING, request ->
                Response.success(request, "Server is reachable.", "PONG"));
        modules(authentication, studentModule, courseModule, libraryModule, shopModule, hospitalModule)
                .forEach(module -> module.registerHandlers(router, context));
        return router;
    }

    /**
     * Returns the fixed module catalog. New optional modules require team review.
     *
     * @return immutable six-module list
     */
    public static List<ServerModule> modules() {
        return modules(
                new InMemoryAuthenticationService(),
                new StudentServerModule(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new ShopServerModule(),
                new HospitalServerModule());
    }

    private static List<ServerModule> modules(
            InMemoryAuthenticationService authentication,
            StudentServerModule studentModule,
            CourseServerModule courseModule,
            LibraryServerModule libraryModule,
            ShopServerModule shopModule,
            HospitalServerModule hospitalModule) {
        return List.of(
                new UserServerModule(authentication),
                studentModule,
                courseModule,
                libraryModule,
                shopModule,
                hospitalModule);
    }
}
