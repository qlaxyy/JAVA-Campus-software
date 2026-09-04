package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.BatchCreateUserAccountsRequest;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.UserAccountListResponse;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.DoctorApplicationView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.ReviewDoctorApplicationRequest;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Account-management workspace visible only to the super administrator. */
public final class UserAdminPanel extends JPanel {

    private final ClientContext context;
    private final UserAccountTableModel tableModel = new UserAccountTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("进入页面后加载账号列表");
    private final JButton addButton = new JButton("新增账号");
    private final JButton importButton = new JButton("批量导入");
    private final JButton editButton = new JButton("编辑账号");
    private final JButton statusButton = new JButton("启用/禁用");
    private final JButton resetButton = new JButton("重置密码");
    private final JButton refreshButton = new JButton("刷新");
    private final JButton doctorReviewButton = new JButton("医生申请审核");
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
        actions.add(importButton);
        actions.add(editButton);
        actions.add(statusButton);
        actions.add(resetButton);
        actions.add(doctorReviewButton);
        actions.add(refreshButton);

        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        addButton.addActionListener(event -> createAccount());
        importButton.addActionListener(event -> importAccounts());
        importButton.setToolTipText(
                "导入 UTF-8 CSV：campusCardNumber,displayName");
        editButton.addActionListener(event -> editAccount());
        statusButton.addActionListener(event -> changeStatus());
        resetButton.addActionListener(event -> resetPassword());
        doctorReviewButton.addActionListener(event -> loadDoctorApplications());
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

    private void importAccounts() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择账号 CSV 文件");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            List<CreateUserAccountRequest> accounts = UserAccountCsvParser.parse(path);
            int confirmation = JOptionPane.showConfirmDialog(
                    this,
                    "将导入 " + accounts.size() + " 个普通账号，初始密码统一为 123456。\n"
                            + "任意一行无效时，本次导入将全部取消。是否继续？",
                    "确认批量导入",
                    JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                runMutation(
                        UserActions.ADMIN_BATCH_CREATE_ACCOUNTS,
                        new BatchCreateUserAccountsRequest(accounts),
                        "正在批量导入账号……");
            }
        } catch (IOException exception) {
            showValidationError("无法读取 CSV 文件：" + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
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

    private void loadDoctorApplications() {
        runRequest(
                "正在加载医生申请……",
                () -> context.send(HospitalActions.LIST_DOCTOR_APPLICATIONS, null),
                response -> {
                    if (response.isSuccess()
                            && response.getData() instanceof DoctorApplicationListResponse data) {
                        chooseDoctorApplication(data);
                    } else {
                        showFailure(response);
                    }
                });
    }

    private void chooseDoctorApplication(DoctorApplicationListResponse response) {
        List<DoctorApplicationView> pending = response.getApplications().stream()
                .filter(application -> application.getStatus()
                        == DoctorApplicationStatus.PENDING)
                .toList();
        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this, "当前没有待审核的医生申请。");
            statusLabel.setText("没有待审核的医生申请");
            return;
        }
        String[] choices = pending.stream()
                .map(application -> application.getDisplayName()
                        + "（" + applicationTypeText(application) + "） · "
                        + application.getDepartmentName() + " · "
                        + application.getDoctorTitle() + " · "
                        + application.getRequestId())
                .toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "选择一条申请查看并审核：",
                "医生申请审核",
                JOptionPane.PLAIN_MESSAGE,
                null,
                choices,
                choices[0]);
        if (selected == null) {
            return;
        }
        int index = java.util.Arrays.asList(choices).indexOf(selected);
        DoctorApplicationView application = pending.get(index);
        Object[] options = {"批准", "拒绝", "取消"};
        String accountDescription = application.getApplicationType()
                == DoctorApplicationType.EXISTING_ACCOUNT
                ? "关联已有一卡通号：" + application.getUsername()
                : "外来医生：批准后由系统生成下一张一卡通号";
        int decision = JOptionPane.showOptionDialog(
                this,
                "类型：" + applicationTypeText(application)
                        + "\n" + accountDescription
                        + "\n姓名：" + application.getDisplayName()
                        + "\n科室：" + application.getDepartmentName()
                        + "\n职称：" + application.getDoctorTitle()
                        + "\n\n批准后将绑定医生档案；新账号初始密码为 123456。",
                "确认审核",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        if (decision == 0 || decision == 1) {
            reviewDoctorApplication(application.getRequestId(), decision == 0);
        }
    }

    private void reviewDoctorApplication(String requestId, boolean approved) {
        runRequest(
                approved ? "正在批准医生申请……" : "正在拒绝医生申请……",
                () -> context.send(
                        HospitalActions.REVIEW_DOCTOR_APPLICATION,
                        new ReviewDoctorApplicationRequest(requestId, approved)),
                response -> {
                    statusLabel.setText(response.getMessage());
                    if (response.isSuccess()) {
                        String message = response.getMessage();
                        if (approved
                                && response.getData() instanceof DoctorApplicationView reviewed) {
                            message += "\n一卡通号：" + reviewed.getUsername();
                            if (reviewed.getApplicationType()
                                    == DoctorApplicationType.EXTERNAL_DOCTOR) {
                                message += "\n初始密码：123456";
                            }
                        }
                        JOptionPane.showMessageDialog(this, message);
                        refreshAccounts();
                    } else {
                        showFailure(response);
                    }
                });
    }

    private static String applicationTypeText(DoctorApplicationView application) {
        return application.getApplicationType() == DoctorApplicationType.EXISTING_ACCOUNT
                ? "关联已有一卡通号 " + application.getUsername()
                : "新建外来医生账号";
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
        importButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        editButton.setEnabled(enabled);
        statusButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        doctorReviewButton.setEnabled(enabled);
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
