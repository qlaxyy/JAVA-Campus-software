package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ListProductsRequest;
import edu.seu.vcampus.common.shop.ListProductsResponse;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Customer storefront: square tiles, adjustable density, and a web-style pager. */
final class ProductCatalogPanel extends JPanel {

    private static final CategoryChoice[] CATEGORIES = {
            new CategoryChoice("全部分类", null),
            new CategoryChoice("文具", 1L),
            new CategoryChoice("日常用品", 2L),
            new CategoryChoice("食品", 3L)
    };

    private static final DensityChoice[] DENSITIES = {
            new DensityChoice("每页自适应", null),
            new DensityChoice("每页 4 件", 4),
            new DensityChoice("每页 6 件", 6),
            new DensityChoice("每页 8 件", 8),
            new DensityChoice("每页 9 件", 9)
    };

    private final ClientContext context;
    private final Consumer<ProductSummaryDto> onAdd;
    private final JTextField keywordField = new JTextField(16);
    private final JComboBox<CategoryChoice> categoryBox = new JComboBox<>(CATEGORIES);
    private final JComboBox<DensityChoice> densityBox = new JComboBox<>(DENSITIES);
    private final JButton searchButton = ShopPalette.accentButton("搜索");
    private final JLabel statusLabel = new JLabel("登录后可浏览上架商品");
    private final JPanel canvas = new JPanel(new GridBagLayout());
    private final JPanel grid = new JPanel();
    private final JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    private List<ProductSummaryDto> catalog = List.of();
    private int pageIndex = 1;
    private ShopCatalogGrid.Plan plan = new ShopCatalogGrid.Plan(2, 2, 200, 4);
    private int renderedPage = -1;
    private int renderedCount = -1;

