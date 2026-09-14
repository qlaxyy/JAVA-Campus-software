package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/** Hospital-owned modal surfaces, avoiding platform dialogs that break visual continuity. */
final class HospitalDialogs {

    private HospitalDialogs() {
    }

    static boolean confirm(
            Component owner,
            String title,
            String message,
            String confirmText,
            boolean warning) {
        return show(owner, title, messagePanel(message), confirmText, "取消", warning);
    }

    static boolean confirmContent(
            Component owner,
            String title,
            JComponent content,
            String confirmText,
            String cancelText) {
        return show(owner, title, content, confirmText, cancelText, false);
    }

    static void information(Component owner, String title, String message) {
        show(owner, title, messagePanel(message), "知道了", null, false);
    }

    static void warning(Component owner, String title, String message) {
        show(owner, title, messagePanel(message), "知道了", null, true);
    }

    private static boolean show(
            Component owner,
            String title,
            JComponent content,
            String confirmText,
            String cancelText,
            boolean warning) {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        AtomicBoolean confirmed = new AtomicBoolean();
        Window parent = SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(HospitalTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        JLabel heading = new JLabel(title);
        heading.setFont(HospitalTheme.uiFont(Font.BOLD, 22F));
        heading.setForeground(warning ? HospitalTheme.WARNING : HospitalTheme.TEXT);
        root.add(heading, BorderLayout.NORTH);

        JPanel body = new HospitalTheme.SurfacePanel(
                warning ? HospitalTheme.WARNING_LIGHT : HospitalTheme.SURFACE,
                14, warning ? HospitalTheme.WARNING : HospitalTheme.BORDER);
        body.setLayout(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        body.add(content, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        if (cancelText != null) {
            JButton cancel = HospitalTheme.quietButton(cancelText);
            cancel.addActionListener(event -> dialog.dispose());
            actions.add(cancel);
        }
        JButton confirm = HospitalTheme.primaryButton(confirmText);
        confirm.addActionListener(event -> {
            confirmed.set(true);
            dialog.dispose();
        });
        actions.add(confirm);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        dialog.getRootPane().getActionMap().put("close", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
        dialog.pack();
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        Dimension preferred = dialog.getSize();
        dialog.setSize(
                Math.min(Math.max(420, preferred.width), Math.max(320, screen.width - 48)),
                Math.min(Math.max(240, preferred.height), Math.max(220, screen.height - 48)));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return confirmed.get();
    }

    private static JTextArea messagePanel(String message) {
        JTextArea copy = HospitalResponsiveLayout.wrappingText(
                message, HospitalTheme.uiFont(Font.PLAIN, 14F), HospitalTheme.TEXT);
        copy.setColumns(36);
        int explicitLines = Math.max(1, message.split("\\R", -1).length);
        copy.setRows(Math.min(10, Math.max(2, explicitLines + message.length() / 34)));
        copy.setBackground(new Color(0, 0, 0, 0));
        return copy;
    }
}
