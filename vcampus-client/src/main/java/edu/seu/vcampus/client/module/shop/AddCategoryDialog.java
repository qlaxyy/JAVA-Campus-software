package edu.seu.vcampus.client.module.shop;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.shop.AddCategoryRequest;
import edu.seu.vcampus.common.shop.ShopActions;
import edu.seu.vcampus.common.shop.ShopCategoryDto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * Shop-admin form to create a product category such as 相机.
 */
final class AddCategoryDialog extends JDialog {

    private final ClientContext context;
    private final Consumer<ShopCategoryDto> onAdded;
    private final JTextField nameField = new JTextField();
    private final JLabel statusLabel = new JLabel("名称 1–40 字，不能与已有分类重复");
    private final JButton saveButton = ShopPalette.accentButton("确认添加");

    AddCategoryDialog(Window owner, ClientContext context, Consumer<ShopCategoryDto> onAdded) {
        super(owner, "新增分类", ModalityType.DOCUMENT_MODAL);
        this.context = context;
        this.onAdded = onAdded;
        setLayout(new BorderLayout(0, 10));
        getContentPane().setBackground(ShopPalette.PAGE);
        JPanel form = new JPanel(new BorderLayout(0, 4));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        JLabel label = new JLabel("分类名称");
        label.setForeground(ShopPalette.TEXT);
        form.add(label, BorderLayout.NORTH);
        form.add(nameField, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));
        statusLabel.setForeground(ShopPalette.MUTED);
        saveButton.addActionListener(event -> save());
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(saveButton, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        setSize(420, 180);
        setLocationRelativeTo(owner);
    }

    private void save() {
        AddCategoryRequest request;
        try {
            request = new AddCategoryRequest(nameField.getText());
        } catch (IllegalArgumentException exception) {
            statusLabel.setText("请填写 1 到 40 个字的分类名称");
            return;
        }
        saveButton.setEnabled(false);
        statusLabel.setText("正在添加……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(ShopActions.ADD_CATEGORY, request);
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        statusLabel.setText(response.getMessage());
                        return;
                    }
                    if (response.getData() instanceof ShopCategoryDto created) {
                        onAdded.accept(created);
                    }
                    dispose();
                } catch (Exception exception) {
                    statusLabel.setText("无法连接服务器");
                }
            }
        }.execute();
    }
}
