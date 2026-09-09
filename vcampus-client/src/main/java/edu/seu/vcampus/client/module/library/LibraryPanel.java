package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.BookCategoryDTO;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookLocationDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/** Search-only catalog page; circulation is handled by the self-service terminal. */
public final class LibraryPanel extends JPanel {

    private static final String[] COLUMNS = {
        "ISBN", "书名", "作者", "分类", "出版社 / 出版年", "语种", "馆藏地（可借/馆藏）"
    };

    private final ClientContext context;
    private final JTextField keywordField = new JTextField();
    private final JButton searchButton = new JButton("搜索");
    private final JComboBox<BookCategoryDTO> categoryFilter = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("输入关键词或选择分类；留空可查看全部开放书目");
    private final JTextArea holdingDetails = new JTextArea(4, 30);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(tableModel);
    private List<BookDTO> books = List.of();
    private boolean working;
    private String lastSearchKeyword;
    private String lastCategoryId;

    /** @param context shared authenticated client context */
    public LibraryPanel(ClientContext context) {
        this.context = context;
        initializeView();
        loadCategories();
    }

    private void initializeView() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.add(new JLabel("检索关键词："), BorderLayout.WEST);
        keywordField.setName("library.search.keyword");
        searchPanel.add(keywordField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        categoryFilter.addItem(null);
        categoryFilter.setName("library.categoryFilter");
        LibraryCategories.render(categoryFilter);
        JPanel searchArea = new JPanel(new BorderLayout(8, 8));
        searchArea.add(searchPanel, BorderLayout.CENTER);
        searchArea.add(categoryFilter, BorderLayout.EAST);

        resultTable.setName("library.searchResults");
        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.getTableHeader().setResizingAllowed(false);
        resultTable.getColumnModel().getColumn(6).setPreferredWidth(320);
        resultTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedHoldingDetails();
            }
        });

        holdingDetails.setName("library.holdingDetails");
        holdingDetails.setEditable(false);
        holdingDetails.setLineWrap(true);
        holdingDetails.setWrapStyleWord(true);
        holdingDetails.setText("请选择一条书目查看完整馆藏地和数量");
        JScrollPane holdingScroll = new JScrollPane(holdingDetails);
        holdingScroll.setBorder(BorderFactory.createTitledBorder("馆藏详情"));

        JPanel resultArea = new JPanel(new BorderLayout(8, 8));
        resultArea.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        resultArea.add(holdingScroll, BorderLayout.SOUTH);
        statusLabel.setName("library.searchStatus");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(searchArea, BorderLayout.NORTH);
        add(resultArea, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        keywordField.addActionListener(event -> search());
    }

    private void search() {
        BookCategoryDTO category = (BookCategoryDTO) categoryFilter.getSelectedItem();
        lastCategoryId = category == null ? null : category.getCategoryId();
        search(keywordField.getText().strip());
    }

    void refreshIfSearched() {
        if (lastSearchKeyword != null) {
            search(lastSearchKeyword);
        }
    }

    private void search(String keyword) {
        if (working) {
            return;
        }
        lastSearchKeyword = keyword;
        String categoryId = lastCategoryId;
        setWorking(true);
        clearResults();
        statusLabel.setText("正在搜索……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.SEARCH_BOOKS,
                        new BookSearchRequest(keyword, categoryId));
            }

            @Override
            protected void done() {
                try {
                    showResponse(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("搜索已中断");
                } catch (ExecutionException exception) {
                    statusLabel.setText("搜索失败：请检查网络后重试");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showResponse(Response response) {
        clearResults();
        if (response == null || !response.isSuccess()) {
            statusLabel.setText("搜索失败：" + (response == null
                    ? "服务器未返回结果" : LibraryMessages.failure(response)));
            return;
        }
        if (!(response.getData() instanceof BookSearchResult result)) {
            statusLabel.setText("搜索失败：服务器返回的数据格式不正确");
            return;
        }
        books = result.getBooks();
        for (BookDTO book : books) {
            tableModel.addRow(new Object[] {
                book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCategoryName(),
                publication(book), blankAsDash(book.getLanguage()), formatLocations(book)
            });
        }
        statusLabel.setText("找到 " + result.getBooks().size()
                + " 本书目；借还请返回模式选择，使用“模拟自助终端”扫描实体书条码");
    }

    private static String publication(BookDTO book) {
        String publisher = blankAsDash(book.getPublisher());
        return book.getPublicationYear() == null
                ? publisher : publisher + " / " + book.getPublicationYear();
    }

    private static String formatLocations(BookDTO book) {
        if (book.getLocations().isEmpty()) {
            return "暂无馆藏";
        }
        return book.getLocations().stream().map(LibraryPanel::formatLocation)
                .collect(Collectors.joining("；"));
    }

    private static String formatLocation(BookLocationDTO location) {
        return location.getLocation() + " " + location.getAvailableCount()
                + "/" + location.getTotalCount();
    }

    private void showSelectedHoldingDetails() {
        int viewRow = resultTable.getSelectedRow();
        if (viewRow < 0) {
            holdingDetails.setText("请选择一条书目查看完整馆藏地和数量");
            return;
        }
        int modelRow = resultTable.convertRowIndexToModel(viewRow);
        if (modelRow >= books.size()) {
            holdingDetails.setText("暂时无法显示馆藏详情，请重新搜索");
            return;
        }
        BookDTO book = books.get(modelRow);
        String locations = book.getLocations().isEmpty()
                ? "暂无馆藏"
                : book.getLocations().stream()
                        .map(location -> location.getLocation() + "：可借 "
                                + location.getAvailableCount() + " 本 / 馆藏 "
                                + location.getTotalCount() + " 本")
                        .collect(Collectors.joining(System.lineSeparator()));
        holdingDetails.setText("《" + book.getTitle() + "》" + System.lineSeparator() + locations);
        holdingDetails.setCaretPosition(0);
    }

    private static String blankAsDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void clearResults() {
        books = List.of();
        tableModel.setRowCount(0);
        holdingDetails.setText("请选择一条书目查看完整馆藏地和数量");
    }

    private void setWorking(boolean value) {
        working = value;
        keywordField.setEnabled(!value);
        categoryFilter.setEnabled(!value);
        searchButton.setEnabled(!value);
        resultTable.setEnabled(!value);
    }

    private void loadCategories() {
        if (context.currentSession().isEmpty()) {
            return;
        }
        new SwingWorker<List<BookCategoryDTO>, Void>() {
            @Override
            protected List<BookCategoryDTO> doInBackground() throws Exception {
                return LibraryCategories.read(context.send(LibraryActions.LIST_CATEGORIES, null));
            }

            @Override
            protected void done() {
                try {
                    for (BookCategoryDTO category : get()) {
                        categoryFilter.addItem(category);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    categoryFilter.setToolTipText("分类加载失败，可继续关键词搜索");
                }
            }
        }.execute();
    }
}
