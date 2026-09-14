package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.ApplyStatusChangeRequest;
import edu.seu.vcampus.common.student.AuditStatusChangeRequest;
import edu.seu.vcampus.common.student.StatusChangeDto;
import edu.seu.vcampus.common.student.StudentActions;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatusChangeDialog extends JDialog {
    private final ClientContext context;
    private final String currentStudentId;
    private final boolean isAdmin;
    private final Runnable onUpdated;

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"ID", "学号", "姓名", "申请类型", "申请原由及变更说明", "申请时间", "审核状态", "审核人"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<StatusChangeDto> changeList = new ArrayList<>();

    // 学生端申请组件
    private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"基本信息变更", "休学", "复学", "退学"});
    private final JComboBox<String> cmbField = new JComboBox<>(new String[]{"籍贯", "姓名", "性别", "民族", "身份证号", "出生日期"});
    private final JTextField txtNewValue = new JTextField(12);
    private final JTextField txtReason = new JTextField(18);

    public StatusChangeDialog(Window parent, ClientContext context, String currentStudentId, boolean isAdmin, Runnable onUpdated) {
        super(parent, isAdmin ? "学籍异动与基本信息维护审核工作台 (管理员)" : "我的学籍申请与基本信息更正", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.currentStudentId = currentStudentId;
        this.isAdmin = isAdmin;
        this.onUpdated = onUpdated;

        setSize(920, 560);
        setLocationRelativeTo(parent);
        initUI();
        loadData();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        if (!isAdmin) {
            JPanel applyPanel = new JPanel(new GridBagLayout());
            applyPanel.setBorder(new TitledBorder("提交基本信息更正 / 学籍异动申请"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            applyPanel.add(new JLabel("申请类型:"), gbc);
            gbc.gridx = 1;
            applyPanel.add(cmbType, gbc);

            JLabel lblField = new JLabel("更正项目:");
            gbc.gridx = 2;
            applyPanel.add(lblField, gbc);
            gbc.gridx = 3;
            applyPanel.add(cmbField, gbc);

            JLabel lblVal = new JLabel("申请新值:");
            gbc.gridx = 4;
            applyPanel.add(lblVal, gbc);
            gbc.gridx = 5;
            applyPanel.add(txtNewValue, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            applyPanel.add(new JLabel("理由/材料说明:"), gbc);
            gbc.gridx = 1; gbc.gridwidth = 4;
            applyPanel.add(txtReason, gbc);

            gbc.gridx = 5; gbc.gridwidth = 1;
            JButton btnSubmit = new JButton("提交申请");
            btnSubmit.setBackground(new Color(187, 247, 208));
            applyPanel.add(btnSubmit, gbc);

            cmbType.addActionListener(e -> {
                boolean isInfoChange = "基本信息变更".equals(cmbType.getSelectedItem());
                cmbField.setEnabled(isInfoChange);
                txtNewValue.setEnabled(isInfoChange);
            });

            btnSubmit.addActionListener(e -> submitApply());
            root.add(applyPanel, BorderLayout.NORTH);
        }

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new TitledBorder(isAdmin ? "全校学生申请记录" : "我的申请记录与进度"));
        root.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnRefresh = new JButton("刷新");
        btnRefresh.addActionListener(e -> loadData());
        bottomBar.add(btnRefresh);

        if (isAdmin) {
            JButton btnApprove = new JButton("同意并同步档案");
            btnApprove.setBackground(new Color(187, 247, 208));
            JButton btnReject = new JButton("驳回申请");
            btnReject.setBackground(new Color(254, 202, 202));

            btnApprove.addActionListener(e -> audit(true));
            btnReject.addActionListener(e -> audit(false));

            bottomBar.add(btnApprove);
            bottomBar.add(btnReject);
        }

        JButton btnClose = new JButton("关闭");
        btnClose.addActionListener(e -> dispose());
        bottomBar.add(btnClose);

        root.add(bottomBar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void loadData() {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    String reqParam = isAdmin ? "" : currentStudentId;
                    return context.send(StudentActions.LIST_STATUS_CHANGES, reqParam);
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Response res = get();
                    tableModel.setRowCount(0);
                    if (res != null && res.isSuccess() && res.getData() instanceof List) {
                        changeList = (List<StatusChangeDto>) res.getData();
                        for (StatusChangeDto c : changeList) {
                            tableModel.addRow(new Object[]{
                                c.getChangeId(),
                                c.getStudentId(),
                                c.getStudentName(),
                                c.getChangeType(),
                                c.getReason(),
                                c.getChangeDate(),
                                c.getAuditStatus(),
                                c.getOperator() != null ? c.getOperator() : "-"
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void submitApply() {
        String type = (String) cmbType.getSelectedItem();
        String field = (String) cmbField.getSelectedItem();
        String newVal = txtNewValue.getText().trim();
        String reasonText = txtReason.getText().trim();

        if ("基本信息变更".equals(type) && newVal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入需要更正的新值！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (reasonText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写更正理由或证明材料说明！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String finalReason = "基本信息变更".equals(type)
            ? String.format("[变更: %s -> %s] %s", field, newVal, reasonText)
            : reasonText;

        ApplyStatusChangeRequest req = new ApplyStatusChangeRequest(currentStudentId, type, finalReason);

        try {
            Response res = context.send(StudentActions.APPLY_STATUS_CHANGE, req);
            if (res != null && res.isSuccess()) {
                JOptionPane.showMessageDialog(this, "申请已提交，请等待管理员审核！", "成功", JOptionPane.INFORMATION_MESSAGE);
                txtNewValue.setText("");
                txtReason.setText("");
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "提交受阻: " + (res != null ? res.getMessage() : "网络超时"), "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "提交异常: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void audit(boolean approve) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先在表格中选择一条待审核记录！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StatusChangeDto item = changeList.get(row);
        if (!"待审核".equals(item.getAuditStatus())) {
            JOptionPane.showMessageDialog(this, "该记录已被处理，不能重复审核！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AuditStatusChangeRequest req = new AuditStatusChangeRequest(item.getChangeId(), approve);

        try {
            Response res = context.send(StudentActions.AUDIT_STATUS_CHANGE, req);
            if (res != null && res.isSuccess()) {
                JOptionPane.showMessageDialog(this, "审批已成功处理！" + (approve ? "\n学生档案已联动实时同步更新！" : ""), "提示", JOptionPane.INFORMATION_MESSAGE);
                loadData();
                if (onUpdated != null) {
                    onUpdated.run();
                }
            } else {
                JOptionPane.showMessageDialog(this, "处理受阻: " + (res != null ? res.getMessage() : "未知异常"), "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "异常: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
