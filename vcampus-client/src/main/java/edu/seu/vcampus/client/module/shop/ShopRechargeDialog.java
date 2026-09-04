package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.RechargeCampusCardRequest;
import edu.seu.vcampus.common.shop.ShopActions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;

final class ShopRechargeDialog {

    private ShopRechargeDialog() {
    }

    static void open(Window owner, ClientContext context, Runnable onChanged) {
        JDialog dialog = new JDialog(owner, "校园卡充值", JDialog.DEFAULT_MODALITY_TYPE);
        JLabel balance = new JLabel("正在读取余额…");
        balance.setFont(new Font("SansSerif", Font.BOLD, 16));
        balance.setForeground(ShopPalette.PRIMARY);
        balance.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        load(context, balance);

        JPanel amounts = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        amounts.setOpaque(false);
        for (int fen : new int[]{1000, 2000, 5000, 10_000}) {
            JButton button = ShopPalette.accentButton(ShopMoney.yuan(fen));
            button.addActionListener(event -> recharge(owner, context, dialog, fen, balance, onChanged));
            amounts.add(button);
        }

        JButton close = ShopPalette.quietButton("关闭");
        close.addActionListener(event -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(close);

        dialog.getContentPane().setBackground(ShopPalette.PAGE);
        dialog.add(balance, BorderLayout.NORTH);
        dialog.add(amounts, BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setSize(420, 200);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void load(ClientContext context, JLabel balance) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.GET_CAMPUS_CARD, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess() && response.getData() instanceof CampusCardView card) {
                        balance.setText("卡号 " + card.getCardNo() + "　余额 " + ShopMoney.yuan(card.getBalanceFen()));
                        return;
                    }
                    balance.setText(response.getMessage());
                } catch (Exception exception) {
                    balance.setText("无法连接服务器");
                }
            }
        }.execute();
    }

    private static void recharge(
            Window owner,
            ClientContext context,
            JDialog dialog,
            int amountFen,
            JLabel balance,
            Runnable onChanged) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.RECHARGE_CAMPUS_CARD, new RechargeCampusCardRequest(amountFen));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        JOptionPane.showMessageDialog(owner, response.getMessage(), "充值失败", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (response.getData() instanceof CampusCardView card) {
                        balance.setText("卡号 " + card.getCardNo() + "　余额 " + ShopMoney.yuan(card.getBalanceFen()));
                    }
                    if (onChanged != null) {
                        onChanged.run();
                    }
                    JOptionPane.showMessageDialog(owner, "充值成功。", "校园卡", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(owner, "无法连接服务器", "充值失败", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
