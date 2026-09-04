package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ListProductsResponse;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Admin back-office: catalog table and stock/shelf actions. */
final class ShopAdminPanel extends JPanel {

    private static final String[] COLUMNS = {"编号", "分类", "标题", "价格", "数量", "图片"};

    private final ClientContext context;
    private final JLabel statusLabel = new JLabel("管理员可维护商品、上下架和库存");
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    ShopAdminPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(header(), BorderLayout.NORTH);
        JTable table = new JTable(model);
        ShopPalette.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        statusLabel.setForeground(ShopPalette.MUTED);
        add(statusLabel, BorderLayout.SOUTH);
        reload();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("商家中心");
        title.setFont(ShopPalette.titleFont());
        title.setForeground(ShopPalette.TEXT);
        JLabel hint = new JLabel("发布闲置 · 照片 · 描述 · 数量");
        hint.setForeground(ShopPalette.MUTED);
        JPanel titles = new JPanel(new BorderLayout(0, 4));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(hint, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(primaryAction("发布闲置", this::openPublish));
        actions.add(actionButton("刷新目录", this::reload));
        actions.add(actionButton("调整库存", () -> notice("调整库存")));
        header.add(titles, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JButton primaryAction(String text, Runnable action) {
        JButton button = ShopPalette.accentButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JButton actionButton(String text, Runnable action) {
        JButton button = ShopPalette.quietButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void openPublish() {
        PublishProductDialog dialog = new PublishProductDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                context,
                this::reload);
        dialog.setVisible(true);
    }

    private void notice(String feature) {
        JOptionPane.showMessageDialog(
                this,
                feature + " 将在管理接口提交中接入。当前页面用于区分管理员工作台。",
                "商家中心",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void reload() {
        statusLabel.setText("正在加载商品目录……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.LIST_PRODUCTS, ListProductsRequest.allOnSale());
            }

            @Override
            protected void done() {
                try {
                    show(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("加载已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("无法连接服务器，请先启动服务端并使用管理员账号登录。");
                }
            }
        }.execute();
    }

    private void show(Response response) {
        model.setRowCount(0);
        if (!response.isSuccess() || !(response.getData() instanceof ListProductsResponse payload)) {
            statusLabel.setText(response.getMessage());
            return;
        }
        List<ProductSummaryDto> products = payload.getProducts();
        for (ProductSummaryDto product : products) {
            model.addRow(new Object[] {
                    product.getProductId(),
                    product.getCategoryName(),
                    product.getName(),
                    String.format("¥%.2f", product.getPriceFen() / 100.0),
                    product.getStockQty(),
                    product.getPhotos().size() + " 张"
            });
        }
        statusLabel.setText("在售 " + products.size() + " 件。点「发布闲置」可按闲鱼模板上架。");
    }
}
