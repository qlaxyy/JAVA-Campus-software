package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
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
import java.util.concurrent.ExecutionException;

/** Minimal end-to-end page for searching the library catalog. */
public final class LibraryPanel extends JPanel {

    private static final String[] COLUMNS = {
        "图书编号", "ISBN", "书名", "作者", "分类", "可借/馆藏"
    };

    private final ClientContext context;
    private final JTextField keywordField = new JTextField();
    private final JButton searchButton = new JButton("搜索");
    private final JLabel statusLabel = new JLabel("输入书名、作者、ISBN 或分类进行搜索");
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    /**
     * Creates the library search page.
     *
     * @param context shared authenticated client context
     */
    public LibraryPanel(ClientContext context) {
        this.context = context;
        initializeView();
    }

    private void initializeView() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.add(new JLabel("检索关键词："), BorderLayout.WEST);
        searchPanel.add(keywordField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        JTable resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        keywordField.addActionListener(event -> search());
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
        if (!response.isSuccess()) {
            statusLabel.setText("搜索失败：" + response.getMessage());
            return;
        }
        if (!(response.getData() instanceof BookSearchResult result)) {
            statusLabel.setText("搜索失败：服务器返回的数据格式不正确");
            return;
        }

        for (BookDTO book : result.getBooks()) {
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
    }

    private void setWorking(boolean working) {
        keywordField.setEnabled(!working);
        searchButton.setEnabled(!working);
    }
}
