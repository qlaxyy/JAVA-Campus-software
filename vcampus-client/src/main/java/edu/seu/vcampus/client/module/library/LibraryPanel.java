package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.BookCategoryDTO;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookLocationDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.CreateReservationRequest;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.ReservationDTO;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
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
import java.awt.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/** Online catalog page for searching titles and creating pickup reservations. */
public final class LibraryPanel extends JPanel {

    private static final String[] COLUMNS = {
        "ISBN", "书名", "作者", "分类", "出版社 / 出版年", "语种", "馆藏", "可借"
    };

    private final ClientContext context;
    private final JTextField keywordField = new JTextField();
    private final JButton searchButton = new JButton("搜索");
    private final JComboBox<BookCategoryDTO> categoryFilter = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("输入关键词或选择分类；留空可查看全部开放书目");
    private final JTextArea holdingDetails = new JTextArea(4, 30);
    private final JComboBox<BookLocationDTO> reservationLocation = new JComboBox<>();
    private final JButton reservationButton = new JButton("预约");
    private final JLabel reservationHint = new JLabel("请先选择书目和取书馆藏地");
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
    private String locallyReservedBookId;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** @param context shared authenticated client context */
    public LibraryPanel(ClientContext context) {
        this.context = context;
        initializeView();
        loadCategories();
    }

    private void initializeView() {
        setLayout(new BorderLayout(12, 12));
        LibraryUiTheme.installPage(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        keywordField.setName("library.search.keyword");
        LibraryUiTheme.styleTextField(keywordField);
        LibraryUiTheme.stylePrimaryButton(searchButton);
        searchButton.setPreferredSize(new java.awt.Dimension(92, 38));
        categoryFilter.addItem(null);
        categoryFilter.setName("library.categoryFilter");
        LibraryCategories.render(categoryFilter);
        LibraryUiTheme.styleComboBox(categoryFilter);
        categoryFilter.setPreferredSize(new java.awt.Dimension(190, 38));

        JPanel searchCard = new JPanel(new BorderLayout(12, 0));
        LibraryUiTheme.styleCard(searchCard);
        searchCard.setBorder(LibraryUiTheme.cardBorder(7, 10));
        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.setOpaque(false);
        JLabel keywordLabel = new JLabel("检索关键词");
        keywordLabel.setForeground(LibraryUiTheme.TEXT);
        searchPanel.add(keywordLabel, BorderLayout.WEST);
        searchPanel.add(keywordField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        JPanel searchArea = new JPanel(new BorderLayout(12, 0));
        searchArea.setOpaque(false);
        searchArea.add(searchPanel, BorderLayout.CENTER);
        JPanel categoryArea = new JPanel(new BorderLayout(8, 0));
        categoryArea.setOpaque(false);
        JLabel categoryLabel = new JLabel("分类");
        categoryLabel.setForeground(LibraryUiTheme.TEXT);
        categoryArea.add(categoryLabel, BorderLayout.WEST);
        categoryArea.add(categoryFilter, BorderLayout.CENTER);
        searchArea.add(categoryArea, BorderLayout.EAST);
        searchCard.add(searchArea, BorderLayout.CENTER);

        resultTable.setName("library.searchResults");
        resultTable.setFillsViewportHeight(true);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.getTableHeader().setResizingAllowed(false);
        LibraryUiTheme.styleTable(resultTable);
        LibraryUiTheme.setColumnWidths(resultTable,
                125, 230, 125, 90, 170, 70, 55, 55);
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
        LibraryUiTheme.styleTextArea(holdingDetails);
        JScrollPane holdingScroll = new JScrollPane(holdingDetails);
        holdingScroll.setBorder(BorderFactory.createLineBorder(LibraryUiTheme.BORDER));

        reservationLocation.setName("library.reservation.location");
        reservationLocation.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (component instanceof JLabel label && value instanceof BookLocationDTO location) {
                    label.setText(location.getLocation());
                }
                return component;
            }
        });
        LibraryUiTheme.styleComboBox(reservationLocation);
        reservationButton.setName("library.reservation.create");
        LibraryUiTheme.stylePrimaryButton(reservationButton);
        reservationHint.setName("library.reservation.hint");
        reservationHint.setForeground(LibraryUiTheme.MUTED);
        JPanel reservationControls = new JPanel(new BorderLayout(8, 8));
        reservationControls.setOpaque(false);
        JPanel reservationInput = new JPanel(new BorderLayout(8, 0));
        reservationInput.setOpaque(false);
        JLabel pickupLabel = new JLabel("取书馆藏地");
        pickupLabel.setForeground(LibraryUiTheme.TEXT);
        reservationInput.add(pickupLabel, BorderLayout.NORTH);
        reservationInput.add(reservationLocation, BorderLayout.CENTER);
        reservationControls.add(reservationInput, BorderLayout.NORTH);
        reservationControls.add(reservationHint, BorderLayout.CENTER);
        reservationControls.add(reservationButton, BorderLayout.SOUTH);

