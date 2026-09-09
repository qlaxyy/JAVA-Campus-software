package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientHealthRecordPanelTest {

    @Test
    void patientNavigatesFromHealthRecordHubToProfileAndHistoryDetail()
            throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260002", "123456".toCharArray()).isSuccess());
            assertTrue(context.send(
                    HospitalActions.SUBMIT_CONSULTATION,
                    new SubmitConsultationRequest(
                            booking.getAppointmentId(),
                            "上呼吸道感染（课程演示）",
                            "建议检查血常规。",
                            "对症处置并休息。",
                            "无。",
                            "症状持续时复诊。"))
                    .isSuccess());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());

            PatientHealthRecordPanel[] panel = new PatientHealthRecordPanel[1];
            AtomicReference<ConsultationRecordView> followUpSource = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PatientHealthRecordPanel(
                        context, () -> { }, followUpSource::set);
                panel[0].activate();
            });

            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openPatientProfileButton").size() == 1));
            assertEquals(1, namedButtons(
                    panel[0], "openPatientHistoryButton").size());
            assertTrue(labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("1 次就诊记录")));

            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openPatientProfileButton").getFirst().doClick());
            assertEquals(1, namedButtons(
                    panel[0], "editPatientHealthProfileButton").size());
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "editPatientHealthProfileButton").getFirst().doClick());
            SwingUtilities.invokeAndWait(() -> {
                namedComponents(panel[0], JTextField.class, "patientBloodType")
                        .getFirst().setText("AB型");
                namedComponents(panel[0], JTextArea.class, "patientAllergies")
                        .getFirst().setText("花粉过敏（课程演示）");
                namedButtons(panel[0], "savePatientHealthProfileButton")
                        .getFirst().doClick();
            });
            assertTrue(awaitCondition(() -> namedLabels(
                    panel[0], "patientHealthRecordDetail").stream()
                    .map(JLabel::getText)
                    .anyMatch(text -> text.contains("花粉过敏（课程演示）"))));

            SwingUtilities.invokeAndWait(panel[0]::activate);
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openPatientHistoryButton").size() == 1));

            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openPatientHistoryButton").getFirst().doClick());
            assertEquals(1, namedButtons(
                    panel[0], "openHealthHistoryDetailButton").size());
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openHealthHistoryDetailButton").getFirst().doClick());
            assertTrue(namedLabels(panel[0], "patientHealthRecordDetail").stream()
                    .map(JLabel::getText)
                    .anyMatch(text -> text.contains("对症处置并休息")));
            assertEquals(1, namedButtons(
                    panel[0], "bookOrdinaryFollowUpButton").size());
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "bookOrdinaryFollowUpButton").getFirst().doClick());
            assertEquals(booking.getAppointmentId(),
                    followUpSource.get().getAppointmentId());

            followUpSource.set(null);
            SwingUtilities.invokeAndWait(panel[0]::activateForOrdinaryFollowUpSelection);
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "selectOrdinaryFollowUpButton").size() == 1));
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "selectOrdinaryFollowUpButton").getFirst().doClick());
            assertEquals(booking.getAppointmentId(),
                    followUpSource.get().getAppointmentId());
        }
    }

    @Test
    void patientOpensExaminationPageAndSeesAutomaticallyUpdatedReviewState()
            throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260002", "123456".toCharArray()).isSuccess());
            ExaminationOrderView order = assertInstanceOf(
                    ExaminationOrderView.class,
                    context.send(
                            HospitalActions.SUBMIT_EXAMINATION_PLAN,
                            new SubmitExaminationPlanRequest(
                                    booking.getAppointmentId(),
                                    "发热待查（课程演示）",
                                    "血常规",
                                    "无需空腹（课程演示）",
                                    "检查完成前注意休息。"))
                            .getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());

            PatientHealthRecordPanel[] panel = new PatientHealthRecordPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new PatientHealthRecordPanel(context, () -> { });
                panel[0].activate();
            });
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openPatientReportsButton").size() == 1));
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openPatientReportsButton").getFirst().doClick());
            assertEquals(1, namedButtons(
                    panel[0], "openPatientReportDetailButton").size());
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openPatientReportDetailButton").getFirst().doClick());
            assertEquals(1, namedButtons(panel[0], "publishDemoReportButton").size());

            assertTrue(context.send(
                    HospitalActions.PUBLISH_DEMO_EXAMINATION_REPORT,
                    new PublishDemoExaminationReportRequest(order.getOrderId()))
                    .isSuccess());
            SwingUtilities.invokeAndWait(panel[0]::activateForFollowUp);
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "bookResultReviewButton").size() == 1));
            assertTrue(context.send(
                    HospitalActions.BOOK_RESULT_REVIEW,
                    new BookResultReviewRequest(order.getOrderId()))
                    .isSuccess());
            SwingUtilities.invokeAndWait(panel[0]::activate);
            assertTrue(awaitCondition(() -> labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("结果已出 · 已安排回诊"))));
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

    private static <T extends Component> List<T> namedComponents(
            Container root,
            Class<T> type,
            String name) {
        return components(root, type).stream()
                .filter(component -> name.equals(component.getName()))
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
