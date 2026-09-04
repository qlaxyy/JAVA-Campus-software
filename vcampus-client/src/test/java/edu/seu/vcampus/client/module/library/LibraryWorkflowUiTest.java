package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.*;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import javax.swing.table.JTableHeader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises actual Swing events and Socket responses without displaying a desktop window. */
class LibraryWorkflowUiTest {

    @Test
    void navigationUsesTabsWithoutDuplicateMyBorrowsButton() throws Exception {
        onEdt(() -> {
            JTabbedPane navigation = (JTabbedPane) new LibraryClientModule().createView(
                    new ClientContext(new CampusClient("127.0.0.1", 1)));
            assertEquals(2, navigation.getTabCount());
            assertEquals("馆藏查询", navigation.getTitleAt(0));
            assertEquals("我的借阅", navigation.getTitleAt(1));
            assertFalse(descendants(navigation).stream()
                    .filter(JButton.class::isInstance).map(JButton.class::cast)
                    .anyMatch(button -> "查看我的借阅".equals(button.getText())));
        });
    }

    @Test
    void allLibraryTablesRejectHeaderDraggingButRetainClickSorting() throws Exception {
        onEdt(() -> {
            JComponent view = new LibraryClientModule().createView(
                    new ClientContext(new CampusClient("127.0.0.1", 1)));
            List<JTable> tables = descendants(view).stream()
                    .filter(JTable.class::isInstance).map(JTable.class::cast).toList();
            assertEquals(3, tables.size());
            for (JTable table : tables) {
                JTableHeader header = table.getTableHeader();
                assertFalse(header.getReorderingAllowed());
                assertFalse(header.getResizingAllowed());
                assertFalse(table.getDragEnabled());
                table.setSize(900, 180);
                table.doLayout();
                header.setSize(900, 30);
                List<Integer> order = java.util.Collections.list(table.getColumnModel().getColumns())
                        .stream().map(column -> column.getModelIndex()).toList();
                List<Integer> widths = java.util.Collections.list(table.getColumnModel().getColumns())
                        .stream().map(column -> column.getWidth()).toList();
                int firstColumnWidth = table.getColumnModel().getColumn(0).getWidth();
                dragHeader(header, firstColumnWidth / 2, firstColumnWidth * 3);
                dragHeader(header, firstColumnWidth - 1, firstColumnWidth + 60);
                assertEquals(order, java.util.Collections.list(table.getColumnModel().getColumns())
                        .stream().map(column -> column.getModelIndex()).toList());
                assertEquals(widths, java.util.Collections.list(table.getColumnModel().getColumns())
                        .stream().map(column -> column.getWidth()).toList());
                header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, firstColumnWidth / 2, 15,
                        1, false, MouseEvent.BUTTON1));
                assertEquals(0, table.getRowSorter().getSortKeys().getFirst().getColumn());
                assertEquals(SortOrder.ASCENDING, table.getRowSorter().getSortKeys().getFirst().getSortOrder());
            }
        });
    }

    private static void dragHeader(JTableHeader header, int fromX, int toX) {
        long time = System.currentTimeMillis();
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_PRESSED,
                time, MouseEvent.BUTTON1_DOWN_MASK, fromX, 15, 1, false, MouseEvent.BUTTON1));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_DRAGGED,
                time + 1, MouseEvent.BUTTON1_DOWN_MASK, toX, 15, 0, false, MouseEvent.NOBUTTON));
        header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_RELEASED,
                time + 2, 0, toX, 15, 1, false, MouseEvent.BUTTON1));
    }

    @Test
    void userCanBorrowNavigateReturnAndSeeHistoryAndRestoredStock() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = context(server);
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            AtomicReference<JTabbedPane> root = new AtomicReference<>();
            onEdt(() -> root.set((JTabbedPane) new LibraryClientModule().createView(context)));
            JTabbedPane navigation = root.get();
            LibraryPanel catalog = (LibraryPanel) navigation.getComponentAt(0);
            MyBorrowPanel borrows = (MyBorrowPanel) navigation.getComponentAt(1);
            JTable searchTable = find(catalog, JTable.class);
            JTable current = named(borrows, JTable.class, "library.currentBorrows");
            JTable history = named(borrows, JTable.class, "library.borrowHistory");
            onEdt(() -> {
                find(catalog, JTextField.class).setText("9787111213826");
                button(catalog, "搜索").doClick();
            });
            awaitUi(() -> searchTable.getRowCount() == 1 && searchTable.isEnabled());
            onEdt(() -> {
                assertEquals("2/5", searchTable.getValueAt(0, 5));
                searchTable.setRowSelectionInterval(0, 0);
                button(catalog, "借阅选中图书").doClick();
            });
            awaitUi(() -> searchTable.isEnabled() && searchTable.getRowCount() == 1
                    && "1/5".equals(searchTable.getValueAt(0, 5)));
            onEdt(() -> {
                assertTrue(label(catalog, "library.borrowOutcome").getText().contains("借阅成功"));
                navigation.setSelectedIndex(navigation.indexOfTab("我的借阅"));
            });
            awaitUi(() -> current.getRowCount() == 1 && current.isEnabled());
            onEdt(() -> {
                assertEquals(1, navigation.getSelectedIndex());
                assertEquals("Java编程思想", current.getValueAt(0, 0));
                assertEquals("借阅中", current.getValueAt(0, 4));
                assertFalse(button(borrows, "归还选中图书").isEnabled());
                current.setRowSelectionInterval(0, 0);
                capture(navigation, "library-current-borrows.png");
                button(borrows, "归还选中图书").doClick();
                // A second click while the worker runs must not enqueue another request.
                button(borrows, "归还选中图书").doClick();
            });
            awaitUi(() -> current.isEnabled() && current.getRowCount() == 0 && history.getRowCount() == 1);
            onEdt(() -> {
                assertTrue(label(borrows, "library.returnOutcome").getText().contains("归还成功"));
                assertEquals("已归还", history.getValueAt(0, 4));
                assertNotEquals("—", history.getValueAt(0, 3));
                named(borrows, JTabbedPane.class, "library.recordTabs").setSelectedIndex(1);
                history.setRowSelectionInterval(0, 0);
                assertFalse(button(borrows, "归还选中图书").isEnabled());
                capture(navigation, "library-return-history.png");
                navigation.setSelectedIndex(0);
            });
            awaitUi(() -> searchTable.isEnabled() && "2/5".equals(searchTable.getValueAt(0, 5)));
        }
    }

    @Test
    void sortingAndHistorySelectionNeverReturnTheWrongRecord() throws Exception {
        AtomicReference<String> returnedId = new AtomicReference<>();
        AtomicInteger returnCalls = new AtomicInteger();
        ActionRouter router = new ActionRouter();
        router.register(LibraryActions.GET_BORROW_RECORDS, request -> Response.success(request, "ok",
                new ArrayList<>(List.of(record("R-Z", "Z book", "BORROWED", false),
                        record("R-A", "A book", returnedId.get() == null ? "BORROWED" : "RETURNED", true),
                        record("R-H", "History book", "RETURNED", false)))));
        router.register(LibraryActions.RETURN_BOOK, request -> {
            returnedId.set(((BookReturnRequest) request.getData()).getRecordId());
            returnCalls.incrementAndGet();
            return Response.success(request, "ok", null);
        });
        try (CampusServer server = new CampusServer(0, 2, router)) {
            server.start();
            MyBorrowPanel panel = createMyBorrows(server);
            JTable table = named(panel, JTable.class, "library.currentBorrows");
            JTable history = named(panel, JTable.class, "library.borrowHistory");
            onEdt(panel::refresh);
            awaitUi(() -> table.isEnabled() && table.getRowCount() == 2);
            onEdt(() -> {
                assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
                assertEquals(ListSelectionModel.SINGLE_SELECTION, history.getSelectionModel().getSelectionMode());
                assertEquals("已逾期，请尽快归还", table.getValueAt(1, 5));
                assertFalse(table.isCellEditable(0, 0));
                table.getRowSorter().toggleSortOrder(0);
                assertEquals("A book", table.getValueAt(0, 0));
                table.setRowSelectionInterval(0, 0);
                JTabbedPane tabs = named(panel, JTabbedPane.class, "library.recordTabs");
                tabs.setSelectedIndex(1);
                history.setRowSelectionInterval(0, 0);
                assertFalse(button(panel, "归还选中图书").isEnabled());
                tabs.setSelectedIndex(0);
                button(panel, "归还选中图书").doClick();
                button(panel, "归还选中图书").doClick();
            });
            awaitUi(() -> table.isEnabled() && table.getRowCount() == 1);
            assertEquals("R-A", returnedId.get());
            assertEquals(1, returnCalls.get());
        }
    }

    @Test
    void borrowSuccessRemainsVisibleWhenStockRefreshFails() throws Exception {
        AtomicInteger searches = new AtomicInteger();
        ActionRouter router = new ActionRouter();
        router.register(LibraryActions.SEARCH_BOOKS, request -> searches.incrementAndGet() == 1
                ? Response.success(request, "ok", new BookSearchResult(List.of(
                        new BookDTO("B1", "isbn", "Test book", "Author", "Category", 1, 1))))
                : Response.failure(request.getRequestId(), ErrorCodes.COMMON_SERVER_ERROR, "failed"));
        router.register(LibraryActions.BORROW_BOOK, request -> Response.success(request, "ok", null));
        try (CampusServer server = new CampusServer(0, 2, router)) {
            server.start();
            AtomicReference<LibraryPanel> holder = new AtomicReference<>();
            onEdt(() -> holder.set(new LibraryPanel(context(server))));
            LibraryPanel panel = holder.get();
            JTable table = find(panel, JTable.class);
            onEdt(() -> {
                find(panel, JTextField.class).setText("Test");
                button(panel, "搜索").doClick();
            });
            awaitUi(() -> table.isEnabled() && table.getRowCount() == 1);
            onEdt(() -> {
                table.setRowSelectionInterval(0, 0);
                button(panel, "借阅选中图书").doClick();
            });
            awaitUi(() -> table.isEnabled()
                    && label(panel, "library.searchStatus").getText().contains("库存刷新失败"));
            onEdt(() -> {
                assertTrue(label(panel, "library.borrowOutcome").getText().startsWith("借阅成功"));
                assertEquals(0, table.getRowCount());
                assertFalse(button(panel, "借阅选中图书").isEnabled());
            });
        }
    }

    @Test
    void returnSuccessRemainsVisibleWhenRecordRefreshFails() throws Exception {
        AtomicInteger queries = new AtomicInteger();
        ActionRouter router = new ActionRouter();
        router.register(LibraryActions.GET_BORROW_RECORDS, request -> queries.incrementAndGet() == 1
                ? Response.success(request, "ok", new ArrayList<>(List.of(record("R1", "Book", "BORROWED", false))))
                : Response.failure(request.getRequestId(), ErrorCodes.COMMON_SERVER_ERROR, "failed"));
        router.register(LibraryActions.RETURN_BOOK, request -> Response.success(request, "ok", null));
        try (CampusServer server = new CampusServer(0, 2, router)) {
            server.start();
            MyBorrowPanel panel = createMyBorrows(server);
            JTable table = named(panel, JTable.class, "library.currentBorrows");
            onEdt(panel::refresh);
            awaitUi(() -> table.isEnabled() && table.getRowCount() == 1);
            onEdt(() -> {
                table.setRowSelectionInterval(0, 0);
                button(panel, "归还选中图书").doClick();
            });
            awaitUi(() -> table.isEnabled()
                    && label(panel, "library.recordsStatus").getText().contains("记录刷新失败"));
            onEdt(() -> {
                assertTrue(label(panel, "library.returnOutcome").getText().startsWith("归还成功"));
                assertEquals(0, table.getRowCount());
                assertFalse(button(panel, "归还选中图书").isEnabled());
            });
        }
    }

    @Test
    void emptyAndMalformedRecordResponsesDoNotEnableReturn() throws Exception {
        AtomicInteger queries = new AtomicInteger();
        ActionRouter router = new ActionRouter();
        router.register(LibraryActions.GET_BORROW_RECORDS, request -> Response.success(request, "ok",
                queries.incrementAndGet() == 1 ? new ArrayList<>() : new ArrayList<>(List.of("invalid record"))));
        try (CampusServer server = new CampusServer(0, 2, router)) {
            server.start();
            MyBorrowPanel panel = createMyBorrows(server);
            onEdt(panel::refresh);
            awaitUi(() -> label(panel, "library.recordsStatus").getText().contains("当前借阅 0"));
            onEdt(() -> {
                assertFalse(button(panel, "归还选中图书").isEnabled());
                panel.refresh();
            });
            awaitUi(() -> label(panel, "library.recordsStatus").getText().contains("数据格式不正确"));
            onEdt(() -> assertFalse(button(panel, "归还选中图书").isEnabled()));
        }
    }

    @Test
    void connectionFailureClearsStaleRowsAndDoesNotClaimReturnFailed() throws Exception {
        ActionRouter router = new ActionRouter();
        router.register(LibraryActions.GET_BORROW_RECORDS, request -> Response.success(request, "ok",
                new ArrayList<>(List.of(record("R1", "Book", "BORROWED", false)))));
        try (CampusServer server = new CampusServer(0, 2, router)) {
            server.start();
            MyBorrowPanel panel = createMyBorrows(server);
            JTable table = named(panel, JTable.class, "library.currentBorrows");
            onEdt(panel::refresh);
            awaitUi(() -> table.isEnabled() && table.getRowCount() == 1);
            server.close();
            onEdt(() -> {
                table.setRowSelectionInterval(0, 0);
                button(panel, "归还选中图书").doClick();
            });
            awaitUi(() -> table.isEnabled()
                    && label(panel, "library.returnOutcome").getText().contains("结果未确认"));
            onEdt(() -> {
                assertEquals(0, table.getRowCount());
                assertFalse(button(panel, "归还选中图书").isEnabled());
                assertTrue(button(panel, "刷新借阅记录").isEnabled());
            });
        }
    }

    private static BorrowRecordDTO record(String id, String title, String status, boolean overdue) {
        LocalDateTime time = LocalDateTime.of(2026, 9, 4, 8, 0);
        return new BorrowRecordDTO(id, "B-" + id, title, time.minusDays(30), time,
                status.equals("RETURNED") ? time : null, status, status.equals("BORROWED") && overdue);
    }

    private static MyBorrowPanel createMyBorrows(CampusServer server) throws Exception {
        AtomicReference<MyBorrowPanel> result = new AtomicReference<>();
        onEdt(() -> result.set(new MyBorrowPanel(context(server))));
        return result.get();
    }

    private static ClientContext context(CampusServer server) {
        return new ClientContext(new CampusClient("127.0.0.1", server.getPort(), 1000));
    }

    private static void onEdt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }

    private static void awaitUi(BooleanSupplier condition) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Timer timer = new Timer(10, event -> {
            try {
                if (condition.getAsBoolean()) { ready.countDown(); }
            } catch (Throwable exception) {
                failure.set(exception);
                ready.countDown();
            }
        });
        onEdt(timer::start);
        try {
            assertTrue(ready.await(5, TimeUnit.SECONDS), "UI did not reach expected state");
            if (failure.get() != null) { throw new AssertionError(failure.get()); }
        } finally {
            onEdt(timer::stop);
        }
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        return descendants(root).stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static <T extends Component> T named(Container root, Class<T> type, String name) {
        return descendants(root).stream().filter(type::isInstance).map(type::cast)
                .filter(component -> name.equals(component.getName())).findFirst().orElseThrow();
    }

    private static JButton button(Container root, String text) {
        return descendants(root).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static JLabel label(Container root, String name) { return named(root, JLabel.class, name); }

    private static List<Component> descendants(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container container) { result.addAll(descendants(container)); }
        }
        return result;
    }

    private static void capture(JComponent component, String filename) {
        if (!Boolean.getBoolean("library.captureUi")) { return; }
        // Displayed JTable instances install this header during addNotify; emulate that offscreen.
        for (Component child : descendants(component)) {
            if (child instanceof JScrollPane scroll
                    && scroll.getViewport().getView() instanceof JTable table) {
                scroll.setColumnHeaderView(table.getTableHeader());
            }
        }
        component.setSize(1100, 620);
        layout(component);
        var image = new java.awt.image.BufferedImage(1100, 620, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(UIManager.getColor("Panel.background"));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            component.printAll(graphics);
            java.nio.file.Path output = java.nio.file.Path.of("target", filename);
            javax.imageio.ImageIO.write(image, "png", output.toFile());
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        } finally {
            graphics.dispose();
        }
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) { layout(nested); }
        }
    }
}
