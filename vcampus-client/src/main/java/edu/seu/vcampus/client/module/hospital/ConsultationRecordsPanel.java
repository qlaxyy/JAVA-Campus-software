package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.ConsultationListResponse;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Patient-owned consultation history with a separate full record page. */
final class ConsultationRecordsPanel extends JPanel {

    private static final String LIST_PAGE = "list";
    private static final String DETAIL_PAGE = "detail";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA);

    private final ClientContext context;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JPanel recordList = verticalList();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel detailTitle = new JLabel("诊疗记录");
    private final JLabel detailSubtitle = new JLabel(" ");
    private final JPanel detailContent = new JPanel(new BorderLayout());
    private int requestVersion;

    ConsultationRecordsPanel(ClientContext context, Runnable backToPatientHome) {
        this.context = context;
        setLayout(new BorderLayout());
        setBackground(HospitalTheme.BACKGROUND);
        pages.setOpaque(false);
        pages.add(createListPage(backToPatientHome), LIST_PAGE);
        pages.add(createDetailPage(), DETAIL_PAGE);
        add(pages, BorderLayout.CENTER);
        showLoading();
    }

    void activate() {
        int version = ++requestVersion;
        showLoading();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_MY_CONSULTATIONS, null);
            }

            @Override
            protected void done() {
                if (version != requestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof ConsultationListResponse list) {
                        showRecords(list);
                    } else {
                        showError(failureMessage(response));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("问诊记录加载已中断，请重新进入页面。");
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动后重试。");
                }
            }
        }.execute();
    }

    private JPanel createListPage(Runnable backToPatientHome) {
        JPanel page = basePage();
        page.add(header(
                "‹ 返回患者首页",
                backToPatientHome,
                "问诊记录",
                "查看医生完成并签署的诊断与处置"), BorderLayout.NORTH);
        page.add(scroll(recordList), BorderLayout.CENTER);
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        page.add(statusLabel, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createDetailPage() {
        JPanel page = basePage();
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JButton back = HospitalTheme.quietButton("‹ 返回问诊记录");
        back.setName("consultationBackButton");
        back.addActionListener(event -> cards.show(pages, LIST_PAGE));
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

    private void showLoading() {
        cards.show(pages, LIST_PAGE);
        recordList.removeAll();
        recordList.add(message("正在读取问诊记录……", HospitalTheme.MUTED, 520));
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setText("进入页面后自动读取最新记录。");
        refresh(recordList);
    }

    private void showRecords(ConsultationListResponse response) {
        recordList.removeAll();
        if (response.getConsultations().isEmpty()) {
            recordList.add(emptyState());
        } else {
            for (ConsultationRecordView record : response.getConsultations()) {
                recordList.add(recordCard(record));
                recordList.add(Box.createVerticalStrut(12));
            }
        }
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setText("诊疗记录由接诊医生签署，患者只能查看，不能修改。");
        refresh(recordList);
    }

    private JPanel recordCard(ConsultationRecordView record) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setName("consultationRecordCard");
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 136));
        card.setPreferredSize(new Dimension(850, 136));

        HospitalTheme.SurfacePanel date = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 12);
        date.setLayout(new BoxLayout(date, BoxLayout.Y_AXIS));
        date.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));
        date.setPreferredSize(new Dimension(150, 100));
        JLabel dateValue = new JLabel(DATE_FORMAT.format(record.getCreatedAt()));
        dateValue.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        dateValue.setForeground(HospitalTheme.PRIMARY_DARK);
        JLabel state = new JLabel(record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                ? "等待检查结果" : "已完成");
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        state.setForeground(record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                ? HospitalTheme.PRIMARY : HospitalTheme.SUCCESS);
        date.add(dateValue);
        date.add(Box.createVerticalGlue());
        date.add(state);

        JPanel copy = verticalList();
        JLabel heading = new JLabel(
                (record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查阶段记录" : visitTypeText(record)) + "  ·  "
                + record.getDepartmentName() + "  ·  "
                + record.getDoctorName() + " " + record.getDoctorTitle());
        heading.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        heading.setForeground(HospitalTheme.TEXT);
        JLabel diagnosis = new JLabel(
                (record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "初步判断：" : "诊断意见：")
                        + record.getDiagnosisOpinion());
        diagnosis.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        diagnosis.setForeground(HospitalTheme.TEXT);
        JLabel id = new JLabel("记录编号 " + record.getConsultationId());
        id.setFont(HospitalTheme.dataFont(Font.PLAIN, 11F));
        id.setForeground(HospitalTheme.MUTED);
        copy.add(heading);
        copy.add(Box.createVerticalStrut(8));
        copy.add(diagnosis);
        copy.add(Box.createVerticalStrut(7));
        copy.add(id);

        JButton open = HospitalTheme.primaryButton("查看完整记录");
        open.setName("openConsultationRecordButton");
        open.addActionListener(event -> showRecord(record));
        card.add(date, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        card.add(open, BorderLayout.EAST);
        return card;
    }

    private void showRecord(ConsultationRecordView record) {
        detailTitle.setText(record.getDepartmentName() + "诊疗记录");
        detailSubtitle.setText(record.getDoctorName() + " " + record.getDoctorTitle()
                + "  ·  签署于 " + DATE_TIME_FORMAT.format(record.getCreatedAt()));
        detailContent.removeAll();

        JPanel columns = HospitalResponsiveLayout.grid(2, 320, 16, 16);
        columns.setOpaque(false);
        HospitalTheme.SurfacePanel diagnosis = informationCard("诊断与检查");
        diagnosis.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "初步判断" : "诊断意见",
                record.getDiagnosisOpinion()));
        diagnosis.add(Box.createVerticalStrut(18));
        diagnosis.add(detailRow("检查建议", record.getExaminationAdvice()));
        diagnosis.add(Box.createVerticalStrut(24));
        diagnosis.add(detailRow("预约编号", record.getAppointmentId()));
        diagnosis.add(Box.createVerticalStrut(12));
        diagnosis.add(detailRow("诊疗记录编号", record.getConsultationId()));

        HospitalTheme.SurfacePanel disposition = informationCard(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查期间安排" : "处置与后续");
        disposition.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "检查期间注意事项" : "处置意见",
                record.getTreatmentAdvice()));
        if (record.getOutcome() == ConsultationOutcome.COMPLETED) {
            disposition.add(Box.createVerticalStrut(18));
            disposition.add(detailRow("简化用药建议", record.getMedicationAdvice()));
        }
        disposition.add(Box.createVerticalStrut(18));
        disposition.add(detailRow(
                record.getOutcome() == ConsultationOutcome.WAITING_FOR_RESULTS
                        ? "后续步骤" : "复诊建议",
                record.getFollowUpAdvice()));
        disposition.add(Box.createVerticalStrut(24));
        disposition.add(message(
                "本系统内容为课程项目中的虚构演示记录，不构成真实医疗建议。",
                HospitalTheme.WARNING,
                420));

        columns.add(diagnosis);
        columns.add(disposition);
        detailContent.add(columns, BorderLayout.CENTER);
        refresh(detailContent);
        cards.show(pages, DETAIL_PAGE);
    }

    private JPanel emptyState() {
        HospitalTheme.SurfacePanel empty = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        JLabel title = new JLabel("暂无问诊记录");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel detail = new JLabel("医生完成接诊并保存后，记录会自动出现在这里。");
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        detail.setForeground(HospitalTheme.MUTED);
        empty.add(title);
        empty.add(Box.createVerticalStrut(8));
        empty.add(detail);
        return empty;
    }

    private void showError(String messageText) {
        recordList.removeAll();
        recordList.add(message(messageText, HospitalTheme.WARNING, 620));
        statusLabel.setForeground(HospitalTheme.WARNING);
        statusLabel.setText(messageText);
        refresh(recordList);
    }

    private static String failureMessage(Response response) {
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())) {
            return "登录已失效，请重新登录后查看问诊记录。";
        }
        if (ErrorCodes.COMMON_UNKNOWN_ACTION.equals(response.getCode())) {
            return "服务器仍是旧版本，请重启服务器和客户端后重试。";
        }
        return "问诊记录加载失败：" + response.getMessage();
    }

    private static String visitTypeText(ConsultationRecordView record) {
        return switch (record.getVisitType()) {
            case FIRST_VISIT -> "初次就诊";
            case FOLLOW_UP -> "复诊";
            case RESULT_REVIEW -> "检查结果回诊";
        };
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
        javax.swing.JTextArea value = HospitalResponsiveLayout.wrappingText(
                valueText, HospitalTheme.uiFont(Font.BOLD, 14F), HospitalTheme.TEXT);
        value.setName("consultationRecordDetail");
        row.add(label);
        row.add(Box.createVerticalStrut(4));
        row.add(value);
        return row;
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

    private static javax.swing.JTextArea message(
            String text, Color color, int ignoredWidth) {
        return HospitalResponsiveLayout.wrappingText(
                text, HospitalTheme.uiFont(Font.PLAIN, 13F), color);
    }

    private static void refresh(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }
}
