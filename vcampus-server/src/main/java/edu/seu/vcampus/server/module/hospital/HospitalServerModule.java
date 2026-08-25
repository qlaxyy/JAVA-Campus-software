package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;

/** Server entry point owned by the hospital-appointment module. */
public final class HospitalServerModule implements ServerModule {

    @Override
    public String id() {
        return ModuleNames.HOSPITAL;
    }

    @Override
    public void registerHandlers(ActionRouter router) {
        // The module owner registers HOSPITAL.* handlers here after contract review.
    }
}
