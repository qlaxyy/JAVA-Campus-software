package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.PatientHealthProfileView;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.hospital.PatientBillView;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalHomePanelTest {

    @Test
    void resultReadyCreatesAVisibleDirectFollowUpAction() throws Exception {
        AtomicBoolean followUpOpened = new AtomicBoolean();
        HospitalHomePanel[] panel = new HospitalHomePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new HospitalHomePanel(
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    () -> { }, () -> { }, () -> { },
                    () -> followUpOpened.set(true), () -> { });
            panel[0].showCareTasks(recordWith(readyExamination(false)));
        });

        List<JButton> actions = namedButtons(panel[0], "patientHomeFollowUpButton");
        assertEquals(1, actions.size());
        assertEquals("立即安排回诊", actions.getFirst().getText());
        SwingUtilities.invokeAndWait(actions.getFirst()::doClick);
        assertTrue(followUpOpened.get());
    }

    @Test
    void unpaidBillCreatesAVisiblePaymentTask() throws Exception {
        AtomicBoolean billsOpened = new AtomicBoolean();
        HospitalHomePanel[] panel = new HospitalHomePanel[1];
        LocalDateTime now = LocalDateTime.now();
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new HospitalHomePanel(
                    () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                    () -> billsOpened.set(true), () -> { }, () -> { },
                    () -> { }, () -> { });
            panel[0].showBillTasks(
                    new PatientBillListResponse(List.of(new PatientBillView(
                            "bill-1", "appointment-1", HospitalBillType.TREATMENT,
                            "诊疗处置服务（课程演示）", 1_800, PaymentStatus.UNPAID,
                            "全科门诊", "陈医生", now, now, null, null))),
                    () -> billsOpened.set(true));
        });

        List<JButton> actions = namedButtons(panel[0], "patientHomeBillingButton");
        assertEquals(1, actions.size());
        SwingUtilities.invokeAndWait(actions.getFirst()::doClick);
        assertTrue(billsOpened.get());
    }

    private static PatientHealthRecordView recordWith(ExaminationOrderView examination) {
        return new PatientHealthRecordView(
                new PatientHealthProfileView("", "", "", "", "", LocalDateTime.now()),
                List.of(),
                List.of(examination));
    }

    private static ExaminationOrderView readyExamination(boolean booked) {
        LocalDateTime now = LocalDateTime.now();
        return new ExaminationOrderView(
                "order-1", "episode-1", "appointment-1", "陈医生", "全科门诊",
                "血常规", "无需空腹", ExaminationStatus.RESULT_READY,
                "演示结果", now.minusHours(1), now, booked);
    }

    private static List<JButton> namedButtons(Container root, String name) {
        return components(root, JButton.class).stream()
                .filter(button -> name.equals(button.getName()))
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
}
