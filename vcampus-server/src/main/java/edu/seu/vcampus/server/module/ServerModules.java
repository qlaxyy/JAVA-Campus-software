package edu.seu.vcampus.server.module;

import edu.seu.vcampus.common.protocol.Actions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.card.AccessCampusCardStore;
import edu.seu.vcampus.server.module.card.CampusCardWallet;
import edu.seu.vcampus.server.module.card.CardServerModule;
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
 * Catalog of the six campus business modules plus the campus-card gateway.
 */
public final class ServerModules {

    private ServerModules() {
    }

    /** Builds the in-memory campus router used by unit tests. */
    public static ActionRouter createRouter() {
        InMemoryAuthenticationService authentication =
                new InMemoryAuthenticationService();
        return createRouter(
                authentication,
                new StudentServerModule(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new ShopServerModule(),
                new HospitalServerModule());
    }

    /**
     * Builds a persistent campus router for compatibility with existing callers.
     * Normal production startup uses {@code VirtualCampusRuntime} and exposes
     * the same wallet through the dedicated card port as well.
     */
    public static ActionRouter createPersistentRouter(Path databasePath) {
        InMemoryAuthenticationService authentication =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        CourseServerModule courseModule = CourseServerModule.createAccessBacked(
                databasePath,
                authentication,
                authentication.teacherDirectory());
        CampusCardWallet campusCards =
                new AccessCampusCardStore(new AccessDatabase(databasePath));
        return createAccessCampusRouter(
                databasePath,
                authentication,
                courseModule,
                campusCards);
    }

    /**
     * Builds the main campus router while keeping every module on the same
     * Access database and the same authentication/teacher directories.
     */
    public static ActionRouter createAccessCampusRouter(
            Path databasePath,
            InMemoryAuthenticationService authentication,
            CourseServerModule courseModule,
            CampusCardWallet campusCards) {
        return createRouter(
                authentication,
                StudentServerModule.createAccessBacked(databasePath, authentication),
                courseModule,
                LibraryServerModule.createAccessBacked(databasePath, campusCards),
                ShopServerModule.createAccessBacked(databasePath),
                HospitalServerModule.createAccessBacked(
                        databasePath,
                        authentication,
                        campusCards));
    }

    /** Builds the isolated campus-card action router for the card TCP port. */
    public static ActionRouter createCardRouter(
            CampusCardWallet wallet,
            ServerContext context) {
        ActionRouter router = new ActionRouter();
        router.register(Actions.PING, request ->
                Response.success(
                        request,
                        "Campus card gateway is reachable.",
                        "PONG"));
        new CardServerModule(wallet).registerHandlers(router, context);
        return router;
    }

    private static ActionRouter createRouter(
            InMemoryAuthenticationService authentication,
            StudentServerModule studentModule,
            CourseServerModule courseModule,
            LibraryServerModule libraryModule,
            ShopServerModule shopModule,
            HospitalServerModule hospitalModule) {
        ActionRouter router = new ActionRouter();
        ServerContext context = context(authentication, courseModule);
        router.register(Actions.PING, request ->
                Response.success(request, "Server is reachable.", "PONG"));
        modules(
                authentication,
                studentModule,
                courseModule,
                libraryModule,
                shopModule,
                hospitalModule)
                .forEach(module -> module.registerHandlers(router, context));
        return router;
    }

    /** Creates the shared authorization context used by both TCP listeners. */
    public static ServerContext context(
            InMemoryAuthenticationService authentication,
            CourseServerModule courseModule) {
        return new ServerContext(
                authentication,
                authentication,
                authentication,
                authentication.teacherDirectory(),
                courseModule.teacherStudentAccess());
    }

    /** Returns the six agreed business modules. */
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
