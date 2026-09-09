package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultationRecordsPanelTest {

    @Test
    void patientOpensCompletedConsultationAsReadOnlyRecord() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "student001", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "teacher001", "123456".toCharArray()).isSuccess());
            assertTrue(context.send(
                    HospitalActions.SUBMIT_CONSULTATION,
                    new SubmitConsultationRequest(
                            booking.getAppointmentId(),
                            "上呼吸道感染（课程演示）",
                            "建议检查血常规。",
                            "对症处置并休息。",
                            "无。",
                            "3天后未缓解时复诊。")).isSuccess());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "student001", "123456".toCharArray()).isSuccess());

            ConsultationRecordsPanel[] panel = new ConsultationRecordsPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new ConsultationRecordsPanel(context, () -> { });
                panel[0].activate();
            });

            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openConsultationRecordButton").size() == 1));
            assertTrue(labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("上呼吸道感染")));
            JButton open = namedButtons(
                    panel[0], "openConsultationRecordButton").getFirst();
            SwingUtilities.invokeAndWait(open::doClick);

            List<String> details = namedLabels(
                    panel[0], "consultationRecordDetail").stream()
                    .map(JLabel::getText)
                    .toList();
            assertEquals(7, details.size());
            assertTrue(details.stream()
                    .anyMatch(text -> text.contains("对症处置并休息")));
            assertTrue(namedButtons(
                    panel[0], "submitConsultationButton").isEmpty());
        }
    }

    private static List<String> labelTexts(Container root) {
        return components(root, JLabel.class).stream()
                .map(JLabel::getText)
                .toList();
    }

    private static List<JButton> namedButtons(Container root, String name) {
        return components(root, JButton.class).stream()
                .filter(button -> name.equals(button.getName()))
                .toList();
    }

    private static List<JLabel> namedLabels(Container root, String name) {
        return components(root, JLabel.class).stream()
                .filter(label -> name.equals(label.getName()))
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
