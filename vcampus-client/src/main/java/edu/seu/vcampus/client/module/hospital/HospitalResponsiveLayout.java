package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
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

    static JPanel adaptiveRow(
            Component main,
            Component aside,
            int stackBelowWidth,
            int gap) {
        return new AdaptiveRowPanel(main, aside, stackBelowWidth, gap);
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

    /** Plain Unicode text that reflows with its parent instead of using fixed-width HTML. */
    static JTextArea wrappingText(String text, Font font, Color color) {
        JTextArea area = new WrappingTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(font);
        area.setForeground(color);
        return area;
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

    /** Keeps a compact action rail beside content, then stacks it when space is tight. */
    private static final class AdaptiveRowPanel extends JPanel {

        private final Component main;
        private final Component aside;
        private final int stackBelowWidth;
        private final int gap;
        private boolean stacked;

        private AdaptiveRowPanel(
                Component main,
                Component aside,
                int stackBelowWidth,
                int gap) {
            super(null);
            if (stackBelowWidth < 1 || gap < 0) {
                throw new IllegalArgumentException(
                        "responsive row dimensions are invalid");
            }
            this.main = main;
            this.aside = aside;
            this.stackBelowWidth = stackBelowWidth;
            this.gap = gap;
            setOpaque(false);
            add(main);
            add(aside);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int width = Math.max(0, getWidth() - insets.left - insets.right);
            int height = Math.max(0, getHeight() - insets.top - insets.bottom);
            boolean nextStacked = width < stackBelowWidth;
            if (nextStacked != stacked) {
                stacked = nextStacked;
                if (getParent() != null) {
                    getParent().revalidate();
                }
            }
            if (stacked) {
                int mainHeight = Math.min(height,
                        main.getPreferredSize().height);
                int asideHeight = Math.min(
                        Math.max(0, height - mainHeight - gap),
                        aside.getPreferredSize().height);
                main.setBounds(insets.left, insets.top, width, mainHeight);
                aside.setBounds(
                        insets.left,
                        insets.top + mainHeight + gap,
                        width,
                        asideHeight);
                return;
            }
            int asideWidth = Math.min(
                    aside.getPreferredSize().width,
                    Math.max(0, width / 2));
            int asideHeight = Math.min(height, aside.getPreferredSize().height);
            main.setBounds(
                    insets.left,
                    insets.top,
                    Math.max(0, width - asideWidth - gap),
                    height);
            aside.setBounds(
                    insets.left + width - asideWidth,
                    insets.top + Math.max(0, (height - asideHeight) / 2),
                    asideWidth,
                    asideHeight);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension mainSize = main.getPreferredSize();
            Dimension asideSize = aside.getPreferredSize();
            int availableWidth = getWidth() > 0 ? getWidth()
                    : mainSize.width + gap + asideSize.width;
            Insets insets = getInsets();
            if (availableWidth < stackBelowWidth) {
                return new Dimension(
                        Math.max(mainSize.width, asideSize.width)
                                + insets.left + insets.right,
                        mainSize.height + gap + asideSize.height
                                + insets.top + insets.bottom);
            }
            return new Dimension(
                    mainSize.width + gap + asideSize.width
                            + insets.left + insets.right,
                    Math.max(mainSize.height, asideSize.height)
                            + insets.top + insets.bottom);
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

    private static final class WrappingTextArea extends JTextArea {

        private WrappingTextArea(String text) {
            super(text);
        }

        @Override
        public Dimension getPreferredSize() {
            int availableWidth = getWidth();
            if (availableWidth <= 0 && getParent() != null) {
                availableWidth = getParent().getWidth();
            }
            if (availableWidth <= 0) {
                return super.getPreferredSize();
            }
            setSize(availableWidth, Short.MAX_VALUE);
            Dimension preferred = super.getPreferredSize();
            return new Dimension(availableWidth, preferred.height);
        }
    }
}
