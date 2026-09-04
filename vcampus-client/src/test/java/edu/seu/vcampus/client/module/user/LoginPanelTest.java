package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.user.CampusCardNumber;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertTrue(labels.contains("欢迎登录"));
        assertTrue(labels.contains("开发阶段测试账号"));
        assertTrue(labels.contains("以下账号统一密码：123456"));
        assertFalse(labels.contains("用户登录"));
        assertFalse(labels.contains("开发期基础登录"));
        assertTrue(labels.contains("普通账号  20260001"));
        assertTrue(labels.contains("医生演示  20260002"));
        assertTrue(labels.contains("超级管理员  20260003"));
        assertTrue(labels.contains("学籍管理员  20260004"));
        assertTrue(labels.contains("选课管理员  20260005"));
        assertTrue(labels.contains("图书馆管理员  20260006"));
        assertTrue(labels.contains("商店管理员  20260007"));
        assertTrue(labels.contains("医院管理员  20260008"));
    }

    @Test
    void credentialValidationRejectsMissingInput() {
        assertEquals("请输入一卡通号", LoginPanel.validationMessage(" ", "secret".toCharArray()));
        assertEquals("一卡通号必须是 8 位数字（年份 + 4 位流水号）",
                LoginPanel.validationMessage("AAA", "secret".toCharArray()));
        assertEquals("请输入密码", LoginPanel.validationMessage("20260001", new char[0]));
        assertNull(LoginPanel.validationMessage("20260001", "123456".toCharArray()));
        assertEquals("20260009", CampusCardNumber.format(2026, 9));
        assertEquals(9, CampusCardNumber.sequence("20260009"));
        assertFalse(CampusCardNumber.isValid("20260000"));
        assertThrows(IllegalArgumentException.class,
                () -> CampusCardNumber.format(2026, 10_000));
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
