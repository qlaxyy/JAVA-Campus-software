package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.CreateOrderRequest;
import edu.seu.vcampus.common.shop.OrderLineRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

final class ShopCheckoutDialog {

    private ShopCheckoutDialog() {
    }

    static boolean pay(Window owner, ClientContext context, List<ShopCartStore.Line> lines) {
        if (lines.isEmpty()) {
            return false;
        }

        int totalFen = 0;
        List<OrderLineRequest> payloadLines = new ArrayList<>();
        for (ShopCartStore.Line line : lines) {
            totalFen += line.subtotalFen();
            payloadLines.add(new OrderLineRequest(line.product().getProductId(), line.quantity()));
        }

        JDialog dialog = new JDialog(owner, "确认付款", JDialog.DEFAULT_MODALITY_TYPE);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        JLabel heading = new JLabel("确认订单后付款");
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(ShopPalette.TEXT);
        heading.setAlignmentX(0f);
        body.add(heading);
        body.add(Box.createVerticalStrut(12));

        for (ShopCartStore.Line line : lines) {
            JLabel row = new JLabel(line.product().getName()
                    + "  ×"
                    + line.quantity()
                    + "    "
                    + ShopMoney.yuan(line.subtotalFen()));
            row.setFont(new Font("SansSerif", Font.PLAIN, 14));
            row.setForeground(ShopPalette.TEXT);
            row.setAlignmentX(0f);
            body.add(row);
            body.add(Box.createVerticalStrut(6));
        }

        body.add(Box.createVerticalStrut(8));
        JLabel total = new JLabel("应付合计  " + ShopMoney.yuan(totalFen));
        total.setFont(new Font("SansSerif", Font.BOLD, 16));
        total.setForeground(ShopPalette.PRIMARY);
        total.setAlignmentX(0f);
        body.add(total);
        body.add(Box.createVerticalStrut(12));

        JLabel cardLabel = new JLabel("校园卡余额加载中…");
        cardLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cardLabel.setForeground(ShopPalette.MUTED);
        cardLabel.setAlignmentX(0f);
        body.add(cardLabel);
        body.add(Box.createVerticalStrut(8));
        JLabel fulfill = new JLabel("配送方式：校内自提");
        fulfill.setForeground(ShopPalette.MUTED);
        fulfill.setAlignmentX(0f);
        body.add(fulfill);
        body.add(Box.createVerticalStrut(16));

        JLabel payHint = new JLabel("支付方式");
        payHint.setFont(new Font("SansSerif", Font.PLAIN, 13));
        payHint.setForeground(ShopPalette.MUTED);
        payHint.setAlignmentX(0f);
        body.add(payHint);
        body.add(Box.createVerticalStrut(6));
        JComboBox<String> method = new JComboBox<>(new String[]{"校园卡"});
        method.setEnabled(false);
        method.setMaximumSize(method.getPreferredSize());
        method.setAlignmentX(0f);
        body.add(method);

        loadCard(context, cardLabel);

        boolean[] paid = {false};
        JButton confirm = ShopPalette.accentButton("确认付款");
        confirm.addActionListener(event -> submit(
                owner,
                context,
                dialog,
                confirm,
                payloadLines,
                paid,
                cardLabel));
        JButton back = ShopPalette.quietButton("返回");
        back.addActionListener(event -> dialog.dispose());

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        actions.add(back);
        actions.add(confirm);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ShopPalette.PAGE);

        dialog.getContentPane().setBackground(ShopPalette.PAGE);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(440, 400);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return paid[0];
    }

    private static void loadCard(ClientContext context, JLabel cardLabel) {
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
                        cardLabel.setText("校园卡 " + card.getCardNo() + "　余额 " + ShopMoney.yuan(card.getBalanceFen()));
                        return;
                    }
                    cardLabel.setText(response.getMessage());
                } catch (Exception exception) {
                    cardLabel.setText("无法读取校园卡");
                }
            }
        }.execute();
    }

    private static void submit(
            Window owner,
            ClientContext context,
            JDialog dialog,
            JButton confirm,
            List<OrderLineRequest> payloadLines,
            boolean[] paid,
            JLabel cardLabel) {
        confirm.setEnabled(false);
        CreateOrderRequest request = new CreateOrderRequest(
                payloadLines, ShopPaymentMethods.CAMPUS_CARD, "校内自提");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.CREATE_ORDER, request);
            }

            @Override
            protected void done() {
                confirm.setEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess() && response.getData() instanceof ShopOrderDto order) {
                        paid[0] = true;
                        dialog.dispose();
                        JOptionPane.showMessageDialog(
                                owner,
                                "付款成功。订单 " + order.getOrderId() + " 已用校园卡扣款 "
                                        + ShopMoney.yuan(order.getTotalFen()) + "。请到校内自提。",
                                "付款成功",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (ErrorCodes.SHOP_INSUFFICIENT_BALANCE.equals(response.getCode())) {
                        int choice = JOptionPane.showOptionDialog(
                                dialog,
                                "余额不足，请充值！",
                                "校园卡",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                new Object[]{"去充值", "取消"},
                                "去充值");
                        if (choice == 0) {
                            ShopRechargeDialog.open(owner, context, () -> loadCard(context, cardLabel));
                        }
                        return;
                    }
                    JOptionPane.showMessageDialog(dialog, response.getMessage(), "付款失败", JOptionPane.WARNING_MESSAGE);
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(dialog, "无法连接服务器", "付款失败", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }
}
