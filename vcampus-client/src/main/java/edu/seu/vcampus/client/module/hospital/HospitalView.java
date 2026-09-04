package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.DoctorApplicationView;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Root view that keeps hospital pages inside the shared application window. */
public final class HospitalView extends JPanel {

    private static final String MODE_SELECT = "mode-select";
    private static final String PATIENT_HOME = "patient-home";
    private static final String SLOT_SEARCH = "slot-search";
    private static final String DOCTOR_HOME = "doctor-home";
    private static final String ADMIN_HOME = "admin-home";

    private final ClientContext context;
    private final CardLayout cards = new CardLayout();
    private final HospitalModePanel modePanel;
    private final HospitalHomePanel homePanel;
    private final SlotSearchPanel slotSearchPanel;
    private HospitalModeAccessView modeAccess;

    public HospitalView(ClientContext context) {
        this.context = context;
        setLayout(cards);
        setBackground(HospitalTheme.BACKGROUND);

        modePanel = new HospitalModePanel(
                () -> openMode(HospitalMode.PATIENT, PATIENT_HOME),
                () -> openMode(HospitalMode.DOCTOR, DOCTOR_HOME),
                () -> openMode(HospitalMode.ADMIN, ADMIN_HOME),
                this::openModeSelector);
        homePanel = new HospitalHomePanel(this::openSlotSearch, this::openModeSelector);
        slotSearchPanel = new SlotSearchPanel(context, this::openPatientHome);
        HospitalStaffHomePanel doctorHome = new HospitalStaffHomePanel(
                "医生工作台",
                "查看排班和患者就诊背景信息，完成诊断与处置",
                List.of(
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "我的排班", "查看本人未来排班和出诊号源。"),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "待接诊患者", "按预约顺序查看今日和待接诊患者。"),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "患者就诊背景信息", "查看当前患者病历、既往就诊和主诉。"),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "诊断与处置", "提交诊断、检查意见、治疗意见和简化处方。")),
                this::openModeSelector);
        HospitalStaffHomePanel adminHome = new HospitalStaffHomePanel(
                "医院管理工作台",
                "维护校医院基础资料、排班、号源和预约秩序",
                List.of(
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "科室管理", "启用、停用和维护校医院科室资料。"),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "医生管理",
                                "提交医生新增申请；账号创建和身份激活须经超级管理员审核。",
                                "提交新增医生申请",
                                this::loadDepartmentsForApplication),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "医生申请记录",
                                "查看审核状态；申请通过后取得外来医生的一卡通号。",
                                "查看申请记录",
                                this::loadDoctorApplications),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "排班管理", "建立、发布或关闭医生排班。"),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "号源与预约管理", "查看号源容量和处理异常预约。")),
                this::openModeSelector);

        add(modePanel, MODE_SELECT);
        add(homePanel, PATIENT_HOME);
        add(slotSearchPanel, SLOT_SEARCH);
        add(doctorHome, DOCTOR_HOME);
        add(adminHome, ADMIN_HOME);
        cards.show(this, MODE_SELECT);

        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                openModeSelector();
            }
        });
    }

    private void openSlotSearch() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先到“用户”模块登录，再使用预约挂号。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, SLOT_SEARCH);
        slotSearchPanel.activate();
    }

    private void openPatientHome() {
        cards.show(this, PATIENT_HOME);
    }

    private void openMode(HospitalMode mode, String cardName) {
        if (!canOpen(mode)) {
            modePanel.showError("当前账号没有进入该模式的权限，请重新检查权限。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, cardName);
    }

    private boolean canOpen(HospitalMode mode) {
        return context.currentSession().isPresent()
                && modeAccess != null
                && modeAccess.canAccess(mode);
    }

    private void openModeSelector() {
        cards.show(this, MODE_SELECT);
        SessionInfo session = context.currentSession().orElse(null);
        if (session == null) {
            modeAccess = null;
            modePanel.showLoginRequired();
            return;
        }
        modePanel.showLoading(session);
        loadModeAccess(session);
    }

    private void loadModeAccess(SessionInfo requestedSession) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_MODE_ACCESS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    SessionInfo currentSession = context.currentSession().orElse(null);
                    if (currentSession == null
                            || !currentSession.getToken().equals(requestedSession.getToken())) {
                        modeAccess = null;
                        modePanel.showLoginRequired();
                        return;
                    }
                    if (response.isSuccess()
                            && response.getData() instanceof HospitalModeAccessView access) {
                        modeAccess = access;
                        modePanel.showAccess(currentSession, access);
                    } else {
                        modeAccess = null;
                        modePanel.showError("权限检查失败：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    modePanel.showError("权限检查已中断，请重试。");
                } catch (ExecutionException exception) {
                    modeAccess = null;
                    modePanel.showError("无法连接服务器，请确认服务器已经启动后重试。");
                }
            }
        }.execute();
    }

    private void loadDepartmentsForApplication() {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_DEPARTMENTS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof DepartmentListResponse data) {
                        showDoctorApplicationDialog(data.getDepartments());
                    } else {
                        JOptionPane.showMessageDialog(HospitalView.this,
                                "无法加载科室：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(HospitalView.this, "无法连接服务器。");
                }
            }
        }.execute();
    }

    private void showDoctorApplicationDialog(List<DepartmentView> departments) {
        JComboBox<String> applicationType = new JComboBox<>(new String[]{
                "关联已有校园账号", "新建外来医生账号"
        });
        JTextField username = new JTextField();
        JTextField displayName = new JTextField();
        JTextField title = new JTextField();
        JComboBox<String> department = new JComboBox<>(departments.stream()
                .map(item -> item.getDepartmentName() + "（" + item.getDepartmentId() + "）")
                .toArray(String[]::new));
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("申请类型："));
        form.add(applicationType);
        form.add(new JLabel("已有一卡通号（仅关联已有账号时填写）："));
        form.add(username);
        form.add(new JLabel("外来医生姓名（仅新建账号时填写）："));
        form.add(displayName);
        form.add(new JLabel("科室："));
        form.add(department);
        form.add(new JLabel("职称："));
        form.add(title);
        displayName.setEnabled(false);
        applicationType.addActionListener(event -> {
            boolean existing = applicationType.getSelectedIndex() == 0;
            username.setEnabled(existing);
            displayName.setEnabled(!existing);
        });
        if (JOptionPane.showConfirmDialog(
                this, form, "提交新增医生申请",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) {
            return;
        }
        int selectedIndex = department.getSelectedIndex();
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "请选择科室。");
            return;
        }
        try {
            SubmitDoctorApplicationRequest request = applicationType.getSelectedIndex() == 0
                    ? SubmitDoctorApplicationRequest.forExistingAccount(
                            username.getText(),
                            departments.get(selectedIndex).getDepartmentId(), title.getText())
                    : SubmitDoctorApplicationRequest.forExternalDoctor(
                            displayName.getText(),
                            departments.get(selectedIndex).getDepartmentId(), title.getText());
            submitDoctorApplication(request);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this,
                    "输入无效：" + exception.getMessage(),
                    "无法提交", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void submitDoctorApplication(SubmitDoctorApplicationRequest request) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.SUBMIT_DOCTOR_APPLICATION, request);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    JOptionPane.showMessageDialog(HospitalView.this, response.getMessage(),
                            response.isSuccess() ? "提交成功" : "提交失败",
                            response.isSuccess()
                                    ? JOptionPane.INFORMATION_MESSAGE
                                    : JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(HospitalView.this, "无法连接服务器。");
                }
            }
        }.execute();
    }

    private void loadDoctorApplications() {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_DOCTOR_APPLICATIONS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof DoctorApplicationListResponse data) {
                        showDoctorApplicationHistory(data.getApplications());
                    } else {
                        JOptionPane.showMessageDialog(HospitalView.this,
                                "无法加载申请记录：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(HospitalView.this, "无法连接服务器。");
                }
            }
        }.execute();
    }

    private void showDoctorApplicationHistory(List<DoctorApplicationView> applications) {
        if (applications.isEmpty()) {
            JOptionPane.showMessageDialog(this, "目前还没有医生申请记录。");
            return;
        }
        String[] columns = {
                "申请时间", "申请类型", "姓名", "科室", "职称", "状态", "一卡通号"
        };
        Object[][] rows = applications.stream()
                .map(application -> new Object[]{
                        application.getCreatedAt().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        applicationTypeText(application),
                        application.getDisplayName(),
                        application.getDepartmentName(),
                        application.getDoctorTitle(),
                        applicationStatusText(application.getStatus()),
                        accountDeliveryText(application)
                })
                .toArray(Object[][]::new);
        JTable table = new JTable(new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(920, 320));

        JPanel content = new JPanel(new java.awt.BorderLayout(0, 10));
        content.add(scroll, java.awt.BorderLayout.CENTER);
        content.add(new JLabel(
                "外来医生获批后自动取得一卡通号，初始密码为 123456；关联已有账号时继续使用原密码。"),
                java.awt.BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(
                this, content, "医生申请记录", JOptionPane.PLAIN_MESSAGE);
    }

    private static String applicationTypeText(DoctorApplicationView application) {
        return application.getApplicationType() == DoctorApplicationType.EXISTING_ACCOUNT
                ? "关联已有账号" : "新建外来医生";
    }

    private static String applicationStatusText(DoctorApplicationStatus status) {
        return switch (status) {
            case PENDING -> "待审核";
            case APPROVED -> "已通过";
            case REJECTED -> "已拒绝";
        };
    }

    private static String accountDeliveryText(DoctorApplicationView application) {
        if (application.getUsername() != null) {
            return application.getUsername();
        }
        return application.getStatus() == DoctorApplicationStatus.REJECTED
                ? "未创建" : "审核通过后自动生成";
    }
}
