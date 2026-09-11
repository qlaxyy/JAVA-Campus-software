package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.hospital.PatientBillView;
import edu.seu.vcampus.common.hospital.PaymentStatus;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;

/** Patient-facing hospital landing page. */
final class HospitalHomePanel extends JPanel {

    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Runnable openSlotSearch;
    private final Runnable openMyAppointments;
    private final Runnable openFollowUp;
    private final JPanel careTaskPanel = new JPanel(new BorderLayout());
    private final JPanel billingTaskPanel = new JPanel(new BorderLayout());

    HospitalHomePanel(
            Runnable openSlotSearch,
            Runnable openSmartTriage,
            Runnable openMyAppointments,
            Runnable openConsultationRecords,
            Runnable openHealthRecord,
            Runnable openBills,
            Runnable openOrdinaryFollowUp,
            Runnable openCareGuide,
            Runnable openFollowUp,
            Runnable switchMode) {
        this.openSlotSearch = openSlotSearch;
        this.openMyAppointments = openMyAppointments;
        this.openFollowUp = openFollowUp;
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        add(HospitalResponsiveLayout.constrainWidth(createHeader(switchMode)),
                BorderLayout.NORTH);
        add(HospitalResponsiveLayout.verticalScroll(createContent(
                openSlotSearch,
                openSmartTriage,
                openMyAppointments,
                openConsultationRecords,
                openHealthRecord,
                openBills,
                openOrdinaryFollowUp,
                openCareGuide)), BorderLayout.CENTER);

        messageLabel.setForeground(HospitalTheme.WARNING);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        add(messageLabel, BorderLayout.SOUTH);
    }

    void showMessage(String message) {
        messageLabel.setText(message);
    }

    void showCareTaskLoading() {
        careTaskPanel.removeAll();
        careTaskPanel.setVisible(false);
        refresh(careTaskPanel);
    }

    void showBillTaskLoading() {
        billingTaskPanel.removeAll();
        billingTaskPanel.setVisible(false);
        refresh(billingTaskPanel);
    }

    void showBillTasks(PatientBillListResponse response, Runnable openBills) {
        billingTaskPanel.removeAll();
        var unpaid = response.getBills().stream()
                .filter(bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID)
                .toList();
        if (unpaid.isEmpty()) {
            billingTaskPanel.setVisible(false);
            refresh(billingTaskPanel);
            return;
        }
        long amount = unpaid.stream().mapToLong(PatientBillView::getAmountCents).sum();
        billingTaskPanel.add(billingTaskCard(unpaid.size(), amount, openBills),
                BorderLayout.CENTER);
        billingTaskPanel.setVisible(true);
        refresh(billingTaskPanel);
    }

    void showCareTasks(PatientHealthRecordView record) {
        careTaskPanel.removeAll();
        ExaminationOrderView task = record.getExaminations().stream()
                .filter(examination -> examination.getStatus() != ExaminationStatus.REVIEWED
                        && examination.getStatus() != ExaminationStatus.CANCELLED)
                .sorted(Comparator.comparingInt(HospitalHomePanel::followUpPriority))
                .findFirst()
                .orElse(null);
        if (task == null) {
            careTaskPanel.setVisible(false);
            refresh(careTaskPanel);
            return;
        }
        careTaskPanel.add(careTaskCard(task), BorderLayout.CENTER);
        careTaskPanel.setVisible(true);
        refresh(careTaskPanel);
    }

    void showGuide() {
        Object[] options = {"开始预约", "稍后再看"};
        int choice = JOptionPane.showOptionDialog(
                this,
                createGuideContent(),
                "校医院使用指南",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            openSlotSearch.run();
        }
    }

