package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.common.shop.ProductSummaryDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Square product tile: cover photo plus price, name and want-button only.
 */
final class ProductCard extends JPanel {

    private static final int RADIUS = 16;

    private final JLayeredPane layers;
    private final PhotoPane photo;
    private final OverlayPane overlay;

    ProductCard(ProductSummaryDto product, Consumer<ProductSummaryDto> onAdd, int cellSize) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension square = new Dimension(cellSize, cellSize);
        setPreferredSize(square);
        setMinimumSize(square);
        setMaximumSize(square);

        JLayeredPane layers = new JLayeredPane();
        PhotoPane photo = new PhotoPane(product);
        OverlayPane overlay = new OverlayPane(product, onAdd);
        layers.add(photo, JLayeredPane.DEFAULT_LAYER);
        layers.add(overlay, JLayeredPane.PALETTE_LAYER);
        add(layers, BorderLayout.CENTER);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openDetail(product, onAdd);
            }
        });
        this.layers = layers;
        this.photo = photo;
        this.overlay = overlay;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int width = layers.getWidth();
        int height = layers.getHeight();
        photo.setBounds(0, 0, width, height);
        int overlayHeight = Math.max(108, Math.min(height * 11 / 20, height - 24));
        overlay.setBounds(0, Math.max(0, height - overlayHeight), width, overlayHeight);
    }

    private void openDetail(ProductSummaryDto product, Consumer<ProductSummaryDto> onAdd) {
        ProductDetailDialog dialog = new ProductDetailDialog(
                SwingUtilities.getWindowAncestor(this), product, onAdd);
        dialog.setVisible(true);
    }

    private static final class PhotoPane extends JPanel {

        private final BufferedImage image;
        private final Color fallback;

        PhotoPane(ProductSummaryDto product) {
            this.image = ShopPhotoSupport.image(product.getCoverPhoto());
            this.fallback = ShopPalette.categoryTone(product.getCategoryName());
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D brush = (Graphics2D) graphics.create();
            brush.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            brush.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int width = getWidth();
            int height = getHeight();
            RoundRectangle2D clip = new RoundRectangle2D.Float(0, 0, width, height, RADIUS, RADIUS);
            brush.setClip(clip);
            brush.setColor(fallback);
            brush.fill(clip);
            if (image != null && image.getWidth() > 0 && image.getHeight() > 0 && width > 0 && height > 0) {
                double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
                int drawWidth = (int) Math.round(image.getWidth() * scale);
                int drawHeight = (int) Math.round(image.getHeight() * scale);
                brush.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight, null);
            }
            brush.dispose();
        }
    }

    private static final class OverlayPane extends JPanel {

        OverlayPane(ProductSummaryDto product, Consumer<ProductSummaryDto> onAdd) {
            setOpaque(false);
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JLabel name = new JLabel(product.getName());
            name.setForeground(Color.WHITE);
            name.setFont(new Font("SansSerif", Font.BOLD, 16));

            JLabel price = new JLabel("¥" + String.format("%.2f", product.getPriceFen() / 100.0));
            price.setFont(new Font("SansSerif", Font.BOLD, 22));
            price.setForeground(Color.WHITE);

            JButton want = ShopPalette.quietButton(product.getStockQty() > 0 ? "我想要" : "已售罄");
            want.setEnabled(product.getStockQty() > 0);
            want.addActionListener(event -> onAdd.accept(product));

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.add(price, BorderLayout.WEST);
            row.add(want, BorderLayout.EAST);

            add(name, BorderLayout.NORTH);
            add(row, BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D brush = (Graphics2D) graphics.create();
            brush.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            brush.setPaint(new GradientPaint(
                    0, 0, new Color(15, 23, 42, 10),
                    0, getHeight(), new Color(15, 23, 42, 200)));
            brush.fillRect(0, 0, getWidth(), getHeight());
            brush.dispose();
            super.paintComponent(graphics);
        }
    }
}
