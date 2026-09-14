package edu.seu.vcampus.client.view;

import org.junit.jupiter.api.Test;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ModulePageTest {
    @Test void singleArrowUsesVisibleInnerBackThenReturnsToCampusHome() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CardLayout layout = new CardLayout();
            JPanel view = new JPanel(layout);
            JPanel root = new JPanel();
            JPanel inner = new JPanel();
            AtomicInteger returns = new AtomicInteger();
            AtomicInteger exits = new AtomicInteger();
            JButton innerBack = new JButton("← 返回模式选择");
            innerBack.addActionListener(event -> { returns.incrementAndGet(); layout.show(view, "root"); });
            inner.add(innerBack);
            view.add(root, "root"); view.add(inner, "inner");
            ModulePage page = new ModulePage("hospital", view, exits::incrementAndGet);
            JButton back = (JButton) ((JPanel) page.getComponent(0)).getComponent(0);
            assertEquals("←", back.getText());
            assertFalse(innerBack.isVisible());
            layout.show(view, "inner"); page.refreshNavigation(); back.doClick();
            assertEquals(1, returns.get()); assertEquals(0, exits.get());
            page.refreshNavigation(); back.doClick();
            assertEquals(1, exits.get());
        });
    }

    @Test void closestBackOverridesModeSwitchAndMirrorsDisabledState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel view = new JPanel(new BorderLayout());
            JButton mode = new JButton("切换使用模式");
            view.add(mode, BorderLayout.NORTH);
            JPanel body = new JPanel();
            JButton inner = new JButton("返回患者首页");
            AtomicInteger count = new AtomicInteger();
            inner.addActionListener(event -> count.incrementAndGet());
            body.add(inner); view.add(body);
            ModulePage page = new ModulePage("hospital", view, () -> fail("must not exit"));
            JButton back = (JButton) ((JPanel) page.getComponent(0)).getComponent(0);
            inner.setEnabled(false); page.refreshNavigation();
            assertFalse(back.isEnabled());
            inner.setEnabled(true); page.refreshNavigation(); back.doClick();
            assertEquals(1, count.get());
            assertFalse(mode.isVisible()); assertFalse(inner.isVisible());
        });
    }
}
