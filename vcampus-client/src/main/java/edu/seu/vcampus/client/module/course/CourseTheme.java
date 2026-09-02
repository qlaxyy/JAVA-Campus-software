package edu.seu.vcampus.client.module.course;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 选课模块统一视觉样式。
 *
 * 配色与项目 MainFrame / Hospital 模块保持一致。
 */
final class CourseTheme {

    /*
     * =========================
     * 主色
     * =========================
     */
    static final Color NAVY =
        new Color(18, 59, 74);

    static final Color PRIMARY =
        new Color(15, 118, 110);

    static final Color PRIMARY_DARK =
        new Color(17, 94, 89);

    static final Color PRIMARY_LIGHT =
        new Color(220, 252, 247);

    /*
     * =========================
     * 页面颜色
     * =========================
     */
    static final Color BACKGROUND =
        new Color(244, 248, 247);

    static final Color SURFACE =
        Color.WHITE;

    static final Color BORDER =
        new Color(218, 226, 225);

    /*
     * =========================
     * 文字颜色
     * =========================
     */
    static final Color TEXT =
        new Color(30, 41, 59);

    static final Color MUTED =
        new Color(100, 116, 139);

    /*
     * =========================
     * 状态颜色
     * =========================
     */
    static final Color SUCCESS =
        new Color(21, 128, 61);

    static final Color SUCCESS_LIGHT =
        new Color(220, 252, 231);

    static final Color WARNING =
        new Color(180, 83, 9);

    static final Color WARNING_LIGHT =
        new Color(255, 247, 237);

    static final Color DANGER =
        new Color(185, 28, 28);

    static final Color DISABLED =
        new Color(235, 240, 239);

    private CourseTheme() {
    }

    /*
     * =========================
     * 标题
     * =========================
     */
    static JLabel title(
        String text) {

        JLabel label =
            new JLabel(text);

        label.setForeground(TEXT);

        label.setFont(
            label.getFont()
                .deriveFont(
                    Font.BOLD,
                    26F));

        return label;
    }

    static JLabel subtitle(
        String text) {

        JLabel label =
            new JLabel(text);

        label.setForeground(MUTED);

        label.setFont(
            label.getFont()
                .deriveFont(
                    14F));

        return label;
    }

    /*
     * =========================
     * 主按钮
     * =========================
     */
    static JButton primaryButton(
        String text) {

        JButton button =
            new JButton(text);

        button.setUI(
            new BasicButtonUI());

        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);

        button.setFont(
            button.getFont()
                .deriveFont(
                    Font.BOLD,
                    14F));

        button.setBorder(
            BorderFactory.createEmptyBorder(
                10,
                18,
                10,
                18));

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        return button;
    }

    /*
     * =========================
     * 次要按钮
     * =========================
     */
    static JButton quietButton(
        String text) {

        JButton button =
            new JButton(text);

        button.setUI(
            new BasicButtonUI());

        button.setBackground(SURFACE);
        button.setForeground(PRIMARY_DARK);

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);

        button.setFont(
            button.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    BORDER),
                BorderFactory.createEmptyBorder(
                    8,
                    14,
                    8,
                    14)));

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        return button;
    }
    /**
     * 给已经创建好的 JButton 套主按钮样式。
     *
     * 用于现有页面，避免为了改 UI
     * 重新创建按钮并影响事件逻辑。
     */
    static void stylePrimaryButton(
        JButton button) {

        button.setUI(
            new BasicButtonUI());

        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);

        button.setFont(
            button.getFont()
                .deriveFont(
                    Font.BOLD,
                    14F));

        button.setBorder(
            BorderFactory.createEmptyBorder(
                10,
                18,
                10,
                18));

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));
    }

    /**
     * 给已经创建好的 JButton
     * 套次要按钮样式。
     */
    static void styleQuietButton(
        JButton button) {

        button.setUI(
            new BasicButtonUI());

        button.setBackground(SURFACE);
        button.setForeground(PRIMARY_DARK);

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);

        button.setFont(
            button.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    BORDER),
                BorderFactory.createEmptyBorder(
                    8,
                    14,
                    8,
                    14)));

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));
    }
    /**
     * 危险操作按钮。
     *
     * 例如退课、删除等。
     */
    static void styleDangerButton(
        JButton button) {

        button.setUI(
            new BasicButtonUI());

        button.setBackground(
            SURFACE);

        button.setForeground(
            DANGER);

        button.setOpaque(
            true);

        button.setContentAreaFilled(
            true);

        button.setFocusPainted(
            false);

        button.setFont(
            button.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    DANGER),
                BorderFactory.createEmptyBorder(
                    8,
                    14,
                    8,
                    14)));

        button.setCursor(
            Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));
    }
    /*
     * =========================
     * 白色圆角卡片
     * =========================
     */
    static class SurfacePanel
        extends JPanel {

        private final Color fill;
        private final int radius;

        SurfacePanel() {

            this(
                SURFACE,
                18);
        }

        SurfacePanel(
            Color fill,
            int radius) {

            this.fill = fill;
            this.radius = radius;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(
            Graphics graphics) {

            Graphics2D copy =
                (Graphics2D)
                    graphics.create();

            copy.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            copy.setColor(fill);

            copy.fillRoundRect(
                0,
                0,
                getWidth(),
                getHeight(),
                radius,
                radius);

            copy.dispose();

            super.paintComponent(
                graphics);
        }
    }
}
