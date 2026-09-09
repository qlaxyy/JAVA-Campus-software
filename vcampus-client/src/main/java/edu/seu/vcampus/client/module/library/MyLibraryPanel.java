package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.common.library.ReservationIdRequest;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/** Displays the authenticated user's borrows and title reservations. */
public final class MyLibraryPanel extends JPanel {

    private static final String[] BORROW_COLUMNS = {
        "书名", "馆藏条码", "借阅时间", "到期时间", "归还时间", "状态", "是否逾期"
    };
    private static final String[] RESERVATION_COLUMNS = {
        "书名", "取书馆藏地", "预约状态", "排队位次", "馆藏条码", "取书截止时间"
    };
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClientContext context;
    private final Runnable reservationChanged;
    private final DefaultTableModel currentModel = readOnlyModel(BORROW_COLUMNS);
    private final DefaultTableModel historyModel = readOnlyModel(BORROW_COLUMNS);
    private final DefaultTableModel reservationModel = readOnlyModel(RESERVATION_COLUMNS);
    private final JTable currentTable = createTable(currentModel, "library.currentBorrows");
    private final JTable historyTable = createTable(historyModel, "library.borrowHistory");
    private final JTable reservationTable = createTable(
            reservationModel, "library.myReservations");
    private final JButton refreshButton = new JButton("刷新全部记录");
    private final JButton cancelReservation = new JButton("取消选中预约");
    private final JLabel reservationHint = new JLabel("只有“排队中”和“待取书”预约可以取消");
    private final JLabel statusLabel = new JLabel("打开此页后加载本人的借阅与预约记录");
    private List<ReservationDTO> reservations = List.of();
    private boolean working;

    /** @param context shared authenticated client context */
    public MyLibraryPanel(ClientContext context) {
        this(context, () -> { });
    }

    MyLibraryPanel(ClientContext context, Runnable reservationChanged) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.reservationChanged = Objects.requireNonNull(
                reservationChanged, "reservationChanged must not be null");
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        statusLabel.setName("library.recordsStatus");
        cancelReservation.setName("library.reservation.cancel");
        reservationHint.setName("library.reservation.cancelHint");

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.add(statusLabel, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);
        JTabbedPane records = new JTabbedPane();
        records.setName("library.recordTabs");
        records.addTab("当前借阅", new JScrollPane(currentTable));
        records.addTab("历史借阅", new JScrollPane(historyTable));
        records.addTab("我的预约", createReservationArea());
        add(header, BorderLayout.NORTH);
        add(records, BorderLayout.CENTER);
        add(new JLabel("借还实体书请返回模式选择，进入“模拟自助终端”"), BorderLayout.SOUTH);

