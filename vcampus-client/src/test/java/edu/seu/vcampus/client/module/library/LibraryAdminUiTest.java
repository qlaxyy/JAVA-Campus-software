package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.library.LibraryActions;
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
            assertEquals(2, onlineTabCount(login(server, "20260001")));
            assertEquals(2, onlineTabCount(login(server, "20260006")));
            assertEquals(3, onlineTabCount(login(server, "20260005")));
            assertEquals(3, onlineTabCount(login(server, "20260000")));
        }
    }

    @Test
    void selectionEnablesOnlyStateTransitionsKnownToBeLegal() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            ClientContext context = login(server, "20260005");
            AtomicReference<LibraryModePanel> root = new AtomicReference<>();
            onEdt(() -> root.set((LibraryModePanel)
                    new LibraryClientModule().createView(context)));
            JTabbedPane navigation = named(root.get(), JTabbedPane.class,
                    "library.navigation");
            LibraryAdminPanel admin = (LibraryAdminPanel) navigation.getComponentAt(2);
            onEdt(() -> {
                named(root.get(), JButton.class, "library.mode.online").doClick();
                navigation.setSelectedIndex(2);
            });

            JTable books = named(admin, JTable.class, "library.admin.books");
            awaitUi(() -> books.getRowCount() > 0 && books.isEnabled());
            JButton newBook = button(admin, "＋ 新建书目");
            assertTrue(newBook.isEnabled());
            assertTrue(newBook.getFont().isBold());
            assertNotEquals(newBook.getBackground(), newBook.getForeground());
            JButton activate = button(admin, "开放借阅");
            JButton deactivate = button(admin, "停止借阅");
            assertFalse(activate.isEnabled());
            assertFalse(deactivate.isEnabled());
            onEdt(() -> books.setRowSelectionInterval(0, 0));
            awaitUi(deactivate::isEnabled);
            assertFalse(activate.isEnabled(), "seed catalog entries are already active");

            onEdt(deactivate::doClick);
            awaitUi(() -> books.isEnabled() && books.getRowCount() == 1
                    && "停止借阅".equals(books.getValueAt(0, 8)));
            assertEquals(0, books.getValueAt(0, 10));
            onEdt(() -> books.setRowSelectionInterval(0, 0));
            awaitUi(activate::isEnabled);

            JTabbedPane areas = named(admin, JTabbedPane.class, "library.admin.tabs");
            onEdt(() -> areas.setSelectedIndex(1));
            JTable copies = named(admin, JTable.class, "library.admin.copies");
            awaitUi(() -> copies.getRowCount() > 0 && copies.isEnabled());
            JButton newCopy = button(admin, "＋ 登记新单册");
            assertTrue(newCopy.isEnabled());
            assertTrue(newCopy.getFont().isBold());
            assertNotEquals(newCopy.getBackground(), newCopy.getForeground());
            JButton shelf = button(admin, "确认归架");
            JButton restore = button(admin, "恢复单册");
            JButton withdraw = button(admin, "注销单册");
            assertFalse(shelf.isEnabled());
            assertFalse(restore.isEnabled());
            assertFalse(withdraw.isEnabled());
            onEdt(() -> copies.setRowSelectionInterval(0, 0));
            awaitUi(withdraw::isEnabled);
            assertEquals("在架", copies.getValueAt(0, 4));
            assertEquals("书目已停止借阅", copies.getValueAt(0, 5));
            assertFalse(shelf.isEnabled(), "an AVAILABLE copy must not be shelved again");
            assertFalse(restore.isEnabled(), "an AVAILABLE copy must not be restored");
            assertFalse(named(admin, JTextField.class, "library.admin.barcode").isEnabled(),
                    "barcodes are immutable after registration");

            String copyId = String.valueOf(copies.getValueAt(0, 0));
            assertTrue(context.send(LibraryActions.WITHDRAW_BOOK_COPY,
                    new BookCopyIdRequest(copyId)).isSuccess());
            onEdt(() -> button(admin, "刷新单册").doClick());
            awaitUi(() -> copies.isEnabled() && "已注销".equals(copies.getValueAt(0, 4)));
            onEdt(() -> copies.setRowSelectionInterval(0, 0));
            awaitUi(restore::isEnabled);
            assertFalse(withdraw.isEnabled());
            assertFalse(named(admin, JTextField.class, "library.admin.location").isEnabled());
            onEdt(restore::doClick);
            awaitUi(() -> copies.isEnabled() && "在架".equals(copies.getValueAt(0, 4)));

            onEdt(() -> areas.setSelectedIndex(2));
            JTable borrows = named(admin, JTable.class, "library.admin.borrows");
            awaitUi(borrows::isEnabled);
            assertFalse(borrows.getModel().isCellEditable(0, 0));
        }
    }

    @Test
    void reservedCopyIsLabeledAndCannotBeEditedOrWithdrawn() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            ClientContext reader = login(server, "20260001");
            assertTrue(reader.send(LibraryActions.CREATE_RESERVATION,
                    new CreateReservationRequest(
                            "B001", "九龙湖校区—中文图书阅览室3")).isSuccess());

            ClientContext librarian = login(server, "20260005");
            AtomicReference<LibraryModePanel> root = new AtomicReference<>();
            onEdt(() -> root.set((LibraryModePanel)
                    new LibraryClientModule().createView(librarian)));
            JTabbedPane navigation = named(
                    root.get(), JTabbedPane.class, "library.navigation");
            LibraryAdminPanel admin = (LibraryAdminPanel) navigation.getComponentAt(2);
            onEdt(() -> {
                named(root.get(), JButton.class, "library.mode.online").doClick();
                navigation.setSelectedIndex(2);
            });
            JTable books = named(admin, JTable.class, "library.admin.books");
            awaitUi(() -> books.getRowCount() > 0 && books.isEnabled());
            onEdt(() -> books.setRowSelectionInterval(0, 0));
            JTabbedPane areas = named(admin, JTabbedPane.class, "library.admin.tabs");
            onEdt(() -> areas.setSelectedIndex(1));

            JTable copies = named(admin, JTable.class, "library.admin.copies");
            awaitUi(() -> copies.getRowCount() > 0 && copies.isEnabled());
            assertEquals("预约待取", copies.getValueAt(0, 4));
            assertEquals("不可借（预约待取）", copies.getValueAt(0, 5));
            onEdt(() -> copies.setRowSelectionInterval(0, 0));
            assertFalse(named(admin, JTextField.class, "library.admin.location").isEnabled());
            assertFalse(named(admin, JTextField.class, "library.admin.callNumber").isEnabled());
            assertFalse(button(admin, "保存位置与索书号").isEnabled());
            assertFalse(button(admin, "确认归架").isEnabled());
            assertFalse(button(admin, "恢复单册").isEnabled());
            assertFalse(button(admin, "注销单册").isEnabled());
        }
    }

    private static int onlineTabCount(ClientContext context) throws Exception {
        AtomicReference<Integer> count = new AtomicReference<>();
        onEdt(() -> {
            LibraryModePanel root = (LibraryModePanel)
                    new LibraryClientModule().createView(context);
            count.set(named(root, JTabbedPane.class, "library.navigation").getTabCount());
        });
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
