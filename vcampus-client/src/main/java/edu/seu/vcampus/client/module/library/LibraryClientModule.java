package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.PlaceholderModuleView;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/** Client entry point owned by the library module. */
public final class LibraryClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.LIBRARY;
    }

    @Override
    public String displayName() {
        return "图书馆";
    }

    @Override
    public JComponent createView(ClientContext context) {
        return PlaceholderModuleView.create(displayName(), "完成图书检索界面和馆藏数据设计");
    }
}
