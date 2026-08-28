package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginPanelTest {

    @Test
    void initialViewOffersCredentialFieldsLoginActionAndTestAccounts() throws Exception {
        AtomicReference<LoginPanel> panelReference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panelReference.set(new LoginPanel(
                new ClientContext(new CampusClient("127.0.0.1", 1)))));

        List<Component> components = descendants(panelReference.get());
        List<String> labels = components.stream()
                .filter(JLabel.class::isInstance)
                .map(JLabel.class::cast)
                .map(JLabel::getText)
                .toList();
        List<String> buttons = components.stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .map(JButton::getText)
                .toList();

        assertEquals(List.of("登录"), buttons);
        assertFalse(labels.contains("用户登录"));
        assertFalse(labels.contains("开发期基础登录"));
        assertTrue(labels.contains("学生：student001 / 123456"));
        assertTrue(labels.contains("教师：teacher001 / 123456"));
        assertTrue(labels.contains("超级管理员：admin / 123456"));
        assertTrue(labels.contains("学籍管理员（学生）：studentadmin / 123456"));
        assertTrue(labels.contains("选课管理员（学生）：courseadmin / 123456"));
        assertTrue(labels.contains("图书管理员（学生）：libraryadmin / 123456"));
        assertTrue(labels.contains("商店管理员（学生）：shopadmin / 123456"));
        assertTrue(labels.contains("医院管理员（患者）：hospitaladmin / 123456"));
    }

    @Test
    void credentialValidationRejectsMissingInput() {
        assertEquals("请输入账户名", LoginPanel.validationMessage(" ", "secret".toCharArray()));
        assertEquals("请输入密码", LoginPanel.validationMessage("student001", new char[0]));
        assertNull(LoginPanel.validationMessage("student001", "123456".toCharArray()));
    }

    private static List<Component> descendants(JPanel panel) {
        List<Component> result = new ArrayList<>();
        collect(panel, result);
        return result;
    }

    private static void collect(Container container, List<Component> result) {
        for (Component component : container.getComponents()) {
            result.add(component);
            if (component instanceof Container child) {
                collect(child, result);
            }
        }
    }
}
