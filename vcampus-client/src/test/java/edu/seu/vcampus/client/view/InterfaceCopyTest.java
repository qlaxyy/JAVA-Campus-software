package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.module.library.*;
import edu.seu.vcampus.client.module.hospital.HospitalView;
import edu.seu.vcampus.client.module.student.StudentView;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InterfaceCopyTest {
    @Test void developerNarrationIsAbsentFromUserFacingComponentTrees() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1, 100));
            for (JComponent panel : List.of(new LibraryAdminPanel(context), new LibraryModePanel(context),
                    new SelfServicePanel(context), new StudentView(context), new HospitalView(context))) {
                List<String> labels = new ArrayList<>();
                collect(panel, labels);
                for (String text : labels) {
                    for (String forbidden : List.of("由服务器再次校验", "实际权限由服务器", "权限校验",
                            "当前数据库", "进入页面后自动读取", "全量信息同步", "全周期管理")) {
                        assertFalse(text.contains(forbidden), panel.getClass().getSimpleName() + ": " + text);
                    }
                }
            }
        });
    }

    private static void collect(Component component, List<String> labels) {
        if (component instanceof JLabel label && label.getText() != null) { labels.add(label.getText()); }
        if (component instanceof JTextArea area && area.getText() != null) { labels.add(area.getText()); }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) { collect(child, labels); }
        }
    }
}
