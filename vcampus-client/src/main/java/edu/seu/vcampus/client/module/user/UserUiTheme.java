package edu.seu.vcampus.client.module.user;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/** Shared visual treatment for the user and teacher administration screens. */
final class UserUiTheme {

    static final Color PAGE = new Color(244, 248, 247);
    static final Color SURFACE = Color.WHITE;
    static final Color PRIMARY = new Color(15, 118, 110);
    static final Color PRIMARY_DARK = new Color(17, 94, 89);
    static final Color PRIMARY_LIGHT = new Color(226, 242, 238);
    static final Color TEXT = new Color(30, 41, 59);
    static final Color MUTED = new Color(100, 116, 139);
    static final Color BORDER = new Color(215, 227, 224);
    static final Color SUCCESS = new Color(22, 101, 52);
    static final Color DANGER = new Color(185, 28, 28);
    private UserUiTheme() {
    }

    static JPanel createSectionHeader(String title, JLabel detail) {
        JPanel panel = new JPanel(new BorderLayout(12, 2));
        panel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20F));
        panel.add(titleLabel, BorderLayout.WEST);

        detail.setForeground(MUTED);
        detail.setFont(detail.getFont().deriveFont(13F));
        detail.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(detail, BorderLayout.EAST);
        return panel;
    }

    static JPanel createStatusBar(JLabel label) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        label.setForeground(PRIMARY_DARK);
        label.setFont(label.getFont().deriveFont(13F));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    static JScrollPane createTableScrollPane(JTable table) {
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER, 1, true));
        scrollPane.getViewport().setBackground(SURFACE);
        return scrollPane;
    }

    static void styleTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT);
        table.setGridColor(new Color(231, 237, 235));
        table.setShowVerticalLines(false);
        table.setRowHeight(36);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new AdministrationCellRenderer());
        table.getTableHeader().setBackground(new Color(237, 244, 242));
        table.getTableHeader().setForeground(PRIMARY_DARK);
        table.getTableHeader().setFont(
                table.getTableHeader().getFont().deriveFont(Font.BOLD, 13F));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
    }

    static void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY, Color.WHITE, PRIMARY);
    }

    static void styleSecondaryButton(JButton button) {
        styleButton(button, SURFACE, PRIMARY_DARK, new Color(139, 199, 193));
    }

    static void styleDangerButton(JButton button) {
        styleButton(button, SURFACE, DANGER, new Color(239, 170, 170));
    }

    static void styleButton(
            JButton button,
            Color background,
            Color foreground,
            Color border) {
        button.setUI(new BasicButtonUI());
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setOpaque(true);
    }

    private static final class AdministrationCellRenderer
            extends DefaultTableCellRenderer {

        private AdministrationCellRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            if (selected) {
                setBackground(PRIMARY_LIGHT);
                setForeground(TEXT);
            } else {
                setBackground(row % 2 == 0 ? SURFACE : new Color(249, 251, 250));
                setForeground(statusColor(value));
            }
            return this;
        }

        private static Color statusColor(Object value) {
            if ("启用".equals(value) || "成功".equals(value)) {
                return SUCCESS;
            }
            if ("禁用".equals(value) || "失败".equals(value)) {
                return DANGER;
            }
            return TEXT;
        }
    }
}
