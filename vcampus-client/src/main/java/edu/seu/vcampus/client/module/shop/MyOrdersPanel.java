package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CancelOrderRequest;
import edu.seu.vcampus.common.shop.ListOrdersResponse;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopOrderStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.util.List;

/** Customer orders: list, detail and cancel with campus-card refund. */
final class MyOrdersPanel extends JPanel {

    private static final String[] COLUMNS = {"订单号", "状态", "金额", "配送", "下单时间"};

    private final ClientContext context;
    private final JTable table;
    private final JLabel status = new JLabel("登录后可查看订单", SwingConstants.LEFT);
    private List<ShopOrderDto> orders = List.of();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    MyOrdersPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(header(), BorderLayout.NORTH);
        table = new JTable(model);
        ShopPalette.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        status.setFont(new Font("SansSerif", Font.PLAIN, 13));
        status.setForeground(ShopPalette.MUTED);
        add(status, BorderLayout.SOUTH);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                reload();
            }
        });
        reload();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("我的订单");
        title.setFont(ShopPalette.titleFont());
        title.setForeground(ShopPalette.TEXT);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton detail = ShopPalette.quietButton("查看明细");
        detail.addActionListener(event -> showDetail());
        JButton cancel = ShopPalette.quietButton("取消并退款");
        cancel.addActionListener(event -> cancelSelected());
        JButton refresh = ShopPalette.quietButton("刷新");
        refresh.addActionListener(event -> reload());
        actions.add(detail);
        actions.add(cancel);
        actions.add(refresh);
        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    void reload() {
        status.setText("正在查询订单…");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.LIST_ORDERS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess() || !(response.getData() instanceof ListOrdersResponse payload)) {
                        status.setText(response.getMessage());
                        return;
                    }
                    orders = payload.getOrders();
                    model.setRowCount(0);
                    for (ShopOrderDto order : orders) {
                        model.addRow(new Object[]{
                                order.getOrderId(),
                                order.getStatus() == ShopOrderStatus.PAID ? "已付款" : "已取消",
                                ShopMoney.yuan(order.getTotalFen()),
                                order.getFulfillHint(),
                                order.getCreatedAt()
                        });
                    }
                    status.setText(orders.isEmpty() ? "还没有订单，去首页挑一件吧" : "共 " + orders.size() + " 笔订单");
                } catch (Exception exception) {
                    status.setText("无法连接服务器");
                }
            }
        }.execute();
    }

    private ShopOrderDto selected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= orders.size()) {
            return null;
        }
        return orders.get(row);
    }

    private void showDetail() {
        ShopOrderDto order = selected();
        if (order == null) {
            status.setText("请先选中一笔订单");
            return;
        }
        StringBuilder text = new StringBuilder();
        for (var item : order.getItems()) {
            text.append(item.getName())
                    .append(" ×")
                    .append(item.getQuantity())
                    .append("  ")
                    .append(ShopMoney.yuan(item.getSubtotalFen()))
                    .append('\n');
        }
        JOptionPane.showMessageDialog(
                this,
                text + "合计 " + ShopMoney.yuan(order.getTotalFen()),
                order.getOrderId(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelSelected() {
        ShopOrderDto order = selected();
        if (order == null) {
            status.setText("请先选中一笔订单");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "取消后金额将退回校园卡，确认取消 " + order.getOrderId() + "？",
                "取消订单",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.CANCEL_ORDER, new CancelOrderRequest(order.getOrderId()));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    status.setText(response.getMessage());
                    reload();
                } catch (Exception exception) {
                    status.setText("无法连接服务器");
                }
            }
        }.execute();
    }
}
