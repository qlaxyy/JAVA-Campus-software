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

/** Entry page for the hospital administrator's operational workspaces. */
final class HospitalStaffHomePanel extends JPanel {

    HospitalStaffHomePanel(
            String titleText,
            String subtitleText,
            List<WorkspaceFeature> features,
            Runnable switchMode) {
        setLayout(new BorderLayout(0, 20));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(HospitalResponsiveLayout.constrainWidth(
                createHeader(titleText, subtitleText, switchMode)), BorderLayout.NORTH);
        add(HospitalResponsiveLayout.verticalScroll(
                createFeatureGrid(features)), BorderLayout.CENTER);
    }

    private JPanel createHeader(String titleText, String subtitleText, Runnable switchMode) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.putClientProperty("module.pageTitle", true);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);

        JButton switchButton = HospitalTheme.quietButton("切换使用模式");
        switchButton.addActionListener(event -> switchMode.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(switchButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createFeatureGrid(List<WorkspaceFeature> features) {
        JPanel grid = HospitalResponsiveLayout.grid(2, 300, 16, 16);
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
        javax.swing.JTextArea detail = HospitalResponsiveLayout.wrappingText(
                feature.description(), HospitalTheme.uiFont(Font.PLAIN, 13F),
                HospitalTheme.MUTED);
        boolean available = feature.action() != null;
        JButton state = available
                ? HospitalTheme.primaryButton(feature.actionText())
                : new JButton("后续实现");
        state.setEnabled(available);
        if (available) {
            state.addActionListener(event -> feature.action().run());
        } else {
            HospitalTheme.applyDisabledStyle(state);
        }
        card.add(title, BorderLayout.NORTH);
        card.add(state, BorderLayout.SOUTH);
        return card;
    }

    record WorkspaceFeature(
            String title,
            String description,
            String actionText,
            Runnable action) {
        WorkspaceFeature(String title, String description) {
            this(title, description, "后续实现", null);
        }
    }
}
