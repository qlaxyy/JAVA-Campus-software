package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.view.ResponsiveLayout;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyInspectionDTO;
import edu.seu.vcampus.common.library.CopyInspectionRequest;
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
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/** Simulates a self-service terminal that scans one physical-copy barcode at a time. */
public final class SelfServicePanel extends JPanel {

    private final ClientContext context;
    private final JTextField barcode = new JTextField(28);
    private final JButton borrow = new JButton("借书登记");
    private final JButton returnCopy = new JButton("归还登记");
    private final JLabel reservationCheck = new JLabel(
            " ",
            SwingConstants.CENTER);
    private final JLabel outcome = new JLabel("", SwingConstants.CENTER);
    private final Runnable circulationChanged;
    private final Timer inspectionTimer;
    private CopyInspectionDTO inspection;
    private int inspectionVersion;
    private boolean working;

    /** @param context shared authenticated client context */
    public SelfServicePanel(ClientContext context) {
        this(context, () -> { });
    }

    SelfServicePanel(ClientContext context, Runnable circulationChanged) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.circulationChanged = Objects.requireNonNull(
                circulationChanged, "circulationChanged must not be null");
        setLayout(new BorderLayout(16, 16));
        LibraryUiTheme.installPage(this);
        setBorder(BorderFactory.createEmptyBorder(10, 28, 14, 28));
        setName("library.selfService");
        barcode.setName("library.selfService.barcode");
        reservationCheck.setName("library.selfService.reservationCheck");
        outcome.setName("library.selfService.outcome");
        reservationCheck.setVisible(false);
        outcome.setVisible(false);
        outcome.addPropertyChangeListener("text", event ->
                outcome.setVisible(!outcome.getText().isBlank()));

        SessionInfo session = context.currentSession().orElse(null);
        String identity = session == null ? "未登录"
                : session.getDisplayName();
        JLabel user = new JLabel("当前用户：" + identity, SwingConstants.LEFT);
        user.setName("library.selfService.user");
        user.setForeground(LibraryUiTheme.PRIMARY_DARK);
        user.setFont(user.getFont().deriveFont(Font.BOLD, 14F));

        JPanel form = new JPanel(new GridBagLayout());
        form.setName("library.selfService.card");
        LibraryUiTheme.styleCard(form);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        form.add(user, c);
        c.gridy = 1;
        JLabel barcodeLabel = new JLabel("馆藏条码");
        barcodeLabel.setForeground(LibraryUiTheme.TEXT);
        barcodeLabel.setFont(barcodeLabel.getFont().deriveFont(Font.BOLD, 13F));
        form.add(barcodeLabel, c);
        c.gridy = 2;
        LibraryUiTheme.styleTextField(barcode);
        barcode.setPreferredSize(new Dimension(520, 48));
        barcode.setFont(barcode.getFont().deriveFont(Font.BOLD, 18F));
        form.add(barcode, c);
        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        LibraryUiTheme.stylePrimaryButton(borrow);
        LibraryUiTheme.styleSecondaryButton(returnCopy);
        LibraryUiTheme.makeLargeButton(borrow);
        LibraryUiTheme.makeLargeButton(returnCopy);
        actions.add(borrow);
        actions.add(returnCopy);
        c.gridy = 3;
        form.add(actions, c);
        c.gridy = 4;
        LibraryUiTheme.styleStatusLabel(reservationCheck);
        form.add(reservationCheck, c);
        c.gridy = 5;
        outcome.setForeground(LibraryUiTheme.TEXT);
        outcome.setFont(outcome.getFont().deriveFont(Font.BOLD, 13F));
        form.add(outcome, c);

        // 让终端卡片保持自身尺寸并居中，不要被拉伸成整页高。
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
        center.add(ResponsiveLayout.compact(form, 720), new GridBagConstraints());
        add(center, BorderLayout.CENTER);

