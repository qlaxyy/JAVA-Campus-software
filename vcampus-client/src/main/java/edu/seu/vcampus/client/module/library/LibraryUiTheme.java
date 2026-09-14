package edu.seu.vcampus.client.module.library;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/** Visual language shared by every screen in the library module. */
final class LibraryUiTheme {

    static final Color PAGE = new Color(244, 248, 247);
    static final Color SURFACE = Color.WHITE;
    static final Color PRIMARY = new Color(15, 118, 110);
    static final Color PRIMARY_DARK = new Color(17, 94, 89);
    static final Color PRIMARY_LIGHT = new Color(226, 242, 238);
    static final Color TEXT = new Color(30, 41, 59);
    static final Color MUTED = new Color(100, 116, 139);
    static final Color BORDER = new Color(211, 225, 222);
    static final Color DANGER = new Color(185, 28, 28);
    static final Color DANGER_LIGHT = new Color(254, 242, 242);
    static final Color WARNING = new Color(161, 98, 7);
    static final Color DISABLED_BACKGROUND = new Color(229, 235, 234);
    static final Color DISABLED_TEXT = new Color(93, 108, 116);

    private LibraryUiTheme() {
    }

    static void installPage(JPanel page) {
        page.setBackground(PAGE);
        page.setForeground(TEXT);
    }

