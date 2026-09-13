package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextView;
import edu.seu.vcampus.common.hospital.DoctorClinicalRecordView;
import edu.seu.vcampus.common.hospital.DoctorAppointmentView;
import edu.seu.vcampus.common.hospital.DoctorFollowUpView;
import edu.seu.vcampus.common.hospital.DoctorScheduleView;
import edu.seu.vcampus.common.hospital.DoctorWorkspaceView;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.MarkAppointmentNoShowRequest;
import edu.seu.vcampus.common.hospital.PatientHealthProfileView;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/** Page-based doctor flow from schedule to queue to one patient's visit context. */
final class DoctorWorkspacePanel extends JPanel {

    private static final int AUTO_REFRESH_MILLIS = 15_000;
    private static final String SCHEDULES_PAGE = "schedules";
    private static final String QUEUE_PAGE = "queue";
    private static final String APPOINTMENT_PAGE = "appointment";
    private static final String CURRENT_VISIT_PAGE = "current-visit";
    private static final String HEALTH_PROFILE_PAGE = "health-profile";
    private static final String HISTORY_PAGE = "history";
    private static final String HISTORY_DETAIL_PAGE = "history-detail";
    private static final String EXAMINATIONS_PAGE = "examinations";
    private static final String FOLLOW_UPS_PAGE = "follow-ups";
    private static final String SIGNED_RECORDS_PAGE = "signed-records";
    private static final String CLINICAL_DETAIL_PAGE = "clinical-detail";
    private static final String FOLLOW_UP_DETAIL_PAGE = "follow-up-detail";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M月d日", Locale.CHINA);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA);

    private final ClientContext context;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final JLabel doctorIdentity = new JLabel("正在读取医生档案……");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel scheduleGrid = new ScheduleWrapPanel();
    private final JPanel followUpSummary = new JPanel(new BorderLayout());
    private final JPanel followUpList = verticalList();
    private final JPanel signedRecordSummary = new JPanel(new BorderLayout());
    private final JPanel signedRecordList = verticalList();
    private final JPanel clinicalDetailContent = new JPanel(new BorderLayout());
    private final JLabel clinicalDetailTitle = new JLabel("诊疗记录详情");
    private final JLabel clinicalDetailSubtitle = new JLabel(" ");
    private final JPanel followUpDetailContent = new JPanel(new BorderLayout());
    private final JLabel followUpDetailTitle = new JLabel("诊疗跟进详情");
    private final JLabel followUpDetailSubtitle = new JLabel(" ");
    private final JLabel followUpTitle = new JLabel("诊疗跟进");
    private final JLabel followUpSubtitle = new JLabel("开具检查后仍未结束的诊疗");
    private final JLabel queueTitle = new JLabel("候诊队列");
    private final JLabel queueSubtitle = new JLabel(" ");
    private final JPanel patientList = verticalList();
    private final JLabel appointmentTitle = new JLabel("患者接诊信息");
    private final JLabel appointmentSubtitle = new JLabel(" ");
    private final JLabel patientSafetySummary = new JLabel();
    private final JPanel appointmentContent = new JPanel(new BorderLayout(16, 0));
    private final JLabel currentVisitTitle = new JLabel("本次接诊");
    private final JLabel currentVisitSubtitle = new JLabel(" ");
    private final JPanel currentVisitContent = new JPanel(new BorderLayout(16, 0));
    private final JLabel healthProfileTitle = new JLabel("患者健康档案");
    private final JLabel healthProfileSubtitle = new JLabel(" ");
    private final JPanel healthProfileContent = new JPanel(new BorderLayout());
    private final JLabel historyTitle = new JLabel("历史就诊");
    private final JLabel historySubtitle = new JLabel(" ");
    private final JPanel historyList = verticalList();
    private final JLabel historyDetailTitle = new JLabel("历史就诊详情");
    private final JLabel historyDetailSubtitle = new JLabel(" ");
    private final JPanel historyDetailContent = new JPanel(new BorderLayout());
    private final JLabel examinationsTitle = new JLabel("检查报告");
    private final JLabel examinationsSubtitle = new JLabel(" ");
    private final JPanel examinationsList = verticalList();
    private final Timer autoRefreshTimer = new Timer(
            AUTO_REFRESH_MILLIS, event -> refreshWorkspaceData());
    private DoctorConsultationContextView currentConsultationContext;
    private String currentAppointmentId;
    private DoctorWorkspaceView workspace;
    private DoctorScheduleView currentSchedule;
    private final Map<String, ConsultationDraft> consultationDrafts = new HashMap<>();
    private int requestVersion;
    private int workspaceRefreshVersion;

