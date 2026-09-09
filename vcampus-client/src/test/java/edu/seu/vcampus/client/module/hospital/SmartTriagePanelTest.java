package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartTriagePanelTest {

    @Test
    void recommendsBookableDepartmentAndOpensItsSlots() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            AtomicReference<String> openedDepartment = new AtomicReference<>();
            SmartTriagePanel[] panel = new SmartTriagePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SmartTriagePanel(
                        context, () -> { }, openedDepartment::set);
                panel[0].activate();
                namedTextArea(panel[0], "triageDescription")
                        .setText("咳嗽并伴有发热，已经持续两天");
                namedButton(panel[0], "submitTriageButton").doClick();
            });

            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openTriageDepartmentButton").size() == 1));
            JButton open = namedButtons(panel[0], "openTriageDepartmentButton").getFirst();
            assertEquals("dept-respiratory", open.getActionCommand());
            SwingUtilities.invokeAndWait(open::doClick);
            assertEquals("dept-respiratory", openedDepartment.get());
        }
    }

    @Test
    void urgentDescriptionSuppressesOrdinaryDepartmentRecommendations() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            SmartTriagePanel[] panel = new SmartTriagePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SmartTriagePanel(context, () -> { }, ignored -> { });
                panel[0].activate();
                namedTextArea(panel[0], "triageDescription").setText("持续胸痛");
                namedButton(panel[0], "submitTriageButton").doClick();
            });

            assertTrue(awaitCondition(() -> components(panel[0], JLabel.class).stream()
                    .anyMatch(label -> "请先寻求线下帮助".equals(label.getText()))));
            assertTrue(namedButtons(panel[0], "openTriageDepartmentButton").isEmpty());
        }
    }

    private static JButton namedButton(Container root, String name) {
        return namedButtons(root, name).stream()
                .findFirst()
                .orElseThrow();
    }

    private static List<JButton> namedButtons(Container root, String name) {
        return components(root, JButton.class).stream()
                .filter(button -> name.equals(button.getName()))
                .toList();
    }

    private static JTextArea namedTextArea(Container root, String name) {
        return components(root, JTextArea.class).stream()
                .filter(area -> name.equals(area.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static <T extends Component> List<T> components(
            Container root,
            Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(components(container, type));
            }
        }
        return found;
    }

    private static boolean awaitCondition(java.util.function.BooleanSupplier condition)
            throws Exception {
        for (int attempt = 0; attempt < 150; attempt++) {
            AtomicReference<Boolean> satisfied = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
