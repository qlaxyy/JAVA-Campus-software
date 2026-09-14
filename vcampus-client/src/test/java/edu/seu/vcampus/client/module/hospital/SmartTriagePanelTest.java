package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.TriageResultView;
import edu.seu.vcampus.common.hospital.TriageFollowUpOptionView;
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
            assertTrue(context.login("20260006", "123456".toCharArray()).isSuccess());
            AtomicReference<String> openedDepartment = new AtomicReference<>();
            SmartTriagePanel[] panel = new SmartTriagePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SmartTriagePanel(
                        context, () -> { }, openedDepartment::set);
                panel[0].activate();
                namedTextArea(panel[0], "triageDescription")
                        .setText("只是咳嗽，已经持续两天");
                namedButton(panel[0], "submitTriageButton").doClick();
            });

            assertTrue(awaitCondition(() -> optionButton(
                    panel[0], "normal") != null));
            SwingUtilities.invokeAndWait(() -> {
                panel[0].setSize(2_048, 900);
                for (int pass = 0; pass < 5; pass++) layoutTree(panel[0]);
                assertTrue(namedComponent(panel[0], "triageCurrentActions").getWidth()
                        <= 860);
            });
            SwingUtilities.invokeAndWait(
                    () -> optionButton(panel[0], "normal").doClick());
            assertTrue(awaitCondition(() -> !namedButtons(
                    panel[0], "confirmTriageSummaryButton").isEmpty()));
            SwingUtilities.invokeAndWait(
                    () -> namedButton(panel[0], "confirmTriageSummaryButton").doClick());
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openTriageDepartmentButton").size() == 1));
            JButton open = namedButtons(panel[0], "openTriageDepartmentButton").getFirst();
            assertEquals("dept-respiratory", open.getActionCommand());
            SwingUtilities.invokeAndWait(open::doClick);
            assertEquals("dept-respiratory", openedDepartment.get());
        }
    }

    @Test
    void severeBreathingChoiceStopsACoughFromContinuingAsOrdinaryGuidance()
            throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            SmartTriagePanel[] panel = new SmartTriagePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SmartTriagePanel(context, () -> { }, ignored -> { });
                panel[0].activate();
                namedTextArea(panel[0], "triageDescription").setText("只是咳嗽");
                namedButton(panel[0], "submitTriageButton").doClick();
            });

            assertTrue(awaitCondition(() -> optionButton(panel[0], "severe") != null));
            SwingUtilities.invokeAndWait(
                    () -> optionButton(panel[0], "severe").doClick());

            assertTrue(awaitCondition(() -> components(panel[0], JLabel.class).stream()
                    .anyMatch(label -> "请先寻求线下帮助".equals(label.getText()))));
            assertTrue(namedButtons(panel[0], "openTriageDepartmentButton").isEmpty());
        }
    }

    @Test
    void urgentDescriptionSuppressesOrdinaryDepartmentRecommendations() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260006", "123456".toCharArray()).isSuccess());
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

    @Test
    void editingRemovesDependentReplyAndKeepsComposerInsideSmallWindow() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260006", "123456".toCharArray()).isSuccess());
            SmartTriagePanel[] panel = new SmartTriagePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SmartTriagePanel(context, () -> {}, ignored -> {});
                namedTextArea(panel[0], "triageDescription").setText("持续胸痛");
                namedButton(panel[0], "submitTriageButton").doClick();
            });
            assertTrue(awaitCondition(() -> components(panel[0], JLabel.class).stream()
                    .anyMatch(label -> "请先寻求线下帮助".equals(label.getText()))));
            SwingUtilities.invokeAndWait(() -> {
                namedButton(panel[0], "editTriageMessageButton").doClick();
                assertEquals("持续胸痛", namedTextArea(panel[0], "triageDescription").getText());
                assertTrue(components(panel[0], JLabel.class).stream()
                        .noneMatch(label -> "请先寻求线下帮助".equals(label.getText())));
                assertTrue(namedButtons(panel[0], "openTriageDepartmentButton").isEmpty());
                for (int width : new int[]{480, 800, 1200}) {
                    panel[0].setSize(width, 500);
                    panel[0].doLayout();
                    Component composer = namedComponent(panel[0], "triageComposer");
                    assertTrue(composer.getY() >= 0);
                    assertTrue(composer.getY() + composer.getHeight() <= 500);
                    assertTrue(namedComponent(panel[0], "triageConversationScroll").getHeight() > 0);
                }
                namedTextArea(panel[0], "triageDescription").setText("牙齿疼了两天");
                namedButton(panel[0], "submitTriageButton").doClick();
            });
            assertTrue(awaitCondition(() -> !namedButtons(panel[0], "confirmTriageSummaryButton").isEmpty()));
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(namedButtons(panel[0], "openTriageDepartmentButton").isEmpty());
                namedButton(panel[0], "confirmTriageSummaryButton").doClick();
                assertEquals("dept-dental",
                        namedButton(panel[0], "openTriageDepartmentButton").getActionCommand());
                renderSnapshot(panel[0], 480, 600);
                renderSnapshot(panel[0], 1100, 720);
                renderSnapshot(panel[0], 2048, 900);
                namedTextArea(panel[0], "triageDescription").setText("现在突然喘不过气");
                namedButton(panel[0], "submitTriageButton").doClick();
                assertTrue(namedButtons(panel[0], "openTriageDepartmentButton").isEmpty());
            });
            assertTrue(awaitCondition(() -> components(panel[0], JLabel.class).stream()
                    .anyMatch(label -> "请先寻求线下帮助".equals(label.getText()))));
        }
    }

    @Test
    void lateReplyCannotRepopulateRestartedConversation() throws Exception {
        var started = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var returned = new java.util.concurrent.CountDownLatch(1);
        var router = new edu.seu.vcampus.server.infrastructure.ActionRouter();
        router.register(edu.seu.vcampus.common.hospital.HospitalActions.GET_TRIAGE_RECOMMENDATION,
                request -> {
                    var chat = (edu.seu.vcampus.common.hospital.TriageChatRequest) request.getData();
                    boolean old = chat.messages().getFirst().text().equals("旧描述");
                    if (old) {
                        started.countDown();
                        try {
                            release.await(3, java.util.concurrent.TimeUnit.SECONDS);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                        }
                        returned.countDown();
                    }
                    return edu.seu.vcampus.common.protocol.Response.success(request, "ok",
                            new edu.seu.vcampus.common.hospital.TriageChatResult(
                                    old ? "旧回复" : "新回复", "", List.of(),
                                    new TriageResultView(false, "继续描述", List.of())));
                });
        try (CampusServer server = new CampusServer(0, 3, router)) {
            server.start();
            var context = new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
            SmartTriagePanel[] panel = new SmartTriagePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SmartTriagePanel(context, () -> {}, ignored -> {});
                namedTextArea(panel[0], "triageDescription").setText("旧描述");
                namedButton(panel[0], "submitTriageButton").doClick();
                assertTrue(!namedButton(panel[0], "submitTriageButton").isEnabled());
            });
            assertTrue(started.await(1, java.util.concurrent.TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                namedButton(panel[0], "resetTriageButton").doClick();
                namedTextArea(panel[0], "triageDescription").setText("新描述");
                namedButton(panel[0], "submitTriageButton").doClick();
            });
            assertTrue(awaitCondition(() -> components(panel[0], JTextArea.class).stream()
                    .anyMatch(area -> area.getText().equals("新回复"))));
            release.countDown();
            assertTrue(returned.await(1, java.util.concurrent.TimeUnit.SECONDS));
            var drained = new java.util.concurrent.CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                javax.swing.Timer timer = new javax.swing.Timer(150, event -> drained.countDown());
                timer.setRepeats(false);
                timer.start();
            });
            assertTrue(drained.await(1, java.util.concurrent.TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(components(panel[0], JTextArea.class).stream()
                        .noneMatch(area -> area.getText().equals("旧回复") || area.getText().equals("旧描述")));
                assertTrue(namedButton(panel[0], "submitTriageButton").isEnabled());
            });
        } finally {
            release.countDown();
        }
    }

    private static void renderSnapshot(SmartTriagePanel panel, int width, int height) {
        panel.setSize(width, height);
        for (int pass = 0; pass < 5; pass++) layoutTree(panel);
        Component composer = namedComponent(panel, "triageComposer");
        assertTrue(composer.getY() + composer.getHeight() <= height);
        JButton send = namedButton(panel, "submitTriageButton");
        java.awt.Rectangle buttonBounds = SwingUtilities.convertRectangle(
                send.getParent(), send.getBounds(), panel);
        assertTrue(buttonBounds.x >= 0 && buttonBounds.y >= 0);
        assertTrue(buttonBounds.x + buttonBounds.width <= width);
        assertTrue(buttonBounds.y + buttonBounds.height <= height);
        var image = new java.awt.image.BufferedImage(width, height,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        try {
            java.nio.file.Path directory = java.nio.file.Path.of("target", "triage-ui");
            java.nio.file.Files.createDirectories(directory);
            javax.imageio.ImageIO.write(image, "png",
                    directory.resolve("chat-" + width + ".png").toFile());
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void layoutTree(Container parent) {
        parent.invalidate();
        parent.doLayout();
        for (Component component : parent.getComponents()) {
            if (component instanceof Container container) layoutTree(container);
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

    private static JButton optionButton(Container root, String optionId) {
        return namedButtons(root, "triageFollowUpOptionButton").stream()
                .filter(button -> optionId.equals(button.getActionCommand()))
                .findFirst()
                .orElse(null);
    }

    private static JTextArea namedTextArea(Container root, String name) {
        return components(root, JTextArea.class).stream()
                .filter(area -> name.equals(area.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static JLabel namedLabel(Container root, String name) {
        return components(root, JLabel.class).stream()
                .filter(label -> name.equals(label.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static Component namedComponent(Container root, String name) {
        return components(root, Component.class).stream()
                .filter(component -> name.equals(component.getName()))
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
