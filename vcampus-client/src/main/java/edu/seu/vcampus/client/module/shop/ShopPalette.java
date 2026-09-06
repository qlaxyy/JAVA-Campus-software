package edu.seu.vcampus.client.module.shop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Shop colors and controls aligned with the campus teal workspace theme.
 */
final class ShopPalette {

    static final Color PRIMARY = new Color(15, 118, 110);
    static final Color PRIMARY_DARK = new Color(17, 94, 89);
    static final Color PRIMARY_LIGHT = new Color(220, 252, 247);
    static final Color NAVY = new Color(18, 59, 74);
    static final Color PAGE = new Color(244, 248, 247);
    static final Color CARD = Color.WHITE;
    static final Color LINE = new Color(215, 227, 224);
    static final Color TEXT = new Color(25, 50, 47);
    static final Color MUTED = new Color(91, 116, 111);
    static final Color HEADER_TEXT = Color.WHITE;

    private ShopPalette() {
    }

    static JButton accentButton(String text) {
        JButton button = new JButton(text);
        paintButton(button, PRIMARY, Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13F));
        return button;
    }

    static JButton quietButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(CARD);
        button.setForeground(PRIMARY_DARK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        return button;
    }

    static void paintButton(JButton button, Color background, Color foreground) {
        button.setUI(new BasicButtonUI());
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(LINE);
        table.setBackground(CARD);
        table.setForeground(TEXT);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setBackground(PAGE);
        table.getTableHeader().setForeground(MUTED);
    }

    static Font titleFont() {
        return new Font("SansSerif", Font.BOLD, 26);
    }

    static Font bodyFont() {
        return new Font("SansSerif", Font.PLAIN, 13);
    }

    static Font priceFont() {
        return new Font("SansSerif", Font.BOLD, 16);
    }

    static Color categoryTone(String categoryName) {
        if (categoryName == null) {
            return PRIMARY_LIGHT;
        }
        return switch (categoryName) {
            case "文具" -> new Color(204, 251, 241);
            case "日用品", "日常用品" -> new Color(207, 250, 254);
            case "食品" -> new Color(209, 250, 229);
            default -> PRIMARY_LIGHT;
        };
    }

    /** Rounded white/teal surface used by the shop header strip. */
    static final class SurfacePanel extends JPanel {

        private final Color fill;
        private final int radius;

        SurfacePanel() {
            this(CARD, 18);
        }

        SurfacePanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(fill);
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
