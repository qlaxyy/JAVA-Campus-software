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

    static final String UI_FONT = "Microsoft YaHei UI";
    static final String DATA_FONT = "Consolas";

    static final Color PRIMARY = new Color(11, 98, 91);
    static final Color PRIMARY_DARK = new Color(23, 59, 55);
    static final Color PRIMARY_LIGHT = new Color(226, 242, 238);
    static final Color NAVIGATION = new Color(12, 78, 73);
    static final Color NAVIGATION_HOVER = new Color(21, 101, 94);
    static final Color NAVIGATION_MUTED = new Color(179, 215, 209);
    static final Color BACKGROUND = new Color(245, 248, 247);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(215, 227, 224);
    static final Color TEXT = new Color(23, 59, 55);
    static final Color MUTED = new Color(91, 113, 109);
    static final Color SUCCESS = new Color(45, 123, 89);
    static final Color SUCCESS_LIGHT = new Color(230, 244, 235);
    static final Color WARNING = new Color(184, 106, 34);
    static final Color WARNING_LIGHT = new Color(252, 241, 227);
    static final Color DISABLED = new Color(235, 240, 238);

    private HospitalTheme() {
    }

    static Font uiFont(int style, float size) {
        return new Font(UI_FONT, style, Math.round(size));
    }

    static Font dataFont(int style, float size) {
        return new Font(DATA_FONT, style, Math.round(size));
    }

    static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        applyPrimaryStyle(button);
        return button;
    }

    static JButton quietButton(String text) {
        JButton button = new JButton(text);
        applyQuietStyle(button);
        return button;
    }

    static void applyPrimaryStyle(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(true);
        button.setFont(uiFont(Font.BOLD, 14F));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
    }

    static void applyQuietStyle(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(SURFACE);
        button.setForeground(PRIMARY_DARK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(true);
        button.setFont(uiFont(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
    }

    static void applyDisabledStyle(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(DISABLED);
        button.setForeground(MUTED);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(true);
        button.setFont(uiFont(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
    }

    /** A code-native rounded surface; no bitmap asset is needed. */
    static class SurfacePanel extends JPanel {

        private final Color fill;
        private final int radius;
        private final Color stroke;

        SurfacePanel() {
            this(SURFACE, 14, null);
        }

        SurfacePanel(Color fill, int radius) {
            this(fill, radius, null);
        }

        SurfacePanel(Color fill, int radius, Color stroke) {
            this.fill = fill;
            this.radius = radius;
            this.stroke = stroke;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(fill);
            copy.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (stroke != null) {
                copy.setColor(stroke);
                copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
