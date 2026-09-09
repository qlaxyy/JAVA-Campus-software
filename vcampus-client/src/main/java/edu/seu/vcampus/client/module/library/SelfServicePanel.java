package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/** Simulates a self-service terminal that scans one physical-copy barcode at a time. */
public final class SelfServicePanel extends JPanel {

    private final ClientContext context;
    private final JTextField barcode = new JTextField(28);
    private final JButton borrow = new JButton("借书登记");
    private final JButton returnCopy = new JButton("归还登记");
    private final JLabel outcome = new JLabel("请扫描或输入实体书馆藏条码", SwingConstants.CENTER);
    private boolean working;

    /** @param context shared authenticated client context */
    public SelfServicePanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(36, 80, 36, 80));
        setName("library.selfService");
        barcode.setName("library.selfService.barcode");
        outcome.setName("library.selfService.outcome");

        SessionInfo session = context.currentSession().orElse(null);
        String identity = session == null ? "未登录"
                : session.getDisplayName();
        JLabel user = new JLabel("当前用户：" + identity, SwingConstants.CENTER);
        user.setName("library.selfService.user");
        add(user, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("模拟自助借还终端"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 8, 10, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        form.add(new JLabel("馆藏条码"), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(barcode, c);
        JPanel actions = new JPanel();
        actions.add(borrow);
        actions.add(returnCopy);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        form.add(actions, c);
        c.gridy = 2;
        form.add(new JLabel("提示：检索页只查书；拿到实体书后在这里登记借还",
                SwingConstants.CENTER), c);
        add(form, BorderLayout.CENTER);
        add(outcome, BorderLayout.SOUTH);

        barcode.addActionListener(event -> {
            if (borrow.isEnabled()) {
                submit(true);
            }
        });
        barcode.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { updateButtons(); }
            public void removeUpdate(DocumentEvent event) { updateButtons(); }
            public void changedUpdate(DocumentEvent event) { updateButtons(); }
        });
        borrow.addActionListener(event -> submit(true));
        returnCopy.addActionListener(event -> submit(false));
        updateButtons();
    }

    private void submit(boolean borrowing) {
        if (working || barcode.getText().isBlank()) {
            return;
        }
        String scanned = barcode.getText().strip();
        setWorking(true);
        outcome.setText(borrowing ? "正在登记借书……" : "正在登记归还……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return borrowing
                        ? context.send(LibraryActions.BORROW_COPY,
                                new CopyBorrowRequest(scanned))
                        : context.send(LibraryActions.RETURN_COPY,
                                new CopyReturnRequest(scanned));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response == null || !response.isSuccess()) {
                        outcome.setText((borrowing ? "借书失败：" : "归还失败：")
                                + (response == null ? "服务器未返回结果"
                                : LibraryMessages.failure(response)));
                        return;
                    }
                    outcome.setText(borrowing
                            ? "借书成功：" + scanned + "，借期 30 天"
                            : "归还成功：" + scanned + "，单册正在等待管理员上架");
                    barcode.setText("");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showUncertain(borrowing);
                } catch (ExecutionException exception) {
                    showUncertain(borrowing);
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showUncertain(boolean borrowing) {
        outcome.setText((borrowing ? "借书" : "归还")
                + "结果未确认，请到线上图书馆刷新“我的图书馆”核对，勿重复提交");
    }

    private void setWorking(boolean value) {
        working = value;
        barcode.setEnabled(!value);
        updateButtons();
    }

    private void updateButtons() {
        boolean enabled = !working && !barcode.getText().isBlank();
        borrow.setEnabled(enabled);
        returnCopy.setEnabled(enabled);
    }
}
