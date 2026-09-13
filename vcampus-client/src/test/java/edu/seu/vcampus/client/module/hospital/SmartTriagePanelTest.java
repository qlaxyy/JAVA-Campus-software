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
            SwingUtilities.invokeAndWait(
                    () -> optionButton(panel[0], "normal").doClick());
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
    void displaysPatientFacingClarificationWithPreviousQuestionNavigation()
            throws Exception {
        SmartTriagePanel[] panel = new SmartTriagePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1));
            panel[0] = new SmartTriagePanel(context, () -> { }, ignored -> { });
            panel[0].activate();
            panel[0].displayTriageResult(new TriageResultView(
                    false,
                    "请补充一个关键信息",
                    List.of(),
                    "疼痛主要位于上腹部还是下腹部？",
                    2,
                    true,
                    false));
        });

        assertTrue(namedTextArea(panel[0], "triageFollowUpQuestion")
                .getText().contains("上腹部还是下腹部"));
        assertEquals("再确认一下",
                namedLabel(panel[0], "triageFollowUpProgress").getText());
        assertTrue(namedButton(panel[0], "submitTriageFollowUpButton").isEnabled());
        assertTrue(namedButton(panel[0], "uncertainTriageFollowUpButton").isEnabled());
        assertTrue(namedButton(panel[0], "previousTriageQuestionButton").isEnabled());

        SwingUtilities.invokeAndWait(
                () -> namedButton(panel[0], "previousTriageQuestionButton").doClick());
        assertTrue(namedComponent(panel[0], "triageInputPage").isVisible());
    }

    @Test
    void displaysServerOwnedQuestionAsPlainLanguageChoiceCards() throws Exception {
        SmartTriagePanel[] panel = new SmartTriagePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1));
            panel[0] = new SmartTriagePanel(context, () -> { }, ignored -> { });
            panel[0].activate();
            panel[0].displayTriageResult(new TriageResultView(
                    false,
                    "请根据现在能观察到的情况选择",
                    List.of(),
                    "safety_breathing",
                    "你现在呼吸是否明显费力？",
                    List.of(
                            new TriageFollowUpOptionView(
                                    "normal", "没有，能够正常说话"),
                            new TriageFollowUpOptionView(
                                    "severe", "很费力，说话需要停下来喘气"),
                            new TriageFollowUpOptionView("unknown", "不确定")),
                    0,
                    false,
                    false));
        });

        assertEquals(3, namedButtons(
                panel[0], "triageFollowUpOptionButton").size());
        assertEquals("请选择最符合的一项",
                namedLabel(panel[0], "triageFollowUpProgress").getText());
        assertTrue(optionButton(panel[0], "normal").getText().contains("正常说话"));
        assertTrue(namedComponent(panel[0], "triageStructuredAnswerPanel").isVisible());
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
