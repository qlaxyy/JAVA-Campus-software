package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.BookBorrowRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Minimal end-to-end page for searching the library catalog. */
public final class LibraryPanel extends JPanel {

    private static final String[] COLUMNS = {
        "图书编号", "ISBN", "书名", "作者", "分类", "可借/馆藏"
    };

    private final ClientContext context;
    private final JTextField keywordField = new JTextField();
    private final JButton searchButton = new JButton("搜索");
    private final JButton borrowButton = new JButton("借阅选中图书");
    private final JLabel statusLabel = new JLabel("输入书名、作者、ISBN 或分类进行搜索");
    private final JTable resultTable;
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private List<BookDTO> displayedBooks = List.of();
    private boolean working;

    /**
     * Creates the library search page.
     *
     * @param context shared authenticated client context
     */
    public LibraryPanel(ClientContext context) {
        this.context = context;
        this.resultTable = new JTable(tableModel);
        initializeView();
    }

    private void initializeView() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.add(new JLabel("检索关键词："), BorderLayout.WEST);
        searchPanel.add(keywordField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        borrowButton.setEnabled(false);

        JPanel actionPanel = new JPanel(new BorderLayout(12, 0));
        actionPanel.add(statusLabel, BorderLayout.CENTER);
        actionPanel.add(borrowButton, BorderLayout.EAST);

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        keywordField.addActionListener(event -> search());
        borrowButton.addActionListener(event -> borrowSelectedBook());
        resultTable.getSelectionModel().addListSelectionListener(
                event -> updateBorrowButtonState());
    }

    private void search() {
        String keyword = keywordField.getText().trim();
        if (keyword.isEmpty()) {
            statusLabel.setText("请输入搜索关键词");
            keywordField.requestFocusInWindow();
            return;
        }

        setWorking(true);
        statusLabel.setText("正在搜索……");

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        LibraryActions.SEARCH_BOOKS,
                        new BookSearchRequest(keyword));
            }

            @Override
            protected void done() {
                try {
                    showResponse(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("搜索已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("搜索失败，请确认服务器已经启动");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showResponse(Response response) {
        tableModel.setRowCount(0);
        displayedBooks = List.of();
        if (!response.isSuccess()) {
            statusLabel.setText("搜索失败：" + response.getMessage());
            return;
        }
        if (!(response.getData() instanceof BookSearchResult result)) {
            statusLabel.setText("搜索失败：服务器返回的数据格式不正确");
            return;
        }

        displayedBooks = result.getBooks();
        for (BookDTO book : displayedBooks) {
            tableModel.addRow(new Object[] {
                book.getBookId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getAvailableCount() + "/" + book.getTotalCount()
            });
        }
        statusLabel.setText("找到 " + result.getBooks().size() + " 本图书");
        updateBorrowButtonState();
    }

    private void borrowSelectedBook() {
        int selectedViewRow = resultTable.getSelectedRow();
        if (selectedViewRow < 0) {
            statusLabel.setText("请先选择一本图书");
            return;
        }
        int selectedModelRow = resultTable.convertRowIndexToModel(selectedViewRow);
        BookDTO selectedBook = displayedBooks.get(selectedModelRow);
        if (selectedBook.getAvailableCount() <= 0) {
            statusLabel.setText("暂无可借库存");
            return;
        }

        setWorking(true);
        statusLabel.setText("正在借阅《" + selectedBook.getTitle() + "》……");

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        LibraryActions.BORROW_BOOK,
                        new BookBorrowRequest(selectedBook.getBookId()));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        statusLabel.setText("借阅失败：" + response.getMessage());
                        setWorking(false);
                        return;
                    }
                    statusLabel.setText("借阅成功，正在刷新库存……");
                    setWorking(false);
                    search();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("借阅已中断");
                    setWorking(false);
                } catch (ExecutionException exception) {
                    statusLabel.setText("借阅失败，请确认服务器已经启动");
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void updateBorrowButtonState() {
        int selectedViewRow = resultTable.getSelectedRow();
        if (working || selectedViewRow < 0) {
            borrowButton.setEnabled(false);
            return;
        }
        int selectedModelRow = resultTable.convertRowIndexToModel(selectedViewRow);
        borrowButton.setEnabled(
                selectedModelRow < displayedBooks.size()
                        && displayedBooks.get(selectedModelRow).getAvailableCount() > 0);
    }

    private void setWorking(boolean working) {
        this.working = working;
        keywordField.setEnabled(!working);
        searchButton.setEnabled(!working);
        updateBorrowButtonState();
    }
}
