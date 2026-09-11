package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.DoctorAppointmentView;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextView;
import edu.seu.vcampus.common.hospital.DoctorClinicalRecordView;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.DoctorScheduleView;
import edu.seu.vcampus.common.hospital.DoctorWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PatientHealthProfileView;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.UpdatePatientHealthProfileRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorWorkspacePanelTest {

    @Test
    void highlightsTheConsultationLinkedToAnOrdinaryFollowUp() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 6, 10, 0);
        ConsultationRecordView source = new ConsultationRecordView(
                "consultation-source",
                "appointment-source",
                "doctor-chen",
                "陈医生",
                "主治医师",
                "U-STUDENT-001",
                "全科门诊",
                VisitType.FIRST_VISIT,
                ConsultationOutcome.COMPLETED,
                "上呼吸道感染",
                "血常规",
                "对症处置",
                "按医嘱用药",
                "症状反复时复诊",
                now.minusDays(3));
        DoctorConsultationContextView consultationContext =
                new DoctorConsultationContextView(
                        new DoctorAppointmentView(
                                "appointment-follow-up",
                                "U-STUDENT-001",
                                3,
                                AppointmentStatus.BOOKED,
                                VisitType.FOLLOW_UP,
                                "appointment-source",
                                now),
                        "全科门诊",
                        "刘医生",
                        "副主任医师",
                        now.plusDays(1),
                        now.plusDays(1).plusMinutes(30),
                        new PatientHealthProfileView(
                                "O型", "无", "无", "无", "联系人", now),
                        List.of(source),
                        List.of());

        DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DoctorWorkspacePanel(
                    new ClientContext(new CampusClient("127.0.0.1", 1)),
                    () -> { });
            panel[0].renderAppointmentContent(consultationContext);
        });

        assertEquals(1, namedComponents(
                panel[0], JPanel.class, "doctorFollowUpSourceCard").size());
        assertEquals(1, namedButtons(
                panel[0], "openFollowUpSourceButton").size());
        assertTrue(labelTexts(panel[0]).stream()
                .anyMatch(text -> text.contains("普通复诊 · 已关联上次诊疗")));
        assertTrue(labelTexts(panel[0]).stream()
                .anyMatch(text -> text.contains("上呼吸道感染")));
    }

    @Test
    void ignoresAConsultationResponseAfterTheDoctorHasOpenedAnotherPatient() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 9, 0);
        DoctorConsultationContextView patientA = consultationContext("A", now);
        DoctorConsultationContextView patientB = consultationContext("B", now);
        DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];

        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DoctorWorkspacePanel(
                    new ClientContext(new CampusClient("127.0.0.1", 1)), () -> { });
            panel[0].renderAppointmentContent(patientB);
            panel[0].renderAppointmentContentIfCurrent("appointment-A", patientA);
        });

        assertTrue(labelTexts(panel[0]).stream().anyMatch(text -> text.contains("patient-B")));
        assertTrue(labelTexts(panel[0]).stream().noneMatch(text -> text.contains("patient-A")));
    }

    @Test
    void restoresAnUnsavedConsultationDraftAfterSwitchingPatients() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 9, 0);
        DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];

        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DoctorWorkspacePanel(
                    new ClientContext(new CampusClient("127.0.0.1", 1)), () -> { });
            panel[0].renderAppointmentContent(consultationContext("A", now));
            namedComponents(panel[0], JTextArea.class, "doctorDiagnosis")
                    .getFirst().setText("x".repeat(1_001));
            namedComponents(panel[0], JTextArea.class, "doctorTreatment")
                    .getFirst().setText("先观察并对症处理");
            panel[0].renderAppointmentContent(consultationContext("B", now));
            panel[0].renderAppointmentContent(consultationContext("A", now));
        });

        assertEquals("x".repeat(1_001), namedComponents(
                panel[0], JTextArea.class, "doctorDiagnosis").getFirst().getText());
        assertEquals("先观察并对症处理", namedComponents(
                panel[0], JTextArea.class, "doctorTreatment").getFirst().getText());
        assertTrue(namedLabels(panel[0], "doctorConsultationDraftStatus").stream()
                .map(JLabel::getText)
                .anyMatch(text -> text.contains("1001/1000")));
    }

    @Test
    void warnsTheDoctorWhenThePatientHasNotFilledAHealthProfile() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 9, 0);
        DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];

        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new DoctorWorkspacePanel(
                    new ClientContext(new CampusClient("127.0.0.1", 1)), () -> { });
            panel[0].renderAppointmentContent(consultationContext("empty", now));
        });

        assertTrue(labelTexts(panel[0]).stream()
                .anyMatch(text -> text.contains("患者尚未填写健康档案")));
        assertTrue(labelTexts(panel[0]).stream()
                .noneMatch(text -> text.contains("最近更新：")));
    }

    @Test
    void wrapsManyScheduleCardsWithinTheVisibleViewport() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 9, 0);
        List<DoctorScheduleView> schedules = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            schedules.add(new DoctorScheduleView(
                    "schedule-" + index,
                    "dept-general",
                    "全科门诊",
                    now.plusDays(index + 1L),
                    now.plusDays(index + 1L).plusMinutes(30),
                    10,
                    10,
                    true,
                    List.of()));
        }
        DoctorWorkspaceView workspace = new DoctorWorkspaceView(
                "doctor-chen", "陈医生", "主治医师",
                "dept-general", "全科门诊", schedules, List.of());

        SwingUtilities.invokeAndWait(() -> {
            try {
                DoctorWorkspacePanel panel = new DoctorWorkspacePanel(
                        new ClientContext(new CampusClient("127.0.0.1", 1)), () -> { });
                Method showWorkspace = DoctorWorkspacePanel.class.getDeclaredMethod(
                        "showWorkspace", DoctorWorkspaceView.class);
                showWorkspace.setAccessible(true);
                showWorkspace.invoke(panel, workspace);
                panel.setSize(1_000, 700);
                for (int pass = 0; pass < 4; pass++) {
                    layoutTree(panel);
                }
                Field field = DoctorWorkspacePanel.class.getDeclaredField("scheduleGrid");
                field.setAccessible(true);
                JPanel grid = (JPanel) field.get(panel);
                JViewport viewport = (JViewport) grid.getParent();
                int maxRight = 0;
                for (Component card : grid.getComponents()) {
                    maxRight = Math.max(maxRight, card.getX() + card.getWidth());
                }
                assertTrue(maxRight <= viewport.getWidth());
                assertTrue(grid.getPreferredSize().height > 400);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        });
    }

    @Test
    void offersNoShowActionOnlyAfterTheScheduleHasEnded() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        DoctorAppointmentView pending = new DoctorAppointmentView(
                "appointment-overdue",
                "patient-overdue",
                2,
                AppointmentStatus.BOOKED,
                VisitType.FIRST_VISIT,
                null,
                now.minusDays(2));
        DoctorScheduleView ended = new DoctorScheduleView(
                "schedule-ended",
                "dept-general",
                "全科门诊",
                now.minusHours(2),
                now.minusHours(1),
                8,
                7,
                true,
                List.of(pending));
        DoctorWorkspaceView workspace = new DoctorWorkspaceView(
                "doctor-chen", "陈医生", "主治医师", "dept-general",
                "全科门诊", List.of(ended), List.of());
        DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];

        SwingUtilities.invokeAndWait(() -> {
            try {
                panel[0] = new DoctorWorkspacePanel(
                        new ClientContext(new CampusClient("127.0.0.1", 1)), () -> { });
                Method showWorkspace = DoctorWorkspacePanel.class.getDeclaredMethod(
                        "showWorkspace", DoctorWorkspaceView.class);
                showWorkspace.setAccessible(true);
                showWorkspace.invoke(panel[0], workspace);
                namedButtons(panel[0], "doctorScheduleButton").getFirst().doClick();
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        });

        assertEquals(1, namedButtons(panel[0], "markAppointmentNoShowButton").size());
        assertEquals("appointment-overdue", namedButtons(
                panel[0], "markAppointmentNoShowButton").getFirst().getActionCommand());
    }

    @Test
    void opensSignedRecordAndItsHistoricalExaminationInSeparatePages() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 9, 0);
        ConsultationRecordView record = new ConsultationRecordView(
                "consultation-signed", "appointment-signed", "doctor-chen",
                "陈医生", "主治医师", "patient-signed", "全科门诊",
                VisitType.FIRST_VISIT, ConsultationOutcome.COMPLETED,
                "诊断", "检查解读", "处置", "用药", "复诊", now);
        ExaminationOrderView examination = new ExaminationOrderView(
                "order-signed", "episode-signed", "appointment-signed",
                "陈医生", "全科门诊", "血常规", "无",
                ExaminationStatus.REVIEWED, "结果摘要", now.minusHours(1), now, false);
        DoctorClinicalRecordView signed = new DoctorClinicalRecordView(
                record, List.of(examination));
        DoctorWorkspaceView workspace = new DoctorWorkspaceView(
                "doctor-chen", "陈医生", "主治医师", "dept-general",
                "全科门诊", List.of(), List.of(), List.of(signed));
        DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];

        SwingUtilities.invokeAndWait(() -> {
            try {
                panel[0] = new DoctorWorkspacePanel(
                        new ClientContext(new CampusClient("127.0.0.1", 1)), () -> { });
                Method showWorkspace = DoctorWorkspacePanel.class.getDeclaredMethod(
                        "showWorkspace", DoctorWorkspaceView.class);
                showWorkspace.setAccessible(true);
                showWorkspace.invoke(panel[0], workspace);
                namedButtons(panel[0], "openDoctorSignedRecordsButton").getFirst().doClick();
                namedButtons(panel[0], "openDoctorSignedRecordDetailButton").getFirst().doClick();
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        });

        assertTrue(labelTexts(panel[0]).stream().anyMatch(text -> text.contains("检查解读")));
        assertEquals(1, namedComponents(
                panel[0], JPanel.class, "doctorExaminationCard").size());
    }

    @Test
    void loadsDoctorSchedulesAndShowsSelectedPendingAppointmentDetails()
            throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login(
                    "20260006", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1")).getData());
            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260029", "123456".toCharArray()).isSuccess());

            DoctorWorkspacePanel[] panel = new DoctorWorkspacePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new DoctorWorkspacePanel(context, () -> { });
                panel[0].activate();
            });

            assertTrue(awaitCondition(() -> labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("陈安") && text.contains("全科门诊"))));
            List<JButton> schedules = namedButtons(panel[0], "doctorScheduleButton");
            assertEquals(3, schedules.size());
            assertTrue(schedules.getFirst().getParent() instanceof Container);
            assertTrue(labelTexts(schedules.getFirst().getParent()).stream()
                    .anyMatch(text -> text.contains("待接诊 5 人")));
            SwingUtilities.invokeAndWait(schedules.getFirst()::doClick);

            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "doctorAppointmentButton").size() == 5));
            JButton patient = namedButtons(panel[0], "doctorAppointmentButton").stream()
                    .filter(button -> {
                        Container card = namedAncestor(button, "doctorAppointmentCard");
                        return card != null
                                && labelTexts(card).contains("U-STUDENT-001");
                    })
                    .findFirst()
                    .orElseThrow();
            SwingUtilities.invokeAndWait(patient::doClick);

            assertTrue(awaitCondition(() -> namedLabels(panel[0],
                    "doctorAppointmentDetail").stream()
                    .map(JLabel::getText)
                    .anyMatch("U-STUDENT-001"::equals)));
            assertTrue(namedLabels(panel[0], "doctorAppointmentDetail").stream()
                    .map(JLabel::getText)
                    .anyMatch("5 号"::equals));
            assertEquals(1, namedButtons(
                    panel[0], "openCurrentVisitButton").size());
            assertEquals(1, namedButtons(
                    panel[0], "openDoctorHealthProfileButton").size());
            assertEquals(1, namedButtons(
                    panel[0], "openDoctorHistoryButton").size());

            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openCurrentVisitButton").getFirst().doClick());
            assertEquals(1, namedComponents(
                    panel[0], JTextArea.class, "doctorDiagnosis").size());
            assertEquals(1, namedComponents(
                    panel[0], JTextArea.class, "doctorTreatment").size());
            assertEquals(1, namedComponents(
                    panel[0], JTextArea.class, "doctorInterimCare").size());
            assertEquals(1, namedButtons(
                    panel[0], "submitConsultationButton").size());
            assertEquals(1, namedButtons(
                    panel[0], "submitExaminationPlanButton").size());
            assertTrue(labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("患者自述健康档案")));
            JPanel consultationForm = namedComponents(
                    panel[0], JPanel.class, "doctorConsultationForm").getFirst();
            assertTrue(consultationForm.getPreferredSize().height > 500);
            JScrollPane visitScroll = namedComponents(
                    panel[0], JScrollPane.class, "doctorCurrentVisitScrollPane").getFirst();
            assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    visitScroll.getVerticalScrollBarPolicy());

            ClientContext patientUpdater = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(patientUpdater.login(
                    "20260006", "123456".toCharArray()).isSuccess());
            assertTrue(patientUpdater.send(
                    HospitalActions.UPDATE_MY_HEALTH_PROFILE,
                    new UpdatePatientHealthProfileRequest(
                            "AB型", "花粉过敏（刚刚更新）", "无", "无", "联系人"))
                    .isSuccess());
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openDoctorHealthProfileButton").getFirst().doClick());
            assertTrue(awaitCondition(() -> labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("花粉过敏（刚刚更新）"))));

            assertTrue(context.send(
                    HospitalActions.SUBMIT_EXAMINATION_PLAN,
                    new SubmitExaminationPlanRequest(
                            booking.getAppointmentId(),
                            "发热待查（课程演示）",
                            "血常规",
                            "无需空腹",
                            "检查完成前注意休息")).isSuccess());
            SwingUtilities.invokeAndWait(panel[0]::activate);
            assertTrue(awaitCondition(() -> namedButtons(
                    panel[0], "openDoctorFollowUpsButton").size() == 1));
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openDoctorFollowUpsButton").getFirst().doClick());
            assertEquals(1, namedComponents(
                    panel[0], JPanel.class, "doctorFollowUpCard").size());
            assertTrue(namedLabels(panel[0], "doctorFollowUpStatus").stream()
                    .map(JLabel::getText)
                    .anyMatch("等待检查结果"::equals));
            assertEquals(1, namedButtons(
                    panel[0], "openDoctorFollowUpDetailButton").size());
            SwingUtilities.invokeAndWait(() -> namedButtons(
                    panel[0], "openDoctorFollowUpDetailButton").getFirst().doClick());
            assertTrue(labelTexts(panel[0]).stream()
                    .anyMatch(text -> text.contains("发热待查（课程演示）")));
        }
    }

    private static List<String> labelTexts(Container root) {
        return components(root, JLabel.class).stream()
                .map(JLabel::getText)
                .toList();
    }

    private static DoctorConsultationContextView consultationContext(
            String suffix,
            LocalDateTime now) {
        return new DoctorConsultationContextView(
                new DoctorAppointmentView(
                        "appointment-" + suffix,
                        "patient-" + suffix,
                        1,
                        AppointmentStatus.BOOKED,
                        VisitType.FIRST_VISIT,
                        null,
                        now),
                "全科门诊",
                "陈医生",
                "主治医师",
                now.plusDays(1),
                now.plusDays(1).plusMinutes(30),
                new PatientHealthProfileView("", "", "", "", "", now),
                List.of(),
                List.of());
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

    private static Container namedAncestor(Component component, String name) {
        Container current = component.getParent();
        while (current != null) {
            if (name.equals(current.getName())) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
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
