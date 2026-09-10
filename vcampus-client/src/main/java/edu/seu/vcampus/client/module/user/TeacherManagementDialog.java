package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.BatchSaveTeacherProfilesRequest;
import edu.seu.vcampus.common.user.SaveTeacherProfileRequest;
import edu.seu.vcampus.common.user.TeacherProfileListResponse;
import edu.seu.vcampus.common.user.TeacherProfileView;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.user.UserActions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Dedicated teacher qualification workspace for the super administrator. */
final class TeacherManagementDialog extends JDialog {

    private final ClientContext context;
    private final List<UserAccountView> accounts;
    private final TeacherProfileTableModel tableModel = new TeacherProfileTableModel();
    private final JTable table = new JTable(tableModel);
    private final JButton importButton = new JButton("批量导入");
    private final JButton addButton = new JButton("新增教师");
    private final JButton editButton = new JButton("修改教师信息");
    private final JButton cancelButton = new JButton("取消教师资格");
    private final JLabel statusLabel = new JLabel("正在加载教师名单……");
    private final JLabel summaryLabel = new JLabel("教师数据尚未加载");
    private List<TeacherProfileView> allProfiles = List.of();
    private boolean busy;

    TeacherManagementDialog(
            Window owner,
            ClientContext context,
            List<UserAccountView> accounts) {
        super(owner, "教师管理", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.accounts = List.copyOf(accounts);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 600);
        setMinimumSize(new java.awt.Dimension(820, 500));
        setLocationRelativeTo(owner);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        content.setBackground(UserUiTheme.PAGE);
        setContentPane(content);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(UserUiTheme.createSectionHeader("教师名单", summaryLabel), BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(importButton);
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(cancelButton);
        top.add(toolbar, BorderLayout.SOUTH);
        content.add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(event -> updateButtons());
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        content.add(UserUiTheme.createTableScrollPane(table), BorderLayout.CENTER);
        content.add(UserUiTheme.createStatusBar(statusLabel), BorderLayout.SOUTH);

        UserUiTheme.stylePrimaryButton(addButton);
        UserUiTheme.styleSecondaryButton(importButton);
        UserUiTheme.styleSecondaryButton(editButton);
        UserUiTheme.styleDangerButton(cancelButton);

        importButton.setToolTipText("导入 UTF-8 CSV：campusCardNumber,department,title");
        importButton.addActionListener(event -> importTeachers());
        addButton.addActionListener(event -> addTeacher());
        editButton.addActionListener(event -> editTeacher());
        cancelButton.addActionListener(event -> cancelTeacher());
        updateButtons();
        refreshProfiles();
    }

    private TeacherProfileView selectedTeacher() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : tableModel.teacherAt(table.convertRowIndexToModel(viewRow));
    }

    private void updateButtons() {
        boolean selected = !busy && selectedTeacher() != null;
        editButton.setEnabled(selected);
        cancelButton.setEnabled(selected);
    }

    private void refreshProfiles() {
        runRequest(
                "正在加载教师名单……",
                () -> context.send(UserActions.ADMIN_LIST_TEACHERS, null),
                response -> {
                    if (response.isSuccess()
                            && response.getData() instanceof TeacherProfileListResponse data) {
                        allProfiles = List.copyOf(data.getTeachers());
                        tableModel.setTeachers(allProfiles);
                        table.clearSelection();
                        long activeCount = allProfiles.stream()
                                .filter(TeacherProfileView::isActive)
                                .count();
                        summaryLabel.setText("共 " + activeCount + " 位有效教师");
                        statusLabel.setText("共 " + activeCount + " 位有效教师");
                    } else {
                        showFailure(response);
                    }
                });
    }