        JPanel detailsArea = new JPanel(new BorderLayout(8, 12));
        detailsArea.setName("library.catalog.details");
        LibraryUiTheme.styleCard(detailsArea);
        JLabel detailTitle = new JLabel("馆藏详情与线上预约");
        detailTitle.setForeground(LibraryUiTheme.TEXT);
        detailTitle.setFont(detailTitle.getFont().deriveFont(java.awt.Font.BOLD, 16F));
        detailsArea.add(detailTitle, BorderLayout.NORTH);
        detailsArea.add(holdingScroll, BorderLayout.CENTER);
        detailsArea.add(reservationControls, BorderLayout.SOUTH);
        detailsArea.setMinimumSize(new java.awt.Dimension(275, 0));
        detailsArea.setPreferredSize(new java.awt.Dimension(320, 0));

        JPanel resultArea = new JPanel(new BorderLayout(8, 8));
        resultArea.setName("library.catalog.results");
        LibraryUiTheme.styleCard(resultArea);
        JLabel resultTitle = new JLabel("检索结果");
        resultTitle.setForeground(LibraryUiTheme.TEXT);
        resultTitle.setFont(resultTitle.getFont().deriveFont(java.awt.Font.BOLD, 16F));
        resultArea.add(resultTitle, BorderLayout.NORTH);
        resultArea.add(LibraryUiTheme.tableScrollPane(resultTable), BorderLayout.CENTER);

        JPanel workspace = new JPanel(new BorderLayout(10, 0));
        workspace.setName("library.catalog.workspace");
        workspace.setOpaque(false);
        workspace.add(resultArea, BorderLayout.CENTER);
        workspace.add(detailsArea, BorderLayout.EAST);

        statusLabel.setName("library.searchStatus");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        LibraryUiTheme.styleStatusLabel(statusLabel);

