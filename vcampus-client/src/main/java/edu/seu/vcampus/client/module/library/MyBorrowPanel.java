package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.BookReturnRequest;
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

/** Displays the authenticated user's active/history records and submits single-record returns. */
public final class MyBorrowPanel extends JPanel {

    private static final String[] COLUMNS = {
        "书名", "借阅时间", "到期时间", "归还时间", "状态", "是否逾期"
    };
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClientContext context;
    private final DefaultTableModel currentModel = createModel();
    private final DefaultTableModel historyModel = createModel();
    private final JTable currentTable = createTable(currentModel, "library.currentBorrows");
    private final JTable historyTable = createTable(historyModel, "library.borrowHistory");
    private final JTabbedPane recordTabs = new JTabbedPane();
    private final JButton refreshButton = new JButton("刷新借阅记录");
    private final JButton returnButton = new JButton("归还选中图书");
    private final JLabel statusLabel = new JLabel("打开此页后加载本人的借阅记录");
    private final JLabel operationLabel = new JLabel("请选择当前借阅中的一本图书进行归还");
    private List<BorrowRecordDTO> currentRecords = List.of();
    private boolean working;

    /** @param context shared authenticated client context */
    public MyBorrowPanel(ClientContext context) {
        this.context = context;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        statusLabel.setName("library.recordsStatus");
        operationLabel.setName("library.returnOutcome");
        recordTabs.setName("library.recordTabs");

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.add(statusLabel, BorderLayout.CENTER);
        header.add(refreshButton, BorderLayout.EAST);
        recordTabs.addTab("当前借阅", new JScrollPane(currentTable));
        recordTabs.addTab("历史借阅", new JScrollPane(historyTable));
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.add(operationLabel, BorderLayout.CENTER);
        footer.add(returnButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(recordTabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        refreshButton.addActionListener(event -> refresh());
        returnButton.addActionListener(event -> returnSelectedBook());
        currentTable.getSelectionModel().addListSelectionListener(event -> updateReturnButton());
        recordTabs.addChangeListener(event -> updateReturnButton());
        updateReturnButton();
    }

    void refresh() {
        refresh(false);
    }

    private void refresh(boolean afterReturn) {
        if (working) {
            return;
        }
        setWorking(true);
        clearRecords();
        statusLabel.setText("正在加载借阅记录……");
        String failurePrefix = afterReturn ? "归还成功，但记录刷新失败：" : "记录加载失败：";
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.GET_BORROW_RECORDS, null);
            }

            @Override
            protected void done() {
                try {
                    showRecords(get(), failurePrefix);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(failurePrefix + "请求已中断，请重新刷新");
                } catch (ExecutionException exception) {
                    statusLabel.setText(failurePrefix + "请检查网络后重新刷新");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showRecords(Response response, String failurePrefix) {
        if (!response.isSuccess()) {
            statusLabel.setText(failurePrefix + LibraryMessages.failure(response));
            return;
        }
        if (!(response.getData() instanceof List<?> values)) {
            statusLabel.setText(failurePrefix + "服务器返回的数据格式不正确");
            return;
        }
        List<BorrowRecordDTO> records = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof BorrowRecordDTO record)
                    || !("BORROWED".equals(record.getStatus())
                    || "RETURNED".equals(record.getStatus()))) {
                statusLabel.setText(failurePrefix + "服务器返回的数据格式不正确");
                return;
            }
            records.add(record);
        }
        currentRecords = records.stream()
                .filter(record -> "BORROWED".equals(record.getStatus())).toList();
        List<BorrowRecordDTO> history = records.stream()
                .filter(record -> "RETURNED".equals(record.getStatus())).toList();
        currentRecords.forEach(record -> currentModel.addRow(row(record)));
        history.forEach(record -> historyModel.addRow(row(record)));
        long overdueCount = currentRecords.stream().filter(BorrowRecordDTO::isOverdue).count();
        statusLabel.setText("当前借阅 " + currentRecords.size() + " 本，历史 " + history.size()
                + " 条，逾期未还 " + overdueCount + " 本");
    }

    private void returnSelectedBook() {
        if (working || recordTabs.getSelectedIndex() != 0 || currentTable.getSelectedRow() < 0) {
            return;
        }
        int row = currentTable.convertRowIndexToModel(currentTable.getSelectedRow());
        if (row >= currentRecords.size()) {
            return;
        }
        BorrowRecordDTO record = currentRecords.get(row);
        setWorking(true);
        operationLabel.setText("正在提交归还《" + record.getBookTitle() + "》……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.RETURN_BOOK,
                        new BookReturnRequest(record.getRecordId()));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        operationLabel.setText("归还失败：" + LibraryMessages.failure(response));
                        setWorking(false);
                        refresh();
                        return;
                    }
                    operationLabel.setText("归还成功：《" + record.getBookTitle()
                            + "》。记录保留在“历史借阅”中。");
                    setWorking(false);
                    refresh(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showUncertainReturn();
                } catch (ExecutionException exception) {
                    showUncertainReturn();
                }
            }
        }.execute();
    }

    private void showUncertainReturn() {
        operationLabel.setText("归还结果未确认，请刷新借阅记录核对后再操作");
        clearRecords();
        statusLabel.setText("请检查网络连接后点击“刷新借阅记录”");
        setWorking(false);
    }

    private void clearRecords() {
        currentModel.setRowCount(0);
        historyModel.setRowCount(0);
        currentRecords = List.of();
    }

    private void setWorking(boolean value) {
        working = value;
        refreshButton.setEnabled(!value);
        currentTable.setEnabled(!value);
        historyTable.setEnabled(!value);
        updateReturnButton();
    }

    private void updateReturnButton() {
        returnButton.setEnabled(!working && recordTabs.getSelectedIndex() == 0
                && currentTable.getSelectedRow() >= 0);
    }

    private static Object[] row(BorrowRecordDTO record) {
        return new Object[] {record.getBookTitle(), format(record.getBorrowTime()),
                format(record.getDueTime()), format(record.getReturnTime()),
                "BORROWED".equals(record.getStatus()) ? "借阅中" : "已归还",
                record.isOverdue() ? "已逾期，请尽快归还" : "否"};
    }

    private static String format(LocalDateTime time) {
        return time == null ? "—" : TIME_FORMAT.format(time);
    }

    private static DefaultTableModel createModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
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
