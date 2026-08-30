package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.UserAccountListResponse;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.common.user.UserAccountView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.HierarchyEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Account-management workspace visible only to the super administrator. */
public final class UserAdminPanel extends JPanel {

    private final ClientContext context;
    private final UserAccountTableModel tableModel = new UserAccountTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("进入页面后加载账号列表");
    private final JButton addButton = new JButton("新增账号");
    private final JButton editButton = new JButton("编辑账号");
    private final JButton statusButton = new JButton("启用/禁用");
    private final JButton resetButton = new JButton("重置密码");
    private final JButton refreshButton = new JButton("刷新");
    private boolean loaded;

    public UserAdminPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(event -> updateButtons());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(addButton);
        actions.add(editButton);
        actions.add(statusButton);
        actions.add(resetButton);
        actions.add(refreshButton);

        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        addButton.addActionListener(event -> createAccount());
        editButton.addActionListener(event -> editAccount());
        statusButton.addActionListener(event -> changeStatus());
        resetButton.addActionListener(event -> resetPassword());
        refreshButton.addActionListener(event -> refreshAccounts());
        updateButtons();

        addHierarchyListener(event -> {
            if (!loaded
                    && (event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing()) {
                loaded = true;
                refreshAccounts();
            }
        });
    }

    private UserAccountView selectedAccount() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : tableModel.accountAt(table.convertRowIndexToModel(viewRow));
    }

    private void updateButtons() {
        boolean selected = selectedAccount() != null;
        editButton.setEnabled(selected);
        statusButton.setEnabled(selected);
        resetButton.setEnabled(selected);
    }

    private void refreshAccounts() {
        runRequest("正在加载账号……",
                () -> context.send(UserActions.ADMIN_LIST_ACCOUNTS, null),
                response -> {
                    if (response.isSuccess()
                            && response.getData() instanceof UserAccountListResponse data) {
                        tableModel.setAccounts(data.getAccounts());
                        table.clearSelection();
                        statusLabel.setText("已加载 " + data.getAccounts().size() + " 个账号");
                    } else {
                        showFailure(response);
                    }
                });
    }
    private void createAccount() {
        UserAccountFormData form = UserAccountEditor.show(this, null);
        if (form == null) {
            return;
        }
        char[] password = "123456".toCharArray();
        try {
            CreateUserAccountRequest request = new CreateUserAccountRequest(
                    form.username(),
                    form.displayName(),
                    PasswordProof.create(form.username(), password),
                    form.scopes());
            runMutation(UserActions.ADMIN_CREATE_ACCOUNT, request, "正在创建账号……");
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void editAccount() {
        UserAccountView selected = selectedAccount();
        if (selected == null) {
            return;
        }
        UserAccountFormData form = UserAccountEditor.show(this, selected);
        if (form == null) {
            return;
        }
        try {
            UpdateUserAccountRequest request = new UpdateUserAccountRequest(
                    selected.getUserId(), form.displayName(), form.scopes());
            runMutation(UserActions.ADMIN_UPDATE_ACCOUNT, request, "正在更新账号……");
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
        }
    }
    private void changeStatus() {
        UserAccountView selected = selectedAccount();
        if (selected == null) {
            return;
        }
        boolean enable = !selected.isEnabled();
        String operation = enable ? "启用" : "禁用";
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定要" + operation + "账号 “" + selected.getUsername() + "” 吗？",
                operation + "账号",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            runMutation(
                    UserActions.ADMIN_UPDATE_STATUS,
                    new UpdateUserStatusRequest(selected.getUserId(), enable),
                    "正在更新账号状态……");
        }
    }

    private void resetPassword() {
        UserAccountView selected = selectedAccount();
        if (selected == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定把账号 “" + selected.getUsername() + "” 的密码重置为 123456 吗？\n"
                        + "该账号当前登录会话会立即失效。",
                "重置密码",
                JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        char[] password = "123456".toCharArray();
        try {
            ResetUserPasswordRequest request = new ResetUserPasswordRequest(
                    selected.getUserId(), PasswordProof.create(selected.getUsername(), password));
            runMutation(UserActions.ADMIN_RESET_PASSWORD, request, "正在重置密码……");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void runMutation(String action, java.io.Serializable data, String progress) {
        runRequest(progress, () -> context.send(action, data), response -> {
            if (response.isSuccess()) {
                statusLabel.setText(response.getMessage());
                refreshAccounts();
            } else {
                showFailure(response);
            }
        });
    }

    private void runRequest(String progress, ResponseCall call, Consumer<Response> completed) {
        setActionsEnabled(false);
        statusLabel.setText(progress);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return call.execute();
            }

            @Override
            protected void done() {
                try {
                    completed.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("操作已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("无法连接服务器，请确认服务器已经启动");
                } finally {
                    setActionsEnabled(true);
                    updateButtons();
                }
            }
        }.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        editButton.setEnabled(enabled);
        statusButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    private void showFailure(Response response) {
        statusLabel.setText("操作失败：" + response.getMessage());
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "输入无效", JOptionPane.WARNING_MESSAGE);
    }

    @FunctionalInterface
    private interface ResponseCall {
        Response execute() throws Exception;
    }
}
