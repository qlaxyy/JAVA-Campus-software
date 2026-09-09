package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotSearchPanelTest {

    @Test
    void ordinaryFollowUpModeButtonOpensConsultationSelector() throws Exception {
        AtomicBoolean selectorOpened = new AtomicBoolean();
        SlotSearchPanel[] panel = new SlotSearchPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new SlotSearchPanel(
                new ClientContext(new CampusClient("127.0.0.1", 1)),
                () -> { },
                () -> selectorOpened.set(true)));

        JButton button = namedButton(panel[0], "followUpModeButton");
        assertTrue(button != null);
        SwingUtilities.invokeAndWait(button::doClick);
        assertTrue(selectorOpened.get());
    }

    @Test
    void opensOrdinaryFollowUpSchedulesFromCompletedConsultation() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260002", "123456".toCharArray()).isSuccess());
            ConsultationRecordView consultation = assertInstanceOf(
                    ConsultationRecordView.class,
                    context.send(
                            HospitalActions.SUBMIT_CONSULTATION,
                            new SubmitConsultationRequest(
                                    booking.getAppointmentId(),
                                    "上呼吸道感染（课程演示）",
                                    "如有需要复查血常规。",
                                    "对症处置并观察。",
                                    "无。",
                                    "症状反复时普通复诊。"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());

            SlotSearchPanel[] panel = new SlotSearchPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SlotSearchPanel(context, () -> { });
                panel[0].activateForFollowUp(consultation);
            });

            assertTrue(awaitCondition(() -> enabledBookingButtons(panel[0]).size() >= 1));
            List<String> labels = components(panel[0], JLabel.class).stream()
                    .map(JLabel::getText)
                    .toList();
            assertTrue(labels.contains("预约普通复诊"));
            assertTrue(labels.stream().anyMatch(text -> text.contains("原接诊医生")));
            assertTrue(labels.stream().anyMatch(text -> text.contains("复诊科室")));
        }
    }

    @Test
    void drillsDownDepartmentsAndFiltersEnabledDailySchedulesByDoctor()
            throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());

            SlotSearchPanel[] panel = new SlotSearchPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SlotSearchPanel(context, () -> { });
                panel[0].activate();
            });

            assertTrue(awaitCondition(() -> enabledButton(panel[0], "外科") != null));
            click(panel[0], "外科");
            assertTrue(awaitCondition(() -> enabledButton(panel[0], "骨科") != null));
            click(panel[0], "骨科");
            assertTrue(awaitCondition(() -> enabledButton(panel[0], "骨关节外科") != null));
            click(panel[0], "骨关节外科");

            assertTrue(awaitCondition(() -> visibleTextField(panel[0]) != null));
            SwingUtilities.invokeAndWait(() -> visibleTextField(panel[0]).setText("林医生"));
            assertTrue(awaitCondition(() -> enabledBookingButtons(panel[0]).size() == 2));

            AtomicReference<List<String>> labels = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(2, enabledBookingButtons(panel[0]).size());
                labels.set(components(panel[0], JLabel.class).stream()
                        .map(JLabel::getText)
                        .filter(text -> text != null && text.startsWith("余号 "))
                        .toList());
            });
            assertEquals(2, labels.get().size());
            assertFalse(labels.get().stream().anyMatch(text -> text.contains(" / ")));

            SwingUtilities.invokeAndWait(() -> visibleTextField(panel[0]).setText("不存在"));
            assertTrue(awaitCondition(() -> enabledBookingButtons(panel[0]).isEmpty()));

            Response booking = context.send(
                    HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit("slot-joint-lin-am"));
            assertTrue(booking.isSuccess());

            click(panel[0], "‹ 重新选择科室");
            JTextField departmentSearch = namedTextField(
                    panel[0], "departmentSearchField");
            assertTrue(departmentSearch != null);
            assertEquals(Component.LEFT_ALIGNMENT, departmentSearch.getAlignmentX());
            SwingUtilities.invokeAndWait(() -> departmentSearch.setText("骨关节"));
            assertTrue(awaitCondition(() -> enabledButton(panel[0], "骨关节外科") != null));
            assertTrue(awaitCondition(() -> enabledButton(panel[0], "查看号源 →") != null));
            assertTrue(components(panel[0], HospitalTheme.SurfacePanel.class).stream()
                    .anyMatch(surface -> new Dimension(360, 68)
                            .equals(surface.getPreferredSize())));
            click(panel[0], "查看号源 →");

            JTextField doctorSearch = namedTextField(panel[0], "doctorSearchField");
            assertTrue(awaitCondition(() -> doctorSearch.isVisible()));
            SwingUtilities.invokeAndWait(() -> doctorSearch.setText("林医生"));
            assertTrue(awaitCondition(() -> enabledBookingButtons(panel[0]).size() == 1));
            assertEquals(1, visibleButtons(panel[0], "已预约").size());
        }
    }

    @Test
    void opensRecommendedDepartmentWithoutRepeatingDepartmentSelection() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());

            SlotSearchPanel[] panel = new SlotSearchPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new SlotSearchPanel(context, () -> { });
                panel[0].activateForDepartment("dept-respiratory");
            });

            assertTrue(awaitCondition(() -> components(panel[0], JLabel.class).stream()
                    .filter(label -> isVisibleWithin(panel[0], label))
                    .map(JLabel::getText)
                    .anyMatch(text -> text != null && text.contains("呼吸内科"))));
            JTextField doctorSearch = namedTextField(panel[0], "doctorSearchField");
            assertTrue(awaitCondition(() -> doctorSearch != null
                    && isVisibleWithin(panel[0], doctorSearch)));
        }
    }

    private static void click(Container root, String text) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AbstractButton button = enabledButton(root, text);
            if (button == null) {
                throw new AssertionError("button not found: " + text);
            }
            button.doClick();
        });
    }

    private static AbstractButton enabledButton(Container root, String text) {
        return components(root, AbstractButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .filter(Component::isEnabled)
                .filter(button -> isVisibleWithin(root, button))
                .findFirst()
                .orElse(null);
    }

    private static JTextField visibleTextField(Container root) {
        return components(root, JTextField.class).stream()
                .filter(field -> isVisibleWithin(root, field))
                .findFirst()
                .orElse(null);
    }

    private static JTextField namedTextField(Container root, String name) {
        return components(root, JTextField.class).stream()
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .orElse(null);
    }

    private static JButton namedButton(Container root, String name) {
        return components(root, JButton.class).stream()
                .filter(button -> name.equals(button.getName()))
                .findFirst()
                .orElse(null);
    }

    private static boolean isVisibleWithin(Container root, Component component) {
        Component current = component;
        while (current != null && current != root) {
            if (!current.isVisible()) {
                return false;
            }
            current = current.getParent();
        }
        return current == root;
    }

    private static List<JButton> enabledBookingButtons(Container root) {
        return components(root, JButton.class).stream()
                .filter(button -> "立即预约".equals(button.getText()))
                .filter(Component::isEnabled)
                .filter(button -> isVisibleWithin(root, button))
                .toList();
    }

    private static List<JButton> visibleButtons(Container root, String text) {
        return components(root, JButton.class).stream()
                .filter(button -> text.equals(button.getText()))
                .filter(button -> isVisibleWithin(root, button))
                .toList();
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
            AtomicBoolean satisfied = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
