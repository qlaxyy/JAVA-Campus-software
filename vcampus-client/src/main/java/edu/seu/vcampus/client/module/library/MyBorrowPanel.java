package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.BorrowRecordDTO;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Displays the authenticated user's current and historical circulation records. */
public final class MyBorrowPanel extends JPanel {

    private static final String[] COLUMNS = {
        "书名", "馆藏条码", "借阅时间", "到期时间", "归还时间", "状态", "是否逾期"
    };
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClientContext context;
    private final DefaultTableModel currentModel = createModel();
    private final DefaultTableModel historyModel = createModel();
    private final JTable currentTable = createTable(currentModel, "library.currentBorrows");
    private final JTable historyTable = createTable(historyModel, "library.borrowHistory");
    private final JButton refreshButton = new JButton("刷新借阅记录");
    private final JLabel statusLabel = new JLabel("打开此页后加载本人的借阅记录");
    private boolean working;

    /** @param context shared authenticated client context */
    public MyBorrowPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        statusLabel.setName("library.recordsStatus");

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.add(statusLabel, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);
        JTabbedPane records = new JTabbedPane();
        records.setName("library.recordTabs");
        records.addTab("当前借阅", new JScrollPane(currentTable));
        records.addTab("历史借阅", new JScrollPane(historyTable));
        add(header, BorderLayout.NORTH);
        add(records, BorderLayout.CENTER);
        add(new JLabel("归还请到“自助借还”页扫描实体书条码"), BorderLayout.SOUTH);

        refreshButton.addActionListener(event -> refresh());
    }

    void refresh() {
        if (working) {
            return;
        }
        setWorking(true);
        clearRecords();
        statusLabel.setText("正在加载借阅记录……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.GET_BORROW_RECORDS, null);
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
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showRecords(Response response) {
        if (response == null || !response.isSuccess()) {
            statusLabel.setText("记录加载失败：" + (response == null
                    ? "服务器未返回结果" : LibraryMessages.failure(response)));
            return;
        }
        if (!(response.getData() instanceof List<?> values)) {
            statusLabel.setText("记录加载失败：服务器返回的数据格式不正确");
            return;
        }
        List<BorrowRecordDTO> records = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof BorrowRecordDTO record)
                    || !("BORROWED".equals(record.getStatus())
                    || "RETURNED".equals(record.getStatus()))) {
                statusLabel.setText("记录加载失败：服务器返回的数据格式不正确");
                return;
            }
            records.add(record);
        }
        List<BorrowRecordDTO> current = records.stream()
                .filter(record -> "BORROWED".equals(record.getStatus())).toList();
        List<BorrowRecordDTO> history = records.stream()
                .filter(record -> "RETURNED".equals(record.getStatus())).toList();
        current.forEach(record -> currentModel.addRow(row(record)));
        history.forEach(record -> historyModel.addRow(row(record)));
        long overdue = current.stream().filter(BorrowRecordDTO::isOverdue).count();
        statusLabel.setText("当前借阅 " + current.size() + " 本，历史 " + history.size()
                + " 条，逾期未还 " + overdue + " 本");
    }

    private void clearRecords() {
        currentModel.setRowCount(0);
        historyModel.setRowCount(0);
    }

    private void setWorking(boolean value) {
        working = value;
        refreshButton.setEnabled(!value);
        currentTable.setEnabled(!value);
        historyTable.setEnabled(!value);
    }

    private static Object[] row(BorrowRecordDTO record) {
        return new Object[] {record.getBookTitle(), record.getBarcode(),
                format(record.getBorrowTime()), format(record.getDueTime()),
                format(record.getReturnTime()),
                "BORROWED".equals(record.getStatus()) ? "借阅中" : "已归还",
                record.isOverdue() ? "已逾期，请尽快归还" : "否"};
    }

    private static String format(LocalDateTime time) {
        return time == null ? "—" : TIME_FORMAT.format(time);
    }

    private static DefaultTableModel createModel() {
        return new DefaultTableModel(COLUMNS, 0) {
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
}
