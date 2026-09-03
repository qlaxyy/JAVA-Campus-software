package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;

import javax.swing.JComponent;

/**
 * Client entry point owned by the course-selection module.
 */
public final class CourseClientModule
    implements ClientModule {

    @Override
    public String id() {
        return ModuleNames.COURSE;
    }

    @Override
    public String displayName() {
        return "选课系统";
    }

    @Override
    public JComponent createView(
        ClientContext context) {

        return new CourseSelectionView(context);
    }
}
