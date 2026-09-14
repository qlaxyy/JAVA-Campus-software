package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.UserAuditLogEntry;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogPanelTest {
    @Test void fullDetailsRemainSelectableAfterResizeSortAndLiteralSearch() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String full = "业务动作：SHOP.UPDATE_PRODUCT；影响 1 条记录；完整说明[商品]".repeat(8);
            AuditLogPanel panel = new AuditLogPanel(List.of(entry("1", "tblShopProduct", full),
                    entry("2", "tblStudentProfile", "学籍更新")));
            panel.addNotify(); // Install JTable's scroll-pane header, also for off-screen rendering.
            JTable table = (JTable) find(panel, "audit.table");
            JTextArea detail = (JTextArea) find(panel, "audit.detail");
            JTextField search = (JTextField) find(panel, "audit.search");
            assertTrue(detail.getText().contains(full));
            assertFalse(detail.isEditable());
            for (int width : new int[]{780, 1120}) {
                panel.setSize(width, 640);
                layout(panel);
                table.prepareRenderer(table.getCellRenderer(0, 5), 0, 5);
                assertTrue(table.getRowHeight(0) > 36);
                assertTrue(detail.getText().contains(full));
            }
            assertTrue(table.getColumnModel().getColumn(1).getWidth() >= 170);
            assertTrue(table.getColumnModel().getColumn(3).getWidth() >= 175);
            table.getRowSorter().toggleSortOrder(3);
            search.setText("[商品]");
            assertEquals(1, table.getRowCount());
            table.setRowSelectionInterval(0, 0);
            assertTrue(detail.getText().contains("tblShopProduct"));
            assertTrue(detail.getText().contains(full));
            search.setText("没有记录");
            assertEquals(0, table.getRowCount());
            search.setText("");
            assertEquals(2, table.getRowCount());
            table.setRowSelectionInterval(0, 0);
            try {
                Path image = Path.of("target/ui-review/audit-log.png");
                Files.createDirectories(image.getParent());
                BufferedImage screenshot = new BufferedImage(1120, 640, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = screenshot.createGraphics();
                panel.printAll(graphics);
                graphics.dispose();
                ImageIO.write(screenshot, "png", image.toFile());
            } catch (java.io.IOException exception) { throw new AssertionError(exception); }
            panel.removeNotify();
        });
    }

    private static UserAuditLogEntry entry(String id, String table, String detail) {
        return new UserAuditLogEntry(id, 1_789_375_500_000L, "U-ADMIN-001", "20260000",
                "超级管理员", "DATABASE.UPDATE", table, true, detail);
    }
    private static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) { return child; }
            if (child instanceof Container container) {
                Component result = find(container, name);
                if (result != null) { return result; }
            }
        }
        return null;
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) { layout(child); }
        }
    }
}
