package edu.seu.vcampus.client.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.util.function.Supplier;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** One application-level back arrow delegates to the active module's nearest back action. */
final class ModulePage extends JPanel {
    private final JComponent view;
    private final Runnable exit;
    private final JButton back = new JButton("←");
    private final JLabel heading = new JLabel();
    private final JLabel identity = new JLabel();
    private final String defaultTitle;
    private final Supplier<String> displayName;
    private final List<JLabel> headings = new ArrayList<>();
    private final Set<Component> retiredHeadings = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Component> observed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<JButton> navigation = new ArrayList<>();
    private JButton activeBack;
    private boolean refreshQueued;

    ModulePage(String moduleId, JComponent view, Runnable exit) {
        this(moduleId, moduleId, () -> "", view, exit);
    }

    ModulePage(String moduleId, String title, Supplier<String> displayName, JComponent view, Runnable exit) {
        super(new BorderLayout());
        this.view = view;
        this.exit = exit;
        this.defaultTitle = title;
        this.displayName = displayName;
        setBackground(new Color(244, 248, 247));
        JPanel bar = new JPanel(new BorderLayout());
        bar.setName("module.header." + moduleId);
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 226, 229)),
                BorderFactory.createEmptyBorder(12, 22, 12, 24)));
        back.setName("module.back." + moduleId);
        back.setUI(new RoundedButtonUI());
        back.setOpaque(false);
        back.setBackground(Color.WHITE);
        back.setForeground(new Color(15, 118, 110));
        back.setFont(back.getFont().deriveFont(23F));
        back.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        back.setPreferredSize(new Dimension(42, 36));
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.getAccessibleContext().setAccessibleName("返回上一页");
        back.addActionListener(event -> {
            refreshNavigation();
            if (activeBack == null) {
                exit.run();
            } else {
                activeBack.doClick();
                queueRefresh();
            }
        });
        JPanel navigationRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        navigationRow.setOpaque(false);
        heading.setName("module.title." + moduleId);
        heading.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        heading.setForeground(new Color(30, 47, 65));
        identity.setName("module.identity." + moduleId);
        identity.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        identity.setForeground(new Color(91, 108, 120));
        navigationRow.add(back);
        navigationRow.add(heading);
        bar.add(navigationRow, BorderLayout.WEST);
        bar.add(identity, BorderLayout.EAST);
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        page.add(bar, BorderLayout.NORTH);
        page.add(view, BorderLayout.CENTER);
        add(ResponsiveLayout.constrain(page), BorderLayout.CENTER);
        observe(view);
        refreshNavigation();
    }

    private void observe(Component component) {
        if (!observed.add(component)) {
            return;
        }
        ResponsiveLayout.prepare(component);
        if (component instanceof JComponent && component.getFont() != null) {
            Font font = component.getFont();
            component.setFont(new Font("Microsoft YaHei", font.getStyle(), font.getSize()));
        }
        if (component instanceof JLabel label
                && Boolean.TRUE.equals(label.getClientProperty("module.pageTitle"))) {
            headings.add(label);
            label.addPropertyChangeListener("text", event -> queueRefresh());
            retireHeading(label);
        }
        component.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent event) { queueRefresh(); }
            @Override public void componentHidden(ComponentEvent event) { queueRefresh(); }
        });
        if (component instanceof JButton button) {
            String text = button.getText();
            if (isNavigation(text)) {
                navigation.add(button);
                button.setVisible(false);
                button.addPropertyChangeListener("enabled", event -> queueRefresh());
            } else {
                Color foreground = button.getForeground();
                Color background = button.getBackground();
                button.setUI(new RoundedButtonUI());
                button.setForeground(foreground);
                button.setBackground(background);
                button.setOpaque(false);
                button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                button.setRolloverEnabled(true);
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                observe(child);
            }
            container.addContainerListener(new ContainerAdapter() {
                @Override public void componentAdded(ContainerEvent event) {
                    observe(event.getChild());
                    queueRefresh();
                }
                @Override public void componentRemoved(ContainerEvent event) {
                    forget(event.getChild());
                    queueRefresh();
                }
            });
        }
    }

    private void forget(Component component) {
        observed.remove(component);
        retiredHeadings.remove(component);
        if (component instanceof JLabel label) { headings.remove(label); }
        if (component instanceof JButton button) { navigation.remove(button); }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) { forget(child); }
        }
    }

    private static boolean isNavigation(String text) {
        return text != null && (text.startsWith("←") || text.startsWith("返回")
                || text.equals("切换使用模式") || text.equals("切换入口"));
    }

    private void queueRefresh() {
        if (!refreshQueued) {
            refreshQueued = true;
            SwingUtilities.invokeLater(() -> {
                refreshQueued = false;
                refreshNavigation();
            });
        }
    }

    /** Does not use isShowing, so detached/off-screen panels are testable as well. */
    void refreshNavigation() {
        identity.setText(displayName.get());
        JLabel currentHeading = null;
        int headingDepth = -1;
        for (JLabel candidate : headings) {
            int depth = visibleDepth(candidate.getParent());
            if (depth > headingDepth) {
                currentHeading = candidate;
                headingDepth = depth;
            }
        }
        heading.setText(currentHeading == null ? defaultTitle : currentHeading.getText());
        activeBack = null;
        int best = -1;
        for (JButton candidate : navigation) {
            if (Boolean.FALSE.equals(candidate.getClientProperty("navigation.available"))) {
                continue;
            }
            int depth = visibleDepth(candidate.getParent());
            if (depth < 0) {
                continue;
            }
            candidate.setVisible(false);
            // A page back action takes precedence over the mode-switch action.
            String text = candidate.getText();
            int score = depth + (text != null && text.startsWith("切换") ? 0 : 1000);
            if (score > best) {
                best = score;
                activeBack = candidate;
            }
        }
        back.setEnabled(activeBack == null || activeBack.isEnabled());
        back.setToolTipText(activeBack == null ? "返回校园服务"
                : activeBack.getToolTipText() != null ? activeBack.getToolTipText()
                : activeBack.getText());
    }

    private int visibleDepth(Container parent) {
        int depth = 0;
        for (Container current = parent; current != null; current = current.getParent()) {
            if (!current.isVisible() && !retiredHeadings.contains(current)) {
                return -1;
            }
            if (current == view) {
                return depth;
            }
            depth++;
        }
        return -1;
    }

    /** Hide only decorative title groups; controls such as refresh and recharge remain in place. */
    private void retireHeading(JLabel label) {
        Component group = label;
        for (Container parent = label.getParent(); parent != null && parent != view;
                parent = parent.getParent()) {
            // CardLayout owns page visibility; never retire a whole card, even an empty one.
            if (parent.getParent() != null
                    && parent.getParent().getLayout() instanceof java.awt.CardLayout) { break; }
            if (!decorative(parent)) { break; }
            group = parent;
        }
        retiredHeadings.add(group);
        group.setVisible(false);
    }

    private static boolean decorative(Component component) {
        if (component instanceof JComponent widget
                && Boolean.TRUE.equals(widget.getClientProperty("module.keepInPage"))) { return false; }
        if (component instanceof JLabel || component instanceof Box.Filler) { return true; }
        if (component instanceof JButton button) { return isNavigation(button.getText()); }
        if (!(component instanceof JPanel panel)) { return false; }
        for (Component child : panel.getComponents()) {
            if (!decorative(child)) { return false; }
        }
        return true;
    }
}
