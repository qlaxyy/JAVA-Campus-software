package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;

/** Shared responsive layout primitives for hospital desktop pages. */
final class HospitalResponsiveLayout {

    private static final int MAXIMUM_CONTENT_WIDTH = 1200;

    private HospitalResponsiveLayout() {
    }

    static JPanel grid(int maximumColumns, int minimumColumnWidth, int horizontalGap,
            int verticalGap) {
        return new ResponsiveGridPanel(
                maximumColumns, minimumColumnWidth, horizontalGap, verticalGap);
    }

    static JPanel constrainWidth(Component content) {
        return new WidthTrackingPanel(content);
    }

    static JScrollPane verticalScroll(Component content) {
        Component view = content instanceof Scrollable scrollable
                && scrollable.getScrollableTracksViewportWidth()
                ? content
                : new WidthTrackingPanel(content);
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private static final class ResponsiveGridPanel extends JPanel {

        private final int maximumColumns;
        private final int minimumColumnWidth;
        private final int horizontalGap;
        private int columns = 1;

        private ResponsiveGridPanel(
                int maximumColumns,
                int minimumColumnWidth,
                int horizontalGap,
                int verticalGap) {
            super(new GridLayout(0, 1, horizontalGap, verticalGap));
            if (maximumColumns < 1 || minimumColumnWidth < 1) {
                throw new IllegalArgumentException(
                        "responsive grid dimensions must be positive");
            }
            this.maximumColumns = maximumColumns;
            this.minimumColumnWidth = minimumColumnWidth;
            this.horizontalGap = horizontalGap;
        }

        @Override
        public void doLayout() {
            updateColumns(availableWidth());
            super.doLayout();
        }

        @Override
        public Dimension getPreferredSize() {
            updateColumns(availableWidth());
            return super.getPreferredSize();
        }

        private int availableWidth() {
            if (getWidth() > 0) {
                return getWidth();
            }
            return getParent() == null ? 0 : getParent().getWidth();
        }

        private void updateColumns(int availableWidth) {
            if (availableWidth <= 0) {
                return;
            }
            int desired = Math.max(1, Math.min(
                    maximumColumns,
                    (availableWidth + horizontalGap)
                            / (minimumColumnWidth + horizontalGap)));
            if (desired == columns) {
                return;
            }
            columns = desired;
            ((GridLayout) getLayout()).setColumns(columns);
            revalidate();
        }
    }

    private static final class WidthTrackingPanel extends JPanel implements Scrollable {

        private int horizontalGutter;

        private WidthTrackingPanel(Component content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        @Override
        public void doLayout() {
            int desiredGutter = Math.max(0, (getWidth() - MAXIMUM_CONTENT_WIDTH) / 2);
            if (desiredGutter != horizontalGutter) {
                horizontalGutter = desiredGutter;
                setBorder(BorderFactory.createEmptyBorder(
                        0, horizontalGutter, 0, horizontalGutter));
            }
            super.doLayout();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? 18 : 12;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL
                    ? Math.max(18, visibleRect.height - 36)
                    : Math.max(12, visibleRect.width - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
