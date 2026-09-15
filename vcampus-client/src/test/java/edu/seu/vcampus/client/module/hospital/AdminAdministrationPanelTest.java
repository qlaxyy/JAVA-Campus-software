package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AdminDepartmentWorkspaceView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.HospitalActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
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
            assertTrue(context.login("20260005", "123456".toCharArray()).isSuccess());
            AdminDepartmentWorkspaceView initialDepartments = assertInstanceOf(
                    AdminDepartmentWorkspaceView.class,
                    context.send(HospitalActions.GET_ADMIN_DEPARTMENT_WORKSPACE, null)
                            .getData());
            int initialDepartmentCount = initialDepartments.getDepartments().size();

            AdminDepartmentPanel[] departments = new AdminDepartmentPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                departments[0] = new AdminDepartmentPanel(context, () -> { });
                departments[0].activate();
            });
            assertTrue(awaitCondition(() -> namedButtons(
                    departments[0], "adminDepartmentButton").size()
                    == initialDepartmentCount));

            SwingUtilities.invokeAndWait(() -> {
                named(departments[0], JTextField.class, "adminDepartmentName")
                        .setText("康复医学（测试）");
                namedButton(departments[0], "saveDepartmentButton").doClick();
            });
            assertTrue(awaitCondition(() -> namedButtons(
                    departments[0], "adminDepartmentButton").size()
                    == initialDepartmentCount + 1));
            AdminDepartmentWorkspaceView workspace = assertInstanceOf(
                    AdminDepartmentWorkspaceView.class,
                    context.send(HospitalActions.GET_ADMIN_DEPARTMENT_WORKSPACE, null)
                            .getData());
            assertEquals(initialDepartmentCount + 1, workspace.getDepartments().size());

            AdminDoctorPanel[] doctors = new AdminDoctorPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                doctors[0] = new AdminDoctorPanel(context, () -> { });
                doctors[0].activate();
            });
            JComboBox<?> target = named(
                    doctors[0], JComboBox.class, "doctorDeactivationTarget");
            assertTrue(awaitCondition(() -> target.getItemCount() > 0));
            AdminScheduleWorkspaceView doctorWorkspace = assertInstanceOf(
                    AdminScheduleWorkspaceView.class,
                    context.send(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null)
                            .getData());
            int doctorCount = doctorWorkspace.getDoctors().size();
            long doctorDepartmentCount = doctorWorkspace.getDoctors().stream()
                    .map(item -> item.getDepartmentId())
                    .distinct()
                    .count();

            AdminDoctorDirectoryPanel[] directory = new AdminDoctorDirectoryPanel[1];
            SwingUtilities.invokeAndWait(() -> {
                directory[0] = new AdminDoctorDirectoryPanel(context, () -> { });
                directory[0].activate();
            });
            assertTrue(awaitCondition(() -> namedComponents(
                    directory[0], JPanel.class, "adminDoctorDirectoryCard").size()
                    == doctorCount));
            assertEquals(doctorDepartmentCount, namedComponents(
                    directory[0], JPanel.class, "adminDoctorDepartmentGroup").size());
            assertEquals(doctorCount, namedComponents(
                    directory[0], JPanel.class, "adminDoctorDirectoryCard").size());
            assertTrue(namedComponents(directory[0], JPanel.class,
                    "adminDoctorDirectoryCard").stream()
                    .allMatch(card -> card.getPreferredSize().height >= 78));
            List<String> departmentNames = namedComponents(
                    directory[0], JLabel.class, "adminDoctorDepartmentName").stream()
                    .map(JLabel::getText)
                    .toList();
            assertTrue(departmentNames.contains("全科门诊"));
            assertTrue(departmentNames.contains("眼科门诊"));
            SwingUtilities.invokeAndWait(() -> {
                renderSnapshot(directory[0], 1_200, 900,
                        "admin-doctors-by-department.png");
                renderSnapshot(directory[0], 520, 760,
                        "admin-doctors-by-department-compact.png");
            });
            SwingUtilities.invokeAndWait(() -> {
                named(doctors[0], JTextArea.class, "doctorDeactivationReason")
                        .setText("医生离岗（页面测试）");
                namedButton(doctors[0], "submitDoctorDeactivationButton").doClick();
            });
            assertTrue(awaitCondition(() -> {
                try {
                    DoctorApplicationListResponse applications = assertInstanceOf(
                            DoctorApplicationListResponse.class,
                            context.send(HospitalActions.LIST_DOCTOR_APPLICATIONS, null)
                                    .getData());
                    return applications.getApplications().stream()
                            .anyMatch(item -> item.getApplicationType()
                                    == DoctorApplicationType.DEACTIVATE_DOCTOR
                                    && item.getStatus() == DoctorApplicationStatus.PENDING);
                } catch (Exception exception) {
                    return false;
                }
            }));

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

    private static <T extends Component> List<T> namedComponents(
            Container root, Class<T> type, String name) {
        return components(root, type).stream()
                .filter(component -> name.equals(component.getName()))
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

    private static void renderSnapshot(
            JPanel panel,
            int width,
            int height,
            String fileName) {
        panel.setSize(width, height);
        for (int pass = 0; pass < 5; pass++) {
            layoutTree(panel);
        }
        var image = new java.awt.image.BufferedImage(
                width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        try {
            Path directory = Path.of("target", "hospital-ui");
            java.nio.file.Files.createDirectories(directory);
            javax.imageio.ImageIO.write(image, "png",
                    directory.resolve(fileName).toFile());
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void layoutTree(Container root) {
        root.invalidate();
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
    }
}
