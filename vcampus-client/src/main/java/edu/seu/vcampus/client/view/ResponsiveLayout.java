package edu.seu.vcampus.client.view;

import javax.swing.*;
import java.awt.*;

/** Shared desktop sizing: readable content widths, compact forms and width-tracking scroll areas. */
public final class ResponsiveLayout {
    public static final int CONTENT_WIDTH = 1280;

    private ResponsiveLayout() { }

    public static JPanel constrain(Component content) {
        return new ContentPanel(content, CONTENT_WIDTH, true);
    }

    public static JPanel compact(Component content, int maximumWidth) {
        return new ContentPanel(content, maximumWidth, false);
    }

    public static JPanel grid(int maximumColumns, int minimumColumnWidth, int gap) {
        return new CardGrid(maximumColumns, minimumColumnWidth, gap, false);
    }

    /** Equal-size cards across every row, while preserving responsive column counts. */
    public static JPanel equalGrid(int maximumColumns, int minimumColumnWidth, int gap) {
        return new CardGrid(maximumColumns, minimumColumnWidth, gap, true);
    }

    public static JPanel verticalContent(LayoutManager layout) {
        return new VerticalPanel(layout);
    }

    /** Three full-width entrance slots; unused slots retain their share of the page. */
    public static JPanel entranceRows(int gap) {
        return new VerticalPanel(new GridLayout(3, 1, gap, gap)) {
            @Override public boolean getScrollableTracksViewportHeight() {
                return getParent() instanceof JViewport viewport
                        && viewport.getHeight() >= getPreferredSize().height;
            }
        };
    }

    public static JScrollPane verticalScroll(Component content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }

    static void prepare(Component component) {
        if (component instanceof JPanel panel && panel.getLayout() != null
                && panel.getLayout().getClass() == FlowLayout.class) {
            FlowLayout flow = (FlowLayout) panel.getLayout();
            WrapLayout wrap = new WrapLayout(flow.getAlignment(), flow.getHgap(), flow.getVgap());
            wrap.setAlignOnBaseline(flow.getAlignOnBaseline());
            panel.setLayout(wrap);
        }
    }