    DoctorWorkspacePanel(ClientContext context, Runnable switchMode) {
        this.context = context;
        setLayout(new BorderLayout());
        setBackground(HospitalTheme.BACKGROUND);

        pages.setOpaque(false);
        pages.add(createSchedulesPage(switchMode), SCHEDULES_PAGE);
        pages.add(createQueuePage(), QUEUE_PAGE);
        pages.add(createAppointmentPage(), APPOINTMENT_PAGE);
        pages.add(createCurrentVisitPage(), CURRENT_VISIT_PAGE);
        pages.add(createHealthProfilePage(), HEALTH_PROFILE_PAGE);
        pages.add(createHistoryPage(), HISTORY_PAGE);
        pages.add(createHistoryDetailPage(), HISTORY_DETAIL_PAGE);
        pages.add(createExaminationsPage(), EXAMINATIONS_PAGE);
        pages.add(createFollowUpsPage(), FOLLOW_UPS_PAGE);
        pages.add(createSignedRecordsPage(), SIGNED_RECORDS_PAGE);
        pages.add(createClinicalDetailPage(), CLINICAL_DETAIL_PAGE);
        pages.add(createFollowUpDetailPage(), FOLLOW_UP_DETAIL_PAGE);
        add(pages, BorderLayout.CENTER);
        showLoading();
        autoRefreshTimer.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                autoRefreshTimer.restart();
            } else {
                autoRefreshTimer.stop();
                workspaceRefreshVersion++;
            }
        });
    }

    void activate() {
        int version = ++requestVersion;
        workspaceRefreshVersion++;
        showLoading();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_DOCTOR_WORKSPACE, null);
            }

            @Override
            protected void done() {
                if (version != requestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof DoctorWorkspaceView loaded) {
                        showWorkspace(loaded);
                    } else {
                        showError(failureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("医生工作台加载已中断，请重新进入医生模式。");
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动后重新进入医生模式。");
                }
            }
        }.execute();
    }

    private JPanel createSchedulesPage(Runnable switchMode) {
        JPanel page = basePage();
        page.add(createSchedulesHeader(switchMode), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        JLabel instruction = new JLabel("选择排班，进入该时段的候诊队列");
        instruction.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        instruction.setForeground(HospitalTheme.TEXT);
        JPanel lead = verticalList();
        lead.add(instruction);
        lead.add(Box.createVerticalStrut(12));
        followUpSummary.setOpaque(false);
        followUpSummary.setVisible(false);
        lead.add(followUpSummary);
        lead.add(Box.createVerticalStrut(10));
        signedRecordSummary.setOpaque(false);
        signedRecordSummary.setVisible(false);
        lead.add(signedRecordSummary);
        content.add(lead, BorderLayout.NORTH);

        scheduleGrid.setOpaque(false);
        JScrollPane scroll = scroll(scheduleGrid);
        content.add(scroll, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);

        statusLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        statusLabel.setForeground(HospitalTheme.MUTED);
        page.add(statusLabel, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createFollowUpsPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回我的排班", this::openSchedulesPage,
                followUpTitle, followUpSubtitle), BorderLayout.NORTH);
        page.add(scroll(followUpList), BorderLayout.CENTER);
        return page;
    }

    private JPanel createSignedRecordsPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回我的排班", this::openSchedulesPage,
                new JLabel("我签署的诊疗记录"),
                new JLabel("只读查看本人已保存的诊疗记录")), BorderLayout.NORTH);
        page.add(scroll(signedRecordList), BorderLayout.CENTER);
        return page;
    }

    private JPanel createClinicalDetailPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回记录列表",
                () -> pageLayout.show(pages, SIGNED_RECORDS_PAGE),
                clinicalDetailTitle, clinicalDetailSubtitle), BorderLayout.NORTH);
        page.add(scroll(clinicalDetailContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createFollowUpDetailPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回诊疗跟进",
                () -> pageLayout.show(pages, FOLLOW_UPS_PAGE),
                followUpDetailTitle, followUpDetailSubtitle), BorderLayout.NORTH);
        page.add(scroll(followUpDetailContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createQueuePage() {
        JPanel page = basePage();
        page.add(pageHeader("返回我的排班", this::openSchedulesPage,
                queueTitle, queueSubtitle), BorderLayout.NORTH);

        HospitalTheme.SurfacePanel queueSurface = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        queueSurface.setLayout(new BorderLayout(0, 14));
        queueSurface.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel heading = new JLabel("按候诊号排列");
        heading.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        heading.setForeground(HospitalTheme.TEXT);
        queueSurface.add(heading, BorderLayout.NORTH);
        queueSurface.add(scroll(patientList), BorderLayout.CENTER);
        page.add(queueSurface, BorderLayout.CENTER);
        return page;
    }

    private JPanel createAppointmentPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回候诊队列", this::openQueuePage,
                appointmentTitle, appointmentSubtitle), BorderLayout.NORTH);
        appointmentContent.setOpaque(false);
        page.add(scroll(appointmentContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createCurrentVisitPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回患者接诊主页", this::openAppointmentHomePage,
                currentVisitTitle, currentVisitSubtitle), BorderLayout.NORTH);
        currentVisitContent.setOpaque(false);
        JScrollPane visitScroll = scroll(currentVisitContent);
        visitScroll.setName("doctorCurrentVisitScrollPane");
        page.add(visitScroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel createHealthProfilePage() {
        JPanel page = basePage();
        page.add(pageHeader("返回患者接诊主页", this::openAppointmentHomePage,
                healthProfileTitle, healthProfileSubtitle), BorderLayout.NORTH);
        healthProfileContent.setOpaque(false);
        page.add(scroll(healthProfileContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createHistoryPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回患者接诊主页", this::openAppointmentHomePage,
                historyTitle, historySubtitle), BorderLayout.NORTH);
        page.add(scroll(historyList), BorderLayout.CENTER);
        return page;
    }

    private JPanel createHistoryDetailPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回历史就诊", this::openHistoryPage,
                historyDetailTitle, historyDetailSubtitle), BorderLayout.NORTH);
        historyDetailContent.setOpaque(false);
        page.add(scroll(historyDetailContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createExaminationsPage() {
        JPanel page = basePage();
        page.add(pageHeader("返回患者接诊主页", this::openAppointmentHomePage,
                examinationsTitle, examinationsSubtitle), BorderLayout.NORTH);
        page.add(scroll(examinationsList), BorderLayout.CENTER);
        return page;
    }

    private JPanel createSchedulesHeader(Runnable switchMode) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = verticalList();
        JLabel title = new JLabel("医生工作台");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 28F));
        title.setForeground(HospitalTheme.TEXT);
        doctorIdentity.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        doctorIdentity.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(doctorIdentity);
        JButton switchButton = HospitalTheme.quietButton("切换使用模式");
        switchButton.addActionListener(event -> switchMode.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(switchButton, BorderLayout.EAST);
        return header;
    }

    private JPanel pageHeader(
            String backText,
            Runnable backAction,
            JLabel title,
            JLabel subtitle) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JButton back = HospitalTheme.quietButton("‹ " + backText);
        back.setName("doctorBackButton");
        back.addActionListener(event -> backAction.run());
        JPanel copy = verticalList();
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        subtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);
        header.add(back, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        return header;
    }

    private void showLoading() {
        workspace = null;
        currentSchedule = null;
        currentConsultationContext = null;
        currentAppointmentId = null;
        doctorIdentity.setText("正在读取医生档案和排班……");
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setText("进入医生模式后自动读取最新数据。");
        scheduleGrid.removeAll();
        scheduleGrid.add(message("正在加载排班……", HospitalTheme.MUTED, 260));
        refresh(scheduleGrid);
        signedRecordSummary.removeAll();
        signedRecordSummary.setVisible(false);
        pageLayout.show(pages, SCHEDULES_PAGE);
    }

    private void showWorkspace(DoctorWorkspaceView loaded) {
        workspace = loaded;
        doctorIdentity.setText(loaded.getDoctorName() + "  ·  "
                + loaded.getDoctorTitle() + "  ·  " + loaded.getDepartmentName());
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setText("每个排班进入独立候诊队列；选择患者后再查看本次接诊信息。");
        renderScheduleCards();
        renderFollowUps();
        renderSignedRecords();
        pageLayout.show(pages, SCHEDULES_PAGE);
    }

    private void renderFollowUps() {
        followUpList.removeAll();
        followUpSummary.removeAll();
        if (workspace.getFollowUps().isEmpty()) {
            followUpSummary.setVisible(false);
            refresh(followUpSummary);
            refresh(followUpList);
            return;
        }
        long readyCount = workspace.getFollowUps().stream()
                .filter(item -> item.getExaminationStatus() == ExaminationStatus.RESULT_READY)
                .count();
        HospitalTheme.SurfacePanel summary = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 14, HospitalTheme.BORDER);
        summary.setLayout(new BorderLayout(18, 0));
        summary.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel copy = new JLabel("<html><b>诊疗跟进 " + workspace.getFollowUps().size()
                + " 人</b><br>等待检查结果或等待完成结果回诊"
                + (readyCount > 0 ? " · 其中 " + readyCount + " 项结果已出" : "")
                + "</html>");
        copy.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        copy.setForeground(HospitalTheme.PRIMARY_DARK);
        JButton open = HospitalTheme.primaryButton("查看诊疗跟进");
        open.setName("openDoctorFollowUpsButton");
        open.addActionListener(event -> pageLayout.show(pages, FOLLOW_UPS_PAGE));
        summary.add(copy, BorderLayout.CENTER);
        summary.add(open, BorderLayout.EAST);
        followUpSummary.add(summary, BorderLayout.CENTER);
        followUpSummary.setVisible(true);

        for (DoctorFollowUpView followUp : workspace.getFollowUps()) {
            followUpList.add(followUpCard(followUp));
            followUpList.add(Box.createVerticalStrut(12));
        }
        refresh(followUpSummary);
        refresh(followUpList);
    }

    private void renderSignedRecords() {
        signedRecordSummary.removeAll();
        signedRecordList.removeAll();
        if (workspace == null || workspace.getSignedRecords().isEmpty()) {
            signedRecordSummary.setVisible(false);
            refresh(signedRecordSummary);
            refresh(signedRecordList);
            return;
        }
        HospitalTheme.SurfacePanel summary = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        summary.setLayout(new BorderLayout(18, 0));
        summary.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel copy = new JLabel("<html><b>我签署的诊疗记录</b><br>最近 "
                + workspace.getSignedRecords().size() + " 条，可在接诊结束后只读查看</html>");
        copy.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        copy.setForeground(HospitalTheme.TEXT);
        JButton open = HospitalTheme.quietButton("查看签署记录");
        open.setName("openDoctorSignedRecordsButton");
        open.addActionListener(event -> pageLayout.show(pages, SIGNED_RECORDS_PAGE));
        summary.add(copy, BorderLayout.CENTER);
        summary.add(open, BorderLayout.EAST);
        signedRecordSummary.add(summary, BorderLayout.CENTER);
        signedRecordSummary.setVisible(true);

        for (DoctorClinicalRecordView record : workspace.getSignedRecords()) {
            signedRecordList.add(signedRecordCard(record));
            signedRecordList.add(Box.createVerticalStrut(12));
        }
        refresh(signedRecordSummary);
        refresh(signedRecordList);
    }

    private JPanel signedRecordCard(DoctorClinicalRecordView clinicalRecord) {
        ConsultationRecordView record = clinicalRecord.getConsultation();
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        JPanel copy = verticalList();
        JLabel title = new JLabel(record.getPatientUserId() + "  ·  "
                + record.getDepartmentName() + "  ·  "
                + DATE_TIME_FORMAT.format(record.getCreatedAt()));
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        title.setForeground(HospitalTheme.TEXT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(message("诊断：" + record.getDiagnosisOpinion(),
                HospitalTheme.MUTED, 560));
        JButton open = HospitalTheme.primaryButton("查看完整记录");
        open.setName("openDoctorSignedRecordDetailButton");
        open.addActionListener(event -> showSignedRecordDetail(clinicalRecord));
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, open, 540, 14),
                BorderLayout.CENTER);
        return card;
    }

    private void showSignedRecordDetail(DoctorClinicalRecordView clinicalRecord) {
        ConsultationRecordView record = clinicalRecord.getConsultation();
        clinicalDetailTitle.setText(record.getPatientUserId() + " · 诊疗记录");
        clinicalDetailSubtitle.setText(record.getDepartmentName() + "  ·  签署于 "
                + DATE_TIME_FORMAT.format(record.getCreatedAt()));
        clinicalDetailContent.removeAll();
        clinicalDetailContent.add(clinicalRecordPanel(clinicalRecord), BorderLayout.NORTH);
        refresh(clinicalDetailContent);
        pageLayout.show(pages, CLINICAL_DETAIL_PAGE);
    }

    private void showFollowUpDetail(DoctorFollowUpView followUp) {
        followUpDetailTitle.setText(followUp.getPatientUserId() + " · 诊疗跟进");
        followUpDetailSubtitle.setText(followUp.getDepartmentName() + "  ·  "
                + doctorFollowUpStatus(followUp));
        followUpDetailContent.removeAll();
        JPanel body = verticalList();
        HospitalTheme.SurfacePanel summary = informationCard("当前跟进状态");
        summary.add(detailRow("检查项目", followUp.getExaminationItem(), false));
        summary.add(Box.createVerticalStrut(12));
        summary.add(detailRow("检查状态", doctorFollowUpStatus(followUp), false));
        if (followUp.isResultReviewBooked()) {
            summary.add(Box.createVerticalStrut(12));
            summary.add(detailRow("回诊安排", followUp.getReviewDoctorName() + "  ·  "
                    + DATE_TIME_FORMAT.format(followUp.getReviewStartTime()), false));
        }
        body.add(summary);
        for (DoctorClinicalRecordView record : followUp.getEpisodeRecords()) {
            body.add(Box.createVerticalStrut(14));
            body.add(clinicalRecordPanel(record, false));
        }
        if (followUp.getEpisodeRecords().isEmpty()) {
            body.add(Box.createVerticalStrut(14));
            body.add(message("本轮暂时没有可显示的阶段诊疗记录。",
                    HospitalTheme.MUTED, 620));
        }
        if (!followUp.getEpisodeExaminations().isEmpty()) {
            body.add(Box.createVerticalStrut(14));
            body.add(sectionTitle("本轮检查资料"));
            body.add(Box.createVerticalStrut(10));
            for (ExaminationOrderView examination : followUp.getEpisodeExaminations()) {
                body.add(doctorExaminationCard(examination));
                body.add(Box.createVerticalStrut(10));
            }
        }
        followUpDetailContent.add(body, BorderLayout.NORTH);
        refresh(followUpDetailContent);
        pageLayout.show(pages, FOLLOW_UP_DETAIL_PAGE);
    }

    private JPanel clinicalRecordPanel(DoctorClinicalRecordView clinicalRecord) {
        return clinicalRecordPanel(clinicalRecord, true);
    }

    private JPanel clinicalRecordPanel(
            DoctorClinicalRecordView clinicalRecord, boolean includeExaminations) {
        ConsultationRecordView record = clinicalRecord.getConsultation();
        HospitalTheme.SurfacePanel card = informationCard(
                DATE_TIME_FORMAT.format(record.getCreatedAt()) + "  ·  "
                        + visitTypeText(record.getVisitType()));
        card.add(detailRow("患者账号", record.getPatientUserId(), true));
        card.add(Box.createVerticalStrut(12));
        card.add(detailRow("诊断 / 判断", record.getDiagnosisOpinion(), false));
        card.add(Box.createVerticalStrut(12));
        card.add(detailRow("检查 / 报告解读", record.getExaminationAdvice(), false));
        card.add(Box.createVerticalStrut(12));
        card.add(detailRow(record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                ? "检查期间注意事项" : "处置意见",
                record.getTreatmentAdvice(), false));
        if (record.getOutcome() == ConsultationOutcome.COMPLETED) {
            card.add(Box.createVerticalStrut(12));
            card.add(detailRow("用药建议", record.getMedicationAdvice(), false));
        }
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(12));
        card.add(detailRow("复诊 / 后续建议", record.getFollowUpAdvice(), false));
        if (includeExaminations && !clinicalRecord.getEpisodeExaminations().isEmpty()) {
            card.add(Box.createVerticalStrut(18));
            card.add(sectionTitle("同一诊疗过程的检查资料"));
            card.add(Box.createVerticalStrut(10));
            for (ExaminationOrderView examination : clinicalRecord.getEpisodeExaminations()) {
                card.add(doctorExaminationCard(examination));
                card.add(Box.createVerticalStrut(10));
            }
        }
        return card;
    }

    private JPanel followUpCard(DoctorFollowUpView followUp) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setName("doctorFollowUpCard");
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        JPanel copy = verticalList();
        JLabel title = new JLabel(followUp.getPatientUserId() + "  ·  "
                + followUp.getExaminationItem());
        title.setFont(HospitalTheme.dataFont(Font.BOLD, 17F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel meta = new JLabel(followUp.getDepartmentName() + "  ·  开单于 "
                + DATE_TIME_FORMAT.format(followUp.getOrderedAt()));
        meta.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        meta.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(7));
        copy.add(meta);

        HospitalTheme.SurfacePanel state = new HospitalTheme.SurfacePanel(
                followUp.getExaminationStatus() == ExaminationStatus.RESULT_READY
                        ? HospitalTheme.PRIMARY_LIGHT : HospitalTheme.BACKGROUND,
                12,
                HospitalTheme.BORDER);
        state.setLayout(new BoxLayout(state, BoxLayout.Y_AXIS));
        state.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel stateText = new JLabel(doctorFollowUpStatus(followUp));
        stateText.setName("doctorFollowUpStatus");
        stateText.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
        stateText.setForeground(followUp.getExaminationStatus() == ExaminationStatus.RESULT_READY
                ? HospitalTheme.PRIMARY_DARK : HospitalTheme.MUTED);
        state.add(stateText);
        if (followUp.isResultReviewBooked()) {
            state.add(Box.createVerticalStrut(4));
            JLabel appointment = new JLabel(followUp.getReviewDoctorName() + "  ·  "
                    + DATE_TIME_FORMAT.format(followUp.getReviewStartTime()));
            appointment.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
            appointment.setForeground(HospitalTheme.MUTED);
            state.add(appointment);
        }
        state.add(Box.createVerticalStrut(8));
        JButton details = HospitalTheme.quietButton("查看详情");
        details.setName("openDoctorFollowUpDetailButton");
        details.addActionListener(event -> showFollowUpDetail(followUp));
        state.add(details);
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, state, 560, 14),
                BorderLayout.CENTER);
        return card;
    }

    private void renderScheduleCards() {
        scheduleGrid.removeAll();
        List<DoctorScheduleView> visibleSchedules = workspace.getSchedules().stream()
                .filter(schedule -> !schedule.getPendingAppointments().isEmpty()
                        || schedule.getRemaining() > 0)
                .toList();
        if (visibleSchedules.isEmpty()) {
            scheduleGrid.add(message(
                    "近期没有待接诊患者或可用号源。新排班发布后，重新进入医生模式会自动显示。",
                    HospitalTheme.MUTED,
                    420));
        } else {
            for (DoctorScheduleView schedule : visibleSchedules) {
                scheduleGrid.add(scheduleCard(schedule));
            }
        }
        refresh(scheduleGrid);
    }

    private JPanel scheduleCard(DoctorScheduleView schedule) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(17, 18, 16, 18));
        card.setPreferredSize(new Dimension(350, 190));

        JPanel dateBlock = verticalList();
        JLabel date = new JLabel(DATE_FORMAT.format(schedule.getStartTime()) + "  "
                + schedule.getStartTime().getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, Locale.CHINA));
        date.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        date.setForeground(HospitalTheme.TEXT);
        JLabel time = new JLabel(TIME_FORMAT.format(schedule.getStartTime()) + "–"
                + TIME_FORMAT.format(schedule.getEndTime()));
        time.setFont(HospitalTheme.dataFont(Font.BOLD, 17F));
        time.setForeground(HospitalTheme.PRIMARY);
        dateBlock.add(date);
        dateBlock.add(Box.createVerticalStrut(4));
        dateBlock.add(time);

        JLabel facts = new JLabel("<html>" + html(schedule.getDepartmentName())
                + "<br><b>待接诊 " + schedule.getPendingAppointments().size()
                + " 人</b>　·　余号 " + schedule.getRemaining() + "/"
                + schedule.getCapacity() + "</html>");
        facts.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        facts.setForeground(HospitalTheme.TEXT);

        JButton open = HospitalTheme.primaryButton("查看候诊队列");
        open.setName("doctorScheduleButton");
        open.setActionCommand(schedule.getScheduleId());
        open.addActionListener(event -> openQueue(schedule));
        card.add(dateBlock, BorderLayout.NORTH);
        card.add(facts, BorderLayout.CENTER);
        card.add(open, BorderLayout.SOUTH);
        return card;
    }

    private void openQueue(DoctorScheduleView schedule) {
        currentSchedule = schedule;
        queueTitle.setText(schedule.getDepartmentName() + " · 候诊队列");
        queueSubtitle.setText(DATE_FORMAT.format(schedule.getStartTime()) + " "
                + TIME_FORMAT.format(schedule.getStartTime()) + "–"
                + TIME_FORMAT.format(schedule.getEndTime()) + "  ·  待接诊 "
                + schedule.getPendingAppointments().size() + " 人");
        patientList.removeAll();
        if (schedule.getPendingAppointments().isEmpty()) {
            patientList.add(message(
                    "这个排班暂时没有待接诊患者。患者完成预约后，重新进入医生模式即可看到。",
                    HospitalTheme.MUTED,
                    520));
        } else {
            for (DoctorAppointmentView appointment : schedule.getPendingAppointments()) {
                patientList.add(patientCard(schedule, appointment));
                patientList.add(Box.createVerticalStrut(12));
            }
        }
        refresh(patientList);
        pageLayout.show(pages, QUEUE_PAGE);
    }

    private JPanel patientCard(
            DoctorScheduleView schedule,
            DoctorAppointmentView appointment) {
        boolean resultReview = appointment.getVisitType() == VisitType.RESULT_REVIEW;
        boolean ordinaryFollowUp = appointment.getVisitType() == VisitType.FOLLOW_UP;
        Color appointmentAccent = resultReview
                ? HospitalTheme.PRIMARY
                : ordinaryFollowUp ? HospitalTheme.WARNING : HospitalTheme.BORDER;
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE,
                14,
                appointmentAccent);
        card.setName("doctorAppointmentCard");
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 15, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        HospitalTheme.SurfacePanel queue = new HospitalTheme.SurfacePanel(
                resultReview
                        ? HospitalTheme.PRIMARY
                        : ordinaryFollowUp
                                ? HospitalTheme.WARNING_LIGHT
                                : HospitalTheme.PRIMARY_LIGHT,
                12);
        queue.setPreferredSize(new Dimension(105, 80));
        queue.setLayout(new BoxLayout(queue, BoxLayout.Y_AXIS));
        queue.setBorder(BorderFactory.createEmptyBorder(11, 10, 10, 10));
        JLabel queueCaption = centered(
                resultReview ? "结果回诊" : ordinaryFollowUp ? "普通复诊" : "候诊",
                HospitalTheme.uiFont(Font.BOLD, 12F),
                resultReview
                        ? new Color(218, 247, 243)
                        : ordinaryFollowUp ? HospitalTheme.WARNING : HospitalTheme.MUTED);
        JLabel queueNumber = centered(String.format("%02d", appointment.getQueueNumber()),
                HospitalTheme.dataFont(Font.BOLD, 28F),
                resultReview
                        ? Color.WHITE
                        : ordinaryFollowUp ? HospitalTheme.WARNING : HospitalTheme.PRIMARY_DARK);
        queue.add(queueCaption);
        queue.add(Box.createVerticalStrut(2));
        queue.add(queueNumber);

        JPanel copy = verticalList();
        JLabel patient = new JLabel(appointment.getPatientUserId());
        patient.setFont(HospitalTheme.dataFont(Font.BOLD, 16F));
        patient.setForeground(HospitalTheme.TEXT);
        JLabel meta = new JLabel(visitTypeText(appointment.getVisitType()) + "  ·  "
                + statusText(appointment.getAppointmentStatus()) + "  ·  预约于 "
                + DATE_TIME_FORMAT.format(appointment.getBookedAt()));
        meta.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        meta.setForeground(HospitalTheme.MUTED);
        copy.add(patient);
        copy.add(Box.createVerticalStrut(7));
        copy.add(meta);

        JButton open = HospitalTheme.primaryButton("查看接诊信息");
        open.setName("doctorAppointmentButton");
        open.setActionCommand(appointment.getAppointmentId());
        open.addActionListener(event -> openAppointment(schedule, appointment));
        JPanel actions = verticalList();
        actions.add(open);
        if (!LocalDateTime.now().isBefore(schedule.getEndTime())) {
            actions.add(Box.createVerticalStrut(8));
            JButton noShow = HospitalTheme.quietButton("标记未到诊");
            noShow.setName("markAppointmentNoShowButton");
            noShow.setActionCommand(appointment.getAppointmentId());
            noShow.addActionListener(event -> markAppointmentNoShow(
                    schedule, appointment, open, noShow));
            actions.add(noShow);
        }
        card.add(queue, BorderLayout.WEST);
        JPanel adaptiveContent = HospitalResponsiveLayout.adaptiveRow(
                copy, actions, 500, 14);
        adaptiveContent.setName("doctorAppointmentAdaptiveContent");
        card.add(adaptiveContent, BorderLayout.CENTER);
        return card;
    }

    private void markAppointmentNoShow(
            DoctorScheduleView schedule,
            DoctorAppointmentView appointment,
            JButton openButton,
            JButton noShowButton) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确认将该患者标记为未到诊？\n\n患者账号："
                        + appointment.getPatientUserId()
                        + "\n预约编号：" + appointment.getAppointmentId()
                        + "\n排班：" + DATE_FORMAT.format(schedule.getStartTime()) + " "
                        + TIME_FORMAT.format(schedule.getStartTime()) + "–"
                        + TIME_FORMAT.format(schedule.getEndTime())
                        + "\n\n确认后不能继续接诊该预约，挂号费不退，也不会生成诊疗费。",
                "标记未到诊",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        openButton.setEnabled(false);
        noShowButton.setEnabled(false);
        noShowButton.setText("正在处理……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.MARK_APPOINTMENT_NO_SHOW,
                        new MarkAppointmentNoShowRequest(appointment.getAppointmentId()));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof DoctorAppointmentView updated
                            && updated.getAppointmentStatus() == AppointmentStatus.NO_SHOW) {
                        consultationDrafts.remove(updated.getAppointmentId());
                        removeCompletedAppointment(updated.getAppointmentId());
                        openQueue(currentSchedule);
                        statusLabel.setForeground(HospitalTheme.SUCCESS);
                        statusLabel.setText("已标记未到诊，候诊队列已自动更新。");
                        refreshWorkspaceData();
                    } else {
                        openButton.setEnabled(true);
                        noShowButton.setEnabled(true);
                        noShowButton.setText("标记未到诊");
                        JOptionPane.showMessageDialog(
                                DoctorWorkspacePanel.this,
                                noShowFailureMessage(response),
                                "处理失败",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    restoreNoShowButtons(openButton, noShowButton);
                } catch (ExecutionException exception) {
                    restoreNoShowButtons(openButton, noShowButton);
                    JOptionPane.showMessageDialog(
                            DoctorWorkspacePanel.this,
                            "无法连接服务器，请确认服务器已经启动。",
                            "处理失败",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private static void restoreNoShowButtons(
            JButton openButton,
            JButton noShowButton) {
        openButton.setEnabled(true);
        noShowButton.setEnabled(true);
        noShowButton.setText("标记未到诊");
    }

    private static String noShowFailureMessage(Response response) {
        if (ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND.equals(response.getCode())) {
            return "该预约不存在，或不属于当前医生。";
        }
        if (ErrorCodes.HOSPITAL_APPOINTMENT_NOT_NO_SHOW.equals(response.getCode())) {
            return "当前预约不能标记为未到诊；可能排班尚未结束，或预约状态已经变化。";
        }
        return "标记未到诊失败：" + response.getMessage();
    }

    private void openAppointment(
            DoctorScheduleView schedule,
            DoctorAppointmentView appointment) {
        currentSchedule = schedule;
        currentConsultationContext = null;
        currentAppointmentId = appointment.getAppointmentId();
        appointmentTitle.setText("患者 " + appointment.getPatientUserId());
        appointmentSubtitle.setText("候诊 " + appointment.getQueueNumber()
                + " 号  ·  " + visitTypeText(appointment.getVisitType())
                + "  ·  " + schedule.getDepartmentName() + "  ·  "
                + DATE_FORMAT.format(schedule.getStartTime()) + " "
                + TIME_FORMAT.format(schedule.getStartTime()));
        appointmentContent.removeAll();
        appointmentContent.add(message(
                "正在核验医生与本次预约的关系，并加载患者接诊背景……",
                HospitalTheme.MUTED,
                520), BorderLayout.NORTH);
        refresh(appointmentContent);
        pageLayout.show(pages, APPOINTMENT_PAGE);
        loadConsultationContext(appointment.getAppointmentId());
    }

    private void loadConsultationContext(String appointmentId) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                        new DoctorConsultationContextRequest(appointmentId));
            }

            @Override
            protected void done() {
                if (!appointmentId.equals(currentAppointmentId)) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData()
                            instanceof DoctorConsultationContextView loaded) {
                        renderAppointmentContentIfCurrent(appointmentId, loaded);
                    } else {
                        showConsultationError(response);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showConsultationError("接诊信息加载已中断，请返回候诊队列后重试。");
                } catch (ExecutionException exception) {
                    showConsultationError("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    void renderAppointmentContent(
            DoctorConsultationContextView consultationContext) {
        currentAppointmentId = consultationContext.getAppointment().getAppointmentId();
        currentConsultationContext = consultationContext;
        appointmentContent.removeAll();
        DoctorAppointmentView appointment = consultationContext.getAppointment();
        PatientHealthProfileView profile = consultationContext.getHealthProfile();
        HospitalTheme.SurfacePanel alert = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 16);
        alert.setLayout(new BorderLayout(20, 0));
        alert.setBorder(BorderFactory.createEmptyBorder(17, 20, 17, 20));
        updatePatientSafetySummary(profile);
        alert.add(patientSafetySummary, BorderLayout.CENTER);

        JPanel index = HospitalResponsiveLayout.grid(2, 320, 16, 16);
        index.setOpaque(false);
        index.add(patientIndexCard(
                "本次接诊",
                visitTypeText(appointment.getVisitType()) + "  ·  候诊 "
                        + appointment.getQueueNumber() + " 号",
                "进入诊断与处置",
                "openCurrentVisitButton",
                this::openCurrentVisitPage,
                true));
        index.add(patientIndexCard(
                "患者自述健康档案",
                "血型、过敏、既往情况和长期用药",
                "查看健康档案",
                "openDoctorHealthProfileButton",
                this::openHealthProfilePage,
                true));
        index.add(patientIndexCard(
                "历史就诊",
                consultationContext.getPreviousConsultations().isEmpty()
                        ? "暂无既往诊疗记录"
                        : consultationContext.getPreviousConsultations().size() + " 条记录",
                "查看历史就诊",
                "openDoctorHistoryButton",
                this::openHistoryPage,
                true));
        index.add(patientIndexCard(
                "检查报告",
                consultationContext.getEpisodeExaminations().isEmpty()
                        ? "本轮诊疗尚无检查单"
                        : consultationContext.getEpisodeExaminations().size()
                                + " 项本轮检查资料",
                consultationContext.getEpisodeExaminations().isEmpty()
                        ? "暂无检查资料" : "查看检查报告",
                "openDoctorReportsButton",
                consultationContext.getEpisodeExaminations().isEmpty()
                        ? null : this::openExaminationsPage,
                !consultationContext.getEpisodeExaminations().isEmpty()));

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(alert, BorderLayout.NORTH);
        JPanel navigation = verticalList();
        ConsultationRecordView source = sourceConsultation(consultationContext);
        if (source != null) {
            navigation.add(sourceConsultationCard(source));
            navigation.add(Box.createVerticalStrut(16));
        }
        navigation.add(index);
        body.add(navigation, BorderLayout.CENTER);
        appointmentContent.add(body, BorderLayout.CENTER);
        renderCurrentVisit(consultationContext);
        renderHealthProfile(consultationContext);
        renderHistory(consultationContext);
        renderExaminations(consultationContext);
        refresh(appointmentContent);
    }

    void renderAppointmentContentIfCurrent(
            String requestedAppointmentId,
            DoctorConsultationContextView consultationContext) {
        if (!requestedAppointmentId.equals(currentAppointmentId)
                || !requestedAppointmentId.equals(
                        consultationContext.getAppointment().getAppointmentId())) {
            return;
        }
        renderAppointmentContent(consultationContext);
    }

    private ConsultationRecordView sourceConsultation(
            DoctorConsultationContextView consultationContext) {
        DoctorAppointmentView appointment = consultationContext.getAppointment();
        if (appointment.getVisitType() != VisitType.FOLLOW_UP
                || appointment.getSourceAppointmentId() == null) {
            return null;
        }
        return consultationContext.getPreviousConsultations().stream()
                .filter(record -> record.getAppointmentId()
                        .equals(appointment.getSourceAppointmentId()))
                .findFirst()
                .orElse(null);
    }

    private JPanel sourceConsultationCard(ConsultationRecordView source) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.WARNING_LIGHT, 14, HospitalTheme.WARNING);
        card.setName("doctorFollowUpSourceCard");
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        JPanel copy = verticalList();
        JLabel heading = new JLabel("普通复诊 · 已关联上次诊疗");
        heading.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        heading.setForeground(HospitalTheme.WARNING);
        JTextArea summary = message(
                DATE_TIME_FORMAT.format(source.getCreatedAt()) + "  ·  "
                        + source.getDoctorName() + "  ·  诊断："
                        + source.getDiagnosisOpinion(),
                HospitalTheme.MUTED,
                660);
        copy.add(heading);
        copy.add(Box.createVerticalStrut(6));
        copy.add(summary);
        JButton open = HospitalTheme.quietButton("查看关联记录");
        open.setName("openFollowUpSourceButton");
        open.addActionListener(event -> showHistoryDetail(source));
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, open, 540, 14),
                BorderLayout.CENTER);
        return card;
    }

    private JPanel patientIndexCard(
            String titleText,
            String description,
            String actionText,
            String actionName,
            Runnable action,
            boolean primary) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        card.setPreferredSize(new Dimension(350, 168));
        JPanel copy = verticalList();
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 19F));
        title.setForeground(HospitalTheme.TEXT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(message(description, HospitalTheme.MUTED, 310));
        JButton button = primary
                ? HospitalTheme.primaryButton(actionText)
                : HospitalTheme.quietButton(actionText);
        button.setName(actionName);
        button.setEnabled(action != null);
        if (action != null) {
            button.addActionListener(event -> action.run());
        }
        card.add(copy, BorderLayout.CENTER);
        card.add(button, BorderLayout.SOUTH);
        return card;
    }

    private void renderCurrentVisit(DoctorConsultationContextView contextView) {
        DoctorAppointmentView appointment = contextView.getAppointment();
        currentVisitTitle.setText("本次接诊");
        currentVisitSubtitle.setText(appointment.getPatientUserId() + "  ·  "
                + contextView.getDepartmentName() + "  ·  候诊 "
                + appointment.getQueueNumber() + " 号");
        currentVisitContent.removeAll();

        HospitalTheme.SurfacePanel booking = informationCard("本次预约");
        booking.add(detailRow("预约编号", appointment.getAppointmentId(), true));
        booking.add(Box.createVerticalStrut(14));
        booking.add(detailRow("患者账号", appointment.getPatientUserId(), true));
        booking.add(Box.createVerticalStrut(14));
        booking.add(detailRow("科室", contextView.getDepartmentName(), false));
        booking.add(Box.createVerticalStrut(14));
        booking.add(detailRow(
                "就诊时间",
                DATE_TIME_FORMAT.format(contextView.getStartTime()) + "–"
                        + TIME_FORMAT.format(contextView.getEndTime()),
                false));
        booking.add(Box.createVerticalStrut(14));
        booking.add(detailRow("候诊序号", appointment.getQueueNumber() + " 号", false));
        booking.add(Box.createVerticalStrut(14));
        booking.add(detailRow("就诊类型", visitTypeText(appointment.getVisitType()), false));
        JPanel columns = HospitalResponsiveLayout.grid(2, 360, 16, 16);
        columns.setName("doctorCurrentVisitResponsiveColumns");
        columns.setOpaque(false);
        columns.add(booking);
        columns.add(consultationForm(contextView));
        if (appointment.getVisitType() == VisitType.RESULT_REVIEW
                && !contextView.getEpisodeExaminations().isEmpty()) {
            ExaminationOrderView examination = contextView
                    .getEpisodeExaminations().getFirst();
            HospitalTheme.SurfacePanel report = new HospitalTheme.SurfacePanel(
                    HospitalTheme.SUCCESS_LIGHT, 14);
            report.setLayout(new BorderLayout());
            report.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
            JLabel reportText = new JLabel("<html><b>本次为检查结果回诊</b><br>"
                    + html(examination.getItemName()) + "："
                    + html(examination.getResultSummary()) + "</html>");
            reportText.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
            reportText.setForeground(HospitalTheme.TEXT);
            report.add(reportText, BorderLayout.CENTER);
            currentVisitContent.add(report, BorderLayout.NORTH);
        }
        currentVisitContent.add(columns, BorderLayout.CENTER);
        refresh(currentVisitContent);
    }

    private void renderHealthProfile(DoctorConsultationContextView contextView) {
        PatientHealthProfileView profile = contextView.getHealthProfile();
        healthProfileTitle.setText("患者健康档案");
        healthProfileSubtitle.setText(contextView.getAppointment().getPatientUserId()
                + "  ·  患者自述信息");
        healthProfileContent.removeAll();
        HospitalTheme.SurfacePanel card = informationCard("患者自述健康摘要");
        card.add(detailRow("血型", profile.getBloodType(), false));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("过敏信息", profile.getAllergies(), false));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("既往情况", profile.getMedicalHistory(), false));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("长期用药", profile.getLongTermMedication(), false));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("紧急联系人", profile.getEmergencyContact(), false));
        card.add(Box.createVerticalStrut(22));
        if (hasPatientProfileData(profile)) {
            card.add(message(
                    "最近更新：" + DATE_TIME_FORMAT.format(profile.getUpdatedAt())
                            + "。这些内容由患者自述，需结合本次问诊核实。",
                    HospitalTheme.MUTED,
                    680));
        } else {
            card.add(message(
                    "患者尚未填写健康档案。过敏史和长期用药等安全信息请在接诊时当面核对。",
                    HospitalTheme.WARNING,
                    680));
        }
        healthProfileContent.add(card, BorderLayout.NORTH);
        refresh(healthProfileContent);
    }

    private void renderHistory(DoctorConsultationContextView contextView) {
        historyTitle.setText("历史就诊");
        historySubtitle.setText(contextView.getAppointment().getPatientUserId()
                + "  ·  按签署时间从近到远排列");
        historyList.removeAll();
        if (contextView.getPreviousConsultations().isEmpty()) {
            historyList.add(message(
                    "该患者暂无既往诊疗记录，可直接返回本次接诊。",
                    HospitalTheme.MUTED,
                    620));
        } else {
            for (ConsultationRecordView record : contextView.getPreviousConsultations()) {
                historyList.add(previousConsultationCard(record));
                historyList.add(Box.createVerticalStrut(12));
            }
        }
        refresh(historyList);
    }

    private void renderExaminations(DoctorConsultationContextView contextView) {
        examinationsTitle.setText("本轮检查报告");
        examinationsSubtitle.setText(contextView.getAppointment().getPatientUserId()
                + "  ·  仅显示当前诊疗过程关联的检查");
        examinationsList.removeAll();
        if (contextView.getEpisodeExaminations().isEmpty()) {
            examinationsList.add(message(
                    "本轮诊疗尚未开具检查单。",
                    HospitalTheme.MUTED,
                    620));
        } else {
            for (ExaminationOrderView examination
                    : contextView.getEpisodeExaminations()) {
                examinationsList.add(doctorExaminationCard(examination));
                examinationsList.add(Box.createVerticalStrut(12));
            }
        }
        refresh(examinationsList);
    }

    private JPanel doctorExaminationCard(ExaminationOrderView examination) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setName("doctorExaminationCard");
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        JPanel copy = verticalList();
        JLabel title = new JLabel(examination.getItemName() + "  ·  "
                + examinationStatusText(examination.getStatus()));
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        title.setForeground(HospitalTheme.TEXT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(9));
        copy.add(message(
                "检查说明：" + examination.getInstructions(),
                HospitalTheme.MUTED,
                720));
        copy.add(Box.createVerticalStrut(7));
        copy.add(message(
                "检查结果：" + examination.getResultSummary(),
                examination.getStatus() == ExaminationStatus.ORDERED
                        ? HospitalTheme.MUTED : HospitalTheme.TEXT,
                720));
        card.add(copy, BorderLayout.CENTER);
        return card;
    }

    private HospitalTheme.SurfacePanel consultationForm(
            DoctorConsultationContextView consultationContext) {
        HospitalTheme.SurfacePanel form = informationCard("本次诊疗记录");
        form.setName("doctorConsultationForm");
        JTextArea explanation = message(
                consultationContext.getAppointment().getVisitType() == VisitType.RESULT_REVIEW
                        ? "请先解读本轮检查报告；可以完成诊疗，也可以再次开检查并等待下次回诊。"
                        : "可以直接完成诊疗，也可以开具一项检查并等待患者回诊。",
                HospitalTheme.MUTED,
                480);
        form.add(explanation);
        form.add(Box.createVerticalStrut(14));

        JTextArea diagnosis = textArea("doctorDiagnosis", 3);
        JTextField examinationItem = new JTextField();
        examinationItem.setName("doctorExaminationItem");
        examinationItem.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        JTextArea examination = textArea("doctorExamination", 2);
        JTextArea nextExaminationInstructions =
                consultationContext.getAppointment().getVisitType() == VisitType.RESULT_REVIEW
                        ? textArea("doctorNextExaminationInstructions", 2)
                        : examination;
        JTextArea interimCare = textArea("doctorInterimCare", 2);
        JTextArea treatment = textArea("doctorTreatment", 3);
        JTextArea medication = textArea("doctorMedication", 2);
        JTextArea followUp = textArea("doctorFollowUp", 2);
        boolean resultReview = consultationContext.getAppointment().getVisitType()
                == VisitType.RESULT_REVIEW;
        String appointmentId = consultationContext.getAppointment().getAppointmentId();
        ConsultationDraft restoredDraft = consultationDrafts.get(appointmentId);
        restoreConsultationDraft(
                restoredDraft,
                resultReview,
                diagnosis,
                examination,
                examinationItem,
                nextExaminationInstructions,
                interimCare,
                treatment,
                medication,
                followUp);
        form.add(formField(
                consultationContext.getAppointment().getVisitType() == VisitType.RESULT_REVIEW
                        ? "检查结果解读 / 修正判断 *"
                        : "诊断意见 / 初步判断 *",
                diagnosis));
        form.add(Box.createVerticalStrut(12));
        if (consultationContext.getAppointment().getVisitType()
                == VisitType.RESULT_REVIEW) {
            form.add(formField("本次报告解读补充", examination));
            form.add(Box.createVerticalStrut(12));
            form.add(message(
                    "如果现有结果仍不足以完成诊疗，可以在下面开具下一轮检查。",
                    HospitalTheme.MUTED,
                    480));
            form.add(Box.createVerticalStrut(12));
            form.add(formField("再次检查项目（再次开检查时必填）", examinationItem));
            form.add(Box.createVerticalStrut(12));
            form.add(formField("再次检查说明（可选）", nextExaminationInstructions));
            form.add(Box.createVerticalStrut(12));
            form.add(formField("下一轮检查期间注意事项（可选）", interimCare));
            form.add(Box.createVerticalStrut(12));
            form.add(message(
                    "选择“再次开检查”时，上一张检查单会标记为已回诊解读，"
                            + "本次不形成最终处置，诊疗过程继续等待下一轮结果。",
                    HospitalTheme.MUTED,
                    480));
            form.add(Box.createVerticalStrut(12));
        } else {
            form.add(formField("检查项目（开检查时必填）", examinationItem));
            form.add(Box.createVerticalStrut(12));
            form.add(formField("检查说明（开检查时可选）", examination));
            form.add(Box.createVerticalStrut(12));
            form.add(formField("检查期间注意事项（可选）", interimCare));
            form.add(Box.createVerticalStrut(12));
            form.add(message(
                    "以下内容只在“完成接诊并保存”时作为正式处置保存；"
                            + "选择“开检查并等待回诊”时不会保存这些内容。",
                    HospitalTheme.MUTED,
                    480));
            form.add(Box.createVerticalStrut(12));
        }
        form.add(formField("正式处置意见（完成接诊时必填）", treatment));
        form.add(Box.createVerticalStrut(12));
        form.add(formField("简化用药建议", medication));
        form.add(Box.createVerticalStrut(12));
        form.add(formField("复诊建议", followUp));
        form.add(Box.createVerticalStrut(16));

        JLabel draftStatus = new JLabel();
        draftStatus.setName("doctorConsultationDraftStatus");
        draftStatus.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        updateDraftStatus(
                draftStatus,
                restoredDraft == null
                        ? "输入内容会在当前客户端运行期间自动暂存"
                        : "已恢复本次运行期间暂存的草稿",
                diagnosis, examination, examinationItem,
                nextExaminationInstructions, interimCare,
                treatment, medication, followUp);
        bindConsultationDraft(
                appointmentId,
                resultReview,
                draftStatus,
                diagnosis,
                examination,
                examinationItem,
                nextExaminationInstructions,
                interimCare,
                treatment,
                medication,
                followUp);
        form.add(draftStatus);
        form.add(Box.createVerticalStrut(8));

        JLabel formStatus = new JLabel(" ");
        formStatus.setName("doctorConsultationStatus");
        formStatus.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        formStatus.setForeground(HospitalTheme.WARNING);
        JButton orderExamination = HospitalTheme.quietButton(
                resultReview ? "再次开检查并等待下次回诊" : "开检查并等待回诊");
        orderExamination.setName("submitExaminationPlanButton");
        JButton submit = HospitalTheme.primaryButton("完成接诊并保存");
        submit.setName("submitConsultationButton");
        submit.addActionListener(event -> submitConsultation(
                consultationContext,
                diagnosis,
                examination,
                treatment,
                medication,
                followUp,
                submit,
                orderExamination,
                formStatus));
        form.add(formStatus);
        form.add(Box.createVerticalStrut(8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(submit);
        orderExamination.addActionListener(event -> submitExaminationPlan(
                consultationContext,
                diagnosis,
                examination,
                examinationItem,
                nextExaminationInstructions,
                interimCare,
                submit,
                orderExamination,
                formStatus));
        actions.add(orderExamination);
        form.add(actions);
        return form;
    }

    private JPanel previousConsultationCard(ConsultationRecordView record) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        JPanel copy = verticalList();
        JLabel heading = new JLabel(DATE_TIME_FORMAT.format(record.getCreatedAt())
                + (record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "  ·  检查阶段记录" : "")
                + "  ·  " + visitTypeText(record.getVisitType())
                + "  ·  " + record.getDepartmentName() + "  ·  "
                + record.getDoctorName());
        heading.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        heading.setForeground(HospitalTheme.TEXT);
        JTextArea diagnosis = message(
                "诊断：" + record.getDiagnosisOpinion(),
                HospitalTheme.MUTED,
                560);
        copy.add(heading);
        copy.add(Box.createVerticalStrut(8));
        copy.add(diagnosis);
        JButton open = HospitalTheme.primaryButton("查看记录详情");
        open.setName("openDoctorHistoryDetailButton");
        open.addActionListener(event -> showHistoryDetail(record));
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, open, 540, 14),
                BorderLayout.CENTER);
        return card;
    }

    private void showHistoryDetail(ConsultationRecordView record) {
        historyDetailTitle.setText(record.getDepartmentName() + "就诊记录");
        historyDetailSubtitle.setText(record.getDoctorName() + " "
                + record.getDoctorTitle() + "  ·  签署于 "
                + DATE_TIME_FORMAT.format(record.getCreatedAt()));
        historyDetailContent.removeAll();
        JPanel columns = HospitalResponsiveLayout.grid(2, 340, 16, 16);
        columns.setOpaque(false);
        HospitalTheme.SurfacePanel diagnosis = informationCard("诊断与检查");
        diagnosis.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "初步判断" : "诊断意见",
                record.getDiagnosisOpinion(),
                false));
        diagnosis.add(Box.createVerticalStrut(18));
        diagnosis.add(detailRow("检查建议", record.getExaminationAdvice(), false));
        diagnosis.add(Box.createVerticalStrut(24));
        diagnosis.add(detailRow("诊疗记录编号", record.getConsultationId(), true));

        HospitalTheme.SurfacePanel treatment = informationCard(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查期间安排" : "处置与后续");
        treatment.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查期间注意事项" : "处置意见",
                record.getTreatmentAdvice(),
                false));
        if (record.getOutcome() == ConsultationOutcome.COMPLETED) {
            treatment.add(Box.createVerticalStrut(18));
            treatment.add(detailRow("用药建议", record.getMedicationAdvice(), false));
        }
        treatment.add(Box.createVerticalStrut(18));
        treatment.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "后续步骤" : "复诊建议",
                record.getFollowUpAdvice(),
                false));
        columns.add(diagnosis);
        columns.add(treatment);
        JPanel body = verticalList();
        body.add(columns);
        List<ExaminationOrderView> historicalExaminations =
                currentConsultationContext == null ? List.of()
                        : currentConsultationContext.getPreviousClinicalRecords().stream()
                                .filter(item -> item.getConsultation().getConsultationId()
                                        .equals(record.getConsultationId()))
                                .findFirst()
                                .map(DoctorClinicalRecordView::getEpisodeExaminations)
                                .orElse(List.of());
        if (!historicalExaminations.isEmpty()) {
            body.add(Box.createVerticalStrut(18));
            body.add(sectionTitle("该次诊疗过程的检查资料"));
            body.add(Box.createVerticalStrut(10));
            for (ExaminationOrderView examination : historicalExaminations) {
                body.add(doctorExaminationCard(examination));
                body.add(Box.createVerticalStrut(10));
            }
        }
        historyDetailContent.add(body, BorderLayout.NORTH);
        refresh(historyDetailContent);
        pageLayout.show(pages, HISTORY_DETAIL_PAGE);
    }

    private void openAppointmentHomePage() {
        if (currentConsultationContext != null) {
            pageLayout.show(pages, APPOINTMENT_PAGE);
        }
    }

    private void openCurrentVisitPage() {
        if (currentConsultationContext != null) {
            pageLayout.show(pages, CURRENT_VISIT_PAGE);
        }
    }

    private void openHealthProfilePage() {
        if (currentConsultationContext == null) {
            return;
        }
        String appointmentId = currentConsultationContext
                .getAppointment().getAppointmentId();
        healthProfileContent.removeAll();
        healthProfileContent.add(message(
                "正在读取患者刚刚保存的健康信息……",
                HospitalTheme.MUTED,
                560), BorderLayout.NORTH);
        refresh(healthProfileContent);
        pageLayout.show(pages, HEALTH_PROFILE_PAGE);
        reloadHealthProfile(appointmentId);
    }

    private void reloadHealthProfile(String appointmentId) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                        new DoctorConsultationContextRequest(appointmentId));
            }

            @Override
            protected void done() {
                if (currentConsultationContext == null
                        || !currentConsultationContext.getAppointment().getAppointmentId()
                                .equals(appointmentId)) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData()
                            instanceof DoctorConsultationContextView loaded) {
                        currentConsultationContext = loaded;
                        updatePatientSafetySummary(loaded.getHealthProfile());
                        renderHealthProfile(loaded);
                    } else {
                        showHealthProfileError(consultationFailureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showHealthProfileError("健康档案加载已中断，请重新进入页面。");
                } catch (ExecutionException exception) {
                    showHealthProfileError("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void showHealthProfileError(String messageText) {
        healthProfileContent.removeAll();
        healthProfileContent.add(message(messageText, HospitalTheme.WARNING, 620),
                BorderLayout.NORTH);
        refresh(healthProfileContent);
    }

    private void updatePatientSafetySummary(PatientHealthProfileView profile) {
        if (hasPatientProfileData(profile)) {
            patientSafetySummary.setText("<html><b>接诊前重点核对</b><br>过敏信息："
                    + html(profile.getAllergies()) + "　·　长期用药："
                    + html(profile.getLongTermMedication()) + "</html>");
            patientSafetySummary.setForeground(HospitalTheme.PRIMARY_DARK);
        } else {
            patientSafetySummary.setText("<html><b>接诊前重点核对</b><br>"
                    + "患者尚未填写健康档案，请当面确认过敏史和长期用药。</html>");
            patientSafetySummary.setForeground(HospitalTheme.WARNING);
        }
        patientSafetySummary.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
    }

    private static boolean hasPatientProfileData(PatientHealthProfileView profile) {
        return List.of(
                        profile.getBloodType(),
                        profile.getAllergies(),
                        profile.getMedicalHistory(),
                        profile.getLongTermMedication(),
                        profile.getEmergencyContact())
                .stream()
                .anyMatch(value -> !"未填写".equals(value));
    }

    private void restoreConsultationDraft(
            ConsultationDraft draft,
            boolean resultReview,
            JTextArea diagnosis,
            JTextArea reportInterpretation,
            JTextField examinationItem,
            JTextArea examinationInstructions,
            JTextArea interimCare,
            JTextArea treatment,
            JTextArea medication,
            JTextArea followUp) {
        if (draft == null) {
            return;
        }
        diagnosis.setText(draft.diagnosis());
        reportInterpretation.setText(resultReview
                ? draft.reportInterpretation() : draft.examinationInstructions());
        examinationItem.setText(draft.examinationItem());
        if (resultReview) {
            examinationInstructions.setText(draft.examinationInstructions());
        }
        interimCare.setText(draft.interimCare());
        treatment.setText(draft.treatment());
        medication.setText(draft.medication());
        followUp.setText(draft.followUp());
    }

    private void bindConsultationDraft(
            String appointmentId,
            boolean resultReview,
            JLabel draftStatus,
            JTextArea diagnosis,
            JTextArea reportInterpretation,
            JTextField examinationItem,
            JTextArea examinationInstructions,
            JTextArea interimCare,
            JTextArea treatment,
            JTextArea medication,
            JTextArea followUp) {
        List<JTextComponent> inputs = new ArrayList<>();
        for (JTextComponent input : List.of(
                diagnosis,
                reportInterpretation,
                examinationItem,
                examinationInstructions,
                interimCare,
                treatment,
                medication,
                followUp)) {
            if (!inputs.contains(input)) {
                inputs.add(input);
            }
        }
        Runnable saveDraft = () -> {
            consultationDrafts.put(appointmentId, new ConsultationDraft(
                    diagnosis.getText(),
                    resultReview ? reportInterpretation.getText() : "",
                    examinationItem.getText(),
                    examinationInstructions.getText(),
                    interimCare.getText(),
                    treatment.getText(),
                    medication.getText(),
                    followUp.getText()));
            updateDraftStatus(
                    draftStatus,
                    "草稿已在当前客户端运行期间自动暂存",
                    inputs.toArray(JTextComponent[]::new));
        };
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                saveDraft.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                saveDraft.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                saveDraft.run();
            }
        };
        inputs.forEach(input -> input.getDocument().addDocumentListener(listener));
    }

    private static void updateDraftStatus(
            JLabel status,
            String prefix,
            JTextComponent... inputs) {
        int maximumLength = maximumInputLength(inputs);
        boolean tooLong = maximumLength > 1_000;
        status.setForeground(tooLong ? HospitalTheme.WARNING : HospitalTheme.MUTED);
        status.setText(tooLong
                ? "有一项内容已达 " + maximumLength
                        + "/1000 个字符，超出后不能提交"
                : prefix + " · 最长字段 " + maximumLength + "/1000");
    }

    private static boolean withinInputLimit(JTextComponent... inputs) {
        return maximumInputLength(inputs) <= 1_000;
    }

    private static int maximumInputLength(JTextComponent... inputs) {
        int maximum = 0;
        for (JTextComponent input : inputs) {
            maximum = Math.max(maximum, input.getText().length());
        }
        return maximum;
    }

    private void openHistoryPage() {
        if (currentConsultationContext != null) {
            pageLayout.show(pages, HISTORY_PAGE);
        }
    }

    private void submitExaminationPlan(
            DoctorConsultationContextView consultationContext,
            JTextArea diagnosis,
            JTextArea reportInterpretation,
            JTextField examinationItem,
            JTextArea examinationInstructions,
            JTextArea interimCare,
            JButton completeButton,
            JButton examinationButton,
            JLabel formStatus) {
        if (!withinInputLimit(
                diagnosis,
                reportInterpretation,
                examinationItem,
                examinationInstructions,
                interimCare)) {
            formStatus.setText("单项内容不能超过 1000 个字符，请根据字数提示先精简内容。");
            return;
        }
        if (diagnosis.getText().isBlank() || examinationItem.getText().isBlank()) {
            formStatus.setText("请填写判断意见和检查项目。");
            return;
        }
        boolean repeatedExamination = consultationContext.getAppointment().getVisitType()
                == VisitType.RESULT_REVIEW;
        String actionText = repeatedExamination
                ? "再次开检查并等待下次回诊" : "开检查并等待回诊";
        int choice = JOptionPane.showConfirmDialog(
                this,
                (repeatedExamination
                        ? "确认解读上一份报告并开具下一张检查单？\n\n"
                        : "确认开具检查单并结束当前排班接诊？\n\n")
                        + "患者账号：" + consultationContext.getAppointment().getPatientUserId()
                        + "\n预约编号：" + consultationContext.getAppointment().getAppointmentId()
                        + "\n\n"
                        + "本次只保存初步判断、检查单和可选注意事项，不形成正式处置。"
                        + "\n患者完成检查后将申请检查结果回诊。"
                        + "\n\n系统会同时生成 ¥30.00 的课程演示检查费，供患者在费用清单中模拟缴费。",
                actionText,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        SubmitExaminationPlanRequest request = new SubmitExaminationPlanRequest(
                consultationContext.getAppointment().getAppointmentId(),
                diagnosis.getText(),
                repeatedExamination ? reportInterpretation.getText() : "",
                examinationItem.getText(),
                examinationInstructions.getText(),
                interimCare.getText());
        completeButton.setEnabled(false);
        examinationButton.setEnabled(false);
        examinationButton.setText("正在开具……");
        formStatus.setForeground(HospitalTheme.MUTED);
        formStatus.setText("服务器正在保存阶段记录和检查单。");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.SUBMIT_EXAMINATION_PLAN, request);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof ExaminationOrderView order) {
                        String appointmentId = consultationContext.getAppointment()
                                .getAppointmentId();
                        consultationDrafts.remove(appointmentId);
                        if (appointmentId.equals(currentAppointmentId)) {
                            removeCompletedAppointment(appointmentId);
                            addFollowUp(order, consultationContext);
                            renderWaitingForResults(order);
                        }
                        refreshWorkspaceData();
                    } else {
                        completeButton.setEnabled(true);
                        examinationButton.setEnabled(true);
                        examinationButton.setText(actionText);
                        formStatus.setForeground(HospitalTheme.WARNING);
                        formStatus.setText(consultationFailureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    completeButton.setEnabled(true);
                    examinationButton.setEnabled(true);
                    examinationButton.setText(actionText);
                    formStatus.setText("保存已中断，请重试。");
                } catch (ExecutionException exception) {
                    completeButton.setEnabled(true);
                    examinationButton.setEnabled(true);
                    examinationButton.setText(actionText);
                    formStatus.setText("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void renderWaitingForResults(ExaminationOrderView order) {
        currentVisitTitle.setText("检查单已开具");
        currentVisitSubtitle.setText(order.getDepartmentName() + "  ·  "
                + order.getItemName());
        currentVisitContent.removeAll();
        HospitalTheme.SurfacePanel result = informationCard("当前诊疗等待检查结果");
        JLabel success = new JLabel("当前预约已完成，患者可以在健康档案中查看检查单。");
        success.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        success.setForeground(HospitalTheme.SUCCESS);
        result.add(success);
        result.add(Box.createVerticalStrut(20));
        result.add(detailRow("检查项目", order.getItemName(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("检查说明", order.getInstructions(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("检查状态", "待完成检查", false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("费用状态", "已生成 ¥30.00 课程演示检查费，等待患者模拟缴费", false));
        result.add(Box.createVerticalStrut(20));
        result.add(message(
                "报告出具后，患者可申请检查结果回诊；原预约不会继续留在候诊队列。",
                HospitalTheme.MUTED,
                680));
        currentVisitContent.add(result, BorderLayout.NORTH);
        refresh(currentVisitContent);
    }

    private void submitConsultation(
            DoctorConsultationContextView consultationContext,
            JTextArea diagnosis,
            JTextArea examination,
            JTextArea treatment,
            JTextArea medication,
            JTextArea followUp,
            JButton submit,
            JButton examinationButton,
            JLabel formStatus) {
        if (!withinInputLimit(
                diagnosis, examination, treatment, medication, followUp)) {
            formStatus.setText("单项内容不能超过 1000 个字符，请根据字数提示先精简内容。");
            return;
        }
        if (diagnosis.getText().isBlank() || treatment.getText().isBlank()) {
            formStatus.setText("请填写诊断意见和处置意见。");
            return;
        }
        SubmitConsultationRequest request = new SubmitConsultationRequest(
                consultationContext.getAppointment().getAppointmentId(),
                diagnosis.getText(),
                examination.getText(),
                treatment.getText(),
                medication.getText(),
                followUp.getText());
        boolean resultReview = consultationContext.getAppointment().getVisitType()
                == VisitType.RESULT_REVIEW;
        String billingNotice = resultReview
                ? "\n\n本次为检查结果回诊，不重复生成诊疗费。"
                : "\n\n系统会同时生成 ¥18.00 的课程演示诊疗费，供患者在费用清单中模拟缴费。";
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确认完成本次接诊？\n\n患者账号："
                        + consultationContext.getAppointment().getPatientUserId()
                        + "\n预约编号："
                        + consultationContext.getAppointment().getAppointmentId()
                        + "\n\n保存后诊疗记录不可直接修改，预约将变为已完成。"
                        + billingNotice,
                "完成接诊",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        submit.setEnabled(false);
        examinationButton.setEnabled(false);
        submit.setText("正在保存……");
        formStatus.setForeground(HospitalTheme.MUTED);
        formStatus.setText("服务器正在核验预约归属并保存诊疗记录。");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.SUBMIT_CONSULTATION, request);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof ConsultationRecordView record) {
                        consultationDrafts.remove(record.getAppointmentId());
                        if (record.getAppointmentId().equals(currentAppointmentId)) {
                            removeCompletedAppointment(record.getAppointmentId());
                            if (consultationContext.getAppointment().getVisitType()
                                    == VisitType.RESULT_REVIEW) {
                                removeFollowUpForEpisode(
                                        consultationContext.getEpisodeExaminations());
                            }
                            renderCompletedConsultation(record);
                        }
                        refreshWorkspaceData();
                    } else {
                        submit.setEnabled(true);
                        examinationButton.setEnabled(true);
                        submit.setText("完成接诊并保存");
                        formStatus.setForeground(HospitalTheme.WARNING);
                        formStatus.setText(consultationFailureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    submit.setEnabled(true);
                    examinationButton.setEnabled(true);
                    submit.setText("完成接诊并保存");
                    formStatus.setText("保存已中断，请重试。");
                } catch (ExecutionException exception) {
                    submit.setEnabled(true);
                    examinationButton.setEnabled(true);
                    submit.setText("完成接诊并保存");
                    formStatus.setText("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void renderCompletedConsultation(ConsultationRecordView record) {
        currentVisitTitle.setText("接诊已完成");
        currentVisitSubtitle.setText(record.getPatientUserId() + "  ·  "
                + record.getDepartmentName() + "  ·  "
                + DATE_TIME_FORMAT.format(record.getCreatedAt()));
        currentVisitContent.removeAll();
        HospitalTheme.SurfacePanel result = informationCard("诊疗记录已保存");
        JLabel success = new JLabel("预约已完成，患者现在可以在问诊记录中查看。");
        success.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        success.setForeground(HospitalTheme.SUCCESS);
        result.add(success);
        result.add(Box.createVerticalStrut(20));
        result.add(detailRow("诊断意见", record.getDiagnosisOpinion(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("检查建议", record.getExaminationAdvice(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("处置意见", record.getTreatmentAdvice(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("用药建议", record.getMedicationAdvice(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow("复诊建议", record.getFollowUpAdvice(), false));
        result.add(Box.createVerticalStrut(14));
        result.add(detailRow(
                "费用状态",
                record.getVisitType() == VisitType.RESULT_REVIEW
                        ? "检查结果回诊不重复生成诊疗费"
                        : "已生成 ¥18.00 课程演示诊疗费，等待患者模拟缴费",
                false));
        result.add(Box.createVerticalStrut(20));
        result.add(message(
                "返回候诊队列后，该患者将不再出现在待接诊列表中。",
                HospitalTheme.MUTED,
                600));
        currentVisitContent.add(result, BorderLayout.CENTER);
        refresh(currentVisitContent);
    }

    private void removeCompletedAppointment(String appointmentId) {
        if (workspace == null || currentSchedule == null) {
            return;
        }
        List<DoctorAppointmentView> remainingAppointments = currentSchedule
                .getPendingAppointments().stream()
                .filter(appointment -> !appointment.getAppointmentId().equals(appointmentId))
                .toList();
        DoctorScheduleView updatedSchedule = new DoctorScheduleView(
                currentSchedule.getScheduleId(),
                currentSchedule.getDepartmentId(),
                currentSchedule.getDepartmentName(),
                currentSchedule.getStartTime(),
                currentSchedule.getEndTime(),
                currentSchedule.getCapacity(),
                currentSchedule.getRemaining(),
                currentSchedule.isPublished(),
                remainingAppointments);
        List<DoctorScheduleView> schedules = new ArrayList<>(workspace.getSchedules());
        for (int index = 0; index < schedules.size(); index++) {
            if (schedules.get(index).getScheduleId().equals(updatedSchedule.getScheduleId())) {
                schedules.set(index, updatedSchedule);
                break;
            }
        }
        workspace = new DoctorWorkspaceView(
                workspace.getDoctorId(),
                workspace.getDoctorName(),
                workspace.getDoctorTitle(),
                workspace.getDepartmentId(),
                workspace.getDepartmentName(),
                schedules,
                workspace.getFollowUps(),
                workspace.getSignedRecords());
        currentSchedule = updatedSchedule;
        renderScheduleCards();
    }

    private void addFollowUp(
            ExaminationOrderView order,
            DoctorConsultationContextView consultationContext) {
        if (workspace == null) {
            return;
        }
        List<DoctorFollowUpView> followUps = new ArrayList<>(workspace.getFollowUps().stream()
                .filter(item -> !item.getEpisodeId().equals(order.getEpisodeId()))
                .toList());
        followUps.add(new DoctorFollowUpView(
                order.getOrderId(),
                order.getEpisodeId(),
                consultationContext.getAppointment().getPatientUserId(),
                order.getDepartmentName(),
                order.getItemName(),
                order.getStatus(),
                order.getOrderedAt(),
                false,
                null,
                null));
        workspace = new DoctorWorkspaceView(
                workspace.getDoctorId(),
                workspace.getDoctorName(),
                workspace.getDoctorTitle(),
                workspace.getDepartmentId(),
                workspace.getDepartmentName(),
                workspace.getSchedules(),
                followUps,
                workspace.getSignedRecords());
        renderFollowUps();
    }

    private void removeFollowUpForEpisode(List<ExaminationOrderView> examinations) {
        if (workspace == null || examinations.isEmpty()) {
            return;
        }
        String episodeId = examinations.getFirst().getEpisodeId();
        workspace = new DoctorWorkspaceView(
                workspace.getDoctorId(),
                workspace.getDoctorName(),
                workspace.getDoctorTitle(),
                workspace.getDepartmentId(),
                workspace.getDepartmentName(),
                workspace.getSchedules(),
                workspace.getFollowUps().stream()
                        .filter(item -> !item.getEpisodeId().equals(episodeId))
                        .toList(),
                workspace.getSignedRecords());
        renderFollowUps();
    }

    private void refreshWorkspaceData() {
        int version = ++workspaceRefreshVersion;
        String requestedUserId = currentUserId();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_DOCTOR_WORKSPACE, null);
            }

            @Override
            protected void done() {
                if (version != workspaceRefreshVersion
                        || !Objects.equals(requestedUserId, currentUserId())) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof DoctorWorkspaceView loaded) {
                        String scheduleId = currentSchedule == null
                                ? null : currentSchedule.getScheduleId();
                        workspace = loaded;
                        currentSchedule = scheduleId == null ? null : loaded.getSchedules().stream()
                                .filter(item -> item.getScheduleId().equals(scheduleId))
                                .findFirst().orElse(null);
                        renderScheduleCards();
                        renderFollowUps();
                        renderSignedRecords();
                        statusLabel.setForeground(HospitalTheme.MUTED);
                        statusLabel.setText("工作台数据已自动更新。");
                    } else {
                        statusLabel.setForeground(HospitalTheme.WARNING);
                        statusLabel.setText(failureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    statusLabel.setForeground(HospitalTheme.WARNING);
                    statusLabel.setText("自动更新失败；下次进入排班页时会再次尝试。");
                }
            }
        }.execute();
    }

    private String currentUserId() {
        return context.currentSession()
                .map(session -> session.getUserId())
                .orElse(null);
    }

    private void showConsultationError(Response response) {
        showConsultationError(consultationFailureMessage(response));
    }

    private void showConsultationError(String messageText) {
        appointmentContent.removeAll();
        appointmentContent.add(message(messageText, HospitalTheme.WARNING, 620),
                BorderLayout.NORTH);
        refresh(appointmentContent);
    }

    private static String consultationFailureMessage(Response response) {
        if (ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND.equals(response.getCode())) {
            return "该预约不存在，或不属于当前医生。";
        }
        if (ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CONSULTABLE.equals(response.getCode())) {
            return "该预约已取消或已经完成，不能再次接诊。";
        }
        if (ErrorCodes.HOSPITAL_CONSULTATION_ALREADY_EXISTS.equals(response.getCode())) {
            return "该预约已经生成诊疗记录，不能重复提交。";
        }
        if (ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID.equals(response.getCode())) {
            return "当前诊疗或检查状态已经变化，请返回候诊队列后重新打开患者。";
        }
        return "接诊操作失败：" + response.getMessage();
    }

    private static JPanel formField(String labelText, JTextArea area) {
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, area.getRows() * 34 + 28));
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.TEXT);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
        field.add(label, BorderLayout.NORTH);
        field.add(scroll, BorderLayout.CENTER);
        return field;
    }

    private static JPanel formField(String labelText, JTextField input) {
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.TEXT);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        field.add(label, BorderLayout.NORTH);
        field.add(input, BorderLayout.CENTER);
        return field;
    }

    private static JTextArea textArea(String name, int rows) {
        JTextArea area = new JTextArea(rows, 30);
        area.setName(name);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        area.setForeground(HospitalTheme.TEXT);
        area.setBackground(HospitalTheme.SURFACE);
        area.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        return area;
    }

    private static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        label.setForeground(HospitalTheme.PRIMARY_DARK);
        return label;
    }

    private HospitalTheme.SurfacePanel informationCard(String titleText) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.TEXT);
        card.add(title);
        card.add(Box.createVerticalStrut(20));
        return card;
    }

    private JPanel detailRow(String labelText, String valueText, boolean dataFont) {
        JPanel row = verticalList();
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        label.setForeground(HospitalTheme.MUTED);
        JLabel value = new JLabel(valueText);
        value.setName("doctorAppointmentDetail");
        value.setFont(dataFont
                ? HospitalTheme.dataFont(Font.PLAIN, 14F)
                : HospitalTheme.uiFont(Font.BOLD, 15F));
        value.setForeground(HospitalTheme.TEXT);
        row.add(label);
        row.add(Box.createVerticalStrut(3));
        row.add(value);
        return row;
    }

    private void openSchedulesPage() {
        currentSchedule = null;
        currentAppointmentId = null;
        currentConsultationContext = null;
        pageLayout.show(pages, SCHEDULES_PAGE);
        refreshWorkspaceData();
    }

    private void openQueuePage() {
        if (currentSchedule == null) {
            openSchedulesPage();
            return;
        }
        openQueue(currentSchedule);
    }

    private void openExaminationsPage() {
        if (currentConsultationContext != null) {
            pageLayout.show(pages, EXAMINATIONS_PAGE);
        }
    }

    private void showError(String messageText) {
        workspace = null;
        currentSchedule = null;
        doctorIdentity.setText("医生档案暂不可用");
        statusLabel.setForeground(HospitalTheme.WARNING);
        statusLabel.setText(messageText);
        scheduleGrid.removeAll();
        scheduleGrid.add(message(messageText, HospitalTheme.WARNING, 520));
        refresh(scheduleGrid);
        pageLayout.show(pages, SCHEDULES_PAGE);
    }

    private static String failureMessage(Response response) {
        if (ErrorCodes.AUTH_FORBIDDEN.equals(response.getCode())) {
            return "当前账号没有有效的医生绑定，无法查看医生工作台。";
        }
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())) {
            return "登录已失效，请重新登录后进入医生模式。";
        }
        if (ErrorCodes.COMMON_UNKNOWN_ACTION.equals(response.getCode())) {
            return "服务器仍是旧版本，请重启服务器和客户端后重试。";
        }
        return "医生工作台加载失败：" + response.getMessage();
    }

    private static JPanel basePage() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(HospitalTheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        return page;
    }

    private static JScrollPane scroll(JPanel content) {
        return HospitalResponsiveLayout.verticalScroll(content);
    }

    /** Flow layout whose preferred height follows the viewport width. */
    private static final class ScheduleWrapPanel extends JPanel
            implements javax.swing.Scrollable {

        private static final int GAP = 14;

        private ScheduleWrapPanel() {
            super(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
        }

        @Override
        public Dimension getPreferredSize() {
            int availableWidth = getParent() instanceof javax.swing.JViewport viewport
                    ? viewport.getExtentSize().width : getWidth();
            if (availableWidth <= 0) {
                return super.getPreferredSize();
            }
            int rowWidth = GAP;
            int rowHeight = 0;
            int totalHeight = GAP;
            for (java.awt.Component component : getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }
                Dimension size = component.getPreferredSize();
                if (rowWidth > GAP && rowWidth + size.width + GAP > availableWidth) {
                    totalHeight += rowHeight + GAP;
                    rowWidth = GAP;
                    rowHeight = 0;
                }
                rowWidth += size.width + GAP;
                rowHeight = Math.max(rowHeight, size.height);
            }
            totalHeight += rowHeight + GAP;
            return new Dimension(availableWidth, totalHeight);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                java.awt.Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(
                java.awt.Rectangle visibleRect, int orientation, int direction) {
            return Math.max(18, visibleRect.height - 36);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private record ConsultationDraft(
            String diagnosis,
            String reportInterpretation,
            String examinationItem,
            String examinationInstructions,
            String interimCare,
            String treatment,
            String medication,
            String followUp) {
    }

    private static JPanel verticalList() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JLabel centered(String text, Font font, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static JTextArea message(String text, Color color, int ignoredWidth) {
        return HospitalResponsiveLayout.wrappingText(
                text, HospitalTheme.uiFont(Font.PLAIN, 13F), color);
    }

    private static String visitTypeText(VisitType visitType) {
        return switch (visitType) {
            case FIRST_VISIT -> "初次就诊";
            case FOLLOW_UP -> "普通复诊";
            case RESULT_REVIEW -> "检查结果回诊";
        };
    }

    private static String examinationStatusText(ExaminationStatus status) {
        return switch (status) {
            case ORDERED -> "待完成检查";
            case RESULT_READY -> "结果已出，待医生回看";
            case REVIEWED -> "医生已回看";
            case CANCELLED -> "已取消";
        };
    }

    private static String doctorFollowUpStatus(DoctorFollowUpView followUp) {
        if (followUp.getExaminationStatus() == ExaminationStatus.ORDERED) {
            return "等待检查结果";
        }
        return followUp.isResultReviewBooked()
                ? "回诊已安排"
                : "结果已出 · 等待患者预约回诊";
    }

    private static String statusText(AppointmentStatus status) {
        return switch (status) {
            case BOOKED -> "待接诊";
            case CANCELLED -> "已取消";
            case COMPLETED -> "已完成";
            case NO_SHOW -> "未就诊";
        };
    }

    private static String html(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void refresh(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }
}
