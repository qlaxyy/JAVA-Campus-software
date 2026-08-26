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
        this.context = context;
        this.loginSucceeded = loginSucceeded;
        initializeView();
    }

    private void initializeView() {
        setLayout(new GridLayout(4, 1, 0, 12));
        setBorder(BorderFactory.createEmptyBorder(150, 260, 150, 260));

        add(labeledPanel("账户名", usernameField));
        add(labeledPanel("密码", passwordField));
        add(loginButton);
        add(statusLabel);

        loginButton.addActionListener(event -> login());
        passwordField.addActionListener(event -> login());
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
