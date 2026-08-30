package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.application.ModuleAccessPolicy;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.ClientModules;
import edu.seu.vcampus.client.module.user.LoginPanel;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * First shared window used to verify that Swing and the socket link work.
 */
public final class MainFrame extends JFrame {

    private static final String LOGIN_CARD = "login";
    private static final String WORKSPACE_CARD = "workspace";
    private static final String MODULE_HOME_CARD = "module-home";
    private static final Color NAVY = new Color(18, 59, 74);
    private static final Color PRIMARY = new Color(15, 118, 110);
    private static final Color SURFACE = new Color(244, 248, 247);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(218, 226, 225);

    private final CampusClient client;
    private final ClientContext context;
    private final CardLayout applicationLayout = new CardLayout();
    private final JPanel applicationPanel = new JPanel(applicationLayout);
    private final JLabel statusLabel = new JLabel("服务器尚未检测", SwingConstants.CENTER);
    private final JLabel sessionLabel = new JLabel();
    private final JButton pingButton = new JButton("测试服务器连接");
    private final JButton logoutButton = new JButton("退出登录");
    private LoginPanel loginPanel;
    private JPanel workspacePanel;

    /**
     * Creates the first shared client window.
     *
     * @param client network client used by the connectivity button
     */
    public MainFrame(CampusClient client) {
        super("虚拟校园系统");
        this.client = client;
        this.context = new ClientContext(client);
        initializeWindow();
    }

    private void initializeWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(createApplicationIcons());
        setMinimumSize(new Dimension(900, 560));
        setLocationByPlatform(true);

        loginPanel = new LoginPanel(context, this::showWorkspace);
        logoutButton.addActionListener(event -> logout());
        pingButton.addActionListener(event -> pingServer());
        applicationPanel.add(loginPanel, LOGIN_CARD);
        setContentPane(applicationPanel);
        applicationLayout.show(applicationPanel, LOGIN_CARD);

