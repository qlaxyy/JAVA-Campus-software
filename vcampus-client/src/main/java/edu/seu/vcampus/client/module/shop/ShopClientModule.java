package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/** Client entry point owned by the campus-shop module. */
public final class ShopClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.SHOP;
    }

    @Override
    public String displayName() {
        return "校园商店";
    }

    @Override
    public JComponent createView(ClientContext context) {
        return new ShopView(context);
    }
}