    private JPanel createHeader(Runnable switchMode) {
        JPanel header = new JPanel();
        header.setLayout(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("校医院");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("校园医疗服务 · 从预约、候诊到诊疗记录");
        subtitle.setForeground(HospitalTheme.MUTED);

        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);

        JButton switchButton = HospitalTheme.quietButton("切换使用模式");
        switchButton.addActionListener(event -> switchMode.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(switchButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createContent(
            Runnable openSlotSearch,
            Runnable openSmartTriage,
            Runnable openMyAppointments,
            Runnable openConsultationRecords,
            Runnable openHealthRecord,
            Runnable openBills,
            Runnable openOrdinaryFollowUp,
            Runnable openCareGuide) {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        JPanel lead = new JPanel();
        lead.setOpaque(false);
        lead.setLayout(new BoxLayout(lead, BoxLayout.Y_AXIS));
        lead.add(createHero());
        lead.add(Box.createVerticalStrut(12));
        careTaskPanel.setOpaque(false);
        careTaskPanel.setVisible(false);
        lead.add(careTaskPanel);
        lead.add(Box.createVerticalStrut(10));
        billingTaskPanel.setOpaque(false);
        billingTaskPanel.setVisible(false);
        lead.add(billingTaskPanel);
        content.add(lead, BorderLayout.NORTH);

        JPanel services = HospitalResponsiveLayout.grid(3, 240, 14, 14);
        services.setOpaque(false);
        services.add(serviceCard("预约挂号", "逐级选择科室，按日期查看排班或搜索医生", true,
                openSlotSearch));
        services.add(serviceCard("我的预约", "查看科室、医生、候诊序号和费用", true,
                openMyAppointments));
        services.add(serviceCard("普通复诊", "从一次已完成的诊疗记录继续预约", true,
                openOrdinaryFollowUp));
        services.add(serviceCard("智能导诊", "描述主要不适，获得可解释的科室建议", true,
                openSmartTriage));
        services.add(serviceCard("问诊记录", "查看医生签署的诊断、处置和复诊建议", true,
                openConsultationRecords));
        services.add(serviceCard("费用清单", "查看待缴、已支付和已退款费用", true,
                openBills));
        services.add(serviceCard("健康档案", "分别查看患者自述、历史就诊与检查资料", true,
                openHealthRecord));
        services.add(serviceCard("就医指南", "了解就诊路线、复诊区别和就诊准备", true,
                openCareGuide));

        content.add(services, BorderLayout.CENTER);
        return content;
    }

    private JPanel billingTaskCard(int count, long amountCents, Runnable openBills) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.WARNING_LIGHT, 16, HospitalTheme.WARNING);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel("费用待办");
        caption.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        caption.setForeground(HospitalTheme.WARNING);
        JLabel title = new JLabel("有 " + count + " 笔费用待缴，共 "
                + String.format("¥%.2f", amountCents / 100.0));
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        title.setForeground(HospitalTheme.TEXT);
        copy.add(caption);
        copy.add(Box.createVerticalStrut(4));
        copy.add(title);
        JButton action = HospitalTheme.primaryButton("查看并模拟缴费");
        action.setName("patientHomeBillingButton");
        action.addActionListener(event -> openBills.run());
        card.add(copy, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private JPanel careTaskCard(ExaminationOrderView task) {
        boolean needsBooking = task.getStatus() == ExaminationStatus.RESULT_READY
                && !task.isResultReviewBooked();
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                needsBooking ? HospitalTheme.PRIMARY : HospitalTheme.PRIMARY_LIGHT,
                18,
                needsBooking ? HospitalTheme.PRIMARY_DARK : HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("当前诊疗待办");
        eyebrow.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        eyebrow.setForeground(needsBooking
                ? new Color(218, 247, 243) : HospitalTheme.MUTED);
        JLabel title = new JLabel(needsBooking
                ? "检查结果已出，请安排回诊"
                : task.isResultReviewBooked()
                        ? "检查结果回诊已安排"
                        : "检查正在等待报告");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 19F));
        title.setForeground(needsBooking ? Color.WHITE : HospitalTheme.PRIMARY_DARK);
        JLabel detail = new JLabel(task.getItemName() + "  ·  " + task.getDepartmentName());
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        detail.setForeground(needsBooking
                ? new Color(218, 247, 243) : HospitalTheme.MUTED);
        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(4));
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(detail);
        JButton action = needsBooking
                ? HospitalTheme.quietButton("立即安排回诊")
                : HospitalTheme.primaryButton(task.isResultReviewBooked()
                        ? "查看我的预约" : "查看检查进度");
        action.setName("patientHomeFollowUpButton");
        action.addActionListener(event -> {
            if (task.isResultReviewBooked()) {
                openMyAppointments.run();
            } else {
                openFollowUp.run();
            }
        });
        card.add(copy, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private JPanel createHero() {
        HospitalTheme.SurfacePanel hero = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY, 22);
        hero.setLayout(new BorderLayout(18, 0));
        hero.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("校园医疗服务导航");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20F));
        JLabel detail = new JLabel("可预约挂号、管理我的预约，并从健康档案进入历次就诊资料。");
        detail.setForeground(new Color(218, 247, 243));
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(detail);

