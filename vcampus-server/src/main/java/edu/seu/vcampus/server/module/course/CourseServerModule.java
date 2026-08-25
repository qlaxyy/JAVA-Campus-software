package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

/** Server entry point owned by the course-selection module. */
public final class CourseServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.COURSE;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        // The module owner registers COURSE.* handlers here after contract review.
    }
}