    private void addTeacher() {
        List<UserAccountView> choices = accounts.stream()
                .filter(UserAccountView::isEnabled)
                .filter(account -> allProfiles.stream().noneMatch(profile ->
                        profile.isActive() && profile.getUserId().equals(account.getUserId())))
                .toList();
        if (choices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "当前没有可添加为教师的账号。");
            return;
        }
        List<AccountChoice> options = choices.stream().map(AccountChoice::new).toList();
        AccountChoice selected = (AccountChoice) JOptionPane.showInputDialog(
                this,
                "选择已有账号：",
                "新增教师",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options.toArray(),
                options.getFirst());
        if (selected == null) {
            return;
        }
        TeacherProfileView existing = allProfiles.stream()
                .filter(profile -> profile.getUserId().equals(selected.account().getUserId()))
                .findFirst()
                .orElse(null);
        showProfileForm(
                "新增教师",
                selected.account().getUserId(),
                selected.account().getUsername(),
                selected.account().getDisplayName(),
                existing == null ? "" : existing.getDepartment(),
                existing == null ? "" : existing.getTitle());
    }

    private void editTeacher() {
        TeacherProfileView teacher = selectedTeacher();
        if (teacher != null) {
            showProfileForm(
                    "修改教师信息",
                    teacher.getUserId(),
                    teacher.getCampusCardNumber(),
                    teacher.getDisplayName(),
                    teacher.getDepartment(),
                    teacher.getTitle());
        }
    }

    private void showProfileForm(
            String dialogTitle,
            String userId,
            String campusCardNumber,
            String displayName,
            String existingDepartment,
            String existingTitle) {
        JTextField department = new JTextField(existingDepartment, 24);
        JTextField title = new JTextField(existingTitle, 24);
        JPanel form = createProfileForm(
                campusCardNumber, displayName, department, title);
        if (JOptionPane.showConfirmDialog(
                this,
                form,
                dialogTitle,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            saveProfile(new SaveTeacherProfileRequest(
                    userId, department.getText(), title.getText(), true),
                    "正在保存教师信息……");
        } catch (IllegalArgumentException exception) {
            showValidationError("院系和职称不能为空。");
        }
    }

    private static JPanel createProfileForm(
            String campusCardNumber,
            String displayName,
            JTextField department,
            JTextField title) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 14, 0);

        JLabel account = new JLabel(campusCardNumber + "  ·  " + displayName);
        account.setForeground(UserUiTheme.PRIMARY_DARK);
        account.setFont(account.getFont().deriveFont(Font.BOLD, 15F));
        form.add(account, constraints);

        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(0, 0, 10, 12);
        form.add(new JLabel("院系"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 10, 0);
        department.setPreferredSize(new java.awt.Dimension(300, 34));
        form.add(department, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(0, 0, 0, 12);
        form.add(new JLabel("职称"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        title.setPreferredSize(new java.awt.Dimension(300, 34));
        form.add(title, constraints);
        return form;
    }

    private void cancelTeacher() {
        TeacherProfileView teacher = selectedTeacher();
        if (teacher == null) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定取消“" + teacher.getDisplayName() + "”的教师资格吗？\n"
                        + "此操作不会禁用或删除该登录账号。",
                "取消教师资格",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            saveProfile(new SaveTeacherProfileRequest(
                    teacher.getUserId(),
                    teacher.getDepartment(),
                    teacher.getTitle(),
                    false),
                    "正在取消教师资格……");
        }
    }

    private void saveProfile(SaveTeacherProfileRequest request, String progress) {
        runRequest(
                progress,
                () -> context.send(UserActions.ADMIN_SAVE_TEACHER_PROFILE, request),
                response -> {
                    if (response.isSuccess()) {
                        statusLabel.setText(response.getMessage());
                        SwingUtilities.invokeLater(this::refreshProfiles);
                    } else {
                        showFailure(response);
                    }
                });
    }

    private void importTeachers() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择教师 CSV 文件");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV 文件 (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            List<SaveTeacherProfileRequest> teachers = TeacherProfileCsvParser.parse(path, accounts);
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "将导入 " + teachers.size() + " 位教师。\n"
                            + "仅维护教师资格，不会新建、禁用或删除登录账号。\n"
                            + "任意一行无效时将全部取消。是否继续？",
                    "确认批量导入",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                runRequest(
                        "正在批量导入教师……",
                        () -> context.send(
                                UserActions.ADMIN_BATCH_SAVE_TEACHERS,
                                new BatchSaveTeacherProfilesRequest(teachers)),
                        response -> {
                            if (response.isSuccess()) {
                                statusLabel.setText(response.getMessage());
                                SwingUtilities.invokeLater(this::refreshProfiles);
                            } else {
                                showFailure(response);
                            }
                        });
            }
        } catch (IOException exception) {
            showValidationError("无法读取 CSV 文件：" + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage());
        }
    }

    private void runRequest(String progress, ResponseCall call, Consumer<Response> completed) {
        setBusy(true);
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
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        importButton.setEnabled(!value);
        addButton.setEnabled(!value);
        updateButtons();
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

    private record AccountChoice(UserAccountView account) {
        @Override
        public String toString() {
            return account.getUsername() + "  " + account.getDisplayName();
        }
    }
}
