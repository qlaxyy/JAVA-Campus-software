package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ListListingsResponse;
import edu.seu.vcampus.common.shop.ListOrdersResponse;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ListProductsResponse;
import edu.seu.vcampus.common.shop.OrderItemDto;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopListingRecordDto;
import edu.seu.vcampus.common.shop.ShopOrderDto;
import edu.seu.vcampus.common.shop.ShopOrderStatus;
import edu.seu.vcampus.common.shop.ShopPaymentMethods;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Admin workbench: catalog edits, listing log and paid orders as peer pages. */
final class ShopAdminPanel extends JPanel {

    private static final String PRODUCTS = "products";
    private static final String LISTINGS = "listings";
    private static final String SALES = "sales";

    private static final String[] PRODUCT_COLUMNS = {"编号", "分类", "标题", "价格", "库存", "图片"};
    private static final String[] LISTING_COLUMNS = {"时间", "操作", "商品编号", "商品", "说明", "操作人"};
    private static final String[] SALE_COLUMNS = {
            "时间", "订单号", "购买者", "账号", "商品", "数量", "单价", "小计", "付款", "状态"};

    private final ClientContext context;
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHost = new JPanel(pages);
    private final JLabel statusLabel = new JLabel("选择上方名录查看上架或成交数据");
    private final JButton productsButton = ShopPalette.accentButton("上架商品");
    private final JButton listingsButton = ShopPalette.quietButton("上架数据");
    private final JButton salesButton = ShopPalette.quietButton("成交订单");
    private final JTable productTable;
    private final DefaultTableModel productModel = readOnly(PRODUCT_COLUMNS);
    private final DefaultTableModel listingModel = readOnly(LISTING_COLUMNS);
    private final DefaultTableModel saleModel = readOnly(SALE_COLUMNS);
    private List<ProductSummaryDto> catalogRows = List.of();
    private String currentPage = PRODUCTS;

    ShopAdminPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 12));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(header(), BorderLayout.NORTH);
        productTable = new JTable(productModel);
        ShopPalette.styleTable(productTable);
        JTable listingTable = new JTable(listingModel);
        ShopPalette.styleTable(listingTable);
        JTable saleTable = new JTable(saleModel);
        ShopPalette.styleTable(saleTable);
        pageHost.setOpaque(false);
        pageHost.add(productPage(), PRODUCTS);
        pageHost.add(scroll(listingTable), LISTINGS);
        pageHost.add(scroll(saleTable), SALES);
        add(pageHost, BorderLayout.CENTER);
        statusLabel.setForeground(ShopPalette.MUTED);
        add(statusLabel, BorderLayout.SOUTH);
        showPage(PRODUCTS);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("商家中心");
        title.setFont(ShopPalette.titleFont());
        title.setForeground(ShopPalette.TEXT);
        JLabel hint = new JLabel("上架商品、上架数据与成交订单同级展示");
        hint.setForeground(ShopPalette.MUTED);
        JPanel titles = new JPanel(new BorderLayout(0, 4));
        titles.setOpaque(false);
        titles.add(title, BorderLayout.NORTH);
        titles.add(hint, BorderLayout.SOUTH);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nav.setOpaque(false);
        productsButton.addActionListener(event -> showPage(PRODUCTS));
        listingsButton.addActionListener(event -> showPage(LISTINGS));
        salesButton.addActionListener(event -> showPage(SALES));
        nav.add(productsButton);
        nav.add(listingsButton);
        nav.add(salesButton);
        header.add(titles, BorderLayout.WEST);
        header.add(nav, BorderLayout.EAST);
        return header;
    }

    private JPanel productPage() {
        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setOpaque(false);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tools.setOpaque(false);
        tools.add(tool("新上架", true, this::openPublish));
        tools.add(tool("修改选中", false, this::openEdit));
        tools.add(tool("刷新", false, this::loadProducts));
        page.add(tools, BorderLayout.NORTH);
        page.add(scroll(productTable), BorderLayout.CENTER);
        return page;
    }

    private JButton tool(String text, boolean primary, Runnable action) {
        JButton button = primary ? ShopPalette.accentButton(text) : ShopPalette.quietButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private static JScrollPane scroll(JTable table) {
        return new JScrollPane(table);
    }

    void reload() {
        showPage(currentPage);
    }

    private void showPage(String page) {
        currentPage = page;
        pages.show(pageHost, page);
        styleNav();
        switch (page) {
            case LISTINGS -> loadListings();
            case SALES -> loadSales();
            default -> loadProducts();
        }
    }

    private void styleNav() {
        paintNav(productsButton, PRODUCTS.equals(currentPage), "上架商品");
        paintNav(listingsButton, LISTINGS.equals(currentPage), "上架数据");
        paintNav(salesButton, SALES.equals(currentPage), "成交订单");
    }

    private static void paintNav(JButton button, boolean selected, String label) {
        button.setText(label);
        if (selected) {
            ShopPalette.paintButton(button, ShopPalette.PRIMARY, java.awt.Color.WHITE);
        } else {
            button.setBackground(ShopPalette.CARD);
            button.setForeground(ShopPalette.PRIMARY_DARK);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ShopPalette.LINE),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        }
    }

    private void openPublish() {
        PublishProductDialog dialog = new PublishProductDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                context,
                this::loadProducts);
        dialog.setVisible(true);
        if (LISTINGS.equals(currentPage) || PRODUCTS.equals(currentPage)) {
            loadListings();
        }
    }

    private void openEdit() {
        int row = productTable.getSelectedRow();
        if (row < 0 || row >= catalogRows.size()) {
            statusLabel.setText("请先在目录中选中一件在售商品");
            return;
        }
        EditProductDialog dialog = new EditProductDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                context,
                catalogRows.get(row),
                () -> {
                    loadProducts();
                    loadListings();
                });
        dialog.setVisible(true);
    }

    private void loadProducts() {
        statusLabel.setText("正在加载在售商品……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.LIST_PRODUCTS, ListProductsRequest.allOnSale());
            }

            @Override
            protected void done() {
                try {
                    showProducts(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("加载已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("无法连接服务器，请先启动服务端并使用管理员账号登录。");
                }
            }
        }.execute();
    }

    private void showProducts(Response response) {
        productModel.setRowCount(0);
        if (!response.isSuccess() || !(response.getData() instanceof ListProductsResponse payload)) {
            statusLabel.setText(response.getMessage());
            return;
        }
        catalogRows = payload.getProducts();
        for (ProductSummaryDto product : catalogRows) {
            productModel.addRow(new Object[]{
                    product.getProductId(),
                    product.getCategoryName(),
                    product.getName(),
                    ShopMoney.yuan(product.getPriceFen()),
                    product.getStockQty(),
                    product.getPhotos().size() + " 张"
            });
        }
        statusLabel.setText("在售 " + catalogRows.size() + " 件。可新上架，或选中一行后修改价格与补货。");
    }

    private void loadListings() {
        statusLabel.setText("正在加载上架数据……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.LIST_LISTINGS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    listingModel.setRowCount(0);
                    if (!response.isSuccess() || !(response.getData() instanceof ListListingsResponse payload)) {
                        statusLabel.setText(response.getMessage());
                        return;
                    }
                    List<ShopListingRecordDto> records = payload.getRecords();
                    for (ShopListingRecordDto record : records) {
                        listingModel.addRow(new Object[]{
                                record.getCreatedAt(),
                                record.getAction(),
                                record.getProductId(),
                                record.getProductName(),
                                record.getDetail(),
                                record.getOperatorName()
                        });
                    }
                    statusLabel.setText(records.isEmpty()
                            ? "还没有上架记录"
                            : "共 " + records.size() + " 条上架数据，与成交订单对照查看。");
                } catch (Exception exception) {
                    statusLabel.setText("无法连接服务器");
                }
            }
        }.execute();
    }

    private void loadSales() {
        statusLabel.setText("正在加载成交订单……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.LIST_SALES, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    saleModel.setRowCount(0);
                    if (!response.isSuccess() || !(response.getData() instanceof ListOrdersResponse payload)) {
                        statusLabel.setText(response.getMessage());
                        return;
                    }
                    int lines = 0;
                    for (ShopOrderDto order : payload.getOrders()) {
                        String pay = ShopPaymentMethods.CAMPUS_CARD.equals(order.getPaymentMethod())
                                ? "校园卡"
                                : order.getPaymentMethod();
                        String status = order.getStatus() == ShopOrderStatus.PAID ? "已付款" : "已取消";
                        for (OrderItemDto item : order.getItems()) {
                            saleModel.addRow(new Object[]{
                                    order.getCreatedAt(),
                                    order.getOrderId(),
                                    order.getBuyerName(),
                                    order.getUserId(),
                                    item.getName(),
                                    item.getQuantity(),
                                    ShopMoney.yuan(item.getUnitPriceFen()),
                                    ShopMoney.yuan(item.getSubtotalFen()),
                                    pay,
                                    status
                            });
                            lines++;
                        }
                    }
                    statusLabel.setText(lines == 0
                            ? "还没有成交订单"
                            : "共 " + lines + " 条成交明细，含购买者、数量和付款信息。");
                } catch (Exception exception) {
                    statusLabel.setText("无法连接服务器");
                }
            }
        }.execute();
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
