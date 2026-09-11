package edu.seu.vcampus.server.module;

import edu.seu.vcampus.common.protocol.Actions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.card.CampusCardWallet;
import edu.seu.vcampus.server.module.card.CardServerModule;
import edu.seu.vcampus.server.module.card.InMemoryCampusCardWallet;
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

    /**
     * Builds the in-memory campus router. Shop and hospital share one memory wallet.
     *
     * @return fully initialized router
     */
    public static ActionRouter createRouter() {
        InMemoryCampusCardWallet wallet = new InMemoryCampusCardWallet();
        return createRouter(
                new InMemoryAuthenticationService(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new HospitalServerModule(wallet),
                new ShopServerModule(wallet));
    }

    /** Builds the production campus router with module data persisted in Access. */
    public static ActionRouter createPersistentRouter(Path databasePath) {
        return createPersistentRouter(databasePath, new InMemoryCampusCardWallet());
    }

    /**
     * Builds the production campus router that pays through {@code campusCards}.
     *
     * @param databasePath shared Access file
     * @param campusCards campus-card wallet, typically a TCP client to port 8889
     * @return campus action router
     */
    public static ActionRouter createPersistentRouter(
            Path databasePath, CampusCardWallet campusCards) {
        return createAccessCampusRouter(
                databasePath,
                UserAuthenticationBootstrap.createAccessBacked(databasePath),
                CourseServerModule.createAccessBacked(databasePath),
                campusCards);
    }

    /**
     * Builds the production campus router using an already constructed login service.
     * The campus-card TCP gateway must share this same authentication instance.
     */
    public static ActionRouter createAccessCampusRouter(
            Path databasePath,
            InMemoryAuthenticationService authentication,
            CourseServerModule courseModule,
            CampusCardWallet campusCards) {
        return createRouter(
                authentication,
                courseModule,
                LibraryServerModule.createAccessBacked(databasePath, campusCards),
                HospitalServerModule.createAccessBacked(databasePath, campusCards),
                new ShopServerModule(campusCards));
    }

    /**
     * Builds the isolated campus-card router listening on the card TCP port.
     *
     * @param wallet Access or memory wallet
     * @param context shared session lookup
     * @return card-only action router
     */
    public static ActionRouter createCardRouter(
            CampusCardWallet wallet,
            ServerContext context) {
        ActionRouter router = new ActionRouter();
        router.register(Actions.PING, request ->
                Response.success(request, "Campus card gateway is reachable.", "PONG"));
        new CardServerModule(wallet).registerHandlers(router, context);
        return router;
    }

    private static ActionRouter createRouter(
            InMemoryAuthenticationService authentication,
            CourseServerModule courseModule,
            LibraryServerModule libraryModule,
            HospitalServerModule hospitalModule,
            ShopServerModule shopModule) {
        ActionRouter router = new ActionRouter();
        ServerContext context = context(authentication, courseModule);
        router.register(Actions.PING, request ->
                Response.success(request, "Server is reachable.", "PONG"));
        modules(authentication, courseModule, libraryModule, hospitalModule, shopModule)
                .forEach(module -> module.registerHandlers(router, context));
        return router;
    }

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

    /**
     * Returns the six campus business modules.
     *
     * @return immutable six-module list
     */
    public static List<ServerModule> modules() {
        return modules(
                new InMemoryAuthenticationService(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new HospitalServerModule(),
                new ShopServerModule());
    }

    private static List<ServerModule> modules(
            InMemoryAuthenticationService authentication,
            CourseServerModule courseModule,
            LibraryServerModule libraryModule,
            HospitalServerModule hospitalModule,
            ShopServerModule shopModule) {
        return List.of(
                new UserServerModule(authentication),
                new StudentServerModule(),
                courseModule,
                libraryModule,
                shopModule,
                hospitalModule);
    }
}
