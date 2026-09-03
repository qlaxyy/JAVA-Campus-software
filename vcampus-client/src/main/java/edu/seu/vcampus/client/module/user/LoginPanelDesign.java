package edu.seu.vcampus.client.module.user;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.im.InputContext;
import java.util.Locale;

/** Visual layout for the shared login page, separated from login behavior. */
final class LoginPanelDesign {

    private static final Color NAVY = new Color(18, 59, 74);
    private static final Color PRIMARY = new Color(15, 118, 110);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(203, 213, 225);
    private static final Color LIGHT_MINT = new Color(236, 253, 245);

    private LoginPanelDesign() {
    }

    static JPanel create(
            JTextField usernameField,
            JPasswordField passwordField,
            JButton loginButton,
            JLabel statusLabel) {
        GradientPanel background = new GradientPanel();
        background.setLayout(new GridBagLayout());
        background.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;

        constraints.gridx = 0;
        constraints.weightx = 0.8;
        constraints.insets = new Insets(0, 0, 0, 28);
        content.add(createBrandPanel(), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        content.add(createLoginCard(
                usernameField, passwordField, loginButton, statusLabel), constraints);

        background.add(content, new GridBagConstraints());
        return background;
    }

    private static JPanel createBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(335, 450));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("JAVA VIRTUAL CAMPUS");
        title.setName("login.brandTitle");
        title.setForeground(new Color(167, 243, 208));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13F));
        panel.add(title, constraints);
        return panel;
    }

    private static JPanel createLoginCard(
            JTextField usernameField,
            JPasswordField passwordField,
            JButton loginButton,
            JLabel statusLabel) {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 24);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 34, 24, 34));
        card.setPreferredSize(new Dimension(445, 500));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("欢迎登录");
        title.setName("login.title");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 27F));
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 6, 0);
        card.add(title, constraints);

        JLabel subtitle = new JLabel("使用统一校园账号进入虚拟校园系统");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(14F));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 20, 0);
        card.add(subtitle, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 7, 0);
        card.add(createFieldLabel("账号"), constraints);

        styleTextField(usernameField);
        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 14, 0);
        card.add(usernameField, constraints);

        constraints.gridy = 4;
        constraints.insets = new Insets(0, 0, 7, 0);
        card.add(createFieldLabel("密码"), constraints);

        styleTextField(passwordField);
        constraints.gridy = 5;
        constraints.insets = new Insets(0, 0, 4, 0);
        card.add(passwordField, constraints);

        JCheckBox showPassword = new JCheckBox("显示密码");
        showPassword.setName("login.showPassword");
        showPassword.setOpaque(false);
        showPassword.setForeground(MUTED);
        showPassword.setFocusPainted(false);
        char echoChar = passwordField.getEchoChar();
        showPassword.addActionListener(event -> passwordField.setEchoChar(
                showPassword.isSelected() ? (char) 0 : echoChar));
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 11, 0);
        card.add(showPassword, constraints);

        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(13F));
        constraints.gridx = 1;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.EAST;
        card.add(statusLabel, constraints);

        stylePrimaryButton(loginButton);
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 0, 16, 0);
        card.add(loginButton, constraints);

        constraints.gridy = 8;
        constraints.insets = new Insets(0, 0, 0, 0);
        card.add(createDemoAccountsPanel(), constraints);
        return card;
    }

    private static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14F));
        return label;
    }

    private static void styleTextField(JTextField field) {
        field.setFont(field.getFont().deriveFont(15F));
        field.setPreferredSize(new Dimension(0, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 11, 8, 11)));
        preferLatinInput(field);
    }

    private static void preferLatinInput(JTextField field) {
        field.enableInputMethods(false);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                InputContext inputContext = field.getInputContext();
                if (inputContext != null) {
                    inputContext.endComposition();
                    inputContext.selectInputMethod(Locale.ENGLISH);
                }
            }
        });
    }

    private static void stylePrimaryButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setPreferredSize(new Dimension(0, 44));
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15F));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setOpaque(true);
    }

    private static JPanel createDemoAccountsPanel() {
        RoundedPanel panel = new RoundedPanel(LIGHT_MINT, 16);
        panel.setName("login.testAccounts");
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(11, 13, 10, 13));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        JLabel title = new JLabel("开发阶段测试账号");
        title.setForeground(NAVY);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13F));
        panel.add(title, constraints);

        JLabel password = new JLabel("以下账号统一密码：123456", SwingConstants.RIGHT);
        password.setForeground(MUTED);
        password.setFont(password.getFont().deriveFont(12F));
        constraints.gridx = 1;
        panel.add(password, constraints);

        JPanel accounts = new JPanel(new GridLayout(0, 2, 12, 3));
        accounts.setOpaque(false);
        String[] labels = {
            "普通账号  student001",
            "医生演示  teacher001",
            "超级管理员  admin",
            "学籍管理员  studentadmin",
            "选课管理员  courseadmin",
            "图书馆管理员  libraryadmin",
            "商店管理员  shopadmin",
            "医院管理员  hospitaladmin"
        };
        for (String text : labels) {
            JLabel label = new JLabel(text);
            label.setForeground(TEXT);
            label.setFont(label.getFont().deriveFont(11F));
            accounts.add(label);
        }

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(8, 0, 0, 0);
        panel.add(accounts, constraints);
        return panel;
    }

    private static final class GradientPanel extends JPanel {
        private GradientPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setPaint(new GradientPaint(
                    0, 0, NAVY,
                    getWidth(), getHeight(), new Color(15, 118, 110)));
            copy.fillRect(0, 0, getWidth(), getHeight());
            copy.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final Color fill;
        private final int radius;

        private RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(fill);
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
