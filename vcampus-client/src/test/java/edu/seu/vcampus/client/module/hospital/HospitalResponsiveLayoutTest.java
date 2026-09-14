package edu.seu.vcampus.client.module.hospital;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalResponsiveLayoutTest {

    @Test
    void gridChangesFromThreeColumnsToTwoAndOne() throws Exception {
        JPanel[] grid = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            grid[0] = HospitalResponsiveLayout.grid(3, 240, 14, 14);
            for (int index = 0; index < 4; index++) {
                JPanel card = new JPanel();
                card.setPreferredSize(new Dimension(240, 100));
                grid[0].add(card);
            }

            layout(grid[0], 800);
            assertEquals(grid[0].getComponent(0).getY(),
                    grid[0].getComponent(2).getY());
            assertTrue(grid[0].getComponent(3).getY()
                    > grid[0].getComponent(0).getY());

            layout(grid[0], 520);
            assertEquals(grid[0].getComponent(0).getY(),
                    grid[0].getComponent(1).getY());
            assertTrue(grid[0].getComponent(2).getY()
                    > grid[0].getComponent(0).getY());

            layout(grid[0], 470);
            assertTrue(grid[0].getComponent(1).getY()
                    > grid[0].getComponent(0).getY());
        });
    }

    @Test
    void verticalScrollTracksViewportWidthAndNeverCutsOffHorizontally()
            throws Exception {
        JScrollPane[] scroll = new JScrollPane[1];
        SwingUtilities.invokeAndWait(() -> {
            JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(900, 800));
            scroll[0] = HospitalResponsiveLayout.verticalScroll(content);
            scroll[0].setSize(500, 300);
            scroll[0].doLayout();
            scroll[0].getViewport().doLayout();
        });

        Component view = scroll[0].getViewport().getView();
        assertTrue(view instanceof Scrollable);
        assertTrue(((Scrollable) view).getScrollableTracksViewportWidth());
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                scroll[0].getHorizontalScrollBarPolicy());
    }

    @Test
    void wideViewportCentersContentInsteadOfStretchingCardsIndefinitely()
            throws Exception {
        JScrollPane[] scroll = new JScrollPane[1];
        JPanel content = new JPanel();
        SwingUtilities.invokeAndWait(() -> {
            content.setPreferredSize(new Dimension(900, 300));
            scroll[0] = HospitalResponsiveLayout.verticalScroll(content);
            scroll[0].setSize(1_600, 500);
            scroll[0].doLayout();
            scroll[0].getViewport().doLayout();
            scroll[0].getViewport().getView().doLayout();
        });

        assertEquals(1_200, content.getWidth());
        assertTrue(content.getX() > 0);
    }

    @Test
    void plainUnicodeCopyWrapsToItsCurrentContainerWidth() throws Exception {
        JTextArea[] copy = new JTextArea[1];
        JPanel host = new JPanel(new java.awt.BorderLayout());
        SwingUtilities.invokeAndWait(() -> {
            copy[0] = HospitalResponsiveLayout.wrappingText(
                    "一段需要随窗口宽度自动换行的中文提示，不依赖 HTML 固定宽度。",
                    new Font("Microsoft YaHei UI", Font.PLAIN, 14), Color.BLACK);
            host.add(copy[0], java.awt.BorderLayout.CENTER);
            host.setSize(260, 120);
            host.doLayout();
        });

        assertTrue(copy[0].getLineWrap());
        assertEquals(260, copy[0].getWidth());
        assertTrue(copy[0].getText().contains("中文提示"));
    }

    @Test
    void administrationWorkspacesStackTheirColumnsInANarrowViewport()
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AdminSchedulePanel schedules = new AdminSchedulePanel(null, () -> { });
            JPanel scheduleBody = namedPanel(
                    schedules, "adminScheduleResponsiveBody");
            layout(scheduleBody, 900);
            assertEquals(scheduleBody.getComponent(0).getY(),
                    scheduleBody.getComponent(1).getY());
            layout(scheduleBody, 700);
            assertTrue(scheduleBody.getComponent(1).getY()
                    > scheduleBody.getComponent(0).getY());

            AdminDepartmentPanel departments = new AdminDepartmentPanel(null, () -> { });
            JPanel departmentBody = namedPanel(
                    departments, "adminDepartmentResponsiveBody");
            layout(departmentBody, 700);
            assertTrue(departmentBody.getComponent(1).getY()
                    > departmentBody.getComponent(0).getY());

            AdminDoctorPanel doctors = new AdminDoctorPanel(null, () -> { });
            JPanel doctorForms = namedPanel(doctors, "adminDoctorResponsiveForms");
            layout(doctorForms, 900);
            assertEquals(doctorForms.getComponent(0).getY(),
                    doctorForms.getComponent(1).getY());
            layout(doctorForms, 700);
            assertTrue(doctorForms.getComponent(1).getY()
                    > doctorForms.getComponent(0).getY());
        });
    }

    @Test
    void actionRailMovesBelowCardContentWhenTheCardNarrows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel main = new JPanel();
            main.setPreferredSize(new Dimension(420, 80));
            JPanel aside = new JPanel();
            aside.setPreferredSize(new Dimension(130, 90));
            JPanel row = HospitalResponsiveLayout.adaptiveRow(main, aside, 600, 16);

            layout(row, 700);
            assertTrue(aside.getX() > main.getX());
            assertEquals(aside.getPreferredSize().height, aside.getHeight());

            layout(row, 520);
            assertTrue(aside.getY() > main.getY());
            assertEquals(main.getX(), aside.getX());
            assertEquals(main.getWidth(), aside.getWidth());
            assertEquals(aside.getPreferredSize().height, aside.getHeight());
        });
    }

    @Test
    void departmentNavigationStaysCompactAndStacksOnSmallWindows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel navigation = new JPanel();
            navigation.setPreferredSize(new Dimension(270, 320));
            JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(700, 500));
            JPanel split = HospitalResponsiveLayout.split(
                    navigation, content, 270, 760, 18);

            layout(split, 1_100);
            assertEquals(270, navigation.getWidth());
            assertTrue(content.getX() > navigation.getX());

            layout(split, 620);
            assertEquals(620, navigation.getWidth());
            assertTrue(navigation.getHeight() <= 360);
            assertTrue(content.getY() > navigation.getY());
        });
    }

    @Test
    void sharedPageHeaderWrapsItsSubtitleInsteadOfClippingOnNarrowWindows()
            throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel header = HospitalPageHeader.create(
                    "智能导诊",
                    "请描述自己的不适；症状严重或迅速加重时，请及时寻求线下医护帮助。",
                    "医院首页", () -> { }, null);
            header.setSize(420, 140);
            for (int pass = 0; pass < 4; pass++) {
                layoutTree(header);
            }
            JTextArea subtitle = namedTextArea(header, "hospitalPageSubtitle");
            assertTrue(subtitle.getHeight()
                    >= subtitle.getFontMetrics(subtitle.getFont()).getHeight() * 2);
            assertTrue(subtitle.getX() + subtitle.getWidth() <= header.getWidth());
        });
    }

    @Test
    void backNavigationLooksAndBehavesLikeAButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel header = HospitalPageHeader.create(
                    "费用清单", "查看费用并完成支付", "医院首页", () -> { }, null);
            JButton back = namedButton(header, "hospitalPageBackButton");

            assertTrue(back.isContentAreaFilled());
            assertTrue(back.isOpaque());
            assertTrue(back.getBorder().getBorderInsets(back).left >= 8);
            assertEquals(Cursor.HAND_CURSOR, back.getCursor().getType());
        });
    }

    @Test
    void patientBillWorkspaceStaysCenteredAtFullScreenWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PatientBillsPanel panel = new PatientBillsPanel(null, () -> { });
            panel.setSize(2_048, 900);
            for (int pass = 0; pass < 4; pass++) {
                layoutTree(panel);
            }
            JPanel content = namedPanel(panel, "patientBillContent");
            assertTrue(content.getWidth() <= 1_200);
            assertTrue(content.getX() >= 0);
        });
    }

    private static void layout(JPanel panel, int width) {
        panel.setSize(width, 500);
        panel.doLayout();
    }

    private static JPanel namedPanel(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel panel && name.equals(panel.getName())) {
                return panel;
            }
            if (child instanceof Container container) {
                JPanel found = namedPanelOrNull(container, name);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("component not found: " + name);
    }

    private static JPanel namedPanelOrNull(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel panel && name.equals(panel.getName())) {
                return panel;
            }
            if (child instanceof Container container) {
                JPanel found = namedPanelOrNull(container, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JTextArea namedTextArea(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextArea area && name.equals(area.getName())) {
                return area;
            }
            if (child instanceof Container container) {
                try {
                    return namedTextArea(container, name);
                } catch (AssertionError ignored) {
                    // Continue looking in sibling containers.
                }
            }
        }
        throw new AssertionError("component not found: " + name);
    }

    private static JButton namedButton(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && name.equals(button.getName())) {
                return button;
            }
            if (child instanceof Container container) {
                try {
                    return namedButton(container, name);
                } catch (AssertionError ignored) {
                    // Continue looking in sibling containers.
                }
            }
        }
        throw new AssertionError("component not found: " + name);
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
    }
}
