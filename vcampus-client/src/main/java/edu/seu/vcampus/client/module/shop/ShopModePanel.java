package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.view.ResponsiveLayout;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Entry page that splits shopping and merchant workbenches, like the hospital mode chooser.
 */
final class ShopModePanel extends JPanel {

    private final JLabel accountLabel = new JLabel("尚未登录");
    private final JLabel statusLabel = new JLabel("请先登录", SwingConstants.CENTER);
    private final JButton shopButton = ShopPalette.accentButton("进入购物");
    private final JButton manageButton = ShopPalette.quietButton("进入管理");

    ShopModePanel(Runnable openShopping, Runnable openManage, Runnable refreshAccess) {
        setLayout(new BorderLayout(0, 20));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(createHeader(refreshAccess), BorderLayout.NORTH);
        add(ResponsiveLayout.verticalScroll(ResponsiveLayout.compact(
                createModes(openShopping, openManage), 1080)), BorderLayout.CENTER);
        statusLabel.setForeground(ShopPalette.MUTED);
        add(statusLabel, BorderLayout.SOUTH);
    }

    void showAccess(SessionInfo session, boolean canManage) {
        accountLabel.setText(accountText(session));
        shopButton.setEnabled(true);
        shopButton.setText("进入购物");
        manageButton.setEnabled(canManage);
        manageButton.setText(canManage ? "进入管理" : "无权限");
        statusLabel.setForeground(ShopPalette.MUTED);
        statusLabel.setText(" ");
    }

    void showLoginRequired() {
        accountLabel.setText("尚未登录");
        shopButton.setEnabled(false);
        shopButton.setText("登录后进入");
        manageButton.setEnabled(false);
        manageButton.setText("登录后进入");
        statusLabel.setForeground(ShopPalette.MUTED);
        statusLabel.setText("请先登录。");
    }

    private JPanel createHeader(Runnable refreshAccess) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("选择商店使用方式");
        title.putClientProperty("module.pageTitle", true);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        title.setForeground(ShopPalette.TEXT);
        accountLabel.setForeground(ShopPalette.PRIMARY_DARK);
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(accountLabel);

        JButton refresh = ShopPalette.quietButton("重新检查权限");
        refresh.addActionListener(event -> refreshAccess.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JPanel createModes(Runnable openShopping, Runnable openManage) {
        JPanel modes = ResponsiveLayout.grid(1, 320, 16);
        modes.setOpaque(false);
        shopButton.addActionListener(event -> openShopping.run());
        manageButton.addActionListener(event -> openManage.run());
        modes.add(modeCard("购物", shopButton));
        modes.add(modeCard("管理", manageButton));
        return modes;
    }

    private JPanel modeCard(String titleText, JButton action) {
        ShopPalette.SurfacePanel card = new ShopPalette.SurfacePanel();
        card.setLayout(new BorderLayout(24, 0));
        card.setPreferredSize(new Dimension(800, 130));
        card.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));

        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22F));
        title.setForeground(ShopPalette.TEXT);
        action.setPreferredSize(new Dimension(230, 48));
        card.add(title, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private static String accountText(SessionInfo session) {
        return session.getDisplayName();
    }
}
