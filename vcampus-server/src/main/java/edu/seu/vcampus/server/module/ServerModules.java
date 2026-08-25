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
        ActionRouter router = new ActionRouter();
        router.register(Actions.PING, request ->
                Response.success(request, "Server is reachable.", "PONG"));
        modules().forEach(module -> module.registerHandlers(router));
        return router;
    }

    /**
     * Returns the fixed module catalog. New optional modules require team review.
     *
     * @return immutable six-module list
     */
    public static List<ServerModule> modules() {
        return List.of(
                new UserServerModule(),
                new StudentServerModule(),
                new CourseServerModule(),
                new LibraryServerModule(),
                new ShopServerModule(),
                new HospitalServerModule());
    }
}
