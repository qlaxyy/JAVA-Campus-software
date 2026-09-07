package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** Focused Swing checks for role visibility and client-side prevention of illegal admin operations. */
class LibraryAdminUiTest {

    @Test
    void onlyLibraryScopedAdministratorsSeeTheManagementWorkspace() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            assertEquals(3, tabCount(login(server, "student001")));
            assertEquals(3, tabCount(login(server, "shopadmin")));
            assertEquals(4, tabCount(login(server, "libraryadmin")));
            assertEquals(4, tabCount(login(server, "admin")));
        }
    }

    @Test
    void selectionEnablesOnlyStateTransitionsKnownToBeLegal() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            ClientContext context = login(server, "libraryadmin");
            AtomicReference<JTabbedPane> root = new AtomicReference<>();
            onEdt(() -> root.set((JTabbedPane) new LibraryClientModule().createView(context)));
            JTabbedPane navigation = root.get();
            LibraryAdminPanel admin = (LibraryAdminPanel) navigation.getComponentAt(3);
            onEdt(() -> navigation.setSelectedIndex(3));

            JTable books = named(admin, JTable.class, "library.admin.books");
            awaitUi(() -> books.getRowCount() > 0 && books.isEnabled());
            JButton activate = button(admin, "开放借阅");
            JButton deactivate = button(admin, "停止借阅");
            assertFalse(activate.isEnabled());
            assertFalse(deactivate.isEnabled());
            onEdt(() -> books.setRowSelectionInterval(0, 0));
            awaitUi(deactivate::isEnabled);
            assertFalse(activate.isEnabled(), "seed catalog entries are already active");

            JTabbedPane areas = named(admin, JTabbedPane.class, "library.admin.tabs");
            onEdt(() -> areas.setSelectedIndex(1));
            JTable copies = named(admin, JTable.class, "library.admin.copies");
            awaitUi(() -> copies.getRowCount() > 0 && copies.isEnabled());
            JButton shelf = button(admin, "确认上架");
            JButton withdraw = button(admin, "注销单册");
            assertFalse(shelf.isEnabled());
            assertFalse(withdraw.isEnabled());
            onEdt(() -> copies.setRowSelectionInterval(0, 0));
            awaitUi(withdraw::isEnabled);
            assertFalse(shelf.isEnabled(), "an AVAILABLE copy must not be shelved again");
            assertFalse(named(admin, JTextField.class, "library.admin.barcode").isEnabled(),
                    "barcodes are immutable after registration");

            onEdt(() -> areas.setSelectedIndex(2));
            JTable borrows = named(admin, JTable.class, "library.admin.borrows");
            awaitUi(borrows::isEnabled);
            assertFalse(borrows.getModel().isCellEditable(0, 0));
        }
    }

    private static int tabCount(ClientContext context) throws Exception {
        AtomicReference<Integer> count = new AtomicReference<>();
        onEdt(() -> count.set(((JTabbedPane) new LibraryClientModule().createView(context)).getTabCount()));
        return count.get();
    }

    private static ClientContext login(CampusServer server, String username) throws Exception {
        ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort(), 2000));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }

    private static JButton button(Container root, String text) {
        return descendants(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(value -> text.equals(value.getText())).findFirst().orElseThrow();
    }

    private static <T extends JComponent> T named(Container root, Class<T> type, String name) {
        return descendants(root).stream().filter(type::isInstance).map(type::cast)
                .filter(value -> name.equals(value.getName())).findFirst().orElseThrow();
    }

    private static List<Component> descendants(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component component : root.getComponents()) {
            result.add(component);
            if (component instanceof Container child) { result.addAll(descendants(child)); }
        }
        return result;
    }

    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }

    private static void awaitUi(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            AtomicReference<Boolean> value = new AtomicReference<>(false);
            onEdt(() -> value.set(condition.getAsBoolean()));
            if (value.get()) { return; }
            Thread.sleep(20);
        }
        fail("timed out waiting for Swing state");
    }
}
