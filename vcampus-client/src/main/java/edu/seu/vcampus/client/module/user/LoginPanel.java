package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.CampusCardNumber;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Unified login view shared by all module developers.
 */
public final class LoginPanel extends JPanel {

    private final ClientContext context;
    private final Consumer<SessionInfo> loginSucceeded;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("登录");
    private final JLabel statusLabel = new JLabel("请输入账号和密码", SwingConstants.CENTER);

    /**
     * Creates the shared login page.
     *
     * @param context shared client context
     */
    public LoginPanel(ClientContext context) {
        this(context, session -> {
        });
    }

    /**
     * Creates the login page and notifies the main window after authentication.
     *
     * @param context shared client context
     * @param loginSucceeded callback receiving the authenticated session
     */
    public LoginPanel(ClientContext context, Consumer<SessionInfo> loginSucceeded) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.loginSucceeded = Objects.requireNonNull(
                loginSucceeded,
                "loginSucceeded must not be null");
        initializeView();
    }

    private void initializeView() {
        usernameField.setName("login.username");
        usernameField.setColumns(18);
        passwordField.setName("login.password");
        passwordField.setColumns(18);
        loginButton.setName("login.submit");
        statusLabel.setName("login.status");

        setLayout(new java.awt.BorderLayout());
        add(LoginPanelDesign.create(
                usernameField, passwordField, loginButton, statusLabel),
                java.awt.BorderLayout.CENTER);

        loginButton.addActionListener(event -> login());
        passwordField.addActionListener(event -> login());
        updateButtons();
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        String validationMessage = validationMessage(username, password);
        if (validationMessage != null) {
            Arrays.fill(password, '\0');
            passwordField.setText("");
            statusLabel.setText(validationMessage);
            usernameField.requestFocusInWindow();
            return;
        }
        setWorking(true, "正在登录……");

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
                        statusLabel.setText("登录成功");
                        loginSucceeded.accept(session);
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

    static String validationMessage(String username, char[] password) {
        if (username == null || username.isBlank()) {
            return "请输入一卡通号";
        }
        if (!CampusCardNumber.isValid(username)) {
            return "一卡通号必须是 8 位数字（年份 + 4 位流水号）";
        }
        if (password == null || password.length == 0) {
            return "请输入密码";
        }
        return null;
    }

    private void setWorking(boolean working, String message) {
        statusLabel.setText(message);
        usernameField.setEnabled(!working);
        passwordField.setEnabled(!working);
        loginButton.setEnabled(!working);
    }

    private void updateButtons() {
        setWorking(false, statusLabel.getText());
    }

    /**
     * Clears sensitive input and prepares the form for another login.
     *
     * @param message optional result shown below the login button
     */
    public void prepareForLogin(String message) {
        passwordField.setText("");
        statusLabel.setText(message == null || message.isBlank() ? " " : message);
        setWorking(false, statusLabel.getText());
        usernameField.requestFocusInWindow();
    }
}
