package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PatientHealthProfileView;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.UpdatePatientHealthProfileRequest;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Patient-owned health-record hub with separate profile and history pages. */
final class PatientHealthRecordPanel extends JPanel {

    private static final String OVERVIEW_PAGE = "overview";
    private static final String PROFILE_PAGE = "profile";
    private static final String PROFILE_EDIT_PAGE = "profile-edit";
    private static final String HISTORY_PAGE = "history";
    private static final String DETAIL_PAGE = "detail";
    private static final String REPORTS_PAGE = "reports";
    private static final String REPORT_DETAIL_PAGE = "report-detail";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm");

    private final ClientContext context;
    private final Consumer<ConsultationRecordView> bookFollowUp;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JPanel overviewContent = new JPanel(new BorderLayout(0, 18));
    private final JPanel profileContent = new JPanel(new BorderLayout());
    private final JPanel profileEditContent = new JPanel(new BorderLayout());
    private final JPanel historyList = verticalList();
    private final JPanel detailContent = new JPanel(new BorderLayout());
    private final JPanel reportList = verticalList();
    private final JPanel reportDetailContent = new JPanel(new BorderLayout());
    private final JLabel reportDetailTitle = new JLabel("检查报告");
    private final JLabel reportDetailSubtitle = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel detailTitle = new JLabel("历史就诊详情");
    private final JLabel detailSubtitle = new JLabel(" ");
    private PatientHealthRecordView healthRecord;
    private boolean openPriorityReportAfterLoad;
    private boolean openHistoryAfterLoad;
    private boolean selectingOrdinaryFollowUp;
    private int requestVersion;

    PatientHealthRecordPanel(ClientContext context, Runnable backToPatientHome) {
        this(context, backToPatientHome, ignored -> { });
    }

    PatientHealthRecordPanel(
            ClientContext context,
            Runnable backToPatientHome,
            Consumer<ConsultationRecordView> bookFollowUp) {
        this.context = context;
        this.bookFollowUp = bookFollowUp;
        setLayout(new BorderLayout());
        setBackground(HospitalTheme.BACKGROUND);
        pages.setOpaque(false);
        pages.add(createOverviewPage(backToPatientHome), OVERVIEW_PAGE);
        pages.add(createProfilePage(), PROFILE_PAGE);
        pages.add(createProfileEditPage(), PROFILE_EDIT_PAGE);
        pages.add(createHistoryPage(), HISTORY_PAGE);
        pages.add(createDetailPage(), DETAIL_PAGE);
        pages.add(createReportsPage(), REPORTS_PAGE);
        pages.add(createReportDetailPage(), REPORT_DETAIL_PAGE);
        add(pages, BorderLayout.CENTER);
        showLoading();
    }

    void activate() {
        openPriorityReportAfterLoad = false;
        openHistoryAfterLoad = false;
        selectingOrdinaryFollowUp = false;
        loadHealthRecord();
    }

