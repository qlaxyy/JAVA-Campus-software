package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;

/**
 * Shop shell: customer tabs plus a merchant desk when the session may administer SHOP.
 */
public final class ShopView extends JPanel {

    private final ClientContext context;
    private final ShopCartStore cart = new ShopCartStore();
    private final JLabel greeting = new JLabel("请先登录后再查询商品");
    private final JLabel modeChip = new JLabel("顾客");
    private final JTabbedPane tabs = new JTabbedPane();
    private boolean lastAdmin;

    /**
     * Builds the shop shell. Tabs refresh when the panel becomes visible after login.
     *
     * @param context shared client services
     */
    public ShopView(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 8));
        setBackground(ShopPalette.PAGE);
        setBorder(BorderFactory.createEmptyBorder(12, 18, 10, 18));
        add(createHeader(), BorderLayout.NORTH);
        tabs.setBackground(ShopPalette.PAGE);
        tabs.setForeground(ShopPalette.TEXT);
        tabs.setOpaque(true);
        add(tabs, BorderLayout.CENTER);
        rebuildTabs(canManageShop());
        cart.addListener(() -> {
            if (tabs.getTabCount() > 1) {
                int count = cart.itemCount();
                tabs.setTitleAt(1, count == 0 ? "购物车" : "购物车(" + count + ")");
            }
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshRole();
            }
        });
    }

    int tabCount() {
        return tabs.getTabCount();
    }

    void refreshRole() {
        boolean admin = canManageShop();
        if (admin == lastAdmin) {
            updateHeader(admin);
            return;
        }
        rebuildTabs(admin);
    }

    private void rebuildTabs(boolean admin) {
        lastAdmin = admin;
        tabs.removeAll();
        tabs.addTab("首页", new ProductCatalogPanel(context, product -> {
            cart.add(product);
            tabs.setSelectedIndex(1);
        }));
        tabs.addTab("购物车", new CartPanel(cart));
        tabs.addTab("我的订单", new MyOrdersPanel());
        if (admin) {
            tabs.addTab("商家中心", new ShopAdminPanel(context));
        }
        int count = cart.itemCount();
        tabs.setTitleAt(1, count == 0 ? "购物车" : "购物车(" + count + ")");
        updateHeader(admin);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JLabel brand = new JLabel("校园商店");
        brand.setFont(new Font("SansSerif", Font.BOLD, 20));
        brand.setForeground(ShopPalette.TEXT);

        modeChip.setOpaque(true);
        modeChip.setBackground(ShopPalette.PRIMARY_LIGHT);
        modeChip.setForeground(ShopPalette.PRIMARY_DARK);
        modeChip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        greeting.setForeground(ShopPalette.MUTED);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(greeting);
        right.add(modeChip);

        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void updateHeader(boolean admin) {
        modeChip.setText(admin ? "商店管理" : "顾客");
        modeChip.setBackground(admin ? ShopPalette.NAVY : ShopPalette.PRIMARY_LIGHT);
        modeChip.setForeground(admin ? ShopPalette.HEADER_TEXT : ShopPalette.PRIMARY_DARK);
        context.currentSession().ifPresentOrElse(
                session -> greeting.setText("你好，" + session.getDisplayName()),
                () -> greeting.setText("请先登录后再查询商品"));
    }

    private boolean canManageShop() {
        return context.currentSession().map(ShopAccess::canManageShop).orElse(false);
    }
}
