package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;

/** Server entry point owned by the student-record module. */
public final class StudentServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.STUDENT;
    }

    @Override
    public void registerHandlers(ActionRouter router) {
        // The module owner registers STUDENT.* handlers here after contract review.
    }
}
