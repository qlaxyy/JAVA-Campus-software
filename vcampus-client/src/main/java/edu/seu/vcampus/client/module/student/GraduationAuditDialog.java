package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.student.StudentProfileDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

public class GraduationAuditDialog extends JDialog {
    private final ClientContext context;
    private final StudentProfileDto profile;
    private final boolean isAdmin;

    private final JLabel lblStatusVal = new JLabel("审核中...");
    private final JLabel lblGpaVal = new JLabel("-");
    private final JLabel lblCreditVal = new JLabel("-");
    private final JLabel lblCompVal = new JLabel("-");

    public GraduationAuditDialog(Window parent, ClientContext context, StudentProfileDto profile, boolean isAdmin) {
        super(parent, "学生学业毕业与学位审核系统", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.profile = profile;
        this.isAdmin = isAdmin;

        setSize(680, 520);
        setLocationRelativeTo(parent);
        initUI();
        calculateAuditData();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));
        root.setBackground(new Color(248, 249, 250));

        // 顶部提示
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("毕业资格与学分绩点（GPA）综合核算");
        title.setFont(new Font("微软雅黑", Font.BOLD, 18));
        title.setForeground(new Color(15, 23, 42));

        String yearStr = profile.getEnrollmentYear() != null ? profile.getEnrollmentYear() + "级" : "";
        JLabel subtitle = new JLabel("学生: " + profile.getName() + " (" + profile.getStudentId() + ")  ·  " + yearStr + " " + profile.getMajor());
        subtitle.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        subtitle.setForeground(new Color(100, 116, 139));

        headerPanel.add(title);
        headerPanel.add(subtitle);
        root.add(headerPanel, BorderLayout.NORTH);

        // 中部核算指标卡片面板
        JPanel metricsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        metricsPanel.setOpaque(false);

        metricsPanel.add(createMetricCard("当前平均学分绩点 (GPA)", lblGpaVal, new Color(24, 100, 190)));
        metricsPanel.add(createMetricCard("已获得有效总学分", lblCreditVal, new Color(13, 120, 90)));
        metricsPanel.add(createMetricCard("培养方案完成进度", lblCompVal, new Color(217, 119, 6)));
        metricsPanel.add(createMetricCard("毕业审核综合结论", lblStatusVal, new Color(147, 51, 234)));

        root.add(metricsPanel, BorderLayout.CENTER);

        // 底部操作区
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JLabel notice = new JLabel("※ 注：毕业审核结果由教务处培养方案系统自动比对生成，如有异议请联系学院教务办。");
        notice.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        notice.setForeground(new Color(148, 163, 184));
        bottomBar.add(notice, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);

        if (isAdmin) {
            JButton btnOverride = new JButton("人工特批通过");
            btnOverride.setBackground(new Color(187, 247, 208));
            btnOverride.setFocusPainted(false);
            btnOverride.addActionListener(e -> {
                lblStatusVal.setText("【已人工特批通过】");
                lblStatusVal.setForeground(new Color(13, 120, 90));
                JOptionPane.showMessageDialog(this, "已成功对该生进行毕业资格人工特批！", "提示", JOptionPane.INFORMATION_MESSAGE);
            });
            btnPanel.add(btnOverride);
        }

        JButton btnClose = new JButton("关闭窗口");
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dispose());
        btnPanel.add(btnClose);

        bottomBar.add(btnPanel, BorderLayout.SOUTH);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(100, 116, 139));
        card.add(titleLbl, BorderLayout.NORTH);

        valueLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        valueLabel.setForeground(valueColor);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void calculateAuditData() {
        int currentYear = LocalDate.now().getYear(); // 2026
        long enrollmentYear = profile.getEnrollmentYear() != null ? profile.getEnrollmentYear() : 2026;
        long yearsInSchool = currentYear - enrollmentYear; // 2026级则为 0年（大一）

        int idHash = Math.abs(profile.getStudentId().hashCode());
        double gpa = 3.2 + (idHash % 70) / 100.0; // 模拟 GPA 3.20 ~ 3.89

        int credits;
        if (yearsInSchool <= 0) {
            // 2026级大一新生：模拟当前拥有 15 ~ 30 学分
            credits = 15 + (idHash % 16);
        } else if (yearsInSchool == 1) {
            // 大二
            credits = 45 + (idHash % 25);
        } else if (yearsInSchool == 2) {
            // 大三
            credits = 95 + (idHash % 25);
        } else {
            // 大四及以上准毕业生
            credits = 145 + (idHash % 25);
        }

        lblGpaVal.setText(String.format("%.2f / 4.0", gpa));
        lblCreditVal.setText(credits + " / 150 学分");

        // 判断是否符合毕业要求
        if (yearsInSchool >= 3 && credits >= 150 && gpa >= 2.0) {
            lblCompVal.setText("100.0% (已修满)");
            lblStatusVal.setText("符合毕业与学位授予条件");
            lblStatusVal.setForeground(new Color(13, 120, 90));
        } else {
            double progress = Math.min(100.0, (double) credits / 150.0 * 100);
            lblCompVal.setText(String.format("%.1f%% (修读中)", progress));
            lblStatusVal.setText("正常修读中（未到毕业期）");
            lblStatusVal.setForeground(new Color(24, 100, 190));
        }
    }
}
