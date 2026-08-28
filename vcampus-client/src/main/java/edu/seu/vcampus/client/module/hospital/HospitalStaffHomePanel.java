package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/** Honest information architecture for staff workspaces not implemented yet. */
final class HospitalStaffHomePanel extends JPanel {

    HospitalStaffHomePanel(
            String titleText,
            String subtitleText,
            List<WorkspaceFeature> features,
            Runnable switchMode) {
        setLayout(new BorderLayout(0, 20));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(createHeader(titleText, subtitleText, switchMode), BorderLayout.NORTH);
        add(createFeatureGrid(features), BorderLayout.CENTER);
        JLabel notice = new JLabel("当前已建立工作台结构，具体业务将在后续提交中逐项接入。");
        notice.setForeground(HospitalTheme.MUTED);
        add(notice, BorderLayout.SOUTH);
    }

    private JPanel createHeader(String titleText, String subtitleText, Runnable switchMode) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel(subtitleText);
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

    private JPanel createFeatureGrid(List<WorkspaceFeature> features) {
        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setOpaque(false);
        features.forEach(feature -> grid.add(featureCard(feature)));
        return grid;
    }

    private JPanel featureCard(WorkspaceFeature feature) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 18, 20));
        JLabel title = new JLabel(feature.title());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel detail = new JLabel("<html><body style='width:220px'>"
                + feature.description() + "</body></html>");
        detail.setForeground(HospitalTheme.MUTED);
        JButton state = new JButton("后续实现");
        state.setEnabled(false);
        card.add(title, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        card.add(state, BorderLayout.SOUTH);
        return card;
    }

    record WorkspaceFeature(String title, String description) {
    }
}