    static JPanel createPageHeader(String eyebrow, String title, String description) {
        JPanel header = new JPanel(new BorderLayout(0, 5));
        header.setOpaque(false);
        JLabel eyebrowLabel = new JLabel(eyebrow.toUpperCase());
        eyebrowLabel.setForeground(PRIMARY);
        eyebrowLabel.setFont(eyebrowLabel.getFont().deriveFont(Font.BOLD, 11F));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24F));

        JPanel titles = new JPanel(new BorderLayout(0, 3));
        titles.setOpaque(false);
        titles.add(titleLabel, BorderLayout.NORTH);
        header.add(eyebrowLabel, BorderLayout.NORTH);
        header.add(titles, BorderLayout.CENTER);
        return header;
    }

    static Border cardBorder(int vertical, int horizontal) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    static void styleCard(JComponent component) {
        component.setOpaque(true);
        component.setBackground(SURFACE);
        component.setBorder(cardBorder(16, 16));
    }

    static void stylePrimaryButton(AbstractButton button) {
        styleButton(button, PRIMARY, Color.WHITE, PRIMARY);
    }

    static void styleSecondaryButton(AbstractButton button) {
        styleButton(button, SURFACE, PRIMARY_DARK, new Color(126, 188, 181));
    }

    static void styleDangerButton(AbstractButton button) {
        styleButton(button, DANGER_LIGHT, DANGER, new Color(238, 176, 176));
    }

    static void styleButton(AbstractButton button, Color background,
            Color foreground, Color border) {
        button.setUI(new StableButtonUI());
        button.putClientProperty("library.enabledBackground", background);
        button.putClientProperty("library.enabledForeground", foreground);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addPropertyChangeListener("enabled", event -> applyButtonState(button));
        applyButtonState(button);
    }

    static void makeLargeButton(AbstractButton button) {
        button.setFont(button.getFont().deriveFont(Font.BOLD, 16F));
        button.setPreferredSize(new Dimension(0, 48));
    }

    static void styleTextField(JTextField field) {
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 38));
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(PRIMARY_DARK);
        field.setFont(field.getFont().deriveFont(13F));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(SURFACE);
        comboBox.setForeground(TEXT);
        comboBox.setFont(comboBox.getFont().deriveFont(13F));
        // Measure after applying the font, leaving space for the taller arrow button.
        comboBox.setPreferredSize(null);
        int contentWidth = 0;
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            contentWidth = Math.max(contentWidth, comboBox.getFontMetrics(comboBox.getFont())
                    .stringWidth(String.valueOf(comboBox.getItemAt(index))));
        }
        comboBox.setPreferredSize(new Dimension(
                Math.max(comboBox.getPreferredSize().width, contentWidth + 60), 38));
    }

    static void styleTextArea(JTextArea area) {
        area.setBackground(new Color(249, 251, 250));
        area.setForeground(TEXT);
        area.setFont(area.getFont().deriveFont(13F));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    static JScrollPane tableScrollPane(JTable table) {
        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    static void styleTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT);
        table.setGridColor(new Color(231, 237, 235));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setRowHeight(34);
        table.setDefaultRenderer(Object.class, new LibraryCellRenderer());
        table.getTableHeader().setBackground(new Color(237, 244, 242));
        table.getTableHeader().setForeground(PRIMARY_DARK);
        table.getTableHeader().setFont(
                table.getTableHeader().getFont().deriveFont(Font.BOLD, 13F));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
    }

    static void styleTabbedPane(JTabbedPane tabs) {
        tabs.setBackground(PAGE);
        tabs.setForeground(TEXT);
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 16F));
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    }

    static void setColumnWidths(JTable table, int... preferredWidths) {
        for (int index = 0; index < preferredWidths.length
                && index < table.getColumnModel().getColumnCount(); index++) {
            TableColumn column = table.getColumnModel().getColumn(index);
            int width = preferredWidths[index];
            column.setPreferredWidth(width);
            column.setMinWidth(Math.min(width, 70));
            if (width <= 100) {
                column.setMaxWidth(width + 20);
            }
        }
    }

    static void styleStatusLabel(JLabel label) {
        label.setOpaque(true);
        label.setBackground(PRIMARY_LIGHT);
        label.setForeground(PRIMARY_DARK);
        label.setFont(label.getFont().deriveFont(13F));
        label.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
    }

    static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(13F));
        return label;
    }

    static JLabel createMetricValue() {
        JLabel label = new JLabel("0", SwingConstants.LEFT);
        label.setForeground(PRIMARY_DARK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22F));
        return label;
    }

    private static void applyButtonState(AbstractButton button) {
        Color enabledBackground = (Color) button.getClientProperty("library.enabledBackground");
        Color enabledForeground = (Color) button.getClientProperty("library.enabledForeground");
        button.setBackground(button.isEnabled() ? enabledBackground : DISABLED_BACKGROUND);
        button.setForeground(button.isEnabled() ? enabledForeground : DISABLED_TEXT);
        button.setCursor(button.isEnabled()
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
    }

    /** Paints disabled text explicitly instead of trusting platform look-and-feel defaults. */
    private static final class StableButtonUI extends BasicButtonUI {
        @Override
        protected void paintText(Graphics graphics, JComponent component,
                Rectangle textRect, String text) {
            AbstractButton button = (AbstractButton) component;
            ButtonModel model = button.getModel();
            graphics.setColor(model.isEnabled() ? button.getForeground() : DISABLED_TEXT);
            FontMetrics metrics = graphics.getFontMetrics();
            int shift = getTextShiftOffset();
            BasicGraphicsUtils.drawStringUnderlineCharAt(
                    graphics,
                    text,
                    button.getDisplayedMnemonicIndex(),
                    textRect.x + shift,
                    textRect.y + metrics.getAscent() + shift);
        }
    }

    private static final class LibraryCellRenderer extends DefaultTableCellRenderer {
        private LibraryCellRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setToolTipText(value == null ? null : value.toString());
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
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
            String text = value == null ? "" : value.toString();
            if (text.contains("逾期") || text.contains("注销")
                    || text.contains("不可借")
                    || text.contains("停止") || text.contains("失败")) {
                return DANGER;
            }
            if (text.contains("可借") || text.contains("开放")
                    || text.contains("成功") || text.contains("待取")) {
                return PRIMARY_DARK;
            }
            if (text.contains("排队") || text.contains("待上架")) {
                return WARNING;
            }
            return TEXT;
        }
    }
}
