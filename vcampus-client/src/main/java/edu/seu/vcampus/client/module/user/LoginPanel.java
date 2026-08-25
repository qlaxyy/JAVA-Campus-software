package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.GridLayout;
import java.util.concurrent.ExecutionException;

/**
 * Minimal development login view shared by all module developers.
 */
public final class LoginPanel extends JPanel {

    private final ClientContext context;
    private final JTextField usernameField = new JTextField("student001");
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("登录");
    private final JButton logoutButton = new JButton("退出登录");
    private final JLabel statusLabel = new JLabel("尚未登录", SwingConstants.CENTER);

    /**
     * Creates the temporary demo login page.
     *
     * @param context shared client context
     */
    public LoginPanel(ClientContext context) {
        this.context = context;
        initializeView();
    }

    private void initializeView() {
        setLayout(new GridLayout(7, 1, 0, 10));
        setBorder(BorderFactory.createEmptyBorder(36, 160, 36, 160));

        add(new JLabel("开发期基础登录", SwingConstants.CENTER));
        add(labeledPanel("用户名", usernameField));
        add(labeledPanel("密码", passwordField));
        add(loginButton);
        add(logoutButton);
        add(statusLabel);
        add(new JLabel(
                "演示账号：student001 / Student@123",
                SwingConstants.CENTER));

        loginButton.addActionListener(event -> login());
        logoutButton.addActionListener(event -> logout());
        updateButtons();
    }

    private JPanel labeledPanel(String label, JTextField field) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.add(new JLabel(label, SwingConstants.RIGHT));
        panel.add(field);
        return panel;
    }

    private void login() {
        setWorking(true, "正在登录……");
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess() && response.getData() instanceof SessionInfo session) {
                        statusLabel.setText(
                                "已登录：" + session.getDisplayName() + "（" + session.getRole() + "）");
                    } else {
                        statusLabel.setText("登录失败：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("登录操作已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("登录失败，请确认服务器已经启动且输入格式正确");
                } finally {
                    passwordField.setText("");
                    setWorking(false, statusLabel.getText());
                }
            }
        }.execute();
    }

    private void logout() {
        setWorking(true, "正在退出……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.logout();
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    statusLabel.setText(response.isSuccess() ? "已退出登录" : response.getMessage());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("退出操作已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("服务器不可用，本地会话已清除");
                } finally {
                    setWorking(false, statusLabel.getText());
                }
            }
        }.execute();
    }

    private void setWorking(boolean working, String message) {
        statusLabel.setText(message);
        loginButton.setEnabled(!working && context.currentSession().isEmpty());
        logoutButton.setEnabled(!working && context.currentSession().isPresent());
    }

    private void updateButtons() {
        setWorking(false, statusLabel.getText());
    }
}
