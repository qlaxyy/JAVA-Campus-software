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
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * First shared window used to verify that Swing and the socket link work.
 */
public final class MainFrame extends JFrame {

    private static final String LOGIN_CARD = "login";
    private static final String WORKSPACE_CARD = "workspace";

    private final CampusClient client;
    private final ClientContext context;
    private final CardLayout applicationLayout = new CardLayout();
    private final JPanel applicationPanel = new JPanel(applicationLayout);
    private final JLabel statusLabel = new JLabel("服务器尚未检测", SwingConstants.CENTER);
    private final JLabel sessionLabel = new JLabel();
    private final JButton pingButton = new JButton("测试服务器连接");
    private final JButton logoutButton = new JButton("退出登录");
    private LoginPanel loginPanel;

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
        applicationPanel.add(createWorkspace(), WORKSPACE_CARD);
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
        CardLayout cardLayout = new CardLayout();
        JPanel contentPanel = new JPanel(cardLayout);
        JPanel navigationPanel = createNavigation(modules, contentPanel, cardLayout);

        workspace.add(headerPanel, BorderLayout.NORTH);
        workspace.add(navigationPanel, BorderLayout.WEST);
        workspace.add(contentPanel, BorderLayout.CENTER);
        workspace.add(statusPanel, BorderLayout.SOUTH);
        return workspace;
    }

    private JPanel createNavigation(
            List<ClientModule> modules,
            JPanel contentPanel,
            CardLayout cardLayout) {
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        navigation.setPreferredSize(new Dimension(180, 0));

        for (ClientModule module : modules) {
            JButton button = new JButton(module.displayName());
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.addActionListener(event -> cardLayout.show(contentPanel, module.id()));
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(8));
            contentPanel.add(module.createView(context), module.id());
        }
        if (!modules.isEmpty()) {
            cardLayout.show(contentPanel, modules.getFirst().id());
        }
        return navigation;
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
        sessionLabel.setText(session.getDisplayName() + "（" + session.getRole() + "）");
        statusLabel.setText("登录成功");
        applicationLayout.show(applicationPanel, WORKSPACE_CARD);
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
