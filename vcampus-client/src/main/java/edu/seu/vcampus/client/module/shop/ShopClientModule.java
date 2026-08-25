package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.PlaceholderModuleView;
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
    public JComponent createView(CampusClient client) {
        return PlaceholderModuleView.create(displayName(), "完成商品浏览界面和库存数据设计");
    }
}
