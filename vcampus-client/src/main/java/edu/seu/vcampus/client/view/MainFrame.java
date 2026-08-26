package edu.seu.vcampus.client.view;

import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.client.module.ClientModules;
import edu.seu.vcampus.client.module.user.LoginPanel;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * First shared window used to verify that Swing and the socket link work.
 */
public final class MainFrame extends JFrame {

    private static final String LOGIN_CARD = "login";
    private static final String WORKSPACE_CARD = "workspace";
    private static final String MODULE_HOME_CARD = "module-home";

    private final CampusClient client;
    private final ClientContext context;
    private final CardLayout applicationLayout = new CardLayout();
    private final JPanel applicationPanel = new JPanel(applicationLayout);
    private final JLabel statusLabel = new JLabel("服务器尚未检测", SwingConstants.CENTER);
    private final JLabel sessionLabel = new JLabel();
    private final JButton pingButton = new JButton("测试服务器连接");
    private final JButton logoutButton = new JButton("退出登录");
    private LoginPanel loginPanel;
    private boolean workspaceInitialized;

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
        setMinimumSize(new Dimension(900, 560));
        setLocationByPlatform(true);

        loginPanel = new LoginPanel(context, this::showWorkspace);
        applicationPanel.add(loginPanel, LOGIN_CARD);
        setContentPane(applicationPanel);
        applicationLayout.show(applicationPanel, LOGIN_CARD);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("虚拟校园系统", SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24F));

        logoutButton.addActionListener(event -> logout());
        JPanel accountPanel = new JPanel(new BorderLayout(12, 0));
        accountPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        accountPanel.add(sessionLabel, BorderLayout.CENTER);
        accountPanel.add(logoutButton, BorderLayout.EAST);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(accountPanel, BorderLayout.EAST);

        pingButton.addActionListener(event -> pingServer());
        JPanel statusPanel = new JPanel(new BorderLayout(16, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(pingButton, BorderLayout.EAST);

        List<ClientModule> modules = ClientModules.all().stream()
                .filter(module -> !ModuleNames.USER.equals(module.id()))
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

    private JPanel createModuleHome(
            List<ClientModule> modules,
            JPanel modulePanel,
            CardLayout moduleLayout) {
        JPanel home = new JPanel(new GridBagLayout());
        JPanel tiles = new JPanel(new GridLayout(0, 3, 18, 18));

        for (ClientModule module : modules) {
            JButton button = new JButton(module.displayName());
            button.setName("module.home." + module.id());
            button.setPreferredSize(new Dimension(180, 84));
            button.setFont(button.getFont().deriveFont(Font.PLAIN, 17F));
            button.addActionListener(event -> moduleLayout.show(modulePanel, module.id()));
            tiles.add(button);
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(24, 24, 24, 24);
        home.add(tiles, constraints);
        return home;
    }

    private JPanel createModulePage(
            ClientModule module,
            JPanel modulePanel,
            CardLayout moduleLayout) {
        JPanel page = new JPanel(new BorderLayout());
        JButton backButton = new JButton("返回模块首页");
        backButton.setName("module.back." + module.id());
        backButton.addActionListener(event -> moduleLayout.show(modulePanel, MODULE_HOME_CARD));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        toolbar.add(backButton, BorderLayout.WEST);

        page.add(toolbar, BorderLayout.NORTH);
        page.add(module.createView(context), BorderLayout.CENTER);
        return page;
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
                }
            }
        }.execute();
    }

    private void showWorkspace(SessionInfo session) {
        initializeWorkspaceIfNeeded();
        sessionLabel.setText(session.getDisplayName() + "（" + session.getRole() + "）");
        statusLabel.setText("登录成功");
        applicationLayout.show(applicationPanel, WORKSPACE_CARD);
    }

    private void initializeWorkspaceIfNeeded() {
        if (workspaceInitialized) {
            return;
        }
        applicationPanel.add(createWorkspace(), WORKSPACE_CARD);
        workspaceInitialized = true;
        applicationPanel.revalidate();
        applicationPanel.repaint();
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
}