    ProductCatalogPanel(ClientContext context, Consumer<ProductSummaryDto> onAdd) {
        this.context = context;
        this.onAdd = onAdd;
        setLayout(new BorderLayout(0, 8));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        add(createSearchBar(), BorderLayout.NORTH);
        canvas.setOpaque(false);
        canvas.setBackground(ShopPalette.PAGE);
        grid.setOpaque(false);
        canvas.add(grid);
        add(canvas, BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        keywordField.addActionListener(event -> query(true));
        searchButton.addActionListener(event -> query(true));
        densityBox.addActionListener(event -> {
            pageIndex = 1;
            renderPage();
        });
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                renderPage();
            }
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                query(false);
            }
        });
        query(true);
    }

    private JPanel createSearchBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(true);
        bar.setBackground(ShopPalette.CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ShopPalette.LINE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        keywordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ShopPalette.LINE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        bar.add(keywordField);
        bar.add(categoryBox);
        bar.add(densityBox);
        bar.add(searchButton);
        return bar;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 4));
        footer.setOpaque(false);
        pager.setOpaque(false);
        statusLabel.setForeground(ShopPalette.MUTED);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        footer.add(pager, BorderLayout.CENTER);
        footer.add(statusLabel, BorderLayout.SOUTH);
        return footer;
    }

    private Integer selectedPageSize() {
        DensityChoice choice = (DensityChoice) densityBox.getSelectedItem();
        return choice == null ? null : choice.pageSize();
    }

    private void query(boolean resetPage) {
        if (resetPage) {
            pageIndex = 1;
        }
        searchButton.setEnabled(false);
        statusLabel.setText("正在搜索……");
        CategoryChoice choice = (CategoryChoice) categoryBox.getSelectedItem();
        ListProductsRequest filter = new ListProductsRequest(
                keywordField.getText(), choice == null ? null : choice.categoryId());
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.LIST_PRODUCTS, filter);
            }

            @Override
            protected void done() {
                try {
                    showResponse(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("搜索已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("无法连接服务器，请先启动服务端并登录。");
                } finally {
                    searchButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showResponse(Response response) {
        if (!response.isSuccess()) {
            catalog = List.of();
            renderPage();
            statusLabel.setText(response.getMessage());
            return;
        }
        if (!(response.getData() instanceof ListProductsResponse payload)) {
            catalog = List.of();
            renderPage();
            statusLabel.setText("返回数据格式不正确");
            return;
        }
        catalog = payload.getProducts();
        renderPage();
    }

    private void renderPage() {
        ShopCatalogGrid.Plan next = ShopCatalogGrid.plan(
                Math.max(1, canvas.getWidth()),
                Math.max(1, canvas.getHeight()),
                selectedPageSize());
        int safePage = ShopCatalogPages.clampPage(pageIndex, catalog.size(), next.pageSize());
        if (next.equals(plan)
                && safePage == renderedPage
                && catalog.size() == renderedCount
                && grid.getComponentCount() == plan.pageSize()) {
            return;
        }
        plan = next;
        pageIndex = safePage;
        renderedPage = pageIndex;
        renderedCount = catalog.size();

        grid.removeAll();
        grid.setLayout(new GridLayout(plan.rows(), plan.columns(), ShopCatalogGrid.GAP, ShopCatalogGrid.GAP));
        grid.setPreferredSize(new Dimension(plan.gridWidth(), plan.gridHeight()));
        List<ProductSummaryDto> visible = ShopCatalogPages.slice(catalog, pageIndex, plan.pageSize());
        for (ProductSummaryDto product : visible) {
            grid.add(new ProductCard(product, onAdd, plan.cellSize()));
        }
        while (grid.getComponentCount() < plan.pageSize()) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setPreferredSize(new Dimension(plan.cellSize(), plan.cellSize()));
            grid.add(empty);
        }
        rebuildPager();
        int pages = ShopCatalogPages.pageCount(catalog.size(), plan.pageSize());
        if (catalog.isEmpty()) {
            statusLabel.setText("没有符合条件的商品");
        } else {
            statusLabel.setText("共 " + catalog.size() + " 件 · 每页 " + plan.pageSize()
                    + " 件正方形卡片 · 第 " + pageIndex + " / " + pages + " 页");
        }
        canvas.revalidate();
        grid.revalidate();
        grid.repaint();
        pager.revalidate();
        pager.repaint();
    }

    private void rebuildPager() {
        pager.removeAll();
        int pages = ShopCatalogPages.pageCount(catalog.size(), plan.pageSize());
        boolean empty = catalog.isEmpty();

        JButton previous = ShopPalette.quietButton("上一页");
        previous.setEnabled(!empty && pageIndex > 1);
        previous.addActionListener(event -> goTo(pageIndex - 1));
        pager.add(previous);

        for (int label : ShopCatalogPages.pageWindow(pageIndex, pages)) {
            if (label == ShopCatalogPages.ELLIPSIS) {
                JLabel dots = new JLabel("…");
                dots.setForeground(ShopPalette.MUTED);
                dots.setFont(new Font("SansSerif", Font.PLAIN, 14));
                pager.add(dots);
                continue;
            }
            pager.add(pageButton(label, !empty && label == pageIndex));
        }

        JButton next = ShopPalette.quietButton("下一页");
        next.setEnabled(!empty && pageIndex < pages);
        next.addActionListener(event -> goTo(pageIndex + 1));
        pager.add(next);
    }

    private JButton pageButton(int page, boolean current) {
        JButton button = current
                ? ShopPalette.accentButton(Integer.toString(page))
                : ShopPalette.quietButton(Integer.toString(page));
        button.setEnabled(!current);
        button.addActionListener(event -> goTo(page));
        return button;
    }

    private void goTo(int page) {
        pageIndex = ShopCatalogPages.clampPage(page, catalog.size(), plan.pageSize());
        renderPage();
    }

    private record CategoryChoice(String label, Long categoryId) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record DensityChoice(String label, Integer pageSize) {
        @Override
        public String toString() {
            return label;
        }
    }
}
