package edu.seu.vcampus.client.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
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
    private final Set<Component> observed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<JButton> navigation = new ArrayList<>();
    private JButton activeBack;
    private boolean refreshQueued;

    ModulePage(String moduleId, JComponent view, Runnable exit) {
        super(new BorderLayout());
        this.view = view;
        this.exit = exit;
        setBackground(new Color(244, 248, 247));
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(getBackground());
        bar.setBorder(BorderFactory.createEmptyBorder(8, 18, 6, 18));
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
        bar.add(back, BorderLayout.WEST);
        add(bar, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        observe(view);
        refreshNavigation();
    }

    private void observe(Component component) {
        if (!observed.add(component)) {
            return;
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
                button.setUI(new RoundedButtonUI());
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
            if (!current.isVisible()) {
                return -1;
            }
            if (current == view) {
                return depth;
            }
            depth++;
        }
        return -1;
    }
}
