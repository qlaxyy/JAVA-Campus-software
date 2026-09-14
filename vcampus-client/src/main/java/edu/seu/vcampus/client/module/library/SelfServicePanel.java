package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
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
            "预约校验：借书时由服务器核验单册是否为当前用户保留",
            SwingConstants.CENTER);
    private final JLabel outcome = new JLabel("请扫描或输入实体书馆藏条码", SwingConstants.CENTER);
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
        form.setPreferredSize(new Dimension(680, 365));
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
        JLabel instruction = LibraryUiTheme.createMutedLabel(
                "线上图书馆负责查询和预约；拿到实体书后在这里登记借还");
        instruction.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(instruction, c);
        c.gridy = 5;
        LibraryUiTheme.styleStatusLabel(reservationCheck);
        form.add(reservationCheck, c);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
        center.add(form, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        LibraryUiTheme.styleStatusLabel(outcome);
        outcome.setFont(outcome.getFont().deriveFont(Font.BOLD, 13F));
        add(outcome, BorderLayout.SOUTH);

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
                ? "服务器正在重新校验借阅条件……"
                : "服务器正在重新校验归还条件……");
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
                        reservationCheck.setText("服务器最终校验未通过，正在刷新单册状态");
                        outcome.setText((borrowing ? "借书失败：" : "归还失败：")
                                + (response == null ? "服务器未返回结果"
                                : LibraryMessages.failure(response)));
                        return;
                    }
                    reservationCheck.setText(borrowing && result.ownedReservation()
                            ? "服务器校验通过：已领取本人预约保留的单册"
                            : "服务器最终校验通过");
                    outcome.setText(borrowing
                            ? "借书成功：" + scanned + "，借期 30 天"
                            : "归还成功：" + scanned + "，单册正在等待管理员上架");
                    barcode.setText("");
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
        inspectionVersion++;
        inspection = null;
        inspectionTimer.stop();
        updateButtons();
        if (barcode.getText().isBlank()) {
            if (!working) {
                reservationCheck.setText("单册预检：请输入馆藏条码");
                outcome.setText("请扫描或输入实体书馆藏条码");
            }
            return;
        }
        reservationCheck.setText("单册预检：等待检查条码……");
        outcome.setText("预检完成前不能执行借还操作");
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
        reservationCheck.setText("单册预检：正在查询条码……");
        if (updateOutcome) {
            outcome.setText("正在检查可执行的借还操作……");
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
                        reservationCheck.setText("单册预检失败：" + (response == null
                                ? "服务器未返回结果" : LibraryMessages.failure(response)));
                        if (updateOutcome) {
                            outcome.setText("当前不能执行借还操作");
                        }
                        return;
                    }
                    inspection = value;
                    reservationCheck.setText("单册预检：" + value.getBookTitle()
                            + "｜" + statusLabel(value.getCopyStatus())
                            + "｜" + value.getStatusMessage());
                    if (updateOutcome) {
                        outcome.setText(value.isBorrowAllowed() || value.isReturnAllowed()
                                ? "预检完成，请点击已启用的操作"
                                : "当前没有可执行的借还操作");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    reservationCheck.setText("单册预检被中断，请重新输入条码");
                } catch (ExecutionException exception) {
                    reservationCheck.setText("单册预检失败，请检查网络后重试");
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
