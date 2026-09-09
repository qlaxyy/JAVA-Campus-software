package edu.seu.vcampus.client.module.shop;

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
import java.awt.GridLayout;

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
        add(createModes(openShopping, openManage), BorderLayout.CENTER);
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
        statusLabel.setText("一次只进入一个工作台。购物处理下单与校园卡，管理处理后台上架。");
    }

    void showLoginRequired() {
        accountLabel.setText("尚未登录");
        shopButton.setEnabled(false);
        shopButton.setText("登录后进入");
        manageButton.setEnabled(false);
        manageButton.setText("登录后进入");
        statusLabel.setForeground(ShopPalette.MUTED);
        statusLabel.setText("请先到“用户管理”登录，商店管理员返回后可选择购物或管理。");
    }

    private JPanel createHeader(Runnable refreshAccess) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("选择商店使用方式");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26F));
        title.setForeground(ShopPalette.TEXT);
        JLabel subtitle = new JLabel("商店管理员可进入购物或管理；一次只打开其中一个工作台");
        subtitle.setForeground(ShopPalette.MUTED);
        accountLabel.setForeground(ShopPalette.PRIMARY_DARK);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        copy.add(Box.createVerticalStrut(8));
        copy.add(accountLabel);

        JButton refresh = ShopPalette.quietButton("重新检查权限");
        refresh.addActionListener(event -> refreshAccess.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JPanel createModes(Runnable openShopping, Runnable openManage) {
        JPanel modes = new JPanel(new GridLayout(1, 2, 16, 0));
        modes.setOpaque(false);
        shopButton.addActionListener(event -> openShopping.run());
        manageButton.addActionListener(event -> openManage.run());
        modes.add(modeCard(
                "购物",
                "浏览商品、加入购物车并用校园卡付款",
                "首页商品<br>购物车<br>校园卡充值与付款<br>我的订单",
                shopButton));
        modes.add(modeCard(
                "管理",
                "需要商店管理范围授权",
                "上架商品<br>在售目录<br>成交订单",
                manageButton));
        return modes;
    }

    private JPanel modeCard(String titleText, String requirement, String features, JButton action) {
        ShopPalette.SurfacePanel card = new ShopPalette.SurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(22, 20, 20, 20));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20F));
        title.setForeground(ShopPalette.TEXT);
        JLabel rule = new JLabel(requirement);
        rule.setForeground(ShopPalette.MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(7));
        heading.add(rule);

        JLabel featureList = new JLabel(
                "<html><body style='line-height:1.8'>" + features + "</body></html>");
        featureList.setForeground(ShopPalette.TEXT);
        action.setPreferredSize(new Dimension(0, 42));
        card.add(heading, BorderLayout.NORTH);
        card.add(featureList, BorderLayout.CENTER);
        card.add(action, BorderLayout.SOUTH);
        return card;
    }

    private static String accountText(SessionInfo session) {
        String accountType = session.canAdminister(ModuleNames.SHOP)
                ? "商店管理员"
                : "顾客";
        if (session.canManageUsers()) {
            accountType = "超级管理员";
        }
        return "当前账号：" + session.getDisplayName() + "（" + accountType + "）";
    }
}
