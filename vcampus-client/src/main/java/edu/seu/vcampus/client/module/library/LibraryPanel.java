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
import javax.swing.ListSelectionModel;
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
    private final JLabel operationLabel = new JLabel("借还操作用于模拟柜台或自助终端登记");
    private final JTable resultTable;
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private List<BookDTO> displayedBooks = List.of();
    private boolean working;
    private String lastSearchKeyword;

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
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.getTableHeader().setResizingAllowed(false);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        borrowButton.setEnabled(false);
        statusLabel.setName("library.searchStatus");
        operationLabel.setName("library.borrowOutcome");

        JPanel actionPanel = new JPanel(new BorderLayout(12, 0));
        actionPanel.add(statusLabel, BorderLayout.CENTER);
        actionPanel.add(borrowButton, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(operationLabel, BorderLayout.NORTH);
        footer.add(actionPanel, BorderLayout.SOUTH);

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        keywordField.addActionListener(event -> search());
        borrowButton.addActionListener(event -> borrowSelectedBook());
        resultTable.getSelectionModel().addListSelectionListener(
                event -> updateBorrowButtonState());
    }

    private void search() {
        search(keywordField.getText().trim(), false);
    }

    void refreshIfSearched() {
        if (lastSearchKeyword != null) {
            search(lastSearchKeyword, false);
        }
    }

    private void search(String keyword, boolean afterBorrow) {
        if (working) {
            return;
        }
        if (keyword.isEmpty()) {
            statusLabel.setText("请输入搜索关键词");
            keywordField.requestFocusInWindow();
            return;
        }

        lastSearchKeyword = keyword;
        String failurePrefix = afterBorrow ? "借阅成功，但库存刷新失败：" : "搜索失败：";

        setWorking(true);
        clearResults();
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
                    showResponse(get(), failurePrefix);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText(failurePrefix + "请求已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText(failurePrefix + "请检查网络后重试查询");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showResponse(Response response, String failurePrefix) {
        clearResults();
        if (!response.isSuccess()) {
            statusLabel.setText(failurePrefix + LibraryMessages.failure(response));
            return;
        }
        if (!(response.getData() instanceof BookSearchResult result)) {
            statusLabel.setText(failurePrefix + "服务器返回的数据格式不正确");
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
        if (working) {
            return;
        }
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
        operationLabel.setText("正在提交借阅《" + selectedBook.getTitle() + "》");
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
                        operationLabel.setText("借阅失败：" + LibraryMessages.failure(response));
                        statusLabel.setText("可刷新查询或查看我的借阅");
                        setWorking(false);
                        return;
                    }
                    operationLabel.setText("借阅成功：《" + selectedBook.getTitle()
                            + "》。到期时间请查看“我的借阅”。");
                    setWorking(false);
                    search(lastSearchKeyword, true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    operationLabel.setText("借阅结果未确认，请到“我的借阅”核对后再操作");
                    clearResults();
                    statusLabel.setText("请求已中断");
                    setWorking(false);
                } catch (ExecutionException exception) {
                    operationLabel.setText("借阅结果未确认，请到“我的借阅”核对后再操作");
                    clearResults();
                    statusLabel.setText("请检查网络连接");
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

    private void clearResults() {
        tableModel.setRowCount(0);
        displayedBooks = List.of();
    }

    private void setWorking(boolean working) {
        this.working = working;
        keywordField.setEnabled(!working);
        searchButton.setEnabled(!working);
        resultTable.setEnabled(!working);
        updateBorrowButtonState();
    }
}
