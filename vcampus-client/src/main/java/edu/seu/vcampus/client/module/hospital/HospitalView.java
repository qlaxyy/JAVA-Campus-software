package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.event.HierarchyEvent;
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
                                "医生管理", "维护医生档案及校园账号绑定。"),
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
}
