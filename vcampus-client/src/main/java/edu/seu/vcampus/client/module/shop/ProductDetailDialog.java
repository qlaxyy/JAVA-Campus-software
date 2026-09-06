package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.shop.ProductSummaryDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;

/**
 * Xianyu-style product detail: photo pager, price, attributes and seller copy.
 */
final class ProductDetailDialog extends JDialog {

    private int photoIndex;

    ProductDetailDialog(
            Window owner,
            ProductSummaryDto product,
            Consumer<ProductSummaryDto> onWant,
            Consumer<ProductSummaryDto> onAddToCart) {
        super(owner, "商品详情", ModalityType.DOCUMENT_MODAL);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ShopPalette.PAGE);

        JLabel photo = new JLabel(ShopPhotoSupport.icon(product.getPhotos().getFirst(), 360, 360), SwingConstants.CENTER);
        photo.setOpaque(true);
        photo.setBackground(ShopPalette.CARD);
        photo.setPreferredSize(new Dimension(360, 360));

        JButton previous = ShopPalette.quietButton("上一张");
        JButton next = ShopPalette.quietButton("下一张");
        JLabel counter = new JLabel(counterText(product), SwingConstants.CENTER);
        counter.setForeground(ShopPalette.MUTED);
        previous.setEnabled(product.getPhotos().size() > 1);
        next.setEnabled(product.getPhotos().size() > 1);
        previous.addActionListener(event -> {
            photoIndex = (photoIndex - 1 + product.getPhotos().size()) % product.getPhotos().size();
            photo.setIcon(ShopPhotoSupport.icon(product.getPhotos().get(photoIndex), 360, 360));
            counter.setText(counterText(product));
        });
        next.addActionListener(event -> {
            photoIndex = (photoIndex + 1) % product.getPhotos().size();
            photo.setIcon(ShopPhotoSupport.icon(product.getPhotos().get(photoIndex), 360, 360));
            counter.setText(counterText(product));
        });

        JPanel pager = new JPanel(new BorderLayout());
        pager.setOpaque(false);
        pager.add(previous, BorderLayout.WEST);
        pager.add(counter, BorderLayout.CENTER);
        pager.add(next, BorderLayout.EAST);

        JPanel gallery = new JPanel(new BorderLayout(0, 8));
        gallery.setBackground(ShopPalette.CARD);
        gallery.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        gallery.add(photo, BorderLayout.CENTER);
        gallery.add(pager, BorderLayout.SOUTH);

        JLabel price = new JLabel("¥" + String.format("%.2f", product.getPriceFen() / 100.0));
        price.setFont(new Font("SansSerif", Font.BOLD, 26));
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

        JLabel description = new JLabel("<html><body style='width:300px'><b>描述</b><br>"
                + escape(product.getDescription()).replace("\n", "<br>")
                + "</body></html>");
        description.setForeground(ShopPalette.TEXT);

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

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(cart);
        actions.add(want);

        JPanel copy = new JPanel(new BorderLayout(0, 10));
        copy.setOpaque(false);
        copy.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(price, BorderLayout.NORTH);
        north.add(title, BorderLayout.CENTER);
        north.add(facts, BorderLayout.SOUTH);
        copy.add(north, BorderLayout.NORTH);
        copy.add(description, BorderLayout.CENTER);
        copy.add(actions, BorderLayout.SOUTH);

        add(gallery, BorderLayout.NORTH);
        add(new JScrollPane(copy), BorderLayout.CENTER);
        setSize(400, 640);
        setLocationRelativeTo(owner);
    }

    private String counterText(ProductSummaryDto product) {
        List<byte[]> photos = product.getPhotos();
        return (photoIndex + 1) + " / " + photos.size();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;");
    }
}
