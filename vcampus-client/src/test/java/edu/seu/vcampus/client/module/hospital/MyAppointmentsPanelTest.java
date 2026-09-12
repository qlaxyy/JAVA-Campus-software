package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.AppointmentListResponse;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyAppointmentsPanelTest {

    @Test
    void automaticallyLoadsAppointmentsForTheLoggedInPatient() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());
            assertTrue(context.send(
                    HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit("slot-general-1")).isSuccess());
            assertTrue(context.send(
                    HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit("slot-general-3")).isSuccess());
            Response listResponse = context.send(
                    HospitalActions.SEARCH_APPOINTMENTS, null);
            AppointmentListResponse list = assertInstanceOf(
                    AppointmentListResponse.class, listResponse.getData());
            String nearDate = DateTimeFormatter.ofPattern("M月d日")
                    .format(list.getAppointments().getFirst().getStartTime());
            String farDate = DateTimeFormatter.ofPattern("M月d日")
                    .format(list.getAppointments().getLast().getStartTime());

            MyAppointmentsPanel[] panel = new MyAppointmentsPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new MyAppointmentsPanel(
                        context,
                        () -> { },
                        () -> { },
                        appointment -> true);
                panel[0].activate();
            });

            assertTrue(awaitCondition(() -> labelTexts(panel[0]).contains("全科门诊")));
            assertTrue(labelTexts(panel[0]).contains("待就诊"));
            assertTrue(labelTexts(panel[0]).contains("候诊 5 号"));
            assertTrue(labelTexts(panel[0]).contains("挂号费 ¥12.00  ·  已支付"));
            assertEquals(2, appointmentCards(panel[0]).size());
            assertEquals(2, components(panel[0], JPanel.class).stream()
                    .filter(component -> "appointmentAdaptiveContent"
                            .equals(component.getName()))
                    .count());
            assertTrue(labelTexts(appointmentCards(panel[0]).getFirst())
                    .contains(nearDate));

            @SuppressWarnings("unchecked")
            JComboBox<String> sortOrder = (JComboBox<String>) components(
                    panel[0], JComboBox.class).stream()
                    .filter(combo -> "appointmentSortOrder".equals(combo.getName()))
                    .findFirst()
                    .orElseThrow();
            SwingUtilities.invokeAndWait(() ->
                    sortOrder.setSelectedItem("时间从远到近"));
            assertTrue(awaitCondition(() -> labelTexts(
                    appointmentCards(panel[0]).getFirst()).contains(farDate)));
            SwingUtilities.invokeAndWait(() ->
                    sortOrder.setSelectedItem("时间从近到远"));
            assertTrue(awaitCondition(() -> labelTexts(
                    appointmentCards(panel[0]).getFirst()).contains(nearDate)));

            JButton cancel = components(panel[0], JButton.class).stream()
                    .filter(button -> "cancelAppointmentButton".equals(button.getName()))
                    .findFirst()
                    .orElseThrow();
            SwingUtilities.invokeAndWait(cancel::doClick);

            assertTrue(awaitCondition(() -> labelTexts(panel[0]).contains("已取消")));
            assertTrue(labelTexts(panel[0]).contains("挂号费 ¥12.00  ·  已退款"));
            HospitalTheme.SurfacePanel cancelledCard = appointmentCards(panel[0]).stream()
                    .filter(card -> labelTexts(card).contains("已取消"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(components(cancelledCard, JButton.class).stream()
                    .noneMatch(button -> "cancelAppointmentButton".equals(button.getName())));
            assertEquals(1, components(panel[0], JButton.class).stream()
                    .filter(button -> "cancelAppointmentButton".equals(button.getName()))
                    .count());
        }
    }

    private static List<String> labelTexts(Container root) {
        return components(root, JLabel.class).stream()
                .map(JLabel::getText)
                .toList();
    }

    private static List<HospitalTheme.SurfacePanel> appointmentCards(Container root) {
        return components(root, HospitalTheme.SurfacePanel.class).stream()
                .filter(component -> "appointmentCard".equals(component.getName()))
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