        refreshButton.addActionListener(event -> refresh());
        cancelReservation.addActionListener(event -> cancelSelectedReservation());
        reservationTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateCancelButton();
            }
        });
        updateCancelButton();
    }

    private JPanel createReservationArea() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(reservationTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.add(reservationHint, BorderLayout.CENTER);
        actions.add(cancelReservation, BorderLayout.EAST);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    void refresh() {
        if (working) {
            return;
        }
        setWorking(true);
        clearRecords();
        statusLabel.setText("正在加载借阅与预约记录……");
        new SwingWorker<LibraryResponses, Void>() {
            @Override
            protected LibraryResponses doInBackground() throws Exception {
                return new LibraryResponses(
                        context.send(LibraryActions.GET_BORROW_RECORDS, null),
                        context.send(LibraryActions.GET_MY_RESERVATIONS, null));
            }

            @Override
            protected void done() {
                try {
                    showRecords(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("记录加载已中断，请重新刷新");
                } catch (ExecutionException exception) {
                    statusLabel.setText("记录加载失败，请检查网络后重新刷新");
                } catch (IllegalArgumentException exception) {
                    statusLabel.setText("记录加载失败：" + exception.getMessage());
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showRecords(LibraryResponses responses) {
        List<BorrowRecordDTO> borrows = readList(
                responses.borrows(), BorrowRecordDTO.class, "借阅记录");
        List<ReservationDTO> loadedReservations = readList(
                responses.reservations(), ReservationDTO.class, "预约记录");
        if (borrows.stream().anyMatch(record -> !("BORROWED".equals(record.getStatus())
                || "RETURNED".equals(record.getStatus())))) {
            throw new IllegalArgumentException("服务器返回的借阅状态不正确");
        }
        if (loadedReservations.stream().anyMatch(
                reservation -> !knownReservationStatus(reservation.getStatus()))) {
            throw new IllegalArgumentException("服务器返回的预约状态不正确");
        }

        List<BorrowRecordDTO> current = borrows.stream()
                .filter(record -> "BORROWED".equals(record.getStatus())).toList();
        List<BorrowRecordDTO> history = borrows.stream()
                .filter(record -> "RETURNED".equals(record.getStatus())).toList();
        current.forEach(record -> currentModel.addRow(borrowRow(record)));
        history.forEach(record -> historyModel.addRow(borrowRow(record)));
        reservations = List.copyOf(loadedReservations);
        reservations.forEach(reservation -> reservationModel.addRow(reservationRow(reservation)));

        long overdue = current.stream().filter(BorrowRecordDTO::isOverdue).count();
        long activeReservations = reservations.stream()
                .filter(reservation -> "WAITING".equals(reservation.getStatus())
                        || "READY_FOR_PICKUP".equals(reservation.getStatus()))
                .count();
        statusLabel.setText("当前借阅 " + current.size() + " 本，历史 " + history.size()
                + " 条，逾期 " + overdue + " 本，有效预约 " + activeReservations + " 条");
    }

    private void cancelSelectedReservation() {
        ReservationDTO reservation = selectedReservation();
        if (working || reservation == null || !isCancellable(reservation)) {
            return;
        }
        setWorking(true);
        statusLabel.setText("正在取消《" + reservation.getBookTitle() + "》的预约……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.CANCEL_RESERVATION,
                        new ReservationIdRequest(reservation.getReservationId()));
            }

            @Override
            protected void done() {
                boolean reload = false;
                try {
                    Response response = get();
                    if (response == null || !response.isSuccess()) {
                        statusLabel.setText("取消预约失败：" + (response == null
                                ? "服务器未返回结果" : LibraryMessages.failure(response)));
                        return;
                    }
                    if (!(response.getData() instanceof ReservationDTO)) {
                        statusLabel.setText("取消结果未确认，请刷新预约记录后核对，勿重复提交");
                        return;
                    }
                    statusLabel.setText("预约已取消，正在刷新记录……");
                    reservationChanged.run();
                    reload = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("取消结果未确认，请刷新预约记录后核对，勿重复提交");
                } catch (ExecutionException exception) {
                    statusLabel.setText("取消结果未确认，请刷新预约记录后核对，勿重复提交");
                } finally {
                    setWorking(false);
                    if (reload) {
                        SwingUtilities.invokeLater(MyLibraryPanel.this::refresh);
                    }
                }
            }
        }.execute();
    }

    private ReservationDTO selectedReservation() {
        int viewRow = reservationTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = reservationTable.convertRowIndexToModel(viewRow);
        return modelRow >= reservations.size() ? null : reservations.get(modelRow);
    }

    private void clearRecords() {
        currentModel.setRowCount(0);
        historyModel.setRowCount(0);
        reservationModel.setRowCount(0);
        reservations = List.of();
        updateCancelButton();
    }

    private void setWorking(boolean value) {
        working = value;
        refreshButton.setEnabled(!value);
        currentTable.setEnabled(!value);
        historyTable.setEnabled(!value);
        reservationTable.setEnabled(!value);
        updateCancelButton();
    }

    private void updateCancelButton() {
        ReservationDTO selected = selectedReservation();
        cancelReservation.setEnabled(!working && selected != null && isCancellable(selected));
        if (selected == null) {
            reservationHint.setText("选择“排队中”或“待取书”的预约后可以取消");
        } else if (isCancellable(selected)) {
            reservationHint.setText("取消后会释放已保留单册，并自动顺延给下一位读者");
        } else {
            reservationHint.setText("该预约已经结束，不能取消");
        }
    }

    private static boolean isCancellable(ReservationDTO reservation) {
        return "WAITING".equals(reservation.getStatus())
                || "READY_FOR_PICKUP".equals(reservation.getStatus());
    }

    private static boolean knownReservationStatus(String status) {
        return switch (status) {
            case "WAITING", "READY_FOR_PICKUP", "FULFILLED", "CANCELED", "EXPIRED" -> true;
            default -> false;
        };
    }

    private static Object[] borrowRow(BorrowRecordDTO record) {
        return new Object[] {record.getBookTitle(), record.getBarcode(),
                format(record.getBorrowTime()), format(record.getDueTime()),
                format(record.getReturnTime()),
                "BORROWED".equals(record.getStatus()) ? "借阅中" : "已归还",
                record.isOverdue() ? "已逾期，请尽快归还" : "否"};
    }

    private static Object[] reservationRow(ReservationDTO reservation) {
        return new Object[] {reservation.getBookTitle(), reservation.getPickupLocation(),
                displayReservationStatus(reservation.getStatus()),
                reservation.getQueuePosition() == null
                        ? "—" : "第 " + reservation.getQueuePosition() + " 位",
                blankAsDash(reservation.getAssignedBarcode()),
                format(reservation.getExpiresAt())};
    }

    private static String displayReservationStatus(String status) {
        return switch (status) {
            case "WAITING" -> "排队中";
            case "READY_FOR_PICKUP" -> "待取书";
            case "FULFILLED" -> "已借阅";
            case "CANCELED" -> "已取消";
            case "EXPIRED" -> "已过期";
            default -> status;
        };
    }

    private static String blankAsDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String format(LocalDateTime time) {
        return time == null ? "—" : TIME_FORMAT.format(time);
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable createTable(DefaultTableModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.setRowHeight(26);
        return table;
    }

    private static <T> List<T> readList(Response response, Class<T> type, String label) {
        if (response == null) {
            throw new IllegalArgumentException("服务器未返回" + label);
        }
        if (!response.isSuccess()) {
            throw new IllegalArgumentException(LibraryMessages.failure(response));
        }
        if (!(response.getData() instanceof List<?> values)
                || values.stream().anyMatch(value -> !type.isInstance(value))) {
            throw new IllegalArgumentException("服务器返回的" + label + "格式不正确");
        }
        List<T> result = new ArrayList<>(values.size());
        values.forEach(value -> result.add(type.cast(value)));
        return result;
    }

    private record LibraryResponses(Response borrows, Response reservations) { }
}
