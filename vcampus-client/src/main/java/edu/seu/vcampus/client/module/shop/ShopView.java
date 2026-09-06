package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.CampusCardView;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.util.List;
import java.util.OptionalInt;

/**
 * Shop shell: students enter shopping directly; shop admins first choose 购物 or 管理.
 */
public final class ShopView extends JPanel {

    static final String CARD_SELECT = "select";
    static final String CARD_SHOP = "shop";
    static final String CARD_MANAGE = "manage";

    private final ClientContext context;
    private final ShopCartStore cart = new ShopCartStore();
    private final CardLayout cards = new CardLayout();
    private final ShopModePanel modePanel;
    private final JPanel shoppingShell = new JPanel(new BorderLayout(0, 8));
    private final JPanel manageShell = new JPanel(new BorderLayout(0, 8));
    private final JLabel greeting = new JLabel("请先登录后再查询商品");
    private final JLabel modeChip = new JLabel("顾客");
    private final JLabel cardChip = new JLabel("校园卡 --");
    private final JButton switchFromShop = ShopPalette.quietButton("切换入口");
    private final JTabbedPane tabs = new JTabbedPane();
    private ProductCatalogPanel catalogPanel;
    private MyOrdersPanel ordersPanel;
    private ShopAdminPanel adminPanel;
    private String visibleCard = CARD_SHOP;

    /**
     * Builds the shop shell. Admins land on the mode chooser when this panel is shown.
     *
     * @param context shared client services
     */
    public ShopView(ClientContext context) {
        this.context = context;
        setLayout(cards);
        setBackground(ShopPalette.PAGE);

        modePanel = new ShopModePanel(this::openShopping, this::openManage, this::openModeSelector);
        buildShoppingShell();
        buildManageShell();
        add(modePanel, CARD_SELECT);
        add(shoppingShell, CARD_SHOP);
        add(manageShell, CARD_MANAGE);
        rebuildShoppingTabs();
        cart.addListener(() -> {
            if (tabs.getTabCount() > 1) {
                int count = cart.itemCount();
                tabs.setTitleAt(1, count == 0 ? "购物车" : "购物车(" + count + ")");
            }
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                enterModule();
            }
        });
        enterModule();
    }

    int tabCount() {
        return tabs.getTabCount();
    }

    String visibleCard() {
        return visibleCard;
    }

    void openShopping() {
        showCard(CARD_SHOP);
        updateShoppingHeader();
        refreshCard();
        if (catalogPanel != null) {
            catalogPanel.reload();
        }
    }

    void openManage() {
        if (!canManageShop()) {
            openModeSelector();
            return;
        }
        showCard(CARD_MANAGE);
        if (adminPanel != null) {
            adminPanel.reload();
        }
    }

    private void enterModule() {
        if (canManageShop()) {
            openModeSelector();
            return;
        }
        openShopping();
    }

    private void openModeSelector() {
        if (!canManageShop()) {
            openShopping();
            return;
        }
        SessionInfo session = context.currentSession().orElse(null);
        if (session == null) {
            modePanel.showLoginRequired();
            showCard(CARD_SELECT);
            return;
        }
        modePanel.showAccess(session, true);
        showCard(CARD_SELECT);
    }

    private void showCard(String name) {
        visibleCard = name;
        cards.show(this, name);
    }

    private void buildShoppingShell() {
        shoppingShell.setBackground(ShopPalette.PAGE);
        shoppingShell.setBorder(BorderFactory.createEmptyBorder(12, 18, 10, 18));
        shoppingShell.add(createShoppingHeader(), BorderLayout.NORTH);
        tabs.setBackground(ShopPalette.PAGE);
        tabs.setForeground(ShopPalette.TEXT);
        tabs.setOpaque(true);
        shoppingShell.add(tabs, BorderLayout.CENTER);
    }

    private void buildManageShell() {
        manageShell.setBackground(ShopPalette.PAGE);
        manageShell.setBorder(BorderFactory.createEmptyBorder(12, 18, 10, 18));
        manageShell.add(createManageHeader(), BorderLayout.NORTH);
        adminPanel = new ShopAdminPanel(context);
        manageShell.add(adminPanel, BorderLayout.CENTER);
    }

    private void rebuildShoppingTabs() {
        tabs.removeAll();
        catalogPanel = new ProductCatalogPanel(context, this::wantThenPay, this::addToCart);
        ordersPanel = new MyOrdersPanel(context);
        tabs.addTab("首页", catalogPanel);
        tabs.addTab("购物车", new CartPanel(cart, this::openCheckout));
        tabs.addTab("我的订单", ordersPanel);
        int count = cart.itemCount();
        tabs.setTitleAt(1, count == 0 ? "购物车" : "购物车(" + count + ")");
        updateShoppingHeader();
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

    private void addToCart(ProductSummaryDto product) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        OptionalInt quantity = ShopQuantityDialog.choose(owner, product, "加入购物车");
        if (quantity.isEmpty()) {
            return;
        }
        cart.add(product, quantity.getAsInt());
        JOptionPane.showMessageDialog(
                owner,
                "已加入购物车",
                "购物车",
                JOptionPane.INFORMATION_MESSAGE);
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
        showCard(CARD_SHOP);
        tabs.setSelectedIndex(2);
    }

    private JPanel createShoppingHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.add(brandLabel("校园商店"), BorderLayout.WEST);

        switchFromShop.addActionListener(event -> openModeSelector());
        JButton recharge = ShopPalette.quietButton("充值");
        recharge.addActionListener(event -> ShopRechargeDialog.open(
                SwingUtilities.getWindowAncestor(this), context, this::refreshCard));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        greeting.setForeground(ShopPalette.MUTED);
        modeChip.setOpaque(true);
        modeChip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        cardChip.setOpaque(true);
        cardChip.setBackground(ShopPalette.CARD);
        cardChip.setForeground(ShopPalette.PRIMARY_DARK);
        cardChip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ShopPalette.LINE),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        right.add(greeting);
        right.add(cardChip);
        right.add(recharge);
        right.add(switchFromShop);
        right.add(modeChip);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createManageHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.add(brandLabel("校园商店 · 管理"), BorderLayout.WEST);
        JButton switchMode = ShopPalette.quietButton("切换入口");
        switchMode.addActionListener(event -> openModeSelector());
        JLabel chip = new JLabel("管理");
        chip.setOpaque(true);
        chip.setBackground(ShopPalette.NAVY);
        chip.setForeground(ShopPalette.HEADER_TEXT);
        chip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(switchMode);
        right.add(chip);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private static JLabel brandLabel(String text) {
        JLabel brand = new JLabel(text);
        brand.setFont(new Font("SansSerif", Font.BOLD, 20));
        brand.setForeground(ShopPalette.TEXT);
        return brand;
    }

    private void updateShoppingHeader() {
        boolean admin = canManageShop();
        modeChip.setText(admin ? "购物" : "顾客");
        modeChip.setBackground(admin ? ShopPalette.PRIMARY_LIGHT : ShopPalette.PRIMARY_LIGHT);
        modeChip.setForeground(ShopPalette.PRIMARY_DARK);
        switchFromShop.setVisible(admin);
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
