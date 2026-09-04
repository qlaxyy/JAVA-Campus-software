package edu.seu.vcampus.server.module.shop;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates small PNG covers for the in-memory demo catalog.
 */
final class ShopDemoPhotos {

    private ShopDemoPhotos() {
    }

    static List<byte[]> forProduct(String categoryName, String title) {
        Color base = switch (categoryName) {
            case "文具" -> new Color(45, 148, 140);
            case "日常用品" -> new Color(56, 132, 163);
            case "食品" -> new Color(62, 148, 108);
            default -> new Color(15, 118, 110);
        };
        List<byte[]> photos = new ArrayList<>();
        photos.add(render(base, title, "主图"));
        photos.add(render(base.darker(), title, "细节"));
        return photos;
    }

    private static byte[] render(Color background, String title, String badge) {
        BufferedImage image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(background);
        graphics.fillRect(0, 0, 320, 320);
        graphics.setColor(new Color(255, 255, 255, 40));
        graphics.fillRoundRect(24, 24, 272, 272, 28, 28);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
        graphics.drawString(trim(title, 10), 40, 160);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 16));
        graphics.drawString(badge, 40, 190);
        graphics.dispose();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", buffer);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return buffer.toByteArray();
    }

    private static String trim(String title, int maxChars) {
        if (title.length() <= maxChars) {
            return title;
        }
        return title.substring(0, maxChars) + "…";
    }
}
