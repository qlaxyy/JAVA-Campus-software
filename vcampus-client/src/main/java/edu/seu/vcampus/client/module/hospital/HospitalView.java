package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.DoctorApplicationView;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/** Root view that keeps hospital pages inside the shared application window. */
public final class HospitalView extends JPanel {

    private static final String MODE_SELECT = "mode-select";
    private static final String PATIENT_HOME = "patient-home";
    private static final String SMART_TRIAGE = "smart-triage";
    private static final String SLOT_SEARCH = "slot-search";
    private static final String MY_APPOINTMENTS = "my-appointments";
    private static final String CONSULTATION_RECORDS = "consultation-records";
    private static final String HEALTH_RECORD = "health-record";
    private static final String PATIENT_BILLS = "patient-bills";
    private static final String PATIENT_CARE_GUIDE = "patient-care-guide";
    private static final String DOCTOR_HOME = "doctor-home";
    private static final String ADMIN_HOME = "admin-home";
    private static final String ADMIN_DEPARTMENTS = "admin-departments";
    private static final String ADMIN_SCHEDULES = "admin-schedules";
    private static final String ADMIN_APPOINTMENTS = "admin-appointments";

    private final ClientContext context;
    private final CardLayout cards = new CardLayout();
    private final HospitalModePanel modePanel;
    private final HospitalHomePanel homePanel;
    private final SmartTriagePanel smartTriagePanel;
    private final SlotSearchPanel slotSearchPanel;
    private final MyAppointmentsPanel myAppointmentsPanel;
    private final ConsultationRecordsPanel consultationRecordsPanel;
    private final PatientHealthRecordPanel patientHealthRecordPanel;
    private final PatientBillsPanel patientBillsPanel;
    private final PatientCareGuidePanel patientCareGuidePanel;
    private final DoctorWorkspacePanel doctorWorkspacePanel;
    private final HospitalStaffHomePanel adminHomePanel;
    private final AdminDepartmentPanel adminDepartmentPanel;
    private final AdminSchedulePanel adminSchedulePanel;
    private final AdminAppointmentPanel adminAppointmentPanel;
    private final Set<String> guidedUserIds = new HashSet<>();
    private HospitalModeAccessView modeAccess;
    private int accessRequestVersion;
    private int patientHomeRequestVersion;

