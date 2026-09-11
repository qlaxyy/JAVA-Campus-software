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

/** Exercises the reader workflow across the online library and terminal modes. */
class LibraryWorkflowUiTest {

    @Test
    void readerChoosesModesAndUsesCatalogTerminalAndPersonalRecords() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            ClientContext context = login(server, "20260006");
            AtomicReference<LibraryModePanel> root = new AtomicReference<>();
            onEdt(() -> root.set((LibraryModePanel)
                    new LibraryClientModule().createView(context)));
            LibraryModePanel modes = root.get();
            JPanel selection = named(modes, JPanel.class, "library.modeSelection");
            JPanel online = named(modes, JPanel.class, "library.online");
            JPanel terminalMode = named(modes, JPanel.class, "library.terminal");
            assertTrue(selection.isVisible());
            assertFalse(online.isVisible());
            assertFalse(terminalMode.isVisible());

            onEdt(() -> named(modes, JButton.class, "library.mode.online").doClick());
            assertFalse(selection.isVisible());
            assertTrue(online.isVisible());
            assertFalse(terminalMode.isVisible());

            JTabbedPane tabs = named(online, JTabbedPane.class, "library.navigation");
            assertEquals(List.of("馆藏查询", "我的图书馆"),
                    java.util.stream.IntStream.range(0, tabs.getTabCount()).mapToObj(tabs::getTitleAt).toList());
            assertFalse(descendants(online).stream().anyMatch(SelfServicePanel.class::isInstance));

            LibraryPanel catalog = (LibraryPanel) tabs.getComponentAt(0);
            MyLibraryPanel records = (MyLibraryPanel) tabs.getComponentAt(1);
            SelfServicePanel terminal = named(terminalMode, SelfServicePanel.class,
                    "library.selfService");
            JLabel terminalUser = named(terminal, JLabel.class, "library.selfService.user");
            assertEquals("当前用户：周一", terminalUser.getText());
            assertFalse(terminalUser.getText().contains("U-STUDENT-001"));
            assertFalse(buttons(catalog).stream().anyMatch(button -> button.getText().contains("借阅")));
            assertFalse(buttons(records).stream().anyMatch(button -> button.getText().contains("归还选中")));

            JTable searchResults = named(catalog, JTable.class, "library.searchResults");
            JTextArea holdingDetails = named(catalog, JTextArea.class, "library.holdingDetails");
            @SuppressWarnings("rawtypes")
            JComboBox reservationLocation = named(
                    catalog, JComboBox.class, "library.reservation.location");
            JButton reserve = named(catalog, JButton.class, "library.reservation.create");
            JLabel reservationHint = named(catalog, JLabel.class, "library.reservation.hint");
            JLabel searchStatus = named(catalog, JLabel.class, "library.searchStatus");
            onEdt(() -> {
                named(catalog, JTextField.class, "library.search.keyword").setText("9787111213826");
                button(catalog, "搜索").doClick();
            });
            awaitUi(() -> searchResults.getRowCount() == 1 && searchResults.isEnabled());
            onEdt(() -> searchResults.setRowSelectionInterval(0, 0));
            awaitUi(() -> holdingDetails.getText().contains("九龙湖校区—中文图书阅览室3")
                    && holdingDetails.getText().contains("可借")
                    && holdingDetails.getText().contains("馆藏")
                    && reservationLocation.getItemCount() > 0 && reserve.isEnabled());
            assertTrue(reservationHint.getText().contains("保留 24 小时"));
            onEdt(reserve::doClick);
            awaitUi(() -> searchStatus.getText().contains("预约成功")
                    && searchStatus.getText().contains("SEU-B001-001"));

            onEdt(() -> tabs.setSelectedIndex(1));
            JTable reservations = named(records, JTable.class, "library.myReservations");
            JButton cancelReservation = named(
                    records, JButton.class, "library.reservation.cancel");
            awaitUi(() -> reservations.getRowCount() == 1 && reservations.isEnabled());
            assertEquals("待取书", reservations.getValueAt(0, 2));
            assertEquals("SEU-B001-001", reservations.getValueAt(0, 4));
            onEdt(() -> reservations.setRowSelectionInterval(0, 0));
            awaitUi(cancelReservation::isEnabled);

            onEdt(() -> named(online, JButton.class, "library.mode.back.online").doClick());
            assertTrue(selection.isVisible());
            onEdt(() -> named(modes, JButton.class, "library.mode.terminal").doClick());
            assertTrue(terminalMode.isVisible());
            assertFalse(online.isVisible());