    private static class VerticalPanel extends JPanel implements Scrollable {
        VerticalPanel(LayoutManager layout) { super(layout); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(20, (orientation == SwingConstants.VERTICAL ? r.height : r.width) - 20);
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class ContentPanel extends VerticalPanel {
        private final Component content;
        private final int maximumWidth;
        private final boolean fillHeight;
        ContentPanel(Component content, int maximumWidth, boolean fillHeight) {
            super(null);
            if (maximumWidth < 1) { throw new IllegalArgumentException("maximumWidth must be positive"); }
            this.content = content;
            this.maximumWidth = maximumWidth;
            this.fillHeight = fillHeight;
            add(content);
        }
        @Override public void doLayout() {
            Insets insets = getInsets();
            int available = Math.max(0, getWidth() - insets.left - insets.right);
            int width = Math.min(maximumWidth, available);
            int height = Math.max(0, getHeight() - insets.top - insets.bottom);
            content.setSize(width, height);
            if (!fillHeight) { height = Math.min(height, content.getPreferredSize().height); }
            content.setBounds(insets.left + (available - width) / 2, insets.top, width, height);
        }
        @Override public Dimension getPreferredSize() {
            // Measuring must not resize children: that invalidates an enclosing BoxLayout
            // while its size-request arrays are still being populated.
            Dimension size = content.getPreferredSize();
            Insets insets = getInsets();
            return new Dimension(Math.min(maximumWidth, size.width) + insets.left + insets.right,
                    size.height + insets.top + insets.bottom);
        }
        @Override public boolean getScrollableTracksViewportHeight() { return fillHeight; }
    }

    private static final class CardGrid extends JPanel {
        private final int maximumColumns;
        private final int minimumWidth;
        private final int gap;
        private final boolean equalHeight;
        CardGrid(int maximumColumns, int minimumWidth, int gap, boolean equalHeight) {
            super(new GridLayout(0, 1, gap, gap));
            if (maximumColumns < 1 || minimumWidth < 1 || gap < 0) {
                throw new IllegalArgumentException("invalid card grid dimensions");
            }
            this.maximumColumns = maximumColumns;
            this.minimumWidth = minimumWidth;
            this.gap = gap;
            this.equalHeight = equalHeight;
            setOpaque(false);
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override public void componentResized(java.awt.event.ComponentEvent event) {
                    invalidate();
                    if (getParent() != null) { getParent().revalidate(); }
                }
            });
        }
        private void updateColumns(int width) {
            if (width <= 0) { return; }
            Insets insets = getInsets();
            int columns = Math.max(1, Math.min(maximumColumns,
                    (width - insets.left - insets.right + gap) / (minimumWidth + gap)));
            GridLayout layout = (GridLayout) getLayout();
            if (columns != layout.getColumns()) {
                layout.setColumns(columns);
            }
        }
        @Override public Dimension getPreferredSize() {
            Container parent = getParent();
            int width = parent == null || parent.getWidth() <= 0 ? getWidth() : parent.getWidth()
                    - parent.getInsets().left - parent.getInsets().right;
            updateColumns(width);
            int columns = ((GridLayout) getLayout()).getColumns();
            int rowHeight = 0, height = 0, cellWidth = 0, count = 0, maximumHeight = 0;
            for (Component child : getComponents()) {
                if (!child.isVisible()) { continue; }
                Dimension size = child.getPreferredSize();
                cellWidth = Math.max(cellWidth, size.width);
                rowHeight = Math.max(rowHeight, size.height);
                maximumHeight = Math.max(maximumHeight, size.height);
                if (++count % columns == 0) { height += rowHeight + gap; rowHeight = 0; }
            }
            if (count % columns != 0) { height += rowHeight + gap; }
            if (equalHeight && count > 0) {
                int rows = (count + columns - 1) / columns;
                height = rows * (maximumHeight + gap);
            }
            Insets insets = getInsets();
            int usedColumns = Math.min(columns, count);
            return new Dimension(insets.left + insets.right + usedColumns * cellWidth
                    + Math.max(0, usedColumns - 1) * gap,
                    insets.top + insets.bottom + Math.max(0, height - gap));
        }
        @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
        @Override public void doLayout() {
            int previous = ((GridLayout) getLayout()).getColumns();
            updateColumns(getWidth());
            Insets insets = getInsets();
            int columns = ((GridLayout) getLayout()).getColumns();
            java.util.List<Component> visible = new java.util.ArrayList<>();
            for (Component child : getComponents()) { if (child.isVisible()) { visible.add(child); } }
            columns = Math.min(columns, Math.max(1, visible.size()));
            int available = Math.max(0, getWidth() - insets.left - insets.right - (columns - 1) * gap);
            int cellWidth = available / columns;
            int left = insets.left + (available - cellWidth * columns) / 2;
            int maximumHeight = 0;
            for (Component child : visible) {
                maximumHeight = Math.max(maximumHeight, child.getPreferredSize().height);
            }
            int y = insets.top;
            for (int start = 0; start < visible.size(); start += columns) {
                int rowHeight = 0;
                for (int j = start; j < Math.min(start + columns, visible.size()); j++) {
                    rowHeight = Math.max(rowHeight, visible.get(j).getPreferredSize().height);
                }
                if (equalHeight) { rowHeight = maximumHeight; }
                for (int j = start; j < Math.min(start + columns, visible.size()); j++) {
                    int column = j - start;
                    int x = left + column * (cellWidth + gap);
                    visible.get(j).setBounds(x, y, cellWidth, rowHeight);
                }
                y += rowHeight + gap;
            }
            if (previous != ((GridLayout) getLayout()).getColumns() && getParent() != null) {
                getParent().revalidate();
            }
        }
    }
}
