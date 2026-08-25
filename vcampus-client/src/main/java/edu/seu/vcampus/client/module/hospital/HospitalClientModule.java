package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.PlaceholderModuleView;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/** Client entry point owned by the hospital-appointment module. */
public final class HospitalClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.HOSPITAL;
    }

    @Override
    public String displayName() {
        return "校医院";
    }

    @Override
    public JComponent createView(ClientContext context) {
        return PlaceholderModuleView.create(displayName(), "完成排班查询界面和预约数据设计");
    }
}
