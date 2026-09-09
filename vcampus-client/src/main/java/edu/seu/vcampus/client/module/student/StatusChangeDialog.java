package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public final class StatusChangeDialog extends JDialog {

    private final ClientContext context;
    private final String studentId;
    private final boolean isAdminMode;
    private final Runnable onSuccessCallback;

    // 学生端组件
    private final JComboBox<String> cmbChangeType = new JComboBox<>(new String[]{"休学", "复学", "转专业", "退学"});
    private final JTextArea txtReason = new JTextArea(3, 20);
    private final JButton btnSubmit = new JButton("提交申请");

    // 历史/审批记录表格
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"申请号", "学号", "姓名", "异动类型", "申请原因", "申请时间", "审核状态", "审核人"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    public StatusChangeDialog(Window owner, ClientContext context, String studentId, boolean isAdminMode, Runnable onSuccessCallback) {
        super(owner, isAdminMode ? "学籍异动全校审批工作台 (系统管理员)" : "学籍异动申请与个人进度 (学生端)", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.studentId = studentId;
        this.isAdminMode = isAdminMode;
        this.onSuccessCallback = onSuccessCallback;

        initUI();
        loadHistory();
    }

    private void initUI() {
        setSize(920, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        // ================= 1. 管理员工作台界面 =================
        if (isAdminMode) {
            JPanel historyPanel = new JPanel(new BorderLayout());
            historyPanel.setBorder(BorderFactory.createTitledBorder("全校学籍异动待审与审批履历"));
            table.setRowHeight(26);
            table.getTableHeader().setReorderingAllowed(false);
            historyPanel.add(new JScrollPane(table), BorderLayout.CENTER);

            // 底部审批操作工具条
            JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
            JButton btnRefresh = createStyledButton("刷新列表", new Color(241, 245, 249), new Color(203, 213, 225));
            JButton btnApprove = createStyledButton("审核通过", new Color(187, 247, 208), new Color(134, 239, 172));
            JButton btnReject = createStyledButton("驳回申请", new Color(254, 202, 202), new Color(248, 113, 113));

            btnRefresh.addActionListener(e -> loadHistory());
            btnApprove.addActionListener(e -> auditSelected(true));
            btnReject.addActionListener(e -> auditSelected(false));

            bottomBar.add(btnRefresh);
            bottomBar.add(btnApprove);
            bottomBar.add(btnReject);
            historyPanel.add(bottomBar, BorderLayout.SOUTH);

            add(historyPanel, BorderLayout.CENTER);

            // ================= 2. 学生申请端界面 =================
        } else {
            // 上方：提交异动表单
            JPanel applyPanel = new JPanel(new BorderLayout(10, 10));
            applyPanel.setBorder(BorderFactory.createTitledBorder("发起学籍异动申请"));

            JPanel inputGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
            inputGrid.add(new JLabel("申请人学号: " + studentId));
            inputGrid.add(new JLabel("申请异动类别:"));
            inputGrid.add(cmbChangeType);

            JPanel reasonPanel = new JPanel(new BorderLayout(8, 0));
            reasonPanel.setBorder(new EmptyBorder(0, 15, 8, 15));
            reasonPanel.add(new JLabel("异动申请理由:"), BorderLayout.WEST);
            txtReason.setLineWrap(true);
            reasonPanel.add(new JScrollPane(txtReason), BorderLayout.CENTER);

            btnSubmit.setBackground(new Color(220, 252, 231));
            btnSubmit.setForeground(Color.BLACK);
            btnSubmit.setFocusPainted(false);
            btnSubmit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(134, 239, 172), 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
            ));
            btnSubmit.addActionListener(e -> submitApplication());
            reasonPanel.add(btnSubmit, BorderLayout.EAST);

            applyPanel.add(inputGrid, BorderLayout.NORTH);
            applyPanel.add(reasonPanel, BorderLayout.CENTER);
            add(applyPanel, BorderLayout.NORTH);

            // 下方：个人申请历史与进度（纯查看，无审核按钮）
            JPanel historyPanel = new JPanel(new BorderLayout());
            historyPanel.setBorder(BorderFactory.createTitledBorder("我的异动申请与审批进度"));
            table.setRowHeight(26);
            table.getTableHeader().setReorderingAllowed(false);
            historyPanel.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
            JButton btnRefresh = createStyledButton("刷新我的申请", new Color(241, 245, 249), new Color(203, 213, 225));
            btnRefresh.addActionListener(e -> loadHistory());
            bottomBar.add(btnRefresh);
            historyPanel.add(bottomBar, BorderLayout.SOUTH);

            add(historyPanel, BorderLayout.CENTER);
        }
    }

    private JButton createStyledButton(String text, Color bg, Color border) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)
        ));
        return btn;
    }

    private void submitApplication() {
        if (studentId == null || studentId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "未能识别当前学生账号，请重新登录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入具体的异动申请理由！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ApplyStatusChangeRequest req = new ApplyStatusChangeRequest(
            studentId, (String) cmbChangeType.getSelectedItem(), reason
        );

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    return context.send(StudentActions.APPLY_STATUS_CHANGE, req);
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Response res = get();
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(StatusChangeDialog.this, "异动申请已成功提交，请等待管理员审核！", "提交成功", JOptionPane.INFORMATION_MESSAGE);
                        txtReason.setText("");
                        loadHistory();
                    } else {
                        JOptionPane.showMessageDialog(StatusChangeDialog.this, res != null ? res.getMessage() : "提交失败", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private void loadHistory() {
        tableModel.setRowCount(0);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    // 管理员查询全部 (传 null)，学生仅查询本人 (传 studentId)
                    return context.send(StudentActions.LIST_STATUS_CHANGES, isAdminMode ? null : studentId);
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Response res = get();
                    if (res != null && res.isSuccess() && res.getData() instanceof List) {
                        List<StatusChangeDto> list = (List<StatusChangeDto>) res.getData();
                        for (StatusChangeDto item : list) {
                            tableModel.addRow(new Object[]{
                                item.getChangeId(),
                                item.getStudentId(),
                                item.getStudentName(),
                                item.getChangeType(),
                                item.getReason(),
                                item.getChangeDate(),
                                item.getAuditStatus(),
                                item.getOperator()
                            });
                        }
                    } else if (res != null && !res.isSuccess()) {
                        JOptionPane.showMessageDialog(StatusChangeDialog.this, res.getMessage(), "提示", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void auditSelected(boolean approved) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择需要处理的异动申请记录！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long changeId = (Long) tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 6);
        if (!"待审核".equals(currentStatus)) {
            JOptionPane.showMessageDialog(this, "该记录已完成审核，请勿重复操作！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AuditStatusChangeRequest req = new AuditStatusChangeRequest(changeId, approved);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    return context.send(StudentActions.AUDIT_STATUS_CHANGE, req);
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Response res = get();
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(StatusChangeDialog.this, "审核完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
                        loadHistory();
                        if (onSuccessCallback != null) onSuccessCallback.run();
                    } else {
                        JOptionPane.showMessageDialog(StatusChangeDialog.this, res != null ? res.getMessage() : "审核权限不足", "警告", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }
}
