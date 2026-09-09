package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.CopyBorrowRequest;
import edu.seu.vcampus.common.library.CopyReturnRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.common.protocol.ErrorCodes;
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
import java.util.List;
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
    private boolean working;

    /** @param context shared authenticated client context */
    public SelfServicePanel(ClientContext context) {
        this(context, () -> { });
    }

    SelfServicePanel(ClientContext context, Runnable circulationChanged) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.circulationChanged = Objects.requireNonNull(
                circulationChanged, "circulationChanged must not be null");
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(36, 80, 36, 80));
        setName("library.selfService");
        barcode.setName("library.selfService.barcode");
        reservationCheck.setName("library.selfService.reservationCheck");
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
        form.add(new JLabel("提示：线上图书馆负责查询和预约；拿到实体书后在这里登记借还",
                SwingConstants.CENTER), c);
        c.gridy = 3;
        form.add(reservationCheck, c);
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
        reservationCheck.setText(borrowing
                ? "预约校验：正在核验单册保留归属……"
                : "预约校验：归还操作不需要预约归属");
        outcome.setText(borrowing ? "正在登记借书……" : "正在登记归还……");
        new SwingWorker<TerminalResult, Void>() {
            @Override
            protected TerminalResult doInBackground() throws Exception {
                if (!borrowing) {
                    return new TerminalResult(context.send(LibraryActions.RETURN_COPY,
                            new CopyReturnRequest(scanned)), false, false);
                }
                ReservationCheck check = inspectOwnReservations(scanned);
                Response response = context.send(LibraryActions.BORROW_COPY,
                        new CopyBorrowRequest(scanned));
                return new TerminalResult(response, check.completed(), check.owned());
            }

            @Override
            protected void done() {
                try {
                    TerminalResult result = get();
                    Response response = result.response();
                    showReservationCheck(result, borrowing);
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
                    circulationChanged.run();
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

    private ReservationCheck inspectOwnReservations(String scanned) {
        try {
            Response response = context.send(LibraryActions.GET_MY_RESERVATIONS, null);
            if (response == null || !response.isSuccess()
                    || !(response.getData() instanceof List<?> values)
                    || values.stream().anyMatch(value -> !(value instanceof ReservationDTO))) {
                return new ReservationCheck(false, false);
            }
            boolean owned = values.stream().map(ReservationDTO.class::cast)
                    .anyMatch(reservation -> "READY_FOR_PICKUP".equals(reservation.getStatus())
                            && scanned.equals(reservation.getAssignedBarcode()));
            return new ReservationCheck(true, owned);
        } catch (Exception exception) {
            return new ReservationCheck(false, false);
        }
    }

    private void showReservationCheck(TerminalResult result, boolean borrowing) {
        if (!borrowing) {
            reservationCheck.setText("预约校验：归还操作不需要预约归属");
            return;
        }
        Response response = result.response();
        if (response != null && response.isSuccess()) {
            reservationCheck.setText(result.ownedReservation()
                    ? "预约校验：通过，这是为当前用户保留的单册"
                    : "预约校验：通过，该单册可由当前用户借阅");
        } else if (response != null
                && ErrorCodes.LIBRARY_COPY_RESERVED_FOR_OTHER.equals(response.getCode())) {
            reservationCheck.setText("预约校验：未通过，该单册已为其他读者保留");
        } else if (result.ownedReservation()) {
            reservationCheck.setText("预约校验：客户端预检匹配，但服务器最终校验未通过");
        } else if (!result.checkCompleted()) {
            reservationCheck.setText("预约校验：客户端预检失败，已由服务器完成最终校验");
        } else {
            reservationCheck.setText("预约校验：单册不属于当前用户的待取预约");
        }
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

    private record ReservationCheck(boolean completed, boolean owned) { }

    private record TerminalResult(
            Response response, boolean checkCompleted, boolean ownedReservation) { }
}
