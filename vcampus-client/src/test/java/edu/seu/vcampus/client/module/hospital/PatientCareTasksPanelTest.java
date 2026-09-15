package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.PatientHealthProfileView;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientCareTasksPanelTest {

    @Test
    void displaysEachOpenTaskSeparatelyAndRoutesItsOwnAction() throws Exception {
        AtomicBoolean followUpOpened = new AtomicBoolean();
        AtomicBoolean appointmentsOpened = new AtomicBoolean();
        PatientCareTasksPanel[] panel = new PatientCareTasksPanel[1];
        LocalDateTime now = LocalDateTime.now();
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new PatientCareTasksPanel(
                    new ClientContext(new CampusClient("127.0.0.1", 1)),
                    () -> { }, () -> followUpOpened.set(true),
                    () -> appointmentsOpened.set(true));
            panel[0].showTasks(new PatientHealthRecordView(
                    new PatientHealthProfileView("", "", "", "", "", now),
                    List.of(), List.of(
                            task("ready", ExaminationStatus.RESULT_READY, false, now),
                            task("booked", ExaminationStatus.RESULT_READY, true, now),
                            task("ordered", ExaminationStatus.ORDERED, false, now))));
        });

        List<JButton> buttons = components(panel[0], JButton.class).stream()
                .filter(button -> "patientCareTaskActionButton".equals(button.getName()))
                .toList();
        assertEquals(3, buttons.size());
        assertTrue(components(panel[0], JLabel.class).stream()
                .map(JLabel::getText)
                .anyMatch("共 3 项待办"::equals));
        SwingUtilities.invokeAndWait(() -> buttons.stream()
                .filter(button -> "ready".equals(button.getActionCommand()))
                .findFirst().orElseThrow().doClick());
        assertTrue(followUpOpened.get());
        SwingUtilities.invokeAndWait(() -> buttons.stream()
                .filter(button -> "booked".equals(button.getActionCommand()))
                .findFirst().orElseThrow().doClick());
        assertTrue(appointmentsOpened.get());
    }

    private static ExaminationOrderView task(
            String id, ExaminationStatus status, boolean booked, LocalDateTime now) {
        return new ExaminationOrderView(
                id, "episode-" + id, "appointment-" + id,
                "韩医生", "耳鼻咽喉诊疗", "抽血", "无需空腹", status,
                status == ExaminationStatus.RESULT_READY ? "演示结果" : "",
                now.minusDays(1),
                status == ExaminationStatus.RESULT_READY ? now : null,
                booked);
    }

    private static <T extends Component> List<T> components(
            Container root, Class<T> type) {
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
