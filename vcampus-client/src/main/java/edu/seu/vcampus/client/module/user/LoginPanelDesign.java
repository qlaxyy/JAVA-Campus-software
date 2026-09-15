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

    private static final Color NAVY = new Color(24, 40, 59);
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
        JPanel form = createLoginCard(usernameField, passwordField, loginButton, statusLabel);
        JPanel brand = createBrandPanel();
        RoundedPanel content = new RoundedPanel(Color.WHITE, 32) {
            @Override public void doLayout() {
                int formWidth = Math.min(480, getWidth() / 2);
                int brandWidth = getWidth() - formWidth;
                brand.setBounds(0, 0, brandWidth, getHeight());
                form.setBounds(brandWidth, 0, formWidth, getHeight());
            }
            @Override protected void paintChildren(Graphics graphics) {
                Graphics2D clipped = (Graphics2D) graphics.create();
                clipped.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 32, 32));
                super.paintChildren(clipped);
                clipped.dispose();
            }
        };
        content.setName("login.composition");
        content.setLayout(null);
        content.add(form);
        content.add(brand);
        GradientPanel background = new GradientPanel() {
            @Override public void doLayout() {
                int width = Math.min(1100, Math.max(0, getWidth() - 48));
                int height = Math.min(600, Math.max(0, getHeight() - 48));
                content.setBounds((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            }
        };
        background.setLayout(null);
        background.setPreferredSize(new Dimension(1150, 650));
        background.add(content);
        return background;
    }

    private static JPanel createBrandPanel() {
        JPanel panel = new CampusPanel();
        panel.setLayout(null);
        panel.setName("login.brand");
        JLabel title = new JLabel("JAVA VIRTUAL CAMPUS", SwingConstants.CENTER);
        title.setName("login.brandTitle");
        title.setForeground(new Color(216, 193, 143));
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        JLabel heading = new JLabel("虚拟校园系统", SwingConstants.CENTER);
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("Microsoft YaHei", Font.BOLD, 30));
        JLabel caption = new JLabel("学习 · 生活 · 服务", SwingConstants.CENTER);
        caption.setForeground(new Color(193, 221, 218));
        caption.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        panel.add(heading); panel.add(title); panel.add(caption);
        panel.setLayout(new java.awt.LayoutManager() {
            public void addLayoutComponent(String name, java.awt.Component component) { }
            public void removeLayoutComponent(java.awt.Component component) { }
            public Dimension preferredLayoutSize(java.awt.Container parent) { return new Dimension(520, 600); }
            public Dimension minimumLayoutSize(java.awt.Container parent) { return new Dimension(320, 450); }
            public void layoutContainer(java.awt.Container parent) {
                int width = parent.getWidth(), height = parent.getHeight();
                heading.setBounds(20, height / 7, width - 40, 46);
                title.setBounds(20, height / 7 + 52, width - 40, 25);
                caption.setBounds(20, height - 76, width - 40, 28);
            }
        });
        return panel;
    }

    private static JPanel createLoginCard(
            JTextField usernameField,
            JPasswordField passwordField,
            JButton loginButton,
            JLabel statusLabel) {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 24);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(24, 34, 24, 34));
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
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 27));
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 6, 0);
        card.add(title, constraints);

        JLabel subtitle = new JLabel("使用一卡通号进入虚拟校园系统");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 20, 0);
        card.add(subtitle, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 0, 7, 0);
        card.add(createFieldLabel("一卡通号"), constraints);

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
        showPassword.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        char echoChar = passwordField.getEchoChar();
        showPassword.addActionListener(event -> passwordField.setEchoChar(
                showPassword.isSelected() ? (char) 0 : echoChar));
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 11, 0);
        card.add(showPassword, constraints);

        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        statusLabel.addPropertyChangeListener("text", event -> statusLabel.setToolTipText(statusLabel.getText()));

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
        card.add(statusLabel, constraints);
        return card;
    }

    private static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        return label;
    }

    private static void styleTextField(JTextField field) {
        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
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
        button.setUI(new edu.seu.vcampus.client.view.RoundedButtonUI());
        button.setPreferredSize(new Dimension(0, 44));
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setOpaque(false);
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
            "超级管理员  20260000",
            "学籍管理员  20260001",
            "选课管理员  20260002",
            "图书馆管理员  20260003",
            "商店管理员  20260004",
            "医院管理员  20260005",
            "学生账号  20260006",
            "教师账号  20260021",
            "医生账号  20260030"
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

    /** Lightweight campus illustration; no external asset or network request is needed. */
    private static final class CampusPanel extends JPanel {
        private CampusPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), getHeight(), PRIMARY));
            copy.fillRect(0, 0, getWidth(), getHeight());
            copy.setColor(new Color(255, 255, 255, 13));
            copy.fillOval(getWidth() / 2, -100, getWidth(), getWidth());
            copy.fillOval(-getWidth() / 2, getHeight() / 2, getWidth(), getWidth());
            copy.translate(getWidth() / 2.0, getHeight() * 0.54);
            double scale = 0.9 * Math.min(getWidth() / 500.0, getHeight() / 500.0);
            copy.scale(scale, scale);
            copy.setColor(new Color(255, 255, 255, 10));
            copy.fillOval(-160, -160, 320, 320);
            copy.setStroke(new java.awt.BasicStroke(2F, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            copy.setColor(new Color(160, 199, 194, 95));
            copy.drawRoundRect(-190, -20, 70, 110, 4, 4);
            copy.drawRoundRect(120, -20, 70, 110, 4, 4);
            for (int y = 0; y < 75; y += 25) {
                copy.drawLine(-175, y, -137, y);
                copy.drawLine(137, y, 175, y);
            }
            copy.setColor(new Color(216, 193, 143));
            java.awt.geom.Path2D roof = new java.awt.geom.Path2D.Double();
            roof.moveTo(-120, -28); roof.lineTo(0, -94); roof.lineTo(120, -28); roof.closePath();
            copy.draw(roof);
            copy.drawRect(-108, -18, 216, 12);
            for (int x : new int[]{-88, -44, 44, 88}) {
                copy.drawRoundRect(x - 6, -5, 12, 92, 3, 3);
            }
            copy.drawRect(-22, 28, 44, 60);
            copy.drawLine(-120, 90, 120, 90);
            copy.drawLine(-132, 100, 132, 100);
            copy.setColor(new Color(173, 210, 203, 115));
            copy.drawLine(-208, 111, 208, 111);
            copy.drawLine(-75, 120, -125, 150);
            copy.drawLine(75, 120, 125, 150);
            copy.drawLine(-155, 161, 155, 161);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class GradientPanel extends JPanel {
        private GradientPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setPaint(new GradientPaint(
                    0, 0, new Color(232, 240, 241),
                    getWidth(), getHeight(), new Color(213, 230, 226)));
            copy.fillRect(0, 0, getWidth(), getHeight());
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(new Color(255, 255, 255, 70));
            copy.fillOval(-getWidth() / 5, -getHeight() / 2, getWidth(), getWidth());
            if (getComponentCount() > 0) {
                java.awt.Rectangle bounds = getComponent(0).getBounds();
                for (int spread = 10; spread >= 1; spread--) {
                    copy.setColor(new Color(24, 40, 59, 3));
                    copy.fillRoundRect(bounds.x - spread, bounds.y - spread + 6,
                            bounds.width + spread * 2, bounds.height + spread * 2, 40, 40);
                }
            }
            copy.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedPanel extends JPanel {
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