            JTextField barcode = named(terminal, JTextField.class, "library.selfService.barcode");
            JButton borrow = button(terminal, "借书登记");
            JButton giveBack = button(terminal, "归还登记");
            assertFalse(borrow.isEnabled());
            assertFalse(giveBack.isEnabled());
            onEdt(() -> barcode.setText("SEU-B001-001"));
            awaitUi(() -> borrow.isEnabled() && !giveBack.isEnabled());
            onEdt(borrow::doClick);
            JLabel outcome = named(terminal, JLabel.class, "library.selfService.outcome");
            JLabel reservationCheck = named(
                    terminal, JLabel.class, "library.selfService.reservationCheck");
            awaitUi(() -> outcome.getText().contains("借书成功"));
            assertTrue(reservationCheck.getText().contains("本人预约保留"));

            JTable current = named(records, JTable.class, "library.currentBorrows");
            onEdt(() -> named(terminalMode, JButton.class,
                    "library.mode.back.terminal").doClick());
            assertTrue(selection.isVisible());
            onEdt(() -> named(modes, JButton.class, "library.mode.online").doClick());
            awaitUi(() -> current.getRowCount() == 1 && current.isEnabled()
                    && "已借阅".equals(reservations.getValueAt(0, 2)));
            assertEquals("SEU-B001-001", current.getValueAt(0, 1));
            onEdt(() -> reservations.setRowSelectionInterval(0, 0));
            assertFalse(cancelReservation.isEnabled());

            onEdt(() -> {
                named(online, JButton.class, "library.mode.back.online").doClick();
                named(modes, JButton.class, "library.mode.terminal").doClick();
                barcode.setText("SEU-B001-001");
            });
            awaitUi(() -> giveBack.isEnabled() && !borrow.isEnabled());
            onEdt(giveBack::doClick);
            awaitUi(() -> outcome.getText().contains("归还成功") && outcome.getText().contains("等待管理员上架"));
            onEdt(() -> {
                named(terminalMode, JButton.class, "library.mode.back.terminal").doClick();
                named(modes, JButton.class, "library.mode.online").doClick();
                tabs.setSelectedIndex(0);
                tabs.setSelectedIndex(1);
            });
            JTable history = named(records, JTable.class, "library.borrowHistory");
            awaitUi(() -> current.getRowCount() == 0 && history.getRowCount() == 1);
            assertEquals("已归还", history.getValueAt(0, 5));
        }
    }

    @Test
    void everyLibraryTableIsReadOnlySingleSelectionAndHasFixedHeaders() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = login(server, "20260003");
            AtomicReference<JComponent> view = new AtomicReference<>();
            onEdt(() -> view.set(new LibraryClientModule().createView(context)));
            List<JTable> tables = descendants(view.get()).stream()
                    .filter(JTable.class::isInstance).map(JTable.class::cast).toList();
            assertEquals(7, tables.size());
            for (JTable table : tables) {
                assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
                assertFalse(table.getTableHeader().getReorderingAllowed());
                assertFalse(table.getTableHeader().getResizingAllowed());
                assertFalse(table.getDragEnabled());
                assertNotNull(table.getRowSorter());
                for (int column = 0; column < table.getColumnCount(); column++) {
                    assertFalse(table.getModel().isCellEditable(0, column));
                }
            }
        }
    }

    @Test
    void leavingCampusServiceModuleResetsLibraryModeSelection() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = login(server, "20260001");
            AtomicReference<LibraryModePanel> root = new AtomicReference<>();
            onEdt(() -> root.set((LibraryModePanel)
                    new LibraryClientModule().createView(context)));
            LibraryModePanel modes = root.get();
            JPanel selection = named(modes, JPanel.class, "library.modeSelection");
            JPanel online = named(modes, JPanel.class, "library.online");
            JPanel terminal = named(modes, JPanel.class, "library.terminal");

            onEdt(() -> named(modes, JButton.class, "library.mode.online").doClick());
            assertTrue(online.isVisible());
            onEdt(modes::onModuleExit);
            assertTrue(selection.isVisible());
            assertFalse(online.isVisible());

            onEdt(() -> named(modes, JButton.class, "library.mode.terminal").doClick());
            assertTrue(terminal.isVisible());
            onEdt(modes::onModuleExit);
            assertTrue(selection.isVisible());
            assertFalse(terminal.isVisible());
        }
    }

    private static ClientContext login(CampusServer server, String username) throws Exception {
        ClientContext context = new ClientContext(new CampusClient("127.0.0.1", server.getPort(), 2000));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }

    private static List<JButton> buttons(Container root) {
        return descendants(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast).toList();
    }

    private static JButton button(Container root, String text) {
        return buttons(root).stream().filter(value -> text.equals(value.getText())).findFirst().orElseThrow();
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

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

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
