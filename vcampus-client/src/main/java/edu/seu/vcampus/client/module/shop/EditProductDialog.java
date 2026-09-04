package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.UpdateProductRequest;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class EditProductDialog extends JDialog {

    private final ClientContext context;
    private final ProductSummaryDto product;
    private final Runnable onSaved;
    private final JTextField titleField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(5, 28);
    private final JTextField priceField = new JTextField();
    private final JSpinner addStock = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
    private final JLabel statusLabel = new JLabel("可改标题、价格，或补货");
    private final JButton saveButton = ShopPalette.accentButton("保存修改");

    EditProductDialog(Window owner, ClientContext context, ProductSummaryDto product, Runnable onSaved) {
        super(owner, "修改商品", ModalityType.DOCUMENT_MODAL);
        this.context = context;
        this.product = product;
        this.onSaved = onSaved;
        titleField.setText(product.getName());
        descriptionArea.setText(product.getDescription());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        priceField.setText(String.format("%.2f", product.getPriceFen() / 100.0));
        setLayout(new BorderLayout());
        getContentPane().setBackground(ShopPalette.PAGE);
        add(body(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        setSize(440, 460);
        setLocationRelativeTo(owner);
    }

    private JPanel body() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        JLabel stock = new JLabel("当前库存 " + product.getStockQty() + " 件");
        stock.setForeground(ShopPalette.MUTED);
        stock.setAlignmentX(0f);
        body.add(labeled("标题", titleField));
        body.add(Box.createVerticalStrut(10));
        body.add(labeled("价格（元）", priceField));
        body.add(Box.createVerticalStrut(10));
        body.add(stock);
        body.add(Box.createVerticalStrut(6));
        body.add(labeled("补货数量", addStock));
        body.add(Box.createVerticalStrut(10));
        JScrollPane description = new JScrollPane(descriptionArea);
        description.setPreferredSize(new Dimension(380, 120));
        body.add(labeled("描述", description));
        return body;
    }

    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 20, 16, 20));
        statusLabel.setForeground(ShopPalette.MUTED);
        saveButton.addActionListener(event -> save());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(saveButton);
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void save() {
        int priceFen;
        try {
            BigDecimal yuan = new BigDecimal(priceField.getText().trim());
            if (yuan.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("price");
            }
            priceFen = yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (RuntimeException exception) {
            statusLabel.setText("请填写正确的价格");
            return;
        }
        UpdateProductRequest request;
        try {
            request = new UpdateProductRequest(
                    product.getProductId(),
                    titleField.getText(),
                    descriptionArea.getText(),
                    priceFen,
                    (Integer) addStock.getValue());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText("标题和描述都必填");
            return;
        }
        saveButton.setEnabled(false);
        statusLabel.setText("正在保存……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.UPDATE_PRODUCT, request);
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        statusLabel.setText(response.getMessage());
                        return;
                    }
                    onSaved.run();
                    dispose();
                } catch (Exception exception) {
                    statusLabel.setText("无法连接服务器");
                }
            }
        }.execute();
    }

    private static JPanel labeled(String title, java.awt.Component field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setAlignmentX(0f);
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(ShopPalette.TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
}
