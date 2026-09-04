package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.OptionalInt;

/**
 * Shop shell: customer tabs plus a merchant desk when the session may administer SHOP.
 */
public final class ShopView extends JPanel {

    private final ClientContext context;
    private final ShopCartStore cart = new ShopCartStore();
    private final JLabel greeting = new JLabel("请先登录后再查询商品");
    private final JLabel modeChip = new JLabel("顾客");
    private final JLabel cardChip = new JLabel("校园卡 --");
    private final JTabbedPane tabs = new JTabbedPane();
    private ProductCatalogPanel catalogPanel;
    private MyOrdersPanel ordersPanel;
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
                refreshCard();
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
        catalogPanel = new ProductCatalogPanel(context, this::wantThenPay);
        ordersPanel = new MyOrdersPanel(context);
        tabs.addTab("首页", catalogPanel);
        tabs.addTab("购物车", new CartPanel(cart, this::openCheckout));
        tabs.addTab("我的订单", ordersPanel);
        if (admin) {
            tabs.addTab("商家中心", new ShopAdminPanel(context));
        }
        int count = cart.itemCount();
        tabs.setTitleAt(1, count == 0 ? "购物车" : "购物车(" + count + ")");
        updateHeader(admin);
        refreshCard();
    }

    private void wantThenPay(ProductSummaryDto product) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        OptionalInt quantity = ShopQuantityDialog.choose(owner, product);
        if (quantity.isEmpty()) {
            return;
        }
        int qty = quantity.getAsInt();
        boolean paid = ShopCheckoutDialog.pay(owner, context, List.of(new ShopCartStore.Line(product, qty)));
        if (paid) {
            afterPaid();
            return;
        }
        cart.add(product, qty);
        tabs.setSelectedIndex(1);
    }

    private void openCheckout() {
        if (cart.itemCount() == 0) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (ShopCheckoutDialog.pay(owner, context, cart.lines())) {
            cart.clear();
            afterPaid();
        }
    }

    private void afterPaid() {
        refreshCard();
        if (catalogPanel != null) {
            catalogPanel.reload();
        }
        if (ordersPanel != null) {
            ordersPanel.reload();
        }
        tabs.setSelectedIndex(2);
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
        cardChip.setOpaque(true);
        cardChip.setBackground(ShopPalette.CARD);
        cardChip.setForeground(ShopPalette.PRIMARY_DARK);
        cardChip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ShopPalette.LINE),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        greeting.setForeground(ShopPalette.MUTED);
        JButton recharge = ShopPalette.quietButton("充值");
        recharge.addActionListener(event -> ShopRechargeDialog.open(
                SwingUtilities.getWindowAncestor(this), context, this::refreshCard));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(greeting);
        right.add(cardChip);
        right.add(recharge);
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

    private void refreshCard() {
        if (context.currentSession().isEmpty()) {
            cardChip.setText("校园卡 --");
            return;
        }
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.GET_CAMPUS_CARD, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess() && response.getData() instanceof CampusCardView card) {
                        cardChip.setText("校园卡 " + ShopMoney.yuan(card.getBalanceFen()));
                        return;
                    }
                    cardChip.setText("校园卡 --");
                } catch (Exception exception) {
                    cardChip.setText("校园卡 --");
                }
            }
        }.execute();
    }

    private boolean canManageShop() {
        return context.currentSession().map(ShopAccess::canManageShop).orElse(false);
    }
}
