package edu.seu.vcampus.client.view;

import java.awt.*;

/** FlowLayout which reserves the height of wrapped rows instead of clipping them. */
public final class WrapLayout extends FlowLayout {
    private int lastWidth = -1;
    public WrapLayout(int alignment, int horizontalGap, int verticalGap) {
        super(alignment, horizontalGap, verticalGap);
    }
    @Override public Dimension preferredLayoutSize(Container target) { return measure(target, false); }
    @Override public Dimension minimumLayoutSize(Container target) { return measure(target, true); }
    @Override public void layoutContainer(Container target) {
        super.layoutContainer(target);
        if (lastWidth != target.getWidth()) {
            lastWidth = target.getWidth();
            if (target.getParent() instanceof javax.swing.JComponent parent) {
                parent.revalidate();
            }
        }
    }

    private Dimension measure(Container target, boolean minimum) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            Container parent = target.getParent();
            if (parent != null && parent.getWidth() > 0) {
                width = parent.getWidth() - parent.getInsets().left - parent.getInsets().right;
                // A side rail must wrap within its own width, not its entire parent.
                if (target.getWidth() > 0) { width = Math.min(width, target.getWidth()); }
            }
            Insets insets = target.getInsets();
            int available = width <= 0 ? Integer.MAX_VALUE
                    : Math.max(1, width - insets.left - insets.right - getHgap() * 2);
            int rowWidth = 0, rowHeight = 0, usedWidth = 0, usedHeight = 0;
            for (Component child : target.getComponents()) {
                if (!child.isVisible()) { continue; }
                Dimension size = minimum ? child.getMinimumSize() : child.getPreferredSize();
                int next = rowWidth == 0 ? size.width : rowWidth + getHgap() + size.width;
                if (rowWidth > 0 && next > available) {
                    usedWidth = Math.max(usedWidth, rowWidth);
                    usedHeight += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth > 0) { rowWidth += getHgap(); }
                rowWidth += size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            usedWidth = Math.max(usedWidth, rowWidth);
            usedHeight += rowHeight;
            return new Dimension(usedWidth + insets.left + insets.right + getHgap() * 2,
                    usedHeight + insets.top + insets.bottom + getVgap() * 2);
        }
    }
}
