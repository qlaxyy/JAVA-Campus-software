package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/** Client entry point owned by the user-management module. */
public final class UserClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.USER;
    }

    @Override
    public String displayName() {
        return "用户管理";
    }

    @Override
    public JComponent createView(ClientContext context) {
        return new LoginPanel(context);
    }
}
