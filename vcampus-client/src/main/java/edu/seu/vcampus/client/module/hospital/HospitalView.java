package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.event.HierarchyEvent;
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
    private static final String ADMIN_DOCTOR_DIRECTORY = "admin-doctor-directory";
    private static final String ADMIN_DOCTORS = "admin-doctors";
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
    private final AdminDoctorDirectoryPanel adminDoctorDirectoryPanel;
    private final AdminDoctorPanel adminDoctorPanel;
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
                this::openOrdinaryFollowUp,
                this::openBills);
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
                                "医生名单",
                                "在岗与已停用医生",
                                "查看医生名单",
                                this::openAdminDoctorDirectory),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "医生申请管理",
                                "申请新增或停用医生，并查看全部审核记录。",
                                "管理医生申请",
                                this::openAdminDoctors),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "排班管理",
                                "建立排班草稿，核对后发布号源，或关闭无预约排班。",
                                "管理排班",
                                this::openAdminSchedules),
                        new HospitalStaffHomePanel.WorkspaceFeature(
                                "号源与预约管理",
                                "查询预约流转，处理尚未开始的异常预约和校园卡退款。",
                                "查看预约订单",
                                this::openAdminAppointments)),
                this::openModeSelector);
        adminDepartmentPanel = new AdminDepartmentPanel(context, this::openAdminHome);
        adminDoctorDirectoryPanel = new AdminDoctorDirectoryPanel(
                context, this::openAdminHome);
        adminDoctorPanel = new AdminDoctorPanel(
                context, this::openAdminHome, this::openAdminDoctorDirectory);
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
        add(adminDoctorDirectoryPanel, ADMIN_DOCTOR_DIRECTORY);
        add(adminDoctorPanel, ADMIN_DOCTORS);
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

    private void openAdminDoctorDirectory() {
        if (!canOpen(HospitalMode.ADMIN)) {
            modePanel.showError("当前账号没有进入医院管理模式的权限。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, ADMIN_DOCTOR_DIRECTORY);
        adminDoctorDirectoryPanel.activate();
    }

    private void openAdminDoctors() {
        if (!canOpen(HospitalMode.ADMIN)) {
            modePanel.showError("当前账号没有进入医院管理模式的权限。");
            cards.show(this, MODE_SELECT);
            return;
        }
        cards.show(this, ADMIN_DOCTORS);
        adminDoctorPanel.activate();
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

    private void showGuideForFirstPatientVisit() {
        SessionInfo session = context.currentSession().orElse(null);
        if (session == null || !guidedUserIds.add(session.getUserId())) {
            return;
        }
        SwingUtilities.invokeLater(homePanel::showGuide);
    }
}