    private void loadHealthRecord() {
        int version = ++requestVersion;
        showLoading();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_MY_HEALTH_RECORD, null);
            }

            @Override
            protected void done() {
                if (version != requestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientHealthRecordView loaded) {
                        showHealthRecord(loaded);
                    } else {
                        showError(failureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("健康档案加载已中断，请重新进入页面。");
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动后重试。");
                }
            }
        }.execute();
    }

    void activateForFollowUp() {
        openPriorityReportAfterLoad = true;
        openHistoryAfterLoad = false;
        selectingOrdinaryFollowUp = false;
        loadHealthRecord();
    }

    void activateForOrdinaryFollowUpSelection() {
        openPriorityReportAfterLoad = false;
        openHistoryAfterLoad = true;
        selectingOrdinaryFollowUp = true;
        loadHealthRecord();
    }

    private JPanel createOverviewPage(Runnable backToPatientHome) {
        JPanel page = basePage();
        page.add(header(
                "‹ 返回患者首页",
                backToPatientHome,
                "我的健康档案",
                "从健康摘要进入患者自述信息和历次就诊"), BorderLayout.NORTH);
        overviewContent.setOpaque(false);
        page.add(scroll(overviewContent), BorderLayout.CENTER);
        statusLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        statusLabel.setForeground(HospitalTheme.MUTED);
        page.add(statusLabel, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createProfilePage() {
        JPanel page = basePage();
        page.add(header(
                "‹ 返回健康档案",
                () -> cards.show(pages, OVERVIEW_PAGE),
                "患者自述健康信息",
                "就诊时供医生参考，不替代医生诊断"), BorderLayout.NORTH);
        profileContent.setOpaque(false);
        page.add(scroll(profileContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createProfileEditPage() {
        JPanel page = basePage();
        page.add(header(
                "‹ 返回患者自述",
                () -> cards.show(pages, PROFILE_PAGE),
                "编辑患者自述",
                "只填写你确认的信息；医生签署的诊疗记录不会被修改"), BorderLayout.NORTH);
        profileEditContent.setOpaque(false);
        page.add(scroll(profileEditContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createHistoryPage() {
        JPanel page = basePage();
        page.add(header(
                "‹ 返回健康档案",
                () -> cards.show(pages, OVERVIEW_PAGE),
                "历史就诊",
                "按签署时间从近到远排列"), BorderLayout.NORTH);
        page.add(scroll(historyList), BorderLayout.CENTER);
        return page;
    }

    private JPanel createDetailPage() {
        JPanel page = basePage();
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JButton back = HospitalTheme.quietButton("‹ 返回历史就诊");
        back.setName("healthRecordDetailBackButton");
        back.addActionListener(event -> cards.show(pages, HISTORY_PAGE));
        JPanel copy = verticalList();
        detailTitle.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        detailTitle.setForeground(HospitalTheme.TEXT);
        detailSubtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        detailSubtitle.setForeground(HospitalTheme.MUTED);
        copy.add(detailTitle);
        copy.add(Box.createVerticalStrut(4));
        copy.add(detailSubtitle);
        header.add(back, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        page.add(header, BorderLayout.NORTH);
        detailContent.setOpaque(false);
        page.add(scroll(detailContent), BorderLayout.CENTER);
        return page;
    }

    private JPanel createReportsPage() {
        JPanel page = basePage();
        page.add(header(
                "‹ 返回健康档案",
                () -> cards.show(pages, OVERVIEW_PAGE),
                "检查与检验",
                "查看检查单、结果状态和回诊安排"), BorderLayout.NORTH);
        page.add(scroll(reportList), BorderLayout.CENTER);
        return page;
    }

    private JPanel createReportDetailPage() {
        JPanel page = basePage();
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JButton back = HospitalTheme.quietButton("‹ 返回检查与检验");
        back.setName("healthReportDetailBackButton");
        back.addActionListener(event -> cards.show(pages, REPORTS_PAGE));
        JPanel copy = verticalList();
        reportDetailTitle.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        reportDetailTitle.setForeground(HospitalTheme.TEXT);
        reportDetailSubtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        reportDetailSubtitle.setForeground(HospitalTheme.MUTED);
        copy.add(reportDetailTitle);
        copy.add(Box.createVerticalStrut(4));
        copy.add(reportDetailSubtitle);
        header.add(back, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        page.add(header, BorderLayout.NORTH);
        reportDetailContent.setOpaque(false);
        page.add(scroll(reportDetailContent), BorderLayout.CENTER);
        return page;
    }

    private void showLoading() {
        healthRecord = null;
        overviewContent.removeAll();
        overviewContent.add(message("正在整理健康档案……", HospitalTheme.MUTED, 520),
                BorderLayout.NORTH);
        statusLabel.setText("进入页面后自动读取最新资料。");
        refresh(overviewContent);
        cards.show(pages, OVERVIEW_PAGE);
    }

    private void showHealthRecord(PatientHealthRecordView loaded) {
        healthRecord = loaded;
        renderOverview();
        renderProfile();
        renderHistory();
        renderReports();
        statusLabel.setText("患者自述信息可自行维护；医生签署的诊疗记录始终只读。");
        cards.show(pages, OVERVIEW_PAGE);
        if (openPriorityReportAfterLoad) {
            openPriorityReportAfterLoad = false;
            loaded.getExaminations().stream()
                    .filter(examination -> examination.getStatus()
                            != ExaminationStatus.REVIEWED
                            && examination.getStatus() != ExaminationStatus.CANCELLED)
                    .sorted((left, right) -> Integer.compare(
                            followUpPriority(left), followUpPriority(right)))
                    .findFirst()
                    .ifPresentOrElse(
                            this::showReportDetail,
                            () -> cards.show(pages, REPORTS_PAGE));
        } else if (openHistoryAfterLoad) {
            openHistoryAfterLoad = false;
            cards.show(pages, HISTORY_PAGE);
        }
    }

    private void renderOverview() {
        overviewContent.removeAll();
        PatientHealthProfileView profile = healthRecord.getHealthProfile();

        HospitalTheme.SurfacePanel alert = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 16);
        alert.setLayout(new BorderLayout(18, 0));
        alert.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel summary = new JLabel("<html><b>就诊前请先核对过敏信息</b><br>当前记录："
                + html(profile.getAllergies()) + "</html>");
        summary.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        summary.setForeground(HospitalTheme.PRIMARY_DARK);
        alert.add(summary, BorderLayout.CENTER);
        JPanel priorityArea = verticalList();
        healthRecord.getExaminations().stream()
                .filter(examination -> examination.getStatus()
                        != ExaminationStatus.REVIEWED
                        && examination.getStatus() != ExaminationStatus.CANCELLED)
                .sorted((left, right) -> Integer.compare(
                        followUpPriority(left), followUpPriority(right)))
                .findFirst()
                .ifPresent(examination -> {
                    priorityArea.add(patientCareTask(examination));
                    priorityArea.add(Box.createVerticalStrut(12));
                });
        priorityArea.add(alert);
        overviewContent.add(priorityArea, BorderLayout.NORTH);

        JPanel index = HospitalResponsiveLayout.grid(3, 230, 16, 16);
        index.setOpaque(false);
        index.add(indexCard(
                "患者自述",
                "血型、过敏、既往情况与长期用药",
                "查看和编辑",
                "openPatientProfileButton",
                this::openProfile));
        index.add(indexCard(
                "历史就诊",
                healthRecord.getConsultations().isEmpty()
                        ? "暂无医生签署的诊疗记录"
                        : healthRecord.getConsultations().size() + " 次就诊记录",
                "查看历史就诊",
                "openPatientHistoryButton",
                this::openHistory));
        index.add(indexCard(
                "检查报告",
                healthRecord.getExaminations().isEmpty()
                        ? "暂无检查单或检查报告"
                        : healthRecord.getExaminations().size() + " 项检查资料",
                "查看检查与报告",
                "openPatientReportsButton",
                this::openReports));
        overviewContent.add(index, BorderLayout.CENTER);
        refresh(overviewContent);
    }

    private JPanel indexCard(
            String titleText,
            String description,
            String actionText,
            String actionName,
            Runnable action) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 20, 22));
        card.setPreferredSize(new Dimension(260, 190));
        JPanel copy = verticalList();
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel detail = message(description, HospitalTheme.MUTED, 220);
        copy.add(title);
        copy.add(Box.createVerticalStrut(9));
        copy.add(detail);
        JButton button = action == null
                ? HospitalTheme.quietButton(actionText)
                : HospitalTheme.primaryButton(actionText);
        button.setName(actionName);
        button.setEnabled(action != null);
        if (action != null) {
            button.addActionListener(event -> action.run());
        }
        card.add(copy, BorderLayout.CENTER);
        card.add(button, BorderLayout.SOUTH);
        return card;
    }

    private void renderProfile() {
        profileContent.removeAll();
        PatientHealthProfileView profile = healthRecord.getHealthProfile();
        HospitalTheme.SurfacePanel card = informationCard("健康摘要");
        card.add(detailRow("血型", profile.getBloodType()));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("过敏信息", profile.getAllergies()));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("既往情况", profile.getMedicalHistory()));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("长期用药", profile.getLongTermMedication()));
        card.add(Box.createVerticalStrut(16));
        card.add(detailRow("紧急联系人", profile.getEmergencyContact()));
        card.add(Box.createVerticalStrut(22));
        card.add(message(
                "最近更新：" + DATE_TIME_FORMAT.format(profile.getUpdatedAt())
                        + "。这些内容由患者自述，医生签署的诊疗记录请到“历史就诊”查看。",
                HospitalTheme.MUTED,
                680));
        card.add(Box.createVerticalStrut(16));
        JButton edit = HospitalTheme.primaryButton("编辑我的健康信息");
        edit.setName("editPatientHealthProfileButton");
        edit.addActionListener(event -> showProfileEditor());
        card.add(edit);
        profileContent.add(card, BorderLayout.NORTH);
        refresh(profileContent);
    }

    private JPanel patientCareTask(ExaminationOrderView examination) {
        boolean needsBooking = examination.getStatus() == ExaminationStatus.RESULT_READY
                && !examination.isResultReviewBooked();
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                needsBooking ? HospitalTheme.PRIMARY : HospitalTheme.PRIMARY_LIGHT,
                16,
                needsBooking ? HospitalTheme.PRIMARY_DARK : HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel copy = verticalList();
        JLabel title = new JLabel(needsBooking
                ? "检查结果已出，请安排回诊"
                : examination.isResultReviewBooked()
                        ? "检查结果回诊已安排"
                        : "检查正在等待报告");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        title.setForeground(needsBooking ? Color.WHITE : HospitalTheme.PRIMARY_DARK);
        JLabel detail = new JLabel(examination.getItemName() + "  ·  "
                + examination.getDepartmentName());
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        detail.setForeground(needsBooking
                ? new Color(218, 247, 243) : HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(6));
        copy.add(detail);
        JButton action = needsBooking
                ? HospitalTheme.quietButton("立即安排回诊")
                : HospitalTheme.primaryButton(examination.isResultReviewBooked()
                        ? "查看回诊状态" : "查看检查进度");
        action.setName("patientFollowUpTaskButton");
        action.addActionListener(event -> showReportDetail(examination));
        card.add(copy, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private void showProfileEditor() {
        PatientHealthProfileView profile = healthRecord.getHealthProfile();
        JTextField bloodType = textField(
                "patientBloodType", editableText(profile.getBloodType()));
        JTextArea allergies = editArea(
                "patientAllergies", editableText(profile.getAllergies()), 3);
        JTextArea history = editArea(
                "patientMedicalHistory", editableText(profile.getMedicalHistory()), 4);
        JTextArea medication = editArea(
                "patientLongTermMedication",
                editableText(profile.getLongTermMedication()),
                3);
        JTextField emergencyContact = textField(
                "patientEmergencyContact", editableText(profile.getEmergencyContact()));
        JLabel saveStatus = new JLabel(" ");
        saveStatus.setName("patientHealthProfileSaveStatus");
        saveStatus.setForeground(HospitalTheme.MUTED);

        HospitalTheme.SurfacePanel form = informationCard("我提供的健康信息");
        form.add(editField("血型", bloodType));
        form.add(Box.createVerticalStrut(13));
        form.add(editField("过敏信息", allergies));
        form.add(Box.createVerticalStrut(13));
        form.add(editField("既往情况", history));
        form.add(Box.createVerticalStrut(13));
        form.add(editField("长期用药", medication));
        form.add(Box.createVerticalStrut(13));
        form.add(editField("紧急联系人", emergencyContact));
        form.add(Box.createVerticalStrut(18));
        form.add(message(
                "没有相关情况时可填写“无”；不确定的内容请留空，并在就诊时向医生说明。",
                HospitalTheme.MUTED,
                680));
        form.add(Box.createVerticalStrut(14));
        JButton save = HospitalTheme.primaryButton("保存我的健康信息");
        save.setName("savePatientHealthProfileButton");
        save.addActionListener(event -> saveHealthProfile(
                bloodType, allergies, history, medication, emergencyContact,
                save, saveStatus));
        form.add(save);
        form.add(Box.createVerticalStrut(8));
        form.add(saveStatus);
        profileEditContent.removeAll();
        profileEditContent.add(form, BorderLayout.NORTH);
        refresh(profileEditContent);
        cards.show(pages, PROFILE_EDIT_PAGE);
    }

    private void saveHealthProfile(
            JTextField bloodType,
            JTextArea allergies,
            JTextArea history,
            JTextArea medication,
            JTextField emergencyContact,
            JButton save,
            JLabel saveStatus) {
        UpdatePatientHealthProfileRequest request = new UpdatePatientHealthProfileRequest(
                bloodType.getText(), allergies.getText(), history.getText(),
                medication.getText(), emergencyContact.getText());
        save.setEnabled(false);
        save.setText("正在保存……");
        saveStatus.setForeground(HospitalTheme.MUTED);
        saveStatus.setText("服务器正在保存你的自述信息。");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.UPDATE_MY_HEALTH_PROFILE, request);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientHealthProfileView updated) {
                        healthRecord = new PatientHealthRecordView(
                                updated,
                                healthRecord.getConsultations(),
                                healthRecord.getExaminations());
                        renderOverview();
                        renderProfile();
                        cards.show(pages, PROFILE_PAGE);
                    } else {
                        save.setEnabled(true);
                        save.setText("保存我的健康信息");
                        saveStatus.setForeground(HospitalTheme.WARNING);
                        saveStatus.setText("保存失败：" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    save.setEnabled(true);
                    save.setText("保存我的健康信息");
                    saveStatus.setText("保存已中断，请重试。");
                } catch (ExecutionException exception) {
                    save.setEnabled(true);
                    save.setText("保存我的健康信息");
                    saveStatus.setText("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void renderHistory() {
        historyList.removeAll();
        if (selectingOrdinaryFollowUp) {
            historyList.add(message(
                    "请选择一条已完成的诊疗记录。系统会按原科室查询新的复诊排班。",
                    HospitalTheme.PRIMARY_DARK,
                    720));
            historyList.add(Box.createVerticalStrut(12));
        }
        if (healthRecord.getConsultations().isEmpty()) {
            historyList.add(message(
                    "暂无历史就诊。医生完成并签署诊疗记录后，会自动出现在这里。",
                    HospitalTheme.MUTED,
                    620));
        } else {
            for (ConsultationRecordView record : healthRecord.getConsultations()) {
                historyList.add(historyCard(record));
                historyList.add(Box.createVerticalStrut(12));
            }
        }
        refresh(historyList);
    }

    private JPanel historyCard(ConsultationRecordView record) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 122));
        JPanel copy = verticalList();
        JLabel title = new JLabel(
                (record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查阶段记录" : visitTypeText(record)) + "  ·  "
                + record.getDepartmentName() + "  ·  "
                + DATE_FORMAT.format(record.getCreatedAt()));
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel diagnosis = message(
                "诊断：" + record.getDiagnosisOpinion(), HospitalTheme.MUTED, 560);
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(diagnosis);
        JButton open = selectingOrdinaryFollowUp
                ? HospitalTheme.quietButton("查看详情")
                : HospitalTheme.primaryButton("查看详情");
        open.setName("openHealthHistoryDetailButton");
        open.addActionListener(event -> showHistoryDetail(record));
        card.add(copy, BorderLayout.CENTER);
        if (selectingOrdinaryFollowUp) {
            JPanel actions = verticalList();
            actions.add(open);
            if (record.getOutcome() == ConsultationOutcome.COMPLETED) {
                actions.add(Box.createVerticalStrut(7));
                JButton select = HospitalTheme.primaryButton("以此记录复诊");
                select.setName("selectOrdinaryFollowUpButton");
                select.addActionListener(event -> bookFollowUp.accept(record));
                actions.add(select);
            }
            card.add(actions, BorderLayout.EAST);
        } else {
            card.add(open, BorderLayout.EAST);
        }
        return card;
    }

    private void showHistoryDetail(ConsultationRecordView record) {
        detailTitle.setText(record.getDepartmentName() + "就诊记录");
        detailSubtitle.setText(record.getDoctorName() + " " + record.getDoctorTitle()
                + "  ·  " + DATE_TIME_FORMAT.format(record.getCreatedAt()));
        detailContent.removeAll();
        JPanel columns = new JPanel(new GridLayout(0, 1, 0, 12));
        columns.setOpaque(false);
        HospitalTheme.SurfacePanel diagnosis = informationCard("诊断与检查");
        diagnosis.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "初步判断" : "诊断意见",
                record.getDiagnosisOpinion()));
        diagnosis.add(Box.createVerticalStrut(18));
        diagnosis.add(detailRow("检查建议", record.getExaminationAdvice()));
        HospitalTheme.SurfacePanel treatment = informationCard(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查期间安排" : "处置与后续");
        treatment.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查期间注意事项" : "处置意见",
                record.getTreatmentAdvice()));
        if (record.getOutcome() == ConsultationOutcome.COMPLETED) {
            treatment.add(Box.createVerticalStrut(18));
            treatment.add(detailRow("用药建议", record.getMedicationAdvice()));
        }
        treatment.add(Box.createVerticalStrut(18));
        treatment.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "后续步骤" : "复诊建议",
                record.getFollowUpAdvice()));
        columns.add(diagnosis);
        columns.add(treatment);
        detailContent.add(columns, BorderLayout.CENTER);
        if (record.getOutcome() == ConsultationOutcome.COMPLETED) {
            HospitalTheme.SurfacePanel nextStep = new HospitalTheme.SurfacePanel(
                    HospitalTheme.PRIMARY_LIGHT, 12);
            nextStep.setLayout(new BorderLayout(0, 12));
            nextStep.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            JPanel copy = verticalList();
            JLabel heading = new JLabel("需要再次就诊？");
            heading.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
            heading.setForeground(HospitalTheme.PRIMARY_DARK);
            JLabel explanation = message(
                    "以本次记录预约原科室排班；普通复诊会建立新的诊疗过程并正常收取挂号费。",
                    HospitalTheme.MUTED,
                    520);
            copy.add(heading);
            copy.add(Box.createVerticalStrut(5));
            copy.add(explanation);
            JButton followUp = HospitalTheme.primaryButton("预约普通复诊");
            followUp.setName("bookOrdinaryFollowUpButton");
            followUp.addActionListener(event -> bookFollowUp.accept(record));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            actions.setOpaque(false);
            actions.add(followUp);
            nextStep.add(copy, BorderLayout.CENTER);
            nextStep.add(actions, BorderLayout.SOUTH);
            JPanel actionArea = new JPanel(new BorderLayout());
            actionArea.setOpaque(false);
            actionArea.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
            actionArea.add(nextStep, BorderLayout.CENTER);
            detailContent.add(actionArea, BorderLayout.SOUTH);
        }
        refresh(detailContent);
        cards.show(pages, DETAIL_PAGE);
    }

    private void renderReports() {
        reportList.removeAll();
        if (healthRecord.getExaminations().isEmpty()) {
            reportList.add(message(
                    "暂无检查资料。医生开具检查单后，项目状态和报告会自动出现在这里。",
                    HospitalTheme.MUTED,
                    650));
        } else {
            for (ExaminationOrderView examination : healthRecord.getExaminations()) {
                reportList.add(reportCard(examination));
                reportList.add(Box.createVerticalStrut(12));
            }
        }
        refresh(reportList);
    }

    private JPanel reportCard(ExaminationOrderView examination) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        JPanel copy = verticalList();
        JLabel title = new JLabel(examination.getItemName() + "  ·  "
                + examinationStatusText(examination));
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel meta = message(
                examination.getDepartmentName() + "  ·  "
                        + examination.getDoctorName() + "  ·  "
                        + DATE_TIME_FORMAT.format(examination.getOrderedAt()),
                HospitalTheme.MUTED,
                560);
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(meta);
        JButton open = HospitalTheme.primaryButton("查看检查详情");
        open.setName("openPatientReportDetailButton");
        open.addActionListener(event -> showReportDetail(examination));
        card.add(copy, BorderLayout.CENTER);
        card.add(open, BorderLayout.EAST);
        return card;
    }

    private void showReportDetail(ExaminationOrderView examination) {
        reportDetailTitle.setText(examination.getItemName());
        reportDetailSubtitle.setText(examination.getDepartmentName() + "  ·  "
                + examination.getDoctorName() + "  ·  "
                + examinationStatusText(examination));
        reportDetailContent.removeAll();

        HospitalTheme.SurfacePanel detail = informationCard("检查单与结果");
        detail.add(detailRow("检查项目", examination.getItemName()));
        detail.add(Box.createVerticalStrut(16));
        detail.add(detailRow("检查说明", examination.getInstructions()));
        detail.add(Box.createVerticalStrut(16));
        detail.add(detailRow("当前状态", examinationStatusText(examination)));
        detail.add(Box.createVerticalStrut(16));
        detail.add(detailRow("检查结果", examination.getResultSummary()));
        detail.add(Box.createVerticalStrut(22));

        JLabel actionStatus = new JLabel(" ");
        actionStatus.setName("patientReportActionStatus");
        actionStatus.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        actionStatus.setForeground(HospitalTheme.MUTED);
        detail.add(actionStatus);
        detail.add(Box.createVerticalStrut(8));
        if (examination.getStatus() == ExaminationStatus.ORDERED) {
            detail.add(reportActionButton(
                    "模拟完成检查并生成报告",
                    "publishDemoReportButton",
                    button -> publishDemoReport(examination, button, actionStatus)));
            detail.add(Box.createVerticalStrut(12));
            detail.add(message(
                    "课程项目没有连接真实检验科或影像系统。此按钮只模拟外部系统出具一份虚构文字报告。",
                    HospitalTheme.WARNING,
                    700));
        } else if (examination.getStatus() == ExaminationStatus.RESULT_READY
                && !examination.isResultReviewBooked()) {
            detail.add(reportActionButton(
                    "申请检查结果回诊",
                    "bookResultReviewButton",
                    button -> bookResultReview(examination, button, actionStatus)));
            detail.add(Box.createVerticalStrut(12));
            detail.add(message(
                    "系统优先安排原医生七天内的可用排班；没有原医生号源时尝试同科室排班，挂号费为 ¥0.00。",
                    HospitalTheme.MUTED,
                    700));
        } else if (examination.isResultReviewBooked()
                && examination.getStatus() == ExaminationStatus.RESULT_READY) {
            JLabel booked = new JLabel("已安排检查结果回诊，可到“我的预约”查看候诊信息。");
            booked.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
            booked.setForeground(HospitalTheme.SUCCESS);
            detail.add(booked);
        } else if (examination.getStatus() == ExaminationStatus.REVIEWED) {
            JLabel reviewed = new JLabel("接诊医生已完成结果解读，本轮检查闭环已完成。");
            reviewed.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
            reviewed.setForeground(HospitalTheme.SUCCESS);
            detail.add(reviewed);
        }
        reportDetailContent.add(detail, BorderLayout.NORTH);
        refresh(reportDetailContent);
        cards.show(pages, REPORT_DETAIL_PAGE);
    }

    private JButton reportActionButton(
            String text,
            String name,
            java.util.function.Consumer<JButton> action) {
        JButton button = HospitalTheme.primaryButton(text);
        button.setName(name);
        button.addActionListener(event -> action.accept(button));
        return button;
    }

    private void publishDemoReport(
            ExaminationOrderView examination,
            JButton button,
            JLabel actionStatus) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确认生成虚构演示报告？\n\n这只用于演示检查—报告—回诊流程，不是真实检查结果。",
                "生成演示报告",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        runReportAction(
                button,
                actionStatus,
                "正在生成演示报告……",
                HospitalActions.PUBLISH_DEMO_EXAMINATION_REPORT,
                new PublishDemoExaminationReportRequest(examination.getOrderId()),
                examination.getOrderId(),
                false);
    }

    private void bookResultReview(
            ExaminationOrderView examination,
            JButton button,
            JLabel actionStatus) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确认由系统安排检查结果回诊？\n\n"
                        + "系统优先安排原接诊医生未来七天内的可用排班；"
                        + "原医生没有可用号源时，将安排同科室其他医生。\n"
                        + "本次挂号费为 ¥0.00。",
                "确认自动安排回诊",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        runReportAction(
                button,
                actionStatus,
                "正在安排回诊……",
                HospitalActions.BOOK_RESULT_REVIEW,
                new BookResultReviewRequest(examination.getOrderId()),
                examination.getOrderId(),
                true);
    }

    private void runReportAction(
            JButton button,
            JLabel actionStatus,
            String loadingText,
            String action,
            Serializable request,
            String orderId,
            boolean bookingAction) {
        button.setEnabled(false);
        actionStatus.setForeground(HospitalTheme.MUTED);
        actionStatus.setText(loadingText);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(action, request);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        actionStatus.setForeground(HospitalTheme.SUCCESS);
                        if (bookingAction
                                && response.getData() instanceof AppointmentBookingView booking) {
                            actionStatus.setText("回诊已安排，可到“我的预约”查看。");
                            JOptionPane.showMessageDialog(
                                    PatientHealthRecordPanel.this,
                                    "回诊已经安排成功。\n\n"
                                            + "医生：" + booking.getDoctorName() + "\n"
                                            + "科室：" + booking.getDepartmentName() + "\n"
                                            + "时间：" + DATE_TIME_FORMAT.format(
                                                    booking.getStartTime()) + "\n"
                                            + "候诊号：" + booking.getQueueNumber() + "\n"
                                            + "挂号费：¥0.00\n\n"
                                            + "你可以到“我的预约”查看完整信息。",
                                    "回诊安排成功",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            actionStatus.setText(
                                    "演示报告已生成，可以继续申请检查结果回诊。");
                        }
                        reloadReportAfterAction(orderId);
                    } else {
                        button.setEnabled(true);
                        actionStatus.setForeground(HospitalTheme.WARNING);
                        actionStatus.setText(reportFailureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    button.setEnabled(true);
                    actionStatus.setText("操作已中断，请重试。");
                } catch (ExecutionException exception) {
                    button.setEnabled(true);
                    actionStatus.setText("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void reloadReportAfterAction(String orderId) {
        int version = ++requestVersion;
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_MY_HEALTH_RECORD, null);
            }

            @Override
            protected void done() {
                if (version != requestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientHealthRecordView loaded) {
                        showHealthRecord(loaded);
                        loaded.getExaminations().stream()
                                .filter(item -> item.getOrderId().equals(orderId))
                                .findFirst()
                                .ifPresent(PatientHealthRecordPanel.this::showReportDetail);
                    } else {
                        showError(failureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("检查资料刷新已中断，请重新进入健康档案。");
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private static String reportFailureMessage(Response response) {
        if (ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID.equals(response.getCode())) {
            return "检查状态已经变化，重新进入健康档案后再试。";
        }
        if (ErrorCodes.HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED.equals(response.getCode())) {
            return "已经安排过检查结果回诊，请到“我的预约”查看。";
        }
        if (ErrorCodes.HOSPITAL_SLOT_FULL.equals(response.getCode())) {
            return "七天内暂时没有可安排的同科室回诊号源。";
        }
        if (ErrorCodes.HOSPITAL_SELF_BOOKING_FORBIDDEN.equals(response.getCode())) {
            return "七天内只有你自己的医生排班，不能为本人安排回诊。";
        }
        return "操作失败：" + response.getMessage();
    }

    private static String examinationStatusText(ExaminationOrderView examination) {
        return switch (examination.getStatus()) {
            case ORDERED -> "待完成检查";
            case RESULT_READY -> examination.isResultReviewBooked()
                    ? "结果已出 · 已安排回诊" : "结果已出 · 待申请回诊";
            case REVIEWED -> "医生已回看";
            case CANCELLED -> "已取消";
        };
    }

    private static String visitTypeText(ConsultationRecordView record) {
        return switch (record.getVisitType()) {
            case FIRST_VISIT -> "初次就诊";
            case FOLLOW_UP -> "复诊";
            case RESULT_REVIEW -> "检查结果回诊";
        };
    }

    private void openProfile() {
        cards.show(pages, PROFILE_PAGE);
    }

    private void openHistory() {
        cards.show(pages, HISTORY_PAGE);
    }

    private void openReports() {
        cards.show(pages, REPORTS_PAGE);
    }

    private void showError(String messageText) {
        overviewContent.removeAll();
        overviewContent.add(message(messageText, HospitalTheme.WARNING, 640),
                BorderLayout.NORTH);
        statusLabel.setForeground(HospitalTheme.WARNING);
        statusLabel.setText(messageText);
        refresh(overviewContent);
        cards.show(pages, OVERVIEW_PAGE);
    }

    private static String failureMessage(Response response) {
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())) {
            return "登录已失效，请重新登录后查看健康档案。";
        }
        if (ErrorCodes.COMMON_UNKNOWN_ACTION.equals(response.getCode())) {
            return "服务器仍是旧版本，请重启服务器和客户端后重试。";
        }
        return "健康档案加载失败：" + response.getMessage();
    }

    private static JPanel header(
            String backText,
            Runnable backAction,
            String titleText,
            String subtitleText) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JButton back = HospitalTheme.quietButton(backText);
        back.addActionListener(event -> backAction.run());
        JPanel copy = verticalList();
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);
        header.add(back, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        return HospitalResponsiveLayout.constrainWidth(header);
    }

    private static HospitalTheme.SurfacePanel informationCard(String titleText) {
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

    private static JPanel detailRow(String labelText, String valueText) {
        JPanel row = verticalList();
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        label.setForeground(HospitalTheme.MUTED);
        JLabel value = new JLabel("<html><body style='width:520px'>"
                + html(valueText) + "</body></html>");
        value.setName("patientHealthRecordDetail");
        value.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
        value.setForeground(HospitalTheme.TEXT);
        row.add(label);
        row.add(Box.createVerticalStrut(4));
        row.add(value);
        return row;
    }

    private static JPanel editField(String labelText, javax.swing.JComponent input) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.TEXT);
        field.add(label, BorderLayout.NORTH);
        if (input instanceof JTextArea area) {
            JScrollPane scroll = new JScrollPane(area);
            scroll.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
            field.add(scroll, BorderLayout.CENTER);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    area.getRows() * 30 + 28));
        } else {
            field.add(input, BorderLayout.CENTER);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        }
        return field;
    }

    private static JTextField textField(String name, String value) {
        JTextField field = new JTextField(value);
        field.setName(name);
        field.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    private static JTextArea editArea(String name, String value, int rows) {
        JTextArea area = new JTextArea(value, rows, 40);
        area.setName(name);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return area;
    }

    private static String editableText(String value) {
        return "未填写".equals(value) ? "" : value;
    }

    private static int followUpPriority(ExaminationOrderView examination) {
        if (examination.getStatus() == ExaminationStatus.RESULT_READY
                && !examination.isResultReviewBooked()) {
            return 0;
        }
        if (examination.isResultReviewBooked()) {
            return 1;
        }
        return 2;
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

    private static JPanel verticalList() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JLabel message(String text, Color color, int width) {
        JLabel label = new JLabel("<html><body style='width:" + width + "px'>"
                + html(text) + "</body></html>");
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        label.setForeground(color);
        return label;
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
