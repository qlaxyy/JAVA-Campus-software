package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSchedulePanelTest {

    @Test
    void createsMultipleDatesAsAtomicDraftsAndFiltersOneSchedule() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260005", "123456".toCharArray()).isSuccess());
            Set<String> existingIds = new HashSet<>();
            workspace(context).getSchedules().forEach(slot -> existingIds.add(slot.getScheduleId()));
            LocalDate firstDate = LocalDate.now().plusDays(8);
            LocalDate secondDate = firstDate.plusDays(2);

            AdminSchedulePanel[] panel = new AdminSchedulePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new AdminSchedulePanel(context, () -> { });
                panel[0].activate();
            });
            assertTrue(awaitCondition(() -> named(panel[0], JComboBox.class,
                    "adminScheduleDoctorBox").getItemCount() > 0));
            SwingUtilities.invokeAndWait(() -> {
                @SuppressWarnings("unchecked")
                JComboBox<AdminDoctorView> doctors = named(panel[0], JComboBox.class,
                        "adminScheduleDoctorBox");
                for (int index = 0; index < doctors.getItemCount(); index++) {
                    if ("doctor-chen".equals(doctors.getItemAt(index).getDoctorId())) {
                        doctors.setSelectedIndex(index);
                        break;
                    }
                }
                named(panel[0], HospitalMultiDateCalendarPicker.class,
                        "adminScheduleMultiDateCalendar").setSelectedDates(
                                Set.of(firstDate, secondDate));
                named(panel[0], JComboBox.class, "adminScheduleStartTimeBox")
                        .setSelectedItem("10:00");
                named(panel[0], JComboBox.class, "adminScheduleEndTimeBox")
                        .setSelectedItem("11:00");
                named(panel[0], AbstractButton.class,
                        "createManualSchedulesButton").doClick();
            });

            assertTrue(awaitCondition(() -> newSchedules(context, existingIds).size() == 4));
            List<String> generatedIds = newSchedules(context, existingIds).stream()
                    .map(slot -> slot.getScheduleId()).toList();
            assertTrue(workspace(context).getSchedules().stream()
                    .filter(slot -> generatedIds.contains(slot.getScheduleId()))
                    .allMatch(slot -> slot.getAvailability() == SlotAvailability.CLOSED));

            assertTrue(awaitCondition(() -> contains(
                    named(panel[0], JComboBox.class, "adminScheduleDateFilter"), firstDate)));
            SwingUtilities.invokeAndWait(() -> {
                named(panel[0], JComboBox.class, "adminScheduleDateFilter")
                        .setSelectedItem(firstDate);
                named(panel[0], JComboBox.class, "adminScheduleTimeFilter")
                        .setSelectedItem("10:00");
                named(panel[0], JComboBox.class, "adminScheduleStatusFilter")
                        .setSelectedItem("已关闭");
                @SuppressWarnings("unchecked")
                JComboBox<Object> doctorFilter = named(panel[0], JComboBox.class,
                        "adminScheduleDoctorFilter");
                for (int index = 0; index < doctorFilter.getItemCount(); index++) {
                    Object option = doctorFilter.getItemAt(index);
                    if (option instanceof AdminDoctorView doctor
                            && "doctor-chen".equals(doctor.getDoctorId())) {
                        doctorFilter.setSelectedIndex(index);
                        break;
                    }
                }
            });
            assertTrue(awaitCondition(() -> slotButtons(panel[0]).size() == 1));
            AbstractButton publish = slotButtons(panel[0]).getFirst();
            assertTrue(generatedIds.contains(publish.getActionCommand()));
            assertEquals("发布号源", publish.getText());
            SwingUtilities.invokeAndWait(publish::doClick);
            assertTrue(awaitCondition(() -> workspace(context).getSchedules().stream()
                    .anyMatch(slot -> publish.getActionCommand().equals(slot.getScheduleId())
                            && slot.getAvailability() == SlotAvailability.AVAILABLE)));

            SwingUtilities.invokeAndWait(() -> named(panel[0], AbstractButton.class,
                    "createManualSchedulesButton").doClick());
            assertTrue(awaitCondition(() -> newSchedules(context, existingIds).size() == 4));
        }
    }

    private static List<edu.seu.vcampus.common.hospital.SlotView> newSchedules(
            ClientContext context, Set<String> existingIds) {
        try {
            return workspace(context).getSchedules().stream()
                    .filter(slot -> !existingIds.contains(slot.getScheduleId())).toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static AdminScheduleWorkspaceView workspace(ClientContext context) {
        try {
            return assertInstanceOf(AdminScheduleWorkspaceView.class,
                    context.send(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null).getData());
        } catch (IOException exception) {
            throw new IllegalStateException("schedule workspace request failed", exception);
        }
    }

    private static List<AbstractButton> slotButtons(Container root) {
        return components(root, AbstractButton.class).stream()
                .filter(button -> "toggleAdminScheduleButton".equals(button.getName()))
                .filter(AbstractButton::isEnabled)
                .toList();
    }

    private static boolean contains(JComboBox<?> box, Object value) {
        for (int index = 0; index < box.getItemCount(); index++) {
            if (value.equals(box.getItemAt(index))) return true;
        }
        return false;
    }

    private static <T extends Component> T named(
            Container root, Class<T> type, String name) {
        return components(root, type).stream()
                .filter(component -> name.equals(component.getName()))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> components(
            Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container container) found.addAll(components(container, type));
        }
        return found;
    }

    private static boolean awaitCondition(java.util.function.BooleanSupplier condition)
            throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            AtomicBoolean satisfied = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) return true;
            Thread.sleep(20);
        }
        return false;
    }
}
