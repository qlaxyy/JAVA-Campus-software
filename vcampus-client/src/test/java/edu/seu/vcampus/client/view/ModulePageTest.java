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
    @Test void retiredHeadersFollowActiveCardAndKeepTheirBackActionsWorking() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CardLayout cards = new CardLayout();
            JPanel view = new JPanel(cards);
            JPanel root = new JPanel(new BorderLayout());
            JPanel inner = new JPanel(new BorderLayout());
            javax.swing.JLabel rootTitle = new javax.swing.JLabel("校园图书馆");
            javax.swing.JLabel innerTitle = new javax.swing.JLabel("图书管理员工作台");
            rootTitle.putClientProperty("module.pageTitle", true);
            innerTitle.putClientProperty("module.pageTitle", true);
            JPanel rootHeader = new JPanel(); rootHeader.add(rootTitle);
            JPanel innerHeader = new JPanel(); innerHeader.add(innerTitle);
            JButton originalBack = new JButton("返回模式选择");
            originalBack.addActionListener(event -> cards.show(view, "root"));
            innerHeader.add(originalBack);
            root.add(rootHeader, BorderLayout.NORTH);
            inner.add(innerHeader, BorderLayout.NORTH);
            view.add(root, "root"); view.add(inner, "inner");
            ModulePage page = new ModulePage("library", "图书馆", () -> "张明", view, () -> fail());
            assertEquals("校园图书馆", namedLabel(page, "module.title.library").getText());
            cards.show(view, "inner"); page.refreshNavigation();
            assertEquals("图书管理员工作台", namedLabel(page, "module.title.library").getText());
            assertFalse(innerHeader.isVisible());
            backButton(page).doClick(); page.refreshNavigation();
            assertEquals("校园图书馆", namedLabel(page, "module.title.library").getText());
        });
    }

    @Test void sharedHeaderKeepsArrowTitleAndPureNameTogetherWithoutRemovingControls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel view = new JPanel(new BorderLayout());
            JPanel header = new JPanel(new BorderLayout());
            JPanel information = new JPanel();
            javax.swing.JLabel originalTitle = new javax.swing.JLabel("选择商店使用方式");
            originalTitle.setFont(originalTitle.getFont().deriveFont(24F));
            originalTitle.putClientProperty("module.pageTitle", true);
            information.add(originalTitle);
            information.add(new javax.swing.JLabel("超级管理员"));
            JButton refresh = new JButton("重新检查权限");
            header.add(information, BorderLayout.WEST);
            header.add(refresh, BorderLayout.EAST);
            view.add(header, BorderLayout.NORTH);
            ModulePage page = new ModulePage("shop", "校园商店", () -> "吴尚扬", view, () -> {});
            javax.swing.JLabel title = namedLabel(page, "module.title.shop");
            javax.swing.JLabel identity = namedLabel(page, "module.identity.shop");
            assertEquals("选择商店使用方式", title.getText());
            assertEquals("吴尚扬", identity.getText());
            assertSame(backButton(page).getParent(), title.getParent());
            assertFalse(information.isVisible());
            assertTrue(refresh.isVisible());
        });
    }

    private static javax.swing.JLabel namedLabel(java.awt.Container parent, String name) {
        for (java.awt.Component child : parent.getComponents()) {
            if (child instanceof javax.swing.JLabel label && name.equals(label.getName())) { return label; }
            if (child instanceof java.awt.Container container) {
                javax.swing.JLabel result = namedLabel(container, name);
                if (result != null) { return result; }
            }
        }
        return null;
    }

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
            JButton back = backButton(page);
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
            JButton back = backButton(page);
            inner.setEnabled(false); page.refreshNavigation();
            assertFalse(back.isEnabled());
            inner.setEnabled(true); page.refreshNavigation(); back.doClick();
            assertEquals(1, count.get());
            assertFalse(mode.isVisible()); assertFalse(inner.isVisible());
        });
    }

    private static JButton backButton(java.awt.Container parent) {
        for (java.awt.Component child : parent.getComponents()) {
            if (child instanceof JButton button && "←".equals(button.getText())) { return button; }
            if (child instanceof java.awt.Container container) {
                JButton result = backButton(container);
                if (result != null) { return result; }
            }
        }
        return null;
    }
}