        inspectionTimer = new Timer(350, event -> inspectBarcode(true));
        inspectionTimer.setRepeats(false);
        barcode.addActionListener(event -> {
            inspectionTimer.stop();
            inspectBarcode(true);
        });
        barcode.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { barcodeChanged(); }
            public void removeUpdate(DocumentEvent event) { barcodeChanged(); }
            public void changedUpdate(DocumentEvent event) { barcodeChanged(); }
        });
        borrow.addActionListener(event -> submit(true));
        returnCopy.addActionListener(event -> submit(false));
        updateButtons();
    }

    private void submit(boolean borrowing) {
        CopyInspectionDTO approved = inspection;
        if (working || approved == null || barcode.getText().isBlank()
                || (borrowing && !approved.isBorrowAllowed())
                || (!borrowing && !approved.isReturnAllowed())) {
            return;
        }
        String scanned = barcode.getText().strip();
        inspection = null;
        setWorking(true);
        reservationCheck.setText(borrowing
                ? "正在办理借书……"
                : "正在办理还书……");
        outcome.setText(borrowing ? "正在登记借书……" : "正在登记归还……");
        new SwingWorker<TerminalResult, Void>() {
            @Override
            protected TerminalResult doInBackground() throws Exception {
                Response response = borrowing
                        ? context.send(LibraryActions.BORROW_COPY,
                                new CopyBorrowRequest(scanned))
                        : context.send(LibraryActions.RETURN_COPY,
                                new CopyReturnRequest(scanned));
                return new TerminalResult(
                        response, approved.isReservedForCurrentUser());
            }

            @Override
            protected void done() {
                try {
                    TerminalResult result = get();
                    Response response = result.response();
                    if (response == null || !response.isSuccess()) {
                        reservationCheck.setText("正在刷新馆藏状态……");
                        outcome.setText((borrowing ? "借书失败：" : "归还失败：")
                                + (response == null ? "服务器未返回结果"
                                : LibraryMessages.failure(response)));
                        return;
                    }
                    // 先清空条码：清空动作会按“无条码”隐藏结果标签，之后再写结论并重新显示，
                    // 否则“已领取预约图书”这类提示刚设置就被隐藏，读者永远看不到。
                    barcode.setText("");
                    reservationCheck.setText(borrowing && result.ownedReservation()
                            ? "已领取预约图书"
                            : "办理成功");
                    reservationCheck.setVisible(true);
                    outcome.setText(borrowing
                            ? "借书成功：" + scanned + "，借期 30 天"
                            : "归还成功：" + scanned + "，单册正在等待管理员上架");
                    circulationChanged.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showUncertain(borrowing);
                } catch (ExecutionException exception) {
                    showUncertain(borrowing);
                } finally {
                    setWorking(false);
                    if (!barcode.getText().isBlank()) {
                        inspectBarcode(false);
                    }
                }
            }
        }.execute();
    }

    private void barcodeChanged() {
        reservationCheck.setVisible(!barcode.getText().isBlank());
        inspectionVersion++;
        inspection = null;
        inspectionTimer.stop();
        updateButtons();
        if (barcode.getText().isBlank()) {
            if (!working) {
                reservationCheck.setText(" ");
                outcome.setText("");
            }
            return;
        }
        reservationCheck.setText("正在查询……");
        outcome.setText(" ");
        inspectionTimer.restart();
    }

    private void inspectBarcode(boolean updateOutcome) {
        if (working || barcode.getText().isBlank()) {
            return;
        }
        String scanned = barcode.getText().strip();
        int version = inspectionVersion;
        inspection = null;
        updateButtons();
        reservationCheck.setText("正在查询……");
        if (updateOutcome) {
            outcome.setText(" ");
        }
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.INSPECT_COPY,
                        new CopyInspectionRequest(scanned));
            }

            @Override
            protected void done() {
                if (version != inspectionVersion
                        || !scanned.equals(barcode.getText().strip())) {
                    return;
                }
                try {
                    Response response = get();
                    if (response == null || !response.isSuccess()
                            || !(response.getData() instanceof CopyInspectionDTO value)) {
                        reservationCheck.setText("查询失败：" + (response == null
                                ? "服务器未返回结果" : LibraryMessages.failure(response)));
                        if (updateOutcome) {
                            outcome.setText("当前不能执行借还操作");
                        }
                        return;
                    }
                    inspection = value;
                    reservationCheck.setText(value.getBookTitle()
                            + "｜" + statusLabel(value.getCopyStatus())
                            + "｜" + value.getStatusMessage());
                    if (updateOutcome) {
                        outcome.setText(value.isBorrowAllowed() || value.isReturnAllowed()
                                ? " "
                                : "当前没有可执行的借还操作");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    reservationCheck.setText("查询已中断，请重新输入条码");
                } catch (ExecutionException exception) {
                    reservationCheck.setText("查询失败，请检查网络后重试");
                } finally {
                    updateButtons();
                }
            }
        }.execute();
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "可借";
            case "RESERVED" -> "预约待取";
            case "LOANED" -> "已借出";
            case "WAITING_SHELVING" -> "待上架";
            case "WITHDRAWN" -> "已注销";
            default -> status;
        };
    }

    private void showUncertain(boolean borrowing) {
        outcome.setText((borrowing ? "借书" : "归还")
                + "结果未确认，请到线上图书馆刷新“我的图书馆”核对，勿重复提交");
    }

    private void setWorking(boolean value) {
        working = value;
        if (value) {
            inspectionTimer.stop();
        }
        barcode.setEnabled(!value);
        updateButtons();
    }

    private void updateButtons() {
        boolean inspected = !working && inspection != null
                && inspection.getBarcode().equals(barcode.getText().strip());
        borrow.setEnabled(inspected && inspection.isBorrowAllowed());
        returnCopy.setEnabled(inspected && inspection.isReturnAllowed());
    }

    private record TerminalResult(Response response, boolean ownedReservation) { }
}
