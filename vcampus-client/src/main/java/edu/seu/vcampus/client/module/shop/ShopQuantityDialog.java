package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.shop.ProductSummaryDto;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.OptionalInt;

final class ShopQuantityDialog {

    private ShopQuantityDialog() {
    }

    static OptionalInt choose(Window owner, ProductSummaryDto product) {
        int stock = Math.max(1, product.getStockQty());
        JDialog dialog = new JDialog(owner, "选择数量", JDialog.DEFAULT_MODALITY_TYPE);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel(product.getName());
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(ShopPalette.TEXT);
        title.setAlignmentX(0f);

        JLabel stockHint = new JLabel("库存 " + product.getStockQty() + " 件，请选择要买的数量");
        stockHint.setFont(new Font("SansSerif", Font.PLAIN, 13));
        stockHint.setForeground(ShopPalette.MUTED);
        stockHint.setAlignmentX(0f);

        SpinnerNumberModel model = new SpinnerNumberModel(1, 1, stock, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setMaximumSize(spinner.getPreferredSize());
        spinner.setAlignmentX(0f);

        body.add(title);
        body.add(Box.createVerticalStrut(8));
        body.add(stockHint);
        body.add(Box.createVerticalStrut(14));
        body.add(spinner);

        int[] chosen = {0};
        JButton next = ShopPalette.accentButton("下一步，去付款");
        next.addActionListener(event -> {
            chosen[0] = ((Number) spinner.getValue()).intValue();
            dialog.dispose();
        });
        JButton cancel = ShopPalette.quietButton("取消");
        cancel.addActionListener(event -> dialog.dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        actions.add(cancel);
        actions.add(next);

        dialog.getContentPane().setBackground(ShopPalette.PAGE);
        dialog.add(body, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return chosen[0] > 0 ? OptionalInt.of(chosen[0]) : OptionalInt.empty();
    }
}