        JButton action = HospitalTheme.quietButton("使用帮助");
        action.addActionListener(event -> showGuide());
        action.setPreferredSize(new Dimension(150, 42));
        hero.add(copy, BorderLayout.CENTER);
        hero.add(action, BorderLayout.EAST);
        return hero;
    }

    private JPanel createGuideContent() {
        JPanel guide = new JPanel(new BorderLayout(0, 18));
        guide.setBackground(HospitalTheme.BACKGROUND);
        guide.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        guide.setPreferredSize(new Dimension(650, 260));

        JLabel introduction = new JLabel(
                "<html><b style='font-size:16px'>第一次使用校医院？</b><br>"
                        + "按照下面三步即可完成当前已经开放的预约挂号流程。</html>");
        introduction.setForeground(HospitalTheme.TEXT);
        guide.add(introduction, BorderLayout.NORTH);

        JPanel steps = new JPanel(new GridLayout(1, 3, 12, 0));
        steps.setOpaque(false);
        steps.add(guideStep(
                "1",
                "选择使用模式",
                "患者模式面向所有已登录用户；医生和管理员模式需要相应权限。"));
        steps.add(guideStep(
                "2",
                "选择可用服务",
                "不确定挂哪个科室时，可先使用智能导诊，再从建议结果直接查看该科室号源。"));
        steps.add(guideStep(
                "3",
                "查找排班并预约",
                "逐级选择或直接搜索具体科室，再选择日期；也可搜索医生后确认排班。"));
        guide.add(steps, BorderLayout.CENTER);

        JLabel hint = new JLabel("关闭后可随时点击首页右上区域的“使用帮助”再次查看。");
        hint.setForeground(HospitalTheme.MUTED);
        guide.add(hint, BorderLayout.SOUTH);
        return guide;
    }

    private JPanel guideStep(
            String number,
            String titleText,
            String description) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel numberLabel = new JLabel(number, SwingConstants.CENTER);
        numberLabel.setOpaque(true);
        numberLabel.setBackground(HospitalTheme.PRIMARY_LIGHT);
        numberLabel.setForeground(HospitalTheme.PRIMARY_DARK);
        numberLabel.setFont(numberLabel.getFont().deriveFont(Font.BOLD, 18F));
        numberLabel.setPreferredSize(new Dimension(36, 36));

        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15F));
        title.setForeground(HospitalTheme.TEXT);
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        heading.add(numberLabel, BorderLayout.WEST);
        heading.add(title, BorderLayout.CENTER);

        JLabel detail = new JLabel(
                "<html><body style='width:150px'>" + description + "</body></html>");
        detail.setForeground(HospitalTheme.MUTED);
        card.add(heading, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        return card;
    }

    private JPanel serviceCard(
            String titleText,
            String description,
            boolean available,
            Runnable action) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));

        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16F));
        title.setForeground(available ? HospitalTheme.PRIMARY_DARK : HospitalTheme.TEXT);
        JLabel detail = new JLabel("<html><body style='width:150px'>" + description
                + "</body></html>");
        detail.setForeground(HospitalTheme.MUTED);

        JButton state = new JButton(available ? "立即使用" : "后续开放");
        state.setEnabled(available);
        state.setFocusPainted(false);
        if (available) {
            state.setForeground(HospitalTheme.PRIMARY_DARK);
            state.addActionListener(event -> action.run());
        }

        card.add(title, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        card.add(state, BorderLayout.SOUTH);
        return card;
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

    private static void refresh(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }
}
