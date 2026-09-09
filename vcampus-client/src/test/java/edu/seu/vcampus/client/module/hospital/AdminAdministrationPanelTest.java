package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AdminDepartmentWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.AbstractButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAdministrationPanelTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void maintainsDepartmentsAndLoadsFilterableAppointmentLedger() throws Exception {
        Path database = temporaryDirectory.resolve("admin-panels.accdb");
        Path log = temporaryDirectory.resolve("admin-panels.log");
        try (HospitalServerProcess server = HospitalServerProcess.start(database, log)) {
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.port()));
            assertTrue(context.login("20260007", "123456".toCharArray()).isSuccess());

            AdminDepartmentPanel[] departments = new AdminDepartmentPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                departments[0] = new AdminDepartmentPanel(context, () -> { });
                departments[0].activate();
            });
            assertTrue(awaitCondition(() -> namedButtons(
                    departments[0], "adminDepartmentButton").size() == 15));

            SwingUtilities.invokeAndWait(() -> {
                named(departments[0], JTextField.class, "adminDepartmentName")
                        .setText("康复医学（测试）");
                namedButton(departments[0], "saveDepartmentButton").doClick();
            });
            assertTrue(awaitCondition(() -> namedButtons(
                    departments[0], "adminDepartmentButton").size() == 16));
            AdminDepartmentWorkspaceView workspace = assertInstanceOf(
                    AdminDepartmentWorkspaceView.class,
                    context.send(HospitalActions.GET_ADMIN_DEPARTMENT_WORKSPACE, null)
                            .getData());
            assertEquals(16, workspace.getDepartments().size());

            AdminAppointmentPanel[] appointments = new AdminAppointmentPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                appointments[0] = new AdminAppointmentPanel(context, () -> { });
                appointments[0].activate();
            });
            assertTrue(awaitCondition(() -> !namedButtons(
                    appointments[0], "adminCancelAppointmentButton").isEmpty()));

            JTextField search = named(
                    appointments[0], JTextField.class, "adminAppointmentSearch");
            SwingUtilities.invokeAndWait(() -> search.setText("不存在的预约编号"));
            assertTrue(awaitCondition(() -> namedButtons(
                    appointments[0], "adminCancelAppointmentButton").isEmpty()));
            SwingUtilities.invokeAndWait(() -> search.setText(""));
            assertTrue(awaitCondition(() -> !namedButtons(
                    appointments[0], "adminCancelAppointmentButton").isEmpty()));
        }
    }

    private static AbstractButton namedButton(Container root, String name) {
        return named(root, AbstractButton.class, name);
    }

    private static List<AbstractButton> namedButtons(Container root, String name) {
        return components(root, AbstractButton.class).stream()
                .filter(button -> name.equals(button.getName()))
                .toList();
    }

    private static <T extends Component> T named(
            Container root, Class<T> type, String name) {
        return components(root, type).stream()
                .filter(component -> name.equals(component.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("component not found: " + name));
    }

    private static <T extends Component> List<T> components(
            Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container container) {
                found.addAll(components(container, type));
            }
        }
        return found;
    }

    private static boolean awaitCondition(java.util.function.BooleanSupplier condition)
            throws Exception {
        for (int attempt = 0; attempt < 250; attempt++) {
            AtomicBoolean satisfied = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) return true;
            Thread.sleep(20);
        }
        return false;
    }
}
