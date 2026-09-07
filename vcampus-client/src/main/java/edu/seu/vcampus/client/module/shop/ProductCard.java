package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.shop.ProductSummaryDto;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Xianyu-style tile: large photo, then a name band and a price band.
 */
final class ProductCard extends JPanel {

    private static final int RADIUS = 16;

    ProductCard(
            ProductSummaryDto product,
            Consumer<ProductSummaryDto> onWant,
            Consumer<ProductSummaryDto> onAddToCart,
            int cellSize) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension card = new Dimension(cellSize, ShopCatalogGrid.cardHeight(cellSize));
        setPreferredSize(card);
        setMinimumSize(card);
        setMaximumSize(card);

        PhotoPane photo = new PhotoPane(product);
        photo.setPreferredSize(new Dimension(cellSize, cellSize));

        JLabel name = new JLabel(nameHtml(product.getName(), cellSize));
        name.setFont(new Font("SansSerif", Font.PLAIN, 13));
        name.setForeground(ShopPalette.TEXT);
        name.setOpaque(true);
        name.setBackground(ShopPalette.CARD);
        name.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ShopPalette.LINE),
                BorderFactory.createEmptyBorder(6, 10, 4, 10)));
        name.setPreferredSize(new Dimension(cellSize, ShopCatalogGrid.NAME_BAND));

        JLabel price = new JLabel(ShopMoney.yuan(product.getPriceFen()));
        price.setFont(ShopPalette.priceFont());
        price.setForeground(ShopPalette.PRICE);
        price.setOpaque(true);
        price.setBackground(ShopPalette.CARD);
        price.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ShopPalette.LINE),
                BorderFactory.createEmptyBorder(4, 10, 8, 10)));
        price.setPreferredSize(new Dimension(cellSize, ShopCatalogGrid.PRICE_BAND));

        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(true);
        text.setBackground(ShopPalette.CARD);
        text.add(name, BorderLayout.NORTH);
        text.add(price, BorderLayout.SOUTH);

        add(photo, BorderLayout.CENTER);
        add(text, BorderLayout.SOUTH);

        MouseAdapter open = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                ProductDetailDialog dialog = new ProductDetailDialog(
                        SwingUtilities.getWindowAncestor(ProductCard.this),
                        product,
                        onWant,
                        onAddToCart);
                dialog.setVisible(true);
            }
        };
        addMouseListener(open);
        photo.addMouseListener(open);
        name.addMouseListener(open);
        price.addMouseListener(open);
        text.addMouseListener(open);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D brush = (Graphics2D) graphics.create();
        brush.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        brush.setColor(ShopPalette.CARD);
        brush.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        brush.dispose();
        super.paintComponent(graphics);
    }

    @Override
    protected void paintBorder(Graphics graphics) {
        Graphics2D brush = (Graphics2D) graphics.create();
        brush.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        brush.setColor(ShopPalette.LINE);
        brush.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        brush.dispose();
    }

    private static String nameHtml(String name, int cellSize) {
        int inner = Math.max(80, cellSize - 24);
        return "<html><body style='width:" + inner + "px'>" + escape(name) + "</body></html>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;");
    }

    private static final class PhotoPane extends JPanel {

        private final BufferedImage image;
        private final Color fallback;

        PhotoPane(ProductSummaryDto product) {
            this.image = ShopPhotoSupport.image(product.getCoverPhoto());
            this.fallback = ShopPalette.categoryTone(product.getCategoryName());
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D brush = (Graphics2D) graphics.create();
            brush.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            brush.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int width = getWidth();
            int height = getHeight();
            Path2D clip = topRoundedRect(width, height, RADIUS);
            brush.setClip(clip);
            brush.setColor(fallback);
            brush.fill(clip);
            if (image != null && image.getWidth() > 0 && image.getHeight() > 0 && width > 0 && height > 0) {
                double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
                int drawWidth = (int) Math.round(image.getWidth() * scale);
                int drawHeight = (int) Math.round(image.getHeight() * scale);
                brush.drawImage(
                        image,
                        (width - drawWidth) / 2,
                        (height - drawHeight) / 2,
                        drawWidth,
                        drawHeight,
                        null);
            }
            brush.dispose();
        }

        private static Path2D topRoundedRect(int width, int height, int radius) {
            float r = Math.min(radius, Math.min(width, height) / 2F);
            Path2D path = new Path2D.Float();
            path.moveTo(r, 0);
            path.lineTo(width - r, 0);
            path.quadTo(width, 0, width, r);
            path.lineTo(width, height);
            path.lineTo(0, height);
            path.lineTo(0, r);
            path.quadTo(0, 0, r, 0);
            path.closePath();
            return path;
        }
    }
}
