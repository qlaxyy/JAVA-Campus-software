package edu.seu.vcampus.client.module.hospital;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;

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

    private static void layout(JPanel panel, int width) {
        panel.setSize(width, 500);
        panel.doLayout();
    }
}
