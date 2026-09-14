package edu.seu.vcampus.server.module.shop;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo catalog photos: bundled Wikimedia stills padded to an 800×800 white
 * square (Taobao-style main image), with generated covers as fallback.
 */
final class ShopDemoPhotos {

    private static final int MAIN_SIZE = 480;
    private static final Map<Long, List<byte[]>> BUNDLED = new ConcurrentHashMap<>();

    private ShopDemoPhotos() {
    }

    static List<byte[]> forProduct(String categoryName, String title) {
        return generated(categoryName, title);
    }

    static List<byte[]> forProduct(long productId, String categoryName, String title) {
        List<byte[]> bundled = BUNDLED.computeIfAbsent(productId, ShopDemoPhotos::loadBundled);
        if (!bundled.isEmpty()) {
            return bundled;
        }
        return generated(categoryName, title);
    }

    private static List<byte[]> loadBundled(long productId) {
        List<byte[]> photos = new ArrayList<>();
        for (int index = 1; index <= 4; index++) {
            byte[] photo = loadOne(productId, index);
            if (photo != null) {
                photos.add(photo);
            }
        }
        return photos;
    }

    private static byte[] loadOne(long productId, int index) {
        String[] suffixes = {".jpg", ".jpeg", ".png"};
        for (String suffix : suffixes) {
            String path = "/shop-demo/" + productId + "-" + index + suffix;
            try (InputStream stream = ShopDemoPhotos.class.getResourceAsStream(path)) {
                if (stream == null) {
                    continue;
                }
                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    continue;
                }
                return toSquareJpeg(image);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        return null;
    }

    private static List<byte[]> generated(String categoryName, String title) {
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

    private static byte[] toSquareJpeg(BufferedImage source) {
        BufferedImage canvas = new BufferedImage(MAIN_SIZE, MAIN_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, MAIN_SIZE, MAIN_SIZE);
        double scale = Math.min(
                MAIN_SIZE / (double) source.getWidth(),
                MAIN_SIZE / (double) source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        graphics.drawImage(source, (MAIN_SIZE - width) / 2, (MAIN_SIZE - height) / 2, width, height, null);
        graphics.dispose();
        return writeJpeg(canvas);
    }

    private static byte[] render(Color accent, String title, String badge) {
        BufferedImage image = new BufferedImage(MAIN_SIZE, MAIN_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, MAIN_SIZE, MAIN_SIZE);
        graphics.setColor(new Color(245, 245, 245));
        graphics.fillRoundRect(40, 40, 400, 300, 24, 24);
        graphics.setColor(accent);
        graphics.fillRoundRect(130, 100, 220, 180, 20, 20);
        graphics.setColor(new Color(32, 32, 32));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
        graphics.drawString(trim(title, 12), 40, 400);
        graphics.setColor(new Color(120, 120, 120));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        graphics.drawString(badge + " · 校园商店", 40, 428);
        graphics.dispose();
        return writeJpeg(image);
    }

    private static byte[] writeJpeg(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "png", buffer);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            return buffer.toByteArray();
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.88f);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(buffer)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), param);
            output.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            writer.dispose();
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