        pack();
        setLocationRelativeTo(null);
    }

    private static List<Image> createApplicationIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64}) {
            BufferedImage image = new BufferedImage(
                    size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new java.awt.Color(15, 118, 110));
            graphics.fillRoundRect(0, 0, size, size, size / 3, size / 3);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD,
                    Math.max(11, Math.round(size * 0.58F))));
            String letter = "V";
            java.awt.FontMetrics metrics = graphics.getFontMetrics();
            int x = (size - metrics.stringWidth(letter)) / 2;
            int y = (size - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString(letter, x, y);
            graphics.dispose();
            icons.add(image);
        }
        return icons;
    }

    private JPanel createWorkspace(SessionInfo session) {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(SURFACE);

        JPanel headerPanel = createWorkspaceHeader();
        JPanel statusPanel = createStatusBar();

        List<ClientModule> modules = ClientModules.all().stream()
                .filter(module -> ModuleAccessPolicy.isVisible(session, module.id()))
                .toList();
        CardLayout moduleLayout = new CardLayout();
        JPanel modulePanel = new JPanel(moduleLayout);
        modulePanel.add(createModuleHome(modules, modulePanel, moduleLayout), MODULE_HOME_CARD);
        for (ClientModule module : modules) {
            modulePanel.add(createModulePage(module, modulePanel, moduleLayout), module.id());
        }
        moduleLayout.show(modulePanel, MODULE_HOME_CARD);

        workspace.add(headerPanel, BorderLayout.NORTH);
        workspace.add(modulePanel, BorderLayout.CENTER);
        workspace.add(statusPanel, BorderLayout.SOUTH);
        return workspace;
    }

    private JPanel createWorkspaceHeader() {
        GradientHeader header = new GradientHeader();
        header.setLayout(new BorderLayout(24, 0));
        header.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        header.setPreferredSize(new Dimension(0, 82));

        JLabel logo = new JLabel("V", SwingConstants.CENTER);
        logo.setOpaque(true);
        logo.setBackground(new Color(52, 101, 114));
        logo.setForeground(Color.WHITE);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 22F));
        logo.setPreferredSize(new Dimension(46, 46));

        JPanel brand = new JPanel(new BorderLayout(14, 0));
        brand.setOpaque(false);
        brand.add(logo, BorderLayout.WEST);

        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 2));
        titles.setOpaque(false);
        JLabel title = new JLabel("虚拟校园系统");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20F));
        JLabel subtitle = new JLabel("VIRTUAL CAMPUS");
        subtitle.setForeground(new Color(167, 243, 208));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.BOLD, 11F));
        titles.add(title);
        titles.add(subtitle);
        brand.add(titles, BorderLayout.CENTER);

        JPanel account = new JPanel(new BorderLayout(18, 0));
        account.setOpaque(false);
        sessionLabel.setForeground(Color.WHITE);
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(Font.BOLD, 14F));
        styleHeaderButton(logoutButton);
        account.add(sessionLabel, BorderLayout.CENTER);
        account.add(logoutButton, BorderLayout.EAST);

        header.add(brand, BorderLayout.WEST);
        header.add(account, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout(16, 0));
        status.setBackground(Color.WHITE);
        status.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)));

        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(13F));
        styleOutlineButton(pingButton);
        status.add(statusLabel, BorderLayout.CENTER);
        status.add(pingButton, BorderLayout.EAST);
        return status;
    }

    private JPanel createModuleHome(
            List<ClientModule> modules,
            JPanel modulePanel,
            CardLayout moduleLayout) {
        JPanel home = new JPanel(new BorderLayout());
        home.setBackground(SURFACE);
        home.setBorder(BorderFactory.createEmptyBorder(28, 34, 30, 34));

        JPanel introduction = new JPanel(new GridLayout(0, 1, 0, 5));
        introduction.setOpaque(false);
        JLabel heading = new JLabel("校园服务");
        heading.setForeground(TEXT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 26F));
        JLabel hint = new JLabel("选择一个服务，开始使用虚拟校园系统");
        hint.setForeground(MUTED);
        hint.setFont(hint.getFont().deriveFont(14F));
        introduction.add(heading);
        introduction.add(hint);
        home.add(introduction, BorderLayout.NORTH);

        JPanel tiles = new JPanel(new GridLayout(0, 3, 18, 18));
        tiles.setOpaque(false);

        for (ClientModule module : modules) {
            JButton button = new ModuleTileButton(module.displayName());
            button.setName("module.home." + module.id());
            button.addActionListener(event -> moduleLayout.show(modulePanel, module.id()));
            tiles.add(button);
        }

        JPanel tileArea = new JPanel(new GridBagLayout());
        tileArea.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(24, 0, 0, 0);
        tileArea.add(tiles, constraints);
        home.add(tileArea, BorderLayout.CENTER);
        return home;
    }

    private JPanel createModulePage(
            ClientModule module,
            JPanel modulePanel,
            CardLayout moduleLayout) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(SURFACE);
        JButton backButton = new JButton("←  返回校园服务");
        backButton.setName("module.back." + module.id());
        backButton.addActionListener(event -> moduleLayout.show(modulePanel, MODULE_HOME_CARD));
        styleOutlineButton(backButton);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 30, 12, 30)));
        toolbar.add(backButton, BorderLayout.WEST);
        JLabel moduleTitle = new JLabel(module.displayName(), SwingConstants.RIGHT);
        moduleTitle.setForeground(TEXT);
        moduleTitle.setFont(moduleTitle.getFont().deriveFont(Font.BOLD, 18F));
        toolbar.add(moduleTitle, BorderLayout.EAST);

        page.add(toolbar, BorderLayout.NORTH);
        page.add(module.createView(context), BorderLayout.CENTER);
        return page;
    }

    private static void styleHeaderButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(121, 191, 184)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static void styleOutlineButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(Color.WHITE);
        button.setForeground(PRIMARY);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(153, 205, 198)),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void pingServer() {
        pingButton.setEnabled(false);
        statusLabel.setText("正在连接服务器……");

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return client.ping();
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        statusLabel.setText("连接成功：" + response.getData());
                    } else {
                        statusLabel.setText("服务器拒绝请求：" + response.getCode());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("连接操作已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("连接失败，请确认服务器已经启动");
                } finally {
                    pingButton.setEnabled(true);
                    if (workspacePanel != null) {
                        workspacePanel.revalidate();
                        workspacePanel.repaint();
                    }
                }
            }
        }.execute();
    }

    private void showWorkspace(SessionInfo session) {
        rebuildWorkspace(session);
        sessionLabel.setText(session.getDisplayName() + "（"
                + accountTypeText(session) + "）");
        statusLabel.setText("登录成功");
        applicationLayout.show(applicationPanel, WORKSPACE_CARD);
    }

    private void rebuildWorkspace(SessionInfo session) {
        if (workspacePanel != null) {
            applicationPanel.remove(workspacePanel);
        }
        workspacePanel = createWorkspace(session);
        applicationPanel.add(workspacePanel, WORKSPACE_CARD);
        applicationPanel.revalidate();
        applicationPanel.repaint();
    }

    private static String accountTypeText(SessionInfo session) {
        return session.canManageUsers() ? "超级管理员" : "普通账号";
    }

    private void logout() {
        logoutButton.setEnabled(false);
        statusLabel.setText("正在退出……");

        new SwingWorker<Response, Void>() {
            private String resultMessage = "已退出登录";

            @Override
            protected Response doInBackground() throws Exception {
                return context.logout();
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        resultMessage = "已清除本地登录状态：" + response.getMessage();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    resultMessage = "退出操作已中断，本地登录状态已清除";
                } catch (ExecutionException exception) {
                    resultMessage = "服务器不可用，本地登录状态已清除";
                } finally {
                    logoutButton.setEnabled(true);
                    loginPanel.prepareForLogin(resultMessage);
                    applicationLayout.show(applicationPanel, LOGIN_CARD);
                }
            }
        }.execute();
    }

    private static final class GradientHeader extends JPanel {
        private GradientHeader() {
            setBackground(NAVY);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setPaint(new GradientPaint(
                    0, 0, NAVY,
                    getWidth(), 0, PRIMARY));
            copy.fillRect(0, 0, getWidth(), getHeight());
            copy.dispose();
        }
    }

    private static final class ModuleTileButton extends JButton {
        private ModuleTileButton(String moduleName) {
            super(moduleName);
            setUI(new BasicButtonUI());
            setPreferredSize(new Dimension(210, 104));
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
            setForeground(TEXT);
            setFont(getFont().deriveFont(Font.BOLD, 18F));
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(getModel().isRollover()
                    ? new Color(238, 250, 247)
                    : Color.WHITE);
            copy.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            copy.setColor(getModel().isRollover()
                    ? new Color(94, 190, 176)
                    : BORDER);
            copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            copy.setColor(PRIMARY);
            copy.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
