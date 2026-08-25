package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.PlaceholderModuleView;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/** Client entry point owned by the student-record module. */
public final class StudentClientModule implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.STUDENT;
    }

    @Override
    public String displayName() {
        return "学生学籍";
    }

    @Override
    public JComponent createView(CampusClient client) {
        return PlaceholderModuleView.create(displayName(), "完成学籍查询界面和字段设计");
    }
}
