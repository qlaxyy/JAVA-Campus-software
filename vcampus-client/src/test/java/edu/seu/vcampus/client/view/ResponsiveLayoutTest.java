package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.module.student.StudentView;
import edu.seu.vcampus.client.module.user.UserAdminPanel;
import edu.seu.vcampus.client.module.library.LibraryPanel;
import edu.seu.vcampus.client.module.library.SelfServicePanel;
import edu.seu.vcampus.client.module.library.LibraryModePanel;
import edu.seu.vcampus.client.module.course.CourseClientModule;
import edu.seu.vcampus.client.application.ClientSession;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.client.module.shop.ShopView;
import edu.seu.vcampus.client.module.hospital.HospitalView;
import edu.seu.vcampus.client.module.card.CardClientModule;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ResponsiveLayoutTest {
    @Test void measuringResponsiveChildrenDoesNotInvalidateBoxLayoutRequests() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel parent = new JPanel();
            parent.setLayout(new BoxLayout(parent, BoxLayout.Y_AXIS));
            JPanel grid = ResponsiveLayout.equalGrid(3, 340, 14);
            for (int i = 0; i < 6; i++) { grid.add(new JLabel("卡片 " + i)); }
            JPanel content = new JPanel(); content.add(new JLabel("表单"));
            JPanel compact = ResponsiveLayout.compact(content, 720);
            parent.add(grid); parent.add(compact);
            for (int width : new int[]{900, 1280, 1920, 900, 1280}) {
                parent.setSize(width, 800);
                Dimension before = content.getSize();
                assertDoesNotThrow(parent::getPreferredSize);
                assertDoesNotThrow(parent::getMinimumSize);
                assertEquals(before, content.getSize(), "size queries must not resize children");
                assertDoesNotThrow(parent::doLayout);
            }
        });
    }

    @Test void studentSixEqualCardsFitLargeWindowWithoutScrolling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ModulePage page = new ModulePage("student", new StudentView(
                    new ClientContext(new CampusClient("127.0.0.1", 1, 100))), () -> {});
            for (int[] size : new int[][]{{1536, 850}, {1920, 1080}, {1280, 800}}) {
                render(page, size[0], size[1]);
                JPanel grid = (JPanel) named(page, "student.profileCards");
                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, grid);
                assertNotNull(viewport);
                assertTrue(viewport.getView().getPreferredSize().height <= viewport.getHeight(),
                        "student content must fit " + size[0] + "x" + size[1] + ": "
                                + viewport.getView().getPreferredSize().height + " > " + viewport.getHeight());
                Dimension first = grid.getComponent(0).getSize();
                for (Component card : grid.getComponents()) { assertEquals(first, card.getSize()); }
            }
        });
    }

    @Test void liveResizeEventsReflowStudentCardsWithoutManualInvalidation() throws Exception {
        java.util.concurrent.atomic.AtomicReference<ModulePage> page = new java.util.concurrent.atomic.AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> page.set(new ModulePage("student", new StudentView(
                new ClientContext(new CampusClient("127.0.0.1", 1, 100))), () -> {})));
        for (int width : new int[]{1920, 900, 1280, 900, 1920}) {
            SwingUtilities.invokeAndWait(() -> page.get().setSize(width, 800));
            for (int pass = 0; pass < 5; pass++) {
                SwingUtilities.invokeAndWait(() -> layoutTree(page.get()));
            }
            SwingUtilities.invokeAndWait(() -> {
                JPanel grid = (JPanel) named(page.get(), "student.profileCards");
                assertEquals(width == 900 ? 2 : 3, ((GridLayout) grid.getLayout()).getColumns());
                assertEquals(grid.getPreferredSize().height, grid.getHeight(),
                        "card height must update after width changes");
            });
        }
    }

    @Test void serviceTilesScrollInsteadOfClippingAtShortWindowHeights() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel tiles = ResponsiveLayout.grid(3, 250, 18);
            for (int i = 0; i < 7; i++) {
                JButton button = new JButton("校园服务 " + i);
                button.setPreferredSize(new Dimension(210, 104)); tiles.add(button);
            }
            JScrollPane scroll = ResponsiveLayout.verticalScroll(ResponsiveLayout.compact(tiles, 1280));
            render(scroll, 832, 240);
            assertTrue(scroll.getVerticalScrollBar().isVisible());
            assertEquals(3, ((GridLayout) tiles.getLayout()).getColumns());
            assertTrue(tiles.getHeight() >= 348);
            render(scroll, 1280, 600);
            assertFalse(scroll.getVerticalScrollBar().isVisible());
            assertEquals(348, tiles.getHeight());
        });
    }

    @Test void contentIsCenteredAndFormsKeepTheirNaturalHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel body = new JPanel(); body.setPreferredSize(new Dimension(600, 240));
            JPanel page = ResponsiveLayout.constrain(body);
            render(page, 1920, 1080);
            assertEquals(1280, body.getWidth()); assertEquals(320, body.getX());
            render(page, 900, 560); assertEquals(900, body.getWidth()); assertEquals(0, body.getX());
            JPanel compact = ResponsiveLayout.compact(body, 720);
            render(compact, 1920, 1080);
            assertEquals(720, body.getWidth()); assertEquals(240, body.getHeight());
            assertEquals(0, body.getY());
        });
    }

    @Test void cardColumnsAdaptInBothResizeDirectionsWithoutStretching() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel grid = ResponsiveLayout.grid(3, 340, 14);
            for (int i = 0; i < 6; i++) {
                JPanel card = new JPanel(); card.setPreferredSize(new Dimension(340, 200)); grid.add(card);
            }
            for (int width : new int[]{850, 1230, 850, 1230}) {
                grid.setSize(width, grid.getPreferredSize().height); grid.doLayout();
                int columns = ((GridLayout) grid.getLayout()).getColumns();
                assertEquals(width == 850 ? 2 : 3, columns);
                assertEquals(width == 850 ? 628 : 414, grid.getPreferredSize().height);
                assertEquals(grid.getPreferredSize().height, grid.getMaximumSize().height);
            }
        });
    }

    @Test void wrappedToolbarsReserveEveryRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel toolbar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
            for (int i = 0; i < 9; i++) {
                JButton button = new JButton("操作 " + i);
                button.setPreferredSize(new Dimension(150, 38)); toolbar.add(button);
            }
            for (int width : new int[]{900, 1280, 400, 1280}) {
                toolbar.setSize(width, 1);
                toolbar.setSize(width, toolbar.getPreferredSize().height); toolbar.doLayout();
                for (Component child : toolbar.getComponents()) {
                    assertTrue(child.getX() >= 0 && child.getY() >= 0);
                    assertTrue(child.getX() + child.getWidth() <= width);
                    assertTrue(child.getY() + child.getHeight() <= toolbar.getHeight());
                }
            }
        });
    }

    @Test void moduleScreensRenderAtSmallMediumAndFullScreenSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1, 100));
            List<JComponent> views = List.of(new UserAdminPanel(context), new StudentView(context),
                    new LibraryPanel(context), new SelfServicePanel(context), new ShopView(context),
                    new HospitalView(context), new CardClientModule().createView(context));
            String[] names = {"users", "student", "library", "terminal", "shop", "hospital", "card"};
            Path folder = Path.of("target", "ui-review");
            try { Files.createDirectories(folder); } catch (Exception e) { throw new AssertionError(e); }
            for (int i = 0; i < views.size(); i++) {
                JComponent view = views.get(i);
                ModulePage page = new ModulePage(names[i], view, () -> {});
                for (int[] size : new int[][]{{900, 560}, {1280, 800}, {1920, 1080}, {900, 560}}) {
                    BufferedImage image = render(page, size[0], size[1]);
                    assertTrue(view.getWidth() > 700 && view.getWidth() <= 1280, names[i]);
                    if ("student".equals(names[i])) {
                        JPanel grid = (JPanel) named(view, "student.profileCards");
                        assertEquals(size[0] == 900 ? 2 : 3, ((GridLayout) grid.getLayout()).getColumns());
                        assertEquals(6, grid.getComponentCount());
                        Dimension first = grid.getComponent(0).getSize();
                        for (Component card : grid.getComponents()) {
                            assertEquals(first, card.getSize(), "all six student cards must be equal-size");
                        }
                    }
                    if ("terminal".equals(names[i])) {
                        JPanel form = (JPanel) named(view, "library.selfService.card");
                        assertTrue(form.getWidth() <= 720);
                        assertTrue(form.getHeight() < 450, "terminal must not grow to screen height");
                    }
                    assertToolbars(view);
                    try { ImageIO.write(image, "png", folder.resolve(names[i] + "-" + size[0] + ".png").toFile()); }
                    catch (Exception e) { throw new AssertionError(e); }
                }
            }
        });
    }

    @Test void adminCourseTabsAndLibraryModeChooserFitSupportedWidths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { Files.createDirectories(Path.of("target", "ui-review")); }
            catch (Exception e) { throw new AssertionError(e); }
            ClientContext context = new ClientContext(new CampusClient("127.0.0.1", 1, 100));
            try {
                var field = ClientContext.class.getDeclaredField("session"); field.setAccessible(true);
                ((ClientSession) field.get(context)).set(new SessionInfo(
                        "layout-test", "U-ADMIN-001", "20260000", "超级管理员", Role.SUPER_ADMIN));
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            JComponent course = new CourseClientModule().createView(context);
            ModulePage page = new ModulePage("course", course, () -> {});
            JTabbedPane tabs = tabs(course);
            assertNotNull(tabs);
            for (int[] size : new int[][]{{900, 560}, {1280, 800}, {1920, 1080}, {900, 560}}) {
                for (int tab = 0; tab < tabs.getTabCount(); tab++) {
                    tabs.setSelectedIndex(tab);
                    BufferedImage image = render(page, size[0], size[1]);
                    assertToolbars(course);
                    try { ImageIO.write(image, "png", Path.of("target", "ui-review",
                            "course-" + tab + "-" + size[0] + ".png").toFile()); }
                    catch (Exception e) { throw new AssertionError(e); }
                }
                JComponent library = new LibraryModePanel(context);
                BufferedImage image = render(new ModulePage("library", library, () -> {}), size[0], size[1]);
                JPanel choices = (JPanel) named(library, "library.modeChoices");
                assertEquals(1, ((GridLayout) choices.getLayout()).getColumns());
                assertEquals(3, choices.getComponentCount());
                int previousBottom = -1;
                for (Component card : choices.getComponents()) {
                    assertTrue(card.getWidth() >= 250);
                    assertTrue(card.getHeight() >= 130);
                    assertTrue(card.getWidth() > card.getHeight(), "library entrances must be horizontal cards");
                    assertEquals(0, card.getX());
                    assertTrue(card.getY() > previousBottom);
                    previousBottom = card.getY() + card.getHeight();
                }
                try { ImageIO.write(image, "png", Path.of("target", "ui-review",
                        "library-modes-" + size[0] + ".png").toFile()); }
                catch (Exception e) { throw new AssertionError(e); }
            }
        });
    }

    private static JTabbedPane tabs(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTabbedPane pane) { return pane; }
            if (child instanceof Container container) {
                JTabbedPane result = tabs(container); if (result != null) { return result; }
            }
        }
        return null;
    }

    private static void assertToolbars(Container parent) {
        if (parent.getLayout() instanceof WrapLayout && parent.getWidth() > 0 && parent.isVisible()) {
            for (Component child : parent.getComponents()) {
                if (child.isVisible() && child instanceof JButton && child.getWidth() <= parent.getWidth()) {
                    assertTrue(child.getY() + child.getHeight() <= parent.getHeight(),
                            ((JButton) child).getText() + " clipped in " + parent.getSize());
                }
            }
        }
        for (Component child : parent.getComponents()) {
            if (child instanceof Container container && child.isVisible()) { assertToolbars(container); }
        }
    }

    private static Component named(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) { return child; }
            if (child instanceof Container container) {
                Component result = named(container, name); if (result != null) { return result; }
            }
        }
        return null;
    }

    private static BufferedImage render(JComponent root, int width, int height) {
        root.setSize(width, height);
        for (int pass = 0; pass < 5; pass++) { invalidateTree(root); layoutTree(root); }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); root.paint(graphics); graphics.dispose();
        return image;
    }
    private static void invalidateTree(Container root) {
        root.invalidate();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container && child.isVisible()) { invalidateTree(container); }
        }
    }
    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container && child.isVisible()) { layoutTree(container); }
        }
    }
}
