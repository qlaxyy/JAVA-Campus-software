package edu.seu.vcampus.client.module.hospital;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

/** Shared left-to-right navigation and title hierarchy for hospital subpages. */
final class HospitalPageHeader {

    private HospitalPageHeader() {
    }

    static JPanel create(
            String titleText,
            String subtitleText,
            String backDestination,
            Runnable back,
            JComponent trailing) {
        JPanel navigation = new JPanel(new BorderLayout(14, 0));
        navigation.setOpaque(false);
        JButton backButton = HospitalTheme.backButton(backDestination);
        backButton.addActionListener(event -> back.run());
        navigation.add(backButton, BorderLayout.WEST);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JTextArea subtitle = HospitalResponsiveLayout.wrappingText(
                subtitleText,
                HospitalTheme.uiFont(Font.PLAIN, 13F),
                HospitalTheme.MUTED);
        subtitle.setName("hospitalPageSubtitle");
        subtitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        copy.add(title);
        copy.add(Box.createVerticalStrut(3));
        copy.add(subtitle);
        navigation.add(copy, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        if (trailing == null) {
            header.add(navigation, BorderLayout.CENTER);
        } else {
            header.add(HospitalResponsiveLayout.adaptiveRow(
                    navigation, trailing, 760, 10), BorderLayout.CENTER);
        }
        return header;
    }
}
