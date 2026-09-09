package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSchedulePanelTest {

    @Test
    void createsDraftAndPublishesItFromAdministratorPage() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260007", "123456".toCharArray()).isSuccess());
            AdminScheduleWorkspaceView before = workspace(context);
            Set<String> existingIds = new HashSet<>();
            before.getSchedules().forEach(slot -> existingIds.add(slot.getScheduleId()));

            AdminSchedulePanel[] panel = new AdminSchedulePanel[1];
            SwingUtilities.invokeAndWait(() -> {
                panel[0] = new AdminSchedulePanel(context, () -> { });
                panel[0].activate();
            });
            assertTrue(awaitCondition(() -> doctorBox(panel[0]).getItemCount() > 0));

            String date = LocalDate.now().plusDays(5)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            SwingUtilities.invokeAndWait(() -> {
                JComboBox<AdminDoctorView> doctors = doctorBox(panel[0]);
                for (int index = 0; index < doctors.getItemCount(); index++) {
                    if (doctors.getItemAt(index).getDoctorId().equals("doctor-chen")) {
                        doctors.setSelectedIndex(index);
                        break;
                    }
                }
                named(panel[0], JTextField.class, "adminScheduleDateField").setText(date);
                named(panel[0], JTextField.class, "adminScheduleStartField").setText("17:00");
                named(panel[0], JTextField.class, "adminScheduleEndField").setText("17:30");
                named(panel[0], JSpinner.class, "adminScheduleCapacitySpinner").setValue(7);
                named(panel[0], JSpinner.class, "adminScheduleFeeSpinner").setValue(16.0);
                namedButton(panel[0], "createAdminScheduleButton").doClick();
            });

            assertTrue(awaitCondition(() -> toggleButtons(panel[0]).stream()
                    .anyMatch(button -> !existingIds.contains(button.getActionCommand()))));
            AbstractButton publish = toggleButtons(panel[0]).stream()
                    .filter(button -> !existingIds.contains(button.getActionCommand()))
                    .findFirst()
                    .orElseThrow();
            String scheduleId = publish.getActionCommand();
            assertTrue("发布号源".equals(publish.getText()));
            SwingUtilities.invokeAndWait(publish::doClick);

            assertTrue(awaitCondition(() -> {
                try {
                    return workspace(context).getSchedules().stream()
                            .anyMatch(slot -> slot.getScheduleId().equals(scheduleId)
                                    && slot.getAvailability() == SlotAvailability.AVAILABLE);
                } catch (Exception exception) {
                    return false;
                }
            }));

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            SlotListResponse slots = assertInstanceOf(
                    SlotListResponse.class,
                    context.send(
                            HospitalActions.SEARCH_SLOTS,
                            SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"))
                            .getData());
            assertTrue(slots.getSlots().stream()
                    .anyMatch(slot -> slot.getScheduleId().equals(scheduleId)));
        }
    }

    private static AdminScheduleWorkspaceView workspace(ClientContext context)
            throws Exception {
        return assertInstanceOf(
                AdminScheduleWorkspaceView.class,
                context.send(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null).getData());
    }

    @SuppressWarnings("unchecked")
    private static JComboBox<AdminDoctorView> doctorBox(Container root) {
        return (JComboBox<AdminDoctorView>) named(
                root, JComboBox.class, "adminScheduleDoctorBox");
    }

    private static AbstractButton namedButton(Container root, String name) {
        return named(root, AbstractButton.class, name);
    }

    private static List<AbstractButton> toggleButtons(Container root) {
        return components(root, AbstractButton.class).stream()
                .filter(button -> "toggleAdminScheduleButton".equals(button.getName()))
                .toList();
    }

    private static <T extends Component> T named(
            Container root,
            Class<T> type,
            String name) {
        return components(root, type).stream()
                .filter(component -> name.equals(component.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("component not found: " + name));
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
        for (int attempt = 0; attempt < 200; attempt++) {
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
