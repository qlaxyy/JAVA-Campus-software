package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.shop.ProductSummaryDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;

/**
 * Product detail window: gallery, scrollable copy, and a pinned action bar.
 */
final class ProductDetailDialog extends JDialog {

    private static final int PHOTO_SIZE = 240;
    private static final int EDGE = 16;

    private int photoIndex;

    ProductDetailDialog(
            Window owner,
            ProductSummaryDto product,
            Consumer<ProductSummaryDto> onWant,
            Consumer<ProductSummaryDto> onAddToCart) {
        super(owner, "商品详情", ModalityType.DOCUMENT_MODAL);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ShopPalette.PAGE);

        add(createGallery(product), BorderLayout.NORTH);
        add(createDetailsScroll(product), BorderLayout.CENTER);
        add(createActions(product, onWant, onAddToCart), BorderLayout.SOUTH);
        setSize(400, 620);
        setMinimumSize(new Dimension(360, 520));
        setLocationRelativeTo(owner);
    }

    private JPanel createGallery(ProductSummaryDto product) {
        JLabel photo = new JLabel(
                ShopPhotoSupport.icon(product.getPhotos().getFirst(), PHOTO_SIZE, PHOTO_SIZE),
                SwingConstants.CENTER);
        photo.setOpaque(true);
        photo.setBackground(ShopPalette.CARD);
        photo.setPreferredSize(new Dimension(PHOTO_SIZE, PHOTO_SIZE));

        JButton previous = ShopPalette.quietButton("上一张");
        JButton next = ShopPalette.quietButton("下一张");
        JLabel counter = new JLabel(counterText(product), SwingConstants.CENTER);
        counter.setForeground(ShopPalette.MUTED);
        boolean multiple = product.getPhotos().size() > 1;
        previous.setEnabled(multiple);
        next.setEnabled(multiple);
        previous.addActionListener(event -> {
            photoIndex = (photoIndex - 1 + product.getPhotos().size()) % product.getPhotos().size();
            photo.setIcon(ShopPhotoSupport.icon(product.getPhotos().get(photoIndex), PHOTO_SIZE, PHOTO_SIZE));
            counter.setText(counterText(product));
        });
        next.addActionListener(event -> {
            photoIndex = (photoIndex + 1) % product.getPhotos().size();
            photo.setIcon(ShopPhotoSupport.icon(product.getPhotos().get(photoIndex), PHOTO_SIZE, PHOTO_SIZE));
            counter.setText(counterText(product));
        });

        JPanel photoWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        photoWrap.setBackground(ShopPalette.CARD);
        photoWrap.setBorder(BorderFactory.createEmptyBorder(EDGE, EDGE, 8, EDGE));
        photoWrap.add(photo);

        JPanel pager = new JPanel(new BorderLayout(12, 0));
        pager.setOpaque(false);
        pager.setBorder(BorderFactory.createEmptyBorder(4, EDGE, EDGE, EDGE));
        pager.add(previous, BorderLayout.WEST);
        pager.add(counter, BorderLayout.CENTER);
        pager.add(next, BorderLayout.EAST);

        JPanel gallery = new JPanel(new BorderLayout());
        gallery.setBackground(ShopPalette.CARD);
        gallery.add(photoWrap, BorderLayout.CENTER);
        gallery.add(pager, BorderLayout.SOUTH);
        return gallery;
    }

    private JScrollPane createDetailsScroll(ProductSummaryDto product) {
        JLabel price = new JLabel("¥" + String.format("%.2f", product.getPriceFen() / 100.0));
        price.setFont(new Font("SansSerif", Font.BOLD, 24));
        price.setForeground(ShopPalette.PRIMARY_DARK);

        JLabel title = new JLabel("<html>" + escape(product.getName()) + "</html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(ShopPalette.TEXT);

        JLabel category = new JLabel("分类：" + product.getCategoryName());
        category.setForeground(ShopPalette.MUTED);
        JLabel quantity = new JLabel("数量：" + product.getStockQty());
        quantity.setForeground(ShopPalette.MUTED);
        JLabel seller = new JLabel("卖家：" + product.getSellerName());
        seller.setForeground(ShopPalette.MUTED);

        JPanel facts = new JPanel(new GridLayout(0, 1, 0, 4));
        facts.setOpaque(false);
        facts.add(category);
        facts.add(quantity);
        facts.add(seller);

        JLabel description = new JLabel("<html><body style='width:280px'><b>描述</b><br>"
                + escape(product.getDescription()).replace("\n", "<br>")
                + "</body></html>");
        description.setForeground(ShopPalette.TEXT);

        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setOpaque(false);
        heading.add(price, BorderLayout.NORTH);
        heading.add(title, BorderLayout.CENTER);
        heading.add(facts, BorderLayout.SOUTH);

        JPanel details = new JPanel(new BorderLayout(0, 12));
        details.setBackground(ShopPalette.PAGE);
        details.setBorder(BorderFactory.createEmptyBorder(12, EDGE, 12, EDGE));
        details.add(heading, BorderLayout.NORTH);
        details.add(description, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(details);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(ShopPalette.PAGE);
        scroll.getViewport().setBackground(ShopPalette.PAGE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel createActions(
            ProductSummaryDto product,
            Consumer<ProductSummaryDto> onWant,
            Consumer<ProductSummaryDto> onAddToCart) {
        JButton cart = ShopPalette.quietButton("加入购物车");
        cart.setEnabled(product.getStockQty() > 0);
        cart.addActionListener(event -> {
            dispose();
            onAddToCart.accept(product);
        });
        JButton want = ShopPalette.accentButton(product.getStockQty() > 0 ? "我想要" : "暂时缺货");
        want.setEnabled(product.getStockQty() > 0);
        want.addActionListener(event -> {
            dispose();
            onWant.accept(product);
        });
        cart.setPreferredSize(new Dimension(0, 40));
        want.setPreferredSize(new Dimension(0, 40));

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(true);
        actions.setBackground(ShopPalette.PAGE);
        actions.setBorder(BorderFactory.createEmptyBorder(8, EDGE, EDGE, EDGE));
        actions.add(cart);
        actions.add(want);
        return actions;
    }

    private String counterText(ProductSummaryDto product) {
        List<byte[]> photos = product.getPhotos();
        return (photoIndex + 1) + " / " + photos.size();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;");
    }
}
