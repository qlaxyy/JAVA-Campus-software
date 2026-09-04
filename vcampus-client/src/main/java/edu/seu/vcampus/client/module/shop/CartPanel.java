package edu.seu.vcampus.client.module.shop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/** Customer cart page. Checkout is wired in a later slice. */
final class CartPanel extends JPanel {

    private static final String[] COLUMNS = {"商品", "分类", "单价", "数量", "小计"};

    private final ShopCartStore cart;
    private final JLabel summary = new JLabel("购物车是空的", SwingConstants.LEFT);
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    CartPanel(ShopCartStore cart) {
        this.cart = cart;
        setLayout(new BorderLayout(0, 12));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(title(), BorderLayout.NORTH);
        JTable table = new JTable(model);
        ShopPalette.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        cart.addListener(this::refresh);
        refresh();
    }

    private JLabel title() {
        JLabel label = new JLabel("购物车");
        label.setFont(ShopPalette.titleFont());
        label.setForeground(ShopPalette.TEXT);
        return label;
    }

    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        summary.setFont(ShopPalette.priceFont());
        summary.setForeground(ShopPalette.PRIMARY_DARK);
        JButton checkout = ShopPalette.accentButton("结算");
        checkout.addActionListener(event -> summary.setText(
                "结算尚未接入服务器。请等待 SHOP.CREATE_ORDER，不要把本机购物车当作已完成功能。"));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(checkout);
        footer.add(summary, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void refresh() {
        model.setRowCount(0);
        for (ShopCartStore.Line line : cart.lines()) {
            model.addRow(new Object[] {
                    line.product().getName(),
                    line.product().getCategoryName(),
                    "¥" + String.format("%.2f", line.product().getPriceFen() / 100.0),
                    line.quantity(),
                    "¥" + String.format("%.2f", line.subtotalFen() / 100.0)
            });
        }
        if (cart.itemCount() == 0) {
            summary.setText("购物车是空的，去首页挑一件吧");
            summary.setFont(new Font("SansSerif", Font.PLAIN, 13));
            summary.setForeground(ShopPalette.MUTED);
            return;
        }
        summary.setFont(ShopPalette.priceFont());
        summary.setForeground(ShopPalette.PRIMARY_DARK);
        summary.setText("已选 " + cart.itemCount() + " 件　合计 ¥"
                + String.format("%.2f", cart.totalFen() / 100.0));
    }
}
