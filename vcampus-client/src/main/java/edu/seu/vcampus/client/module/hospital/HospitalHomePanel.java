package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** Patient-facing hospital landing page. */
final class HospitalHomePanel extends JPanel {

    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    HospitalHomePanel(Runnable openSlotSearch, Runnable switchMode) {
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        add(createHeader(switchMode), BorderLayout.NORTH);
        add(createContent(openSlotSearch), BorderLayout.CENTER);

        messageLabel.setForeground(HospitalTheme.WARNING);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        add(messageLabel, BorderLayout.SOUTH);
    }

    void showMessage(String message) {
        messageLabel.setText(message);
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
        JLabel subtitle = new JLabel("校园医疗服务 · 先完成查询，再逐步扩展完整就诊流程");
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

    private JPanel createContent(Runnable openSlotSearch) {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(createHero(openSlotSearch), BorderLayout.NORTH);

        JPanel services = new JPanel(new GridLayout(2, 3, 14, 14));
        services.setOpaque(false);
        services.add(serviceCard("预约挂号", "查询未来 7 天的科室与医生号源", true,
                openSlotSearch));
        services.add(serviceCard("智能导诊", "描述症状，获得科室建议", false, null));
        services.add(serviceCard("问诊记录", "查看诊断、处置与复诊状态", false, null));
        services.add(serviceCard("费用清单", "查看待缴和历史费用", false, null));
        services.add(serviceCard("健康档案", "汇总个人健康与就诊信息", false, null));
        services.add(serviceCard("就医指南", "校医院时间、地点与注意事项", false, null));
        content.add(services, BorderLayout.CENTER);
        return content;
    }

    private JPanel createHero(Runnable openSlotSearch) {
        HospitalTheme.SurfacePanel hero = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY, 22);
        hero.setLayout(new BorderLayout(18, 0));
        hero.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("需要看医生？从预约挂号开始");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20F));
        JLabel detail = new JLabel("按科室、日期和医生筛选号源；满号信息也会如实显示。");
        detail.setForeground(new Color(218, 247, 243));
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(detail);

        JButton action = HospitalTheme.quietButton("进入预约挂号");
        action.addActionListener(event -> openSlotSearch.run());
        action.setPreferredSize(new Dimension(150, 42));
        hero.add(copy, BorderLayout.CENTER);
        hero.add(action, BorderLayout.EAST);
        return hero;
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
}
