package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** Entry page that separates account permissions from the active hospital workspace. */
final class HospitalModePanel extends JPanel {

    private final JLabel accountLabel = new JLabel("尚未登录");
    private final JLabel statusLabel = new JLabel("请先登录", SwingConstants.CENTER);
    private final JButton patientButton = HospitalTheme.primaryButton("检查权限中");
    private final JButton doctorButton = HospitalTheme.quietButton("检查权限中");
    private final JButton adminButton = HospitalTheme.quietButton("检查权限中");

    HospitalModePanel(
            Runnable openPatient,
            Runnable openDoctor,
            Runnable openAdmin,
            Runnable refreshAccess) {
        setLayout(new BorderLayout(0, 20));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        add(createHeader(refreshAccess), BorderLayout.NORTH);
        add(createModes(openPatient, openDoctor, openAdmin), BorderLayout.CENTER);

        statusLabel.setForeground(HospitalTheme.MUTED);
        add(statusLabel, BorderLayout.SOUTH);
        disableAllButtons("检查权限中");
    }

    void showLoading(SessionInfo session) {
        accountLabel.setText(accountText(session));
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setText("正在由服务器检查当前账号可进入的医院模式……");
        disableAllButtons("检查权限中");
    }

    void showAccess(SessionInfo session, HospitalModeAccessView access) {
        accountLabel.setText(accountText(session));
        configure(patientButton, access.canAccess(HospitalMode.PATIENT), "进入患者模式");
        configure(doctorButton, access.canAccess(HospitalMode.DOCTOR), "进入医生模式");
        configure(adminButton, access.canAccess(HospitalMode.ADMIN), "进入管理模式");
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setText("模式只切换当前工作台；实际权限由服务器根据账号和医院绑定判断。");
    }

    void showLoginRequired() {
        accountLabel.setText("尚未登录");
        disableAllButtons("登录后检查");
        statusLabel.setForeground(HospitalTheme.WARNING);
        statusLabel.setText("请先到“用户管理”登录，再返回校医院选择模式。");
    }

    void showError(String message) {
        disableAllButtons("暂不可用");
        statusLabel.setForeground(HospitalTheme.WARNING);
        statusLabel.setText(message);
    }

    private JPanel createHeader(Runnable refreshAccess) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("选择校医院使用模式");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("同一账号可以拥有多个身份，但一次只进入一个工作台");
        subtitle.setForeground(HospitalTheme.MUTED);
        accountLabel.setForeground(HospitalTheme.PRIMARY_DARK);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        copy.add(Box.createVerticalStrut(8));
        copy.add(accountLabel);

        JButton refresh = HospitalTheme.quietButton("重新检查权限");
        refresh.addActionListener(event -> refreshAccess.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JPanel createModes(
            Runnable openPatient,
            Runnable openDoctor,
            Runnable openAdmin) {
        JPanel modes = new JPanel(new GridLayout(1, 3, 16, 0));
        modes.setOpaque(false);
        patientButton.addActionListener(event -> openPatient.run());
        doctorButton.addActionListener(event -> openDoctor.run());
        adminButton.addActionListener(event -> openAdmin.run());
        modes.add(modeCard(
                "患者模式",
                "所有已登录用户均可使用",
                "预约挂号<br>问诊记录<br>费用清单<br>健康档案",
                patientButton));
        modes.add(modeCard(
                "医生模式",
                "需要有效的医院医生绑定",
                "我的排班<br>待接诊患者<br>患者就诊背景信息<br>诊断与处置",
                doctorButton));
        modes.add(modeCard(
                "管理员模式",
                "需要医院管理范围授权",
                "科室管理<br>医生管理<br>排班管理<br>号源与预约管理",
                adminButton));
        return modes;
    }

    private JPanel modeCard(
            String titleText,
            String requirement,
            String features,
            JButton action) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel rule = new JLabel(requirement);
        rule.setForeground(HospitalTheme.MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(7));
        heading.add(rule);

        JLabel featureList = new JLabel(
                "<html><body style='line-height:1.8'>" + features + "</body></html>");
        featureList.setForeground(HospitalTheme.TEXT);
        action.setPreferredSize(new Dimension(0, 42));
        card.add(heading, BorderLayout.NORTH);
        card.add(featureList, BorderLayout.CENTER);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private void disableAllButtons(String text) {
        disable(patientButton, text);
        disable(doctorButton, text);
        disable(adminButton, text);
    }

    private static void configure(JButton button, boolean enabled, String enabledText) {
        button.setEnabled(enabled);
        button.setText(enabled ? enabledText : "无权限");
    }

    private static void disable(JButton button, String text) {
        button.setEnabled(false);
        button.setText(text);
    }

    private static String accountText(SessionInfo session) {
        return "当前账号：" + session.getDisplayName() + "（" + session.getRole() + "）";
    }
}