    public HospitalView(ClientContext context) {
        this.context = context;
        setLayout(cards);
        setBackground(HospitalTheme.BACKGROUND);

        modePanel = new HospitalModePanel(
                () -> openMode(HospitalMode.PATIENT, PATIENT_HOME),
                () -> openMode(HospitalMode.DOCTOR, DOCTOR_HOME),
                () -> openMode(HospitalMode.ADMIN, ADMIN_HOME),
                this::openModeSelector);
        homePanel = new HospitalHomePanel(
                this::openSlotSearch,
                this::openSmartTriage,
                this::openMyAppointments,
                this::openConsultationRecords,
                this::openHealthRecord,
                this::openBills,
                this::openOrdinaryFollowUpSelection,
                this::openCareGuide,
                this::openFollowUp,
                this::openModeSelector);
        slotSearchPanel = new SlotSearchPanel(
                context,
                this::openPatientHome,
                this::openOrdinaryFollowUpSelection);
        smartTriagePanel = new SmartTriagePanel(
                context,
                this::openPatientHome,
                this::openTriageDepartmentSlots);
        myAppointmentsPanel = new MyAppointmentsPanel(
                context,
                this::openPatientHome,
                this::openSlotSearch);
        consultationRecordsPanel = new ConsultationRecordsPanel(
                context,
                this::openPatientHome);
        patientHealthRecordPanel = new PatientHealthRecordPanel(
                context,
                this::openPatientHome,
                this::openOrdinaryFollowUp);
        patientBillsPanel = new PatientBillsPanel(context, this::openPatientHome);
        patientCareGuidePanel = new PatientCareGuidePanel(
                this::openPatientHome,
                this::openSlotSearch,
                this::openOrdinaryFollowUpSelection,
                this::openHealthRecord);
        doctorWorkspacePanel = new DoctorWorkspacePanel(context, this::openModeSelector);
        adminHomePanel = new HospitalStaffHomePanel(
                "医院管理工作台",
                "维护校医院基础资料、排班、号源和预约秩序",
                List.of(
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "科室管理",
                                "维护科室层级、挂号入口和启用状态。",
                                "管理科室",
                                this::openAdminDepartments),
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
                                "排班管理",
                                "建立排班草稿，核对后发布号源，或关闭无预约排班。",
                                "管理排班",
                                this::openAdminSchedules),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "号源与预约管理",
                                "查询预约流转，处理尚未开始的异常预约和模拟退款。",
                                "查看预约订单",
                                this::openAdminAppointments)),
                this::openModeSelector);
        adminDepartmentPanel = new AdminDepartmentPanel(context, this::openAdminHome);
        adminSchedulePanel = new AdminSchedulePanel(context, this::openAdminHome);
        adminAppointmentPanel = new AdminAppointmentPanel(context, this::openAdminHome);

        add(modePanel, MODE_SELECT);
        add(homePanel, PATIENT_HOME);
        add(smartTriagePanel, SMART_TRIAGE);
        add(slotSearchPanel, SLOT_SEARCH);
        add(myAppointmentsPanel, MY_APPOINTMENTS);
        add(consultationRecordsPanel, CONSULTATION_RECORDS);
        add(patientHealthRecordPanel, HEALTH_RECORD);
        add(patientBillsPanel, PATIENT_BILLS);
        add(patientCareGuidePanel, PATIENT_CARE_GUIDE);
        add(doctorWorkspacePanel, DOCTOR_HOME);
        add(adminHomePanel, ADMIN_HOME);
        add(adminDepartmentPanel, ADMIN_DEPARTMENTS);
        add(adminSchedulePanel, ADMIN_SCHEDULES);
        add(adminAppointmentPanel, ADMIN_APPOINTMENTS);
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

    private void openSmartTriage() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先到“用户”模块登录，再使用智能导诊。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, SMART_TRIAGE);
        smartTriagePanel.activate();
    }

    private void openTriageDepartmentSlots(String departmentId) {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("登录状态已失效，请重新登录后查看号源。");
            cards.show(this, PATIENT_HOME);
            return;
        }
        cards.show(this, SLOT_SEARCH);
        slotSearchPanel.activateForDepartment(departmentId);
    }

    private void openPatientHome() {
        cards.show(this, PATIENT_HOME);
        refreshPatientHome();
    }

    private void openMyAppointments() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先到“用户”模块登录，再查看预约。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, MY_APPOINTMENTS);
        myAppointmentsPanel.activate();
    }

    private void openConsultationRecords() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先到“用户”模块登录，再查看问诊记录。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, CONSULTATION_RECORDS);
        consultationRecordsPanel.activate();
    }

    private void openHealthRecord() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先到“用户”模块登录，再查看健康档案。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, HEALTH_RECORD);
        patientHealthRecordPanel.activate();
    }

    private void openBills() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先登录，再查看费用清单。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, PATIENT_BILLS);
        patientBillsPanel.activate();
    }

    private void openCareGuide() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先登录，再查看就医指南。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, PATIENT_CARE_GUIDE);
    }

    private void openFollowUp() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先登录，再处理检查与回诊待办。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, HEALTH_RECORD);
        patientHealthRecordPanel.activateForFollowUp();
    }

    private void openOrdinaryFollowUp(ConsultationRecordView source) {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先登录，再预约普通复诊。");
            return;
        }
        cards.show(this, SLOT_SEARCH);
        slotSearchPanel.activateForFollowUp(source);
    }

    private void openOrdinaryFollowUpSelection() {
        if (!canOpen(HospitalMode.PATIENT)) {
            homePanel.showMessage("请先登录，再预约普通复诊。");
            return;
        }
        homePanel.showMessage(" ");
        cards.show(this, HEALTH_RECORD);
        patientHealthRecordPanel.activateForOrdinaryFollowUpSelection();
    }

    private void openAdminHome() {
        cards.show(this, ADMIN_HOME);
    }

    private void openAdminDepartments() {
        if (!canOpen(HospitalMode.ADMIN)) {
            modePanel.showError("当前账号没有进入医院管理模式的权限。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, ADMIN_DEPARTMENTS);
        adminDepartmentPanel.activate();
    }

    private void openAdminSchedules() {
        if (!canOpen(HospitalMode.ADMIN)) {
            modePanel.showError("当前账号没有进入医院管理模式的权限。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, ADMIN_SCHEDULES);
        adminSchedulePanel.activate();
    }

    private void openAdminAppointments() {
        if (!canOpen(HospitalMode.ADMIN)) {
            modePanel.showError("当前账号没有进入医院管理模式的权限。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, ADMIN_APPOINTMENTS);
        adminAppointmentPanel.activate();
    }

    private void openMode(HospitalMode mode, String cardName) {
        if (!canOpen(mode)) {
            modePanel.showError("当前账号没有进入该模式的权限，请重试或重新进入校医院。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, cardName);
        if (mode == HospitalMode.PATIENT) {
            refreshPatientHome();
            showGuideForFirstPatientVisit();
        } else if (mode == HospitalMode.DOCTOR) {
            doctorWorkspacePanel.activate();
        }
    }

    private void refreshPatientHome() {
        int version = ++patientHomeRequestVersion;
        homePanel.showCareTaskLoading();
        homePanel.showBillTaskLoading();
        if (!canOpen(HospitalMode.PATIENT)) {
            return;
        }
        refreshPatientBills(version);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_MY_HEALTH_RECORD, null);
            }

            @Override
            protected void done() {
                if (version != patientHomeRequestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientHealthRecordView record) {
                        homePanel.showCareTasks(record);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ignored) {
                    // Core patient services remain usable when the optional task summary fails.
                }
            }
        }.execute();
    }

    private void refreshPatientBills(int version) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_MY_BILLS, null);
            }

            @Override
            protected void done() {
                if (version != patientHomeRequestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientBillListResponse bills) {
                        homePanel.showBillTasks(bills, HospitalView.this::openBills);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ignored) {
                    // Other patient services remain usable when fee summary loading fails.
                }
            }
        }.execute();
    }

    private boolean canOpen(HospitalMode mode) {
        return context.currentSession().isPresent()
                && modeAccess != null
                && modeAccess.canAccess(mode);
    }

    private void openModeSelector() {
        int requestVersion = ++accessRequestVersion;
        cards.show(this, MODE_SELECT);
        SessionInfo session = context.currentSession().orElse(null);
        if (session == null) {
            modeAccess = null;
            modePanel.showLoginRequired();
            return;
        }
        modePanel.showLoading(session);
        loadModeAccess(session, requestVersion);
    }

    private void loadModeAccess(SessionInfo requestedSession, int requestVersion) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_MODE_ACCESS, null);
            }

            @Override
            protected void done() {
                if (requestVersion != accessRequestVersion) {
                    return;
                }
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

    private void showGuideForFirstPatientVisit() {
        SessionInfo session = context.currentSession().orElse(null);
        if (session == null || !guidedUserIds.add(session.getUserId())) {
            return;
        }
        SwingUtilities.invokeLater(homePanel::showGuide);
    }
}
