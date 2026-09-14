package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke-tests the redesigned library at the application's supported window sizes. */
class LibraryResponsiveLayoutTest {

    @Test
    void keyReaderLayoutsRenderAtMinimumAndLargeSizes() throws Exception {
        ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1, 100));
        LibraryModePanel root = new LibraryModePanel(context);

        for (Dimension size : List.of(new Dimension(900, 560), new Dimension(1280, 760))) {
            SwingUtilities.invokeAndWait(() -> {
                root.onModuleExit();
                render(root, size);
                assertContained(root, named(root, JButton.class, "library.mode.online"));
                assertContained(root, named(root, JButton.class, "library.mode.terminal"));

                named(root, JButton.class, "library.mode.online").doClick();
                render(root, size);
                JPanel results = named(root, JPanel.class, "library.catalog.results");
                JPanel details = named(root, JPanel.class, "library.catalog.details");
                assertTrue(results.getWidth() >= 300, "catalog results must remain usable");
                assertTrue(details.getWidth() >= 250, "holding details must remain readable");
                assertContained(root, results);
                assertContained(root, details);

                JTabbedPane navigation = named(
                        root, JTabbedPane.class, "library.navigation");
                navigation.setSelectedIndex(1);
                render(root, size);
                JTable currentBorrows = named(
                        root, JTable.class, "library.currentBorrows");
                assertTrue(currentBorrows.getWidth() >= 500,
                        "personal records table must remain usable");
                assertTrue(currentBorrows.getHeight() >= 100,
                        "personal records table must retain vertical space");
                assertContained(root, currentBorrows);

                named(root, JButton.class, "library.mode.back.online").doClick();
                named(root, JButton.class, "library.mode.terminal").doClick();
                render(root, size);
                JPanel terminal = named(root, JPanel.class, "library.selfService.card");
                assertTrue(terminal.getWidth() >= 500,
                        "terminal card must remain wide enough; actual=" + terminal.getWidth());
                assertContained(root, terminal);

                LibraryAdminPanel admin = new LibraryAdminPanel(context);
                render(admin, size);
                JPanel bookEditor = named(
                        admin, JPanel.class, "library.admin.bookEditor");
                JTable books = named(admin, JTable.class, "library.admin.books");
                assertTrue(bookEditor.getWidth() >= 300,
                        "book editor must remain readable");
                assertTrue(books.getWidth() >= 300,
                        "admin catalog must remain readable");
                assertContained(admin, books);
            });
        }
    }

    private static void render(JComponent component, Dimension size) {
        component.setSize(size);
        layoutTree(component);
        BufferedImage image = new BufferedImage(
                size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        component.paint(graphics);
        graphics.dispose();
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child && component.isVisible()) {
                layoutTree(child);
            }
        }
    }

    private static void assertContained(Container root, Component component) {
        Rectangle bounds = SwingUtilities.convertRectangle(
                component.getParent(), component.getBounds(), root);
        assertTrue(bounds.x >= 0 && bounds.y >= 0,
                "component must not start outside the page");
        assertTrue(bounds.getMaxX() <= root.getWidth() && bounds.getMaxY() <= root.getHeight(),
                "component must not overflow the page");
    }

    private static <T extends JComponent> T named(
            Container root, Class<T> type, String name) {
        return descendants(root).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(component -> name.equals(component.getName()))
                .findFirst()
                .orElseThrow();
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
}
