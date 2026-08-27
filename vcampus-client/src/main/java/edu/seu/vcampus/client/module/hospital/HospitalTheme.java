package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Shared visual rules for the hospital module. */
final class HospitalTheme {

    static final Color PRIMARY = new Color(15, 118, 110);
    static final Color PRIMARY_DARK = new Color(17, 94, 89);
    static final Color PRIMARY_LIGHT = new Color(220, 252, 247);
    static final Color BACKGROUND = new Color(244, 248, 247);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(215, 227, 224);
    static final Color TEXT = new Color(25, 50, 47);
    static final Color MUTED = new Color(91, 116, 111);
    static final Color SUCCESS = new Color(21, 128, 61);
    static final Color SUCCESS_LIGHT = new Color(220, 252, 231);
    static final Color WARNING = new Color(180, 83, 9);
    static final Color WARNING_LIGHT = new Color(255, 247, 237);
    static final Color DISABLED = new Color(235, 240, 239);

    private HospitalTheme() {
    }

    static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14F));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return button;
    }

    static JButton quietButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SURFACE);
        button.setForeground(PRIMARY_DARK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        return button;
    }

    /** A code-native rounded surface; no bitmap asset is needed. */
    static class SurfacePanel extends JPanel {

        private final Color fill;
        private final int radius;

        SurfacePanel() {
            this(SURFACE, 18);
        }

        SurfacePanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(fill);
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
