package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Exercises queue, cancellation and reserved-copy feedback in the Swing client. */
class LibraryReservationUiTest {

    @Test
    void unavailableLocationExplainsQueueAndActiveReservationCanBeCanceled() throws Exception {
        try (CampusServer server = new CampusServer(0, 4)) {
            server.start();
            ClientContext firstBorrower = login(server, "20260001");
            ClientContext secondBorrower = login(server, "20260002");
            assertTrue(firstBorrower.send(LibraryActions.BORROW_COPY,
                    new CopyBorrowRequest("SEU-B005-001")).isSuccess());
            assertTrue(secondBorrower.send(LibraryActions.BORROW_COPY,
                    new CopyBorrowRequest("SEU-B005-002")).isSuccess());

            ClientContext waitingReader = login(server, "20260003");
            AtomicReference<LibraryModePanel> root = new AtomicReference<>();
            onEdt(() -> root.set((LibraryModePanel)
                    new LibraryClientModule().createView(waitingReader)));
            onEdt(() -> named(root.get(), JButton.class, "library.mode.online").doClick());
            JTabbedPane navigation = named(root.get(), JTabbedPane.class, "library.navigation");
            LibraryPanel catalog = (LibraryPanel) navigation.getComponentAt(0);
            JTable results = named(catalog, JTable.class, "library.searchResults");
            onEdt(() -> {
                named(catalog, JTextField.class, "library.search.keyword")
                        .setText("9787101003048");
                button(catalog, "搜索").doClick();
            });
            awaitUi(() -> results.getRowCount() == 1 && results.isEnabled());
            onEdt(() -> results.setRowSelectionInterval(0, 0));

            @SuppressWarnings("rawtypes")
            JComboBox locations = named(catalog, JComboBox.class, "library.reservation.location");
            JButton reserve = named(catalog, JButton.class, "library.reservation.create");
            JLabel hint = named(catalog, JLabel.class, "library.reservation.hint");
            awaitUi(() -> locations.getItemCount() == 2 && reserve.isEnabled());
            assertTrue(hint.getText().contains("进入预约队列"));
            onEdt(reserve::doClick);
            JLabel searchStatus = named(catalog, JLabel.class, "library.searchStatus");
            awaitUi(() -> searchStatus.getText().contains("预约成功")
                    && searchStatus.getText().contains("当前第 1 位"));

            MyLibraryPanel myLibrary = (MyLibraryPanel) navigation.getComponentAt(1);
            onEdt(() -> navigation.setSelectedIndex(1));
            JTabbedPane recordTabs = named(myLibrary, JTabbedPane.class, "library.recordTabs");
            assertEquals(List.of("当前借阅", "历史借阅", "我的预约"),
                    java.util.stream.IntStream.range(0, recordTabs.getTabCount())
                            .mapToObj(recordTabs::getTitleAt).toList());
            JTable reservations = named(myLibrary, JTable.class, "library.myReservations");
            awaitUi(() -> reservations.getRowCount() == 1 && reservations.isEnabled());
            assertEquals("排队中", reservations.getValueAt(0, 2));
            assertEquals("第 1 位", reservations.getValueAt(0, 3));
            assertEquals("—", reservations.getValueAt(0, 4));
            assertEquals("—", reservations.getValueAt(0, 5));

            JButton cancel = named(myLibrary, JButton.class, "library.reservation.cancel");
            onEdt(() -> reservations.setRowSelectionInterval(0, 0));
            awaitUi(cancel::isEnabled);
            onEdt(cancel::doClick);
            awaitUi(() -> reservations.isEnabled()
                    && "已取消".equals(reservations.getValueAt(0, 2)));
            onEdt(() -> reservations.setRowSelectionInterval(0, 0));
            assertFalse(cancel.isEnabled());
        }
    }

    @Test
    void terminalClearlyRejectsCopyReservedForAnotherReader() throws Exception {
        try (CampusServer server = new CampusServer(0, 3)) {
            server.start();
            ClientContext owner = login(server, "20260001");
            String location = search(owner, "9787111213826").getBooks().getFirst()
                    .getLocations().getFirst().getLocation();
            assertTrue(owner.send(LibraryActions.CREATE_RESERVATION,
                    new CreateReservationRequest("B001", location)).isSuccess());

            ClientContext other = login(server, "20260002");
            AtomicReference<SelfServicePanel> panel = new AtomicReference<>();
            onEdt(() -> panel.set(new SelfServicePanel(other)));
            JTextField barcode = named(
                    panel.get(), JTextField.class, "library.selfService.barcode");
            JButton borrow = button(panel.get(), "借书登记");
            JButton giveBack = button(panel.get(), "归还登记");
            onEdt(() -> barcode.setText("SEU-B001-001"));

            JLabel check = named(
                    panel.get(), JLabel.class, "library.selfService.reservationCheck");
            JLabel outcome = named(panel.get(), JLabel.class, "library.selfService.outcome");
            awaitUi(() -> check.getText().contains("其他读者预约保留"));
            assertFalse(borrow.isEnabled());
            assertFalse(giveBack.isEnabled());
            assertTrue(outcome.getText().contains("没有可执行"));
        }
    }

    private static BookSearchResult search(ClientContext context, String keyword) throws Exception {
        Response response = context.send(
                LibraryActions.SEARCH_BOOKS,
                new edu.seu.vcampus.common.library.BookSearchRequest(keyword, null));
        assertTrue(response.isSuccess(), response.getMessage());
        return (BookSearchResult) response.getData();
    }

    private static ClientContext login(CampusServer server, String username) throws Exception {
        ClientContext context = new ClientContext(
                new CampusClient("127.0.0.1", server.getPort(), 2_000));
        assertTrue(context.login(username, "123456".toCharArray()).isSuccess());
        return context;
    }

    private static JButton button(Container root, String text) {
        return descendants(root).stream().filter(JButton.class::isInstance)
                .map(JButton.class::cast).filter(value -> text.equals(value.getText()))
                .findFirst().orElseThrow();
    }

    private static <T extends JComponent> T named(
            Container root, Class<T> type, String name) {
        return descendants(root).stream().filter(type::isInstance).map(type::cast)
                .filter(value -> name.equals(value.getName())).findFirst().orElseThrow();
    }

    private static List<Component> descendants(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component component : root.getComponents()) {
            result.add(component);
            if (component instanceof Container child) {
                result.addAll(descendants(child));
            }
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
            if (value.get()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("timed out waiting for Swing state");
    }
}
