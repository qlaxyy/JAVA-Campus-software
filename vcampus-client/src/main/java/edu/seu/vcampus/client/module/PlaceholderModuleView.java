package edu.seu.vcampus.client.module;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Temporary view replaced independently by each module owner.
 */
public final class PlaceholderModuleView {

    private PlaceholderModuleView() {
    }

    /**
     * Creates a clearly marked placeholder that keeps navigation runnable.
     *
     * @param moduleName Chinese module name
     * @param firstGoal first development milestone
     * @return placeholder panel
     */
    public static JPanel create(String moduleName, String firstGoal) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel title = new JLabel(moduleName, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24F));
        JLabel hint = new JLabel("第一阶段：" + firstGoal, SwingConstants.CENTER);
        hint.setFont(hint.getFont().deriveFont(15F));

        panel.add(title, BorderLayout.NORTH);
        panel.add(hint, BorderLayout.CENTER);
        return panel;
    }
}
