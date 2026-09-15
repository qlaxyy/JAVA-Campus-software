package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.library.AddBookRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;
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

/**
 * 在线目录的分页控件与服务器往返。
 *
 * <p>演示书目只有 5 本，凑不满一页，所以这里先用馆员账号把书目补到 23 本，再验证读者端
 * 翻页：第一页 20 条、第二页 3 条，页码与按钮可用性随服务器回显的页码走，而不是随本地
 * 计数——否则别人删书后界面会停在一个不存在的页上。
 */
class LibraryCatalogPagingUiTest {

    private static final int SEED_BOOKS = 5;
    private static final int ADDED_BOOKS = 18;
    private static final int TOTAL_BOOKS = SEED_BOOKS + ADDED_BOOKS;

    @Test
    void readerPagesThroughTheWholeCatalogAndSeesWhereTheResultEnds() throws Exception {
        try (CampusServer server = new CampusServer(0, 4)) {
            server.start();
            ClientContext librarian = login(server, "20260003");
            for (int index = 0; index < ADDED_BOOKS; index++) {
                Response added = librarian.send(LibraryActions.ADD_BOOK, new AddBookRequest(
                        "978%010d".formatted(index), "分页测试书目" + index, "测试作者",
                        "C001", "东南大学出版社", 2026, "中文", 1_000));
                assertTrue(added.isSuccess(), added.getMessage());
            }

            ClientContext reader = login(server, "20260006");
            AtomicReference<LibraryPanel> root = new AtomicReference<>();
            onEdt(() -> root.set(new LibraryPanel(reader)));
            LibraryPanel catalog = root.get();

            JTextField keyword = named(catalog, JTextField.class, "library.search.keyword");
            JTable results = named(catalog, JTable.class, "library.searchResults");
            JButton previous = named(catalog, JButton.class, "library.search.previousPage");
            JButton next = named(catalog, JButton.class, "library.search.nextPage");
            JLabel page = named(catalog, JLabel.class, "library.search.page");
            JLabel status = named(catalog, JLabel.class, "library.searchStatus");

            onEdt(() -> {
                keyword.setText("");
                button(catalog, "搜索").doClick();
            });
            awaitUi(() -> results.isEnabled() && results.getRowCount() == 20);
            assertEquals("第 1 / 2 页", page.getText());
            assertFalse(previous.isEnabled(), "第一页没有上一页");
            assertTrue(next.isEnabled());
            assertTrue(status.getText().contains("共 " + TOTAL_BOOKS + " 本书目"), status.getText());
            assertTrue(status.getText().contains("本页显示 1–20"), status.getText());

            onEdt(next::doClick);
            awaitUi(() -> results.isEnabled() && "第 2 / 2 页".equals(page.getText()));
            assertEquals(TOTAL_BOOKS - 20, results.getRowCount(), "最后一页只剩余数");
            assertTrue(previous.isEnabled());
            assertFalse(next.isEnabled(), "最后一页没有下一页");
            assertTrue(status.getText().contains("本页显示 21–" + TOTAL_BOOKS), status.getText());

            onEdt(previous::doClick);
            awaitUi(() -> results.isEnabled() && "第 1 / 2 页".equals(page.getText()));
            assertEquals(20, results.getRowCount());

            // 换关键词要回到第一页，否则新结果集会从旧页码开始，可能整页空白。
            onEdt(() -> {
                keyword.setText("分页测试书目0");
                button(catalog, "搜索").doClick();
            });
            awaitUi(() -> results.isEnabled() && results.getRowCount() == 1);
            assertEquals("第 1 / 1 页", page.getText());
            assertFalse(previous.isEnabled());
            assertFalse(next.isEnabled());
            assertTrue(status.getText().contains("共 1 本书目"), status.getText());
        }
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

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void awaitUi(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            AtomicReference<Boolean> value = new AtomicReference<>(false);
            onEdt(() -> value.set(condition.getAsBoolean()));
            if (value.get()) { return; }
            Thread.sleep(20);
        }
        fail("timed out waiting for Swing state");
    }
}
