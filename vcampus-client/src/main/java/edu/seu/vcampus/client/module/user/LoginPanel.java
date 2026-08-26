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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Minimal development login view shared by all module developers.
 */
public final class LoginPanel extends JPanel {

    private final ClientContext context;
    private final Consumer<SessionInfo> loginSucceeded;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("登录");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    /**
     * Creates the temporary demo login page.
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
        setLayout(new GridBagLayout());

        usernameField.setName("login.username");
        usernameField.setColumns(18);
        passwordField.setName("login.password");
        passwordField.setColumns(18);
        loginButton.setName("login.submit");
        statusLabel.setName("login.status");

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        addField(formPanel, constraints, 0, "账户名", usernameField);
        addField(formPanel, constraints, 1, "密码", passwordField);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        formPanel.add(loginButton, constraints);

        constraints.gridy = 3;
        formPanel.add(statusLabel, constraints);

        constraints.gridy = 4;
        formPanel.add(createTestAccountsPanel(), constraints);

        add(formPanel, new GridBagConstraints());

        loginButton.addActionListener(event -> login());
        passwordField.addActionListener(event -> login());
        updateButtons();
    }

    private JPanel createTestAccountsPanel() {
        JPanel accountsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        accountsPanel.setName("login.testAccounts");
        accountsPanel.setBorder(BorderFactory.createTitledBorder("测试账号（开发阶段）"));
        accountsPanel.add(new JLabel("学生：student001 / Student@123"));
        accountsPanel.add(new JLabel("教师：teacher001 / Teacher@123"));
        accountsPanel.add(new JLabel("管理员：admin / Admin@123"));
        return accountsPanel;
    }

    private void addField(
            JPanel formPanel,
            GridBagConstraints constraints,
            int row,
            String label,
            JTextField field) {
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.gridx = 0;
        formPanel.add(new JLabel(label, SwingConstants.RIGHT), constraints);

        constraints.weightx = 1.0;
        constraints.gridx = 1;
        formPanel.add(field, constraints);
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
            return "请输入账户名";
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
