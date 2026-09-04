package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.ProductSummaryDto;
import edu.seu.vcampus.common.shop.PublishProductRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopCategories;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin publish form modeled on Xianyu's sell template.
 */
final class PublishProductDialog extends JDialog {

    private static final CategoryChoice[] CATEGORIES = {
            new CategoryChoice("文具", ShopCategories.STATIONERY),
            new CategoryChoice("日常用品", ShopCategories.DAILY),
            new CategoryChoice("食品", ShopCategories.FOOD)
    };

    private final ClientContext context;
    private final Runnable onPublished;
    private final List<byte[]> photos = new ArrayList<>();
    private final JPanel photoStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JTextField titleField = new JTextField();
    private final JTextArea descriptionArea = new JTextArea(6, 28);
    private final JComboBox<CategoryChoice> categoryBox = new JComboBox<>(CATEGORIES);
    private final JTextField priceField = new JTextField();
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JLabel statusLabel = new JLabel("至少 1 张照片，最多 9 张");
    private final JButton publishButton = ShopPalette.accentButton("确认发布");

    PublishProductDialog(Window owner, ClientContext context, Runnable onPublished) {
        super(owner, "发布闲置", ModalityType.DOCUMENT_MODAL);
        this.context = context;
        this.onPublished = onPublished;
        setLayout(new BorderLayout());
        getContentPane().setBackground(ShopPalette.PAGE);
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        setSize(520, 680);
        setLocationRelativeTo(owner);
        refreshPhotoStrip();
    }

    private JScrollPane buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        form.add(section("商品图片", photoSection()));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("标题", titleField));
        form.add(Box.createVerticalStrut(10));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(460, 120));
        form.add(labeled("描述", descriptionScroll));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("分类 / 属性", categoryBox));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("价格（元）", priceField));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("数量", quantitySpinner));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ShopPalette.PAGE);
        wrap.add(form, BorderLayout.NORTH);
        return new JScrollPane(wrap);
    }

    private JPanel photoSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        photoStrip.setOpaque(false);
        JButton add = ShopPalette.quietButton("添加照片");
        add.addActionListener(event -> choosePhotos());
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        JLabel hint = new JLabel("参考闲鱼：先放图，再写卖点");
        hint.setForeground(ShopPalette.MUTED);
        north.add(hint, BorderLayout.WEST);
        north.add(add, BorderLayout.EAST);
        section.add(north, BorderLayout.NORTH);
        section.add(photoStrip, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(8, 18, 14, 18));
        footer.setOpaque(false);
        statusLabel.setForeground(ShopPalette.MUTED);
        publishButton.addActionListener(event -> publish());
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(publishButton, BorderLayout.EAST);
        return footer;
    }

    private void choosePhotos() {
        if (photos.size() >= 9) {
            statusLabel.setText("最多 9 张照片");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("图片", "jpg", "jpeg", "png", "webp", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        for (File file : chooser.getSelectedFiles()) {
            if (photos.size() >= 9) {
                break;
            }
            try {
                byte[] compressed = ShopPhotoSupport.compressFile(file);
                if (compressed.length > ProductSummaryDto.MAX_PHOTO_BYTES) {
                    JOptionPane.showMessageDialog(this, "图片过大：" + file.getName(), "发布闲置", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                photos.add(compressed);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "无法使用图片：" + file.getName(), "发布闲置", JOptionPane.WARNING_MESSAGE);
            }
        }
        refreshPhotoStrip();
    }

    private void refreshPhotoStrip() {
        photoStrip.removeAll();
        for (byte[] photo : List.copyOf(photos)) {
            JPanel tile = new JPanel(new BorderLayout());
            tile.setPreferredSize(new Dimension(72, 88));
            tile.setBackground(ShopPalette.CARD);
            tile.setBorder(BorderFactory.createLineBorder(ShopPalette.LINE));
            JLabel thumb = new JLabel(ShopPhotoSupport.icon(photo, 72, 72));
            JButton remove = ShopPalette.quietButton("删");
            remove.addActionListener(event -> {
                photos.remove(photo);
                refreshPhotoStrip();
            });
            tile.add(thumb, BorderLayout.CENTER);
            tile.add(remove, BorderLayout.SOUTH);
            photoStrip.add(tile);
        }
        photoStrip.revalidate();
        photoStrip.repaint();
        statusLabel.setText("已选 " + photos.size() + " 张照片，至少 1 张");
    }

    private void publish() {
        CategoryChoice category = (CategoryChoice) categoryBox.getSelectedItem();
        int priceFen;
        try {
            BigDecimal yuan = new BigDecimal(priceField.getText().trim());
            if (yuan.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("price");
            }
            priceFen = yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (RuntimeException exception) {
            statusLabel.setText("请填写正确的价格");
            return;
        }
        if (photos.isEmpty() || titleField.getText().isBlank() || descriptionArea.getText().isBlank()
                || category == null) {
            statusLabel.setText("照片、标题、分类和描述都必填");
            return;
        }
        PublishProductRequest request;
        try {
            request = new PublishProductRequest(
                    titleField.getText(),
                    category.categoryId(),
                    descriptionArea.getText(),
                    priceFen,
                    (Integer) quantitySpinner.getValue(),
                    photos);
        } catch (IllegalArgumentException exception) {
            statusLabel.setText("上架信息不完整");
            return;
        }
        publishButton.setEnabled(false);
        statusLabel.setText("正在发布……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.PUBLISH_PRODUCT, request);
            }

            @Override
            protected void done() {
                publishButton.setEnabled(true);
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        statusLabel.setText(response.getMessage());
                        return;
                    }
                    onPublished.run();
                    dispose();
                } catch (Exception exception) {
                    statusLabel.setText("无法连接服务器");
                }
            }
        }.execute();
    }

    private static JPanel section(String title, JPanel body) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setForeground(ShopPalette.TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel labeled(String title, java.awt.Component field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setForeground(ShopPalette.TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private record CategoryChoice(String label, long categoryId) {
        @Override
        public String toString() {
            return label;
        }
    }
}
