package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** Patient-facing campus hospital visit guide. */
final class PatientCareGuidePanel extends JPanel {

    PatientCareGuidePanel(
            Runnable back,
            Runnable openRegistration,
            Runnable openFollowUp,
            Runnable openHealthRecord) {
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(HospitalResponsiveLayout.constrainWidth(createHeader(back)),
                BorderLayout.NORTH);
        add(createContent(openRegistration, openFollowUp, openHealthRecord),
                BorderLayout.CENTER);
    }

    private JComponent createHeader(Runnable back) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JPanel copy = verticalPanel();
        JLabel title = new JLabel("就医指南");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 28F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("第一次来校医院时，先确认自己应该走哪条就诊路线");
        subtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        JButton backButton = HospitalTheme.quietButton("返回医院首页");
        backButton.addActionListener(event -> back.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(backButton, BorderLayout.EAST);
        return header;
    }

    private JComponent createContent(
            Runnable openRegistration,
            Runnable openFollowUp,
            Runnable openHealthRecord) {
        JPanel content = verticalPanel();
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        content.add(createEmergencyNotice());
        content.add(Box.createVerticalStrut(14));
        content.add(createRouteBoard());
        content.add(Box.createVerticalStrut(14));
        content.add(createRouteChoices(openRegistration, openFollowUp, openHealthRecord));
        content.add(Box.createVerticalStrut(14));
        content.add(createPreparationCard());
        content.add(Box.createVerticalStrut(14));
        content.add(createServiceInformation());

        return HospitalResponsiveLayout.verticalScroll(content);
    }

    private JComponent createEmergencyNotice() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.WARNING_LIGHT, 14, HospitalTheme.WARNING);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel marker = new JLabel("紧急情况");
        marker.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
        marker.setForeground(HospitalTheme.WARNING);
        JLabel text = new JLabel(
                "<html><b>胸痛、呼吸困难、意识异常或严重出血时，不要等待线上挂号。</b>"
                        + "<br>请立即联系校内急救人员或拨打 120。</html>");
        text.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        text.setForeground(HospitalTheme.TEXT);
        card.add(marker, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JComponent createRouteBoard() {
        HospitalTheme.SurfacePanel board = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_DARK, 16);
        board.setLayout(new BorderLayout(0, 14));
        board.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        JLabel caption = new JLabel("CAMPUS CLINIC · 就诊路线");
        caption.setFont(HospitalTheme.dataFont(Font.BOLD, 12F));
        caption.setForeground(HospitalTheme.NAVIGATION_MUTED);
        board.add(caption, BorderLayout.NORTH);

        JPanel route = HospitalResponsiveLayout.grid(4, 145, 10, 10);
        route.setOpaque(false);
        route.add(routeStep("01", "选择科室", "按症状选择具体科室"));
        route.add(routeStep("02", "预约排班", "确认医生、日期与余号"));
        route.add(routeStep("03", "按号就诊", "查看预约和候诊序号"));
        route.add(routeStep("04", "查看后续", "诊疗记录、检查与费用"));
        board.add(route, BorderLayout.CENTER);
        return board;
    }

    private JComponent routeStep(String number, String titleText, String detailText) {
        JPanel step = verticalPanel();
        step.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JLabel numberLabel = new JLabel(number);
        numberLabel.setFont(HospitalTheme.dataFont(Font.BOLD, 13F));
        numberLabel.setForeground(HospitalTheme.NAVIGATION_MUTED);
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        title.setForeground(Color.WHITE);
        JLabel detail = new JLabel("<html>" + detailText + "</html>");
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        detail.setForeground(new Color(210, 232, 228));
        step.add(numberLabel);
        step.add(Box.createVerticalStrut(6));
        step.add(title);
        step.add(Box.createVerticalStrut(4));
        step.add(detail);
        return step;
    }

    private JComponent createRouteChoices(
            Runnable openRegistration,
            Runnable openFollowUp,
            Runnable openHealthRecord) {
        JPanel section = verticalPanel();
        JLabel title = sectionTitle("根据你的情况选择入口");
        section.add(title);
        section.add(Box.createVerticalStrut(10));
        JPanel choices = HospitalResponsiveLayout.grid(3, 230, 14, 14);
        choices.setOpaque(false);
        choices.add(choiceCard(
                "第一次看这个问题",
                "选择具体科室、医生和日期，建立一次新的初诊预约。",
                "预约挂号",
                "guideRegistrationButton",
                openRegistration));
        choices.add(choiceCard(
                "以前看过，需要再次就诊",
                "从一条已完成的诊疗记录发起普通复诊，并重新选择排班。",
                "选择复诊记录",
                "guideFollowUpButton",
                openFollowUp));
        choices.add(choiceCard(
                "医生让我检查后回来",
                "先在健康档案查看检查状态；报告出具后再申请检查结果回诊。",
                "查看检查与回诊",
                "guideHealthRecordButton",
                openHealthRecord));
        section.add(choices);
        return section;
    }

    private JComponent choiceCard(
            String titleText,
            String detailText,
            String actionText,
            String actionName,
            Runnable action) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(230, 158));
        JLabel title = new JLabel("<html><b>" + titleText + "</b></html>");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 15F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel detail = new JLabel("<html>" + detailText + "</html>");
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        detail.setForeground(HospitalTheme.MUTED);
        JButton button = HospitalTheme.quietButton(actionText);
        button.setName(actionName);
        button.addActionListener(event -> action.run());
        card.add(title, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        card.add(button, BorderLayout.SOUTH);
        return card;
    }

    private JComponent createPreparationCard() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 14, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel left = verticalPanel();
        left.add(sectionTitle("就诊前准备"));
        left.add(Box.createVerticalStrut(8));
        left.add(body("• 确认预约日期、时段、医生和候诊序号"));
        left.add(body("• 准备校园身份信息及既往检查资料"));
        JPanel right = verticalPanel();
        right.add(sectionTitle("向医生说明"));
        right.add(Box.createVerticalStrut(8));
        right.add(body("• 当前症状、开始时间和变化情况"));
        right.add(body("• 过敏史、既往情况和正在使用的药物"));
        JPanel columns = HospitalResponsiveLayout.grid(2, 280, 22, 12);
        columns.setOpaque(false);
        columns.add(left);
        columns.add(right);
        card.add(columns, BorderLayout.CENTER);
        return card;
    }

    private JComponent createServiceInformation() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel title = sectionTitle("开放时间与地点");
        JLabel detail = new JLabel(
                "<html>课程项目不保存真实校医院地址和排班政策。具体开放时间、地点、"
                        + "停诊通知及急救联系方式请以学校官方公告为准。</html>");
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        detail.setForeground(HospitalTheme.MUTED);
        card.add(title, BorderLayout.WEST);
        card.add(detail, BorderLayout.CENTER);
        return card;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        label.setForeground(HospitalTheme.TEXT);
        return label;
    }

    private JLabel body(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        label.setForeground(HospitalTheme.MUTED);
        return label;
    }

    private JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