        add(searchCard, BorderLayout.NORTH);
        add(workspace, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        searchButton.addActionListener(event -> search());
        keywordField.addActionListener(event -> search());
        reservationLocation.addActionListener(event -> updateReservationControls());
        reservationButton.addActionListener(event -> createReservation());
        updateReservationControls();
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
                publication(book), blankAsDash(book.getLanguage()),
                book.getTotalCount(), book.getAvailableCount()
            });
        }
        statusLabel.setText("找到 " + result.getBooks().size()
                + " 本书目；选择书目和馆藏地后可预约，实体书借还在模拟终端登记");
    }

    private static String publication(BookDTO book) {
        String publisher = blankAsDash(book.getPublisher());
        return book.getPublicationYear() == null
                ? publisher : publisher + " / " + book.getPublicationYear();
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
        reservationLocation.removeAllItems();
        book.getLocations().forEach(reservationLocation::addItem);
        if (reservationLocation.getItemCount() > 0) {
            reservationLocation.setSelectedIndex(0);
        }
        updateReservationControls();
    }

    private void createReservation() {
        BookDTO book = selectedBook();
        BookLocationDTO location = (BookLocationDTO) reservationLocation.getSelectedItem();
        if (working || book == null || location == null) {
            return;
        }
        setWorking(true);
        statusLabel.setText("正在提交预约……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.CREATE_RESERVATION,
                        new CreateReservationRequest(book.getBookId(), location.getLocation()));
            }

            @Override
            protected void done() {
                try {
                    showReservationResponse(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("预约已中断，请到“我的图书馆”核对后再操作");
                } catch (ExecutionException exception) {
                    statusLabel.setText("预约结果未确认，请到“我的图书馆”核对，勿重复提交");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private void showReservationResponse(Response response) {
        if (response == null || !response.isSuccess()) {
            BookDTO selected = selectedBook();
            if (response != null
                    && ErrorCodes.LIBRARY_DUPLICATE_RESERVATION.equals(response.getCode())
                    && selected != null) {
                locallyReservedBookId = selected.getBookId();
            }
            statusLabel.setText("预约失败：" + (response == null
                    ? "服务器未返回结果" : LibraryMessages.failure(response)));
            return;
        }
        if (!(response.getData() instanceof ReservationDTO reservation)) {
            statusLabel.setText("预约结果未确认，请到“我的图书馆”核对，勿重复提交");
            return;
        }
        if ("READY_FOR_PICKUP".equals(reservation.getStatus())) {
            statusLabel.setText("预约成功：已保留馆藏条码 "
                    + blankAsDash(reservation.getAssignedBarcode()) + "，请于 "
                    + formatTime(reservation.getExpiresAt()) + " 前取书");
        } else if ("WAITING".equals(reservation.getStatus())) {
            statusLabel.setText("预约成功：已进入“" + reservation.getPickupLocation()
                    + "”队列，当前第 " + reservation.getQueuePosition() + " 位");
        } else {
            statusLabel.setText("预约已提交，请到“我的图书馆”查看最新状态");
        }
        locallyReservedBookId = reservation.getBookId();
    }

    private BookDTO selectedBook() {
        int viewRow = resultTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = resultTable.convertRowIndexToModel(viewRow);
        return modelRow >= books.size() ? null : books.get(modelRow);
    }

    private void updateReservationControls() {
        BookDTO book = selectedBook();
        BookLocationDTO location = (BookLocationDTO) reservationLocation.getSelectedItem();
        reservationLocation.setEnabled(!working && book != null
                && reservationLocation.getItemCount() > 0);
        boolean alreadyReserved = book != null && book.getBookId().equals(locallyReservedBookId);
        reservationButton.setEnabled(!working && book != null && location != null
                && !alreadyReserved);
        if (book == null) {
            reservationHint.setText("请先选择书目和取书馆藏地");
        } else if (alreadyReserved) {
            reservationHint.setText("该书目已预约；可到“我的图书馆”查看或取消");
        } else if (location == null) {
            reservationHint.setText("该书目暂无可预约的馆藏地");
        } else if (location.getAvailableCount() > 0) {
            reservationHint.setText("当前可借 " + location.getAvailableCount()
                    + " 本；预约成功后立即保留 24 小时（1 天）");
        } else {
            reservationHint.setText("当前无可借单册；提交后将进入预约队列并按顺序等待");
        }
    }

    void reservationStateChanged() {
        locallyReservedBookId = null;
        refreshIfSearched();
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? "—" : TIME_FORMAT.format(time);
    }

    private static String blankAsDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void clearResults() {
        books = List.of();
        tableModel.setRowCount(0);
        holdingDetails.setText("请选择一条书目查看完整馆藏地和数量");
        reservationLocation.removeAllItems();
        updateReservationControls();
    }

    private void setWorking(boolean value) {
        working = value;
        keywordField.setEnabled(!value);
        categoryFilter.setEnabled(!value);
        searchButton.setEnabled(!value);
        resultTable.setEnabled(!value);
        updateReservationControls();
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
