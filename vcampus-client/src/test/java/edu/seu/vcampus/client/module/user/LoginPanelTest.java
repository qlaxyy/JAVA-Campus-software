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
    @Test void loginCompositionStaysCenteredAndCompactAtEveryWindowSize() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel panel = new LoginPanel(new ClientContext(new CampusClient("127.0.0.1", 1)));
            try { java.nio.file.Files.createDirectories(java.nio.file.Path.of("target", "ui-review")); }
            catch (java.io.IOException exception) { throw new AssertionError(exception); }
            for (int[] size : new int[][]{{900, 560}, {1150, 650}, {1920, 1080}, {900, 560}}) {
                panel.setSize(size[0], size[1]);
                for (int pass = 0; pass < 4; pass++) { layoutTree(panel); }
                Component composition = descendants(panel).stream()
                        .filter(c -> "login.composition".equals(c.getName())).findFirst().orElseThrow();
                assertTrue(composition.getWidth() <= 1100);
                assertTrue(composition.getHeight() <= 600);
                assertTrue(Math.abs(composition.getX() * 2 + composition.getWidth() - size[0]) <= 1);
                assertTrue(Math.abs(composition.getY() * 2 + composition.getHeight() - size[1]) <= 1);
                JLabel brand = (JLabel) descendants(panel).stream()
                        .filter(c -> "login.brandTitle".equals(c.getName())).findFirst().orElseThrow();
                assertEquals(javax.swing.SwingConstants.CENTER, brand.getHorizontalAlignment());
                assertTrue(Math.abs(brand.getX() * 2 + brand.getWidth() - brand.getParent().getWidth()) <= 1);
                Component username = descendants(panel).stream()
                        .filter(c -> "login.username".equals(c.getName())).findFirst().orElseThrow();
                assertTrue(username.getWidth() >= 280 && username.getWidth() <= 420);
                assertEquals(42, username.getHeight());
                java.awt.Point inputPosition = SwingUtilities.convertPoint(username.getParent(), username.getLocation(), composition);
                assertTrue(inputPosition.x > composition.getWidth() / 2, "login inputs belong on the right");
                assertEquals(0, brand.getParent().getX(), "campus illustration belongs on the left");
                java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size[0], size[1], java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D graphics = image.createGraphics(); panel.paint(graphics); graphics.dispose();
                try { javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("target", "ui-review", "login-new-" + size[0] + ".png").toFile()); }
                catch (java.io.IOException exception) { throw new AssertionError(exception); }
            }
        });
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child && child.isVisible()) { layoutTree(child); }
        }
    }


    @Test
    void initialViewOffersCredentialsWithoutPublicDemoAccounts() throws Exception {
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
        assertFalse(labels.contains("开发阶段测试账号"));
        assertFalse(labels.contains("以下账号统一密码：123456"));
        assertFalse(labels.contains("用户登录"));
        assertFalse(labels.contains("开发期基础登录"));
        assertTrue(labels.stream().noneMatch(text -> text.matches(".*2026\\d{4}")));
    }

    @Test
    void credentialValidationRejectsMissingInput() {
        assertEquals("请输入一卡通号", LoginPanel.validationMessage(" ", "secret".toCharArray()));
        assertEquals("一卡通号必须是 8 位数字（年份 + 4 位流水号）",
                LoginPanel.validationMessage("AAA", "secret".toCharArray()));
        assertEquals("请输入密码", LoginPanel.validationMessage("20260006", new char[0]));
        assertNull(LoginPanel.validationMessage("20260006", "123456".toCharArray()));
        assertEquals("20260009", CampusCardNumber.format(2026, 9));
        assertEquals(9, CampusCardNumber.sequence("20260009"));
        assertTrue(CampusCardNumber.isValid("20260000"));
        assertFalse(CampusCardNumber.isRegularAccountNumber("20260000"));
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
