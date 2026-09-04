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
import java.util.List;

/** Customer cart: change quantity, remove lines, then campus-card checkout. */
final class CartPanel extends JPanel {

    private static final String[] COLUMNS = {"商品", "分类", "单价", "数量", "小计"};

    private final ShopCartStore cart;
    private final Runnable onCheckout;
    private final JTable table;
    private final JLabel summary = new JLabel("购物车是空的", SwingConstants.LEFT);
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    CartPanel(ShopCartStore cart, Runnable onCheckout) {
        this.cart = cart;
        this.onCheckout = onCheckout;
        setLayout(new BorderLayout(0, 12));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(title(), BorderLayout.NORTH);
        this.table = new JTable(model);
        ShopPalette.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        cart.addListener(this::refresh);
        refresh();
    }

    private ShopCartStore.Line selectedLine() {
        int row = table.getSelectedRow();
        List<ShopCartStore.Line> lines = cart.lines();
        if (row < 0 || row >= lines.size()) {
            return null;
        }
        return lines.get(row);
    }

    private void changeSelected(int delta) {
        ShopCartStore.Line line = selectedLine();
        if (line == null) {
            return;
        }
        cart.setQuantity(line.product().getProductId(), line.quantity() + delta);
    }

    private void removeSelected() {
        ShopCartStore.Line line = selectedLine();
        if (line == null) {
            return;
        }
        cart.remove(line.product().getProductId());
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
        JButton minus = ShopPalette.quietButton("数量 -1");
        minus.addActionListener(event -> changeSelected(-1));
        JButton plus = ShopPalette.quietButton("数量 +1");
        plus.addActionListener(event -> changeSelected(1));
        JButton remove = ShopPalette.quietButton("删除");
        remove.addActionListener(event -> removeSelected());
        JButton checkout = ShopPalette.accentButton("结算");
        checkout.addActionListener(event -> onCheckout.run());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(minus);
        right.add(plus);
        right.add(remove);
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
                    ShopMoney.yuan(line.product().getPriceFen()),
                    line.quantity(),
                    ShopMoney.yuan(line.subtotalFen())
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
        summary.setText("已选 " + cart.itemCount() + " 件　合计 " + ShopMoney.yuan(cart.totalFen()));
    }
}
