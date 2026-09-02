package edu.seu.vcampus.client.module.shop;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Customer orders page. Query/cancel APIs arrive in a later slice. */
final class MyOrdersPanel extends JPanel {

    private static final String[] COLUMNS = {"订单号", "状态", "金额", "下单时间"};

    MyOrdersPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("我的订单");
        title.setFont(ShopPalette.titleFont());
        title.setForeground(ShopPalette.TEXT);
        add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        ShopPalette.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel empty = new JLabel(
                "订单查询尚未接入服务器。当前没有 SHOP.LIST_ORDERS / CANCEL_ORDER，本页不能当作已完成链路。",
                SwingConstants.LEFT);
        empty.setFont(new Font("SansSerif", Font.PLAIN, 13));
        empty.setForeground(ShopPalette.MUTED);
        add(empty, BorderLayout.SOUTH);
    }
}
