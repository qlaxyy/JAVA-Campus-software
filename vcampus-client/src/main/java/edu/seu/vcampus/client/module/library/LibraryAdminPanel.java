package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.library.AddBookCopyRequest;
import edu.seu.vcampus.common.library.AddBookCategoryRequest;
import edu.seu.vcampus.common.library.AddBookRequest;
import edu.seu.vcampus.common.library.AdminBorrowQueryRequest;
import edu.seu.vcampus.common.library.AdminBorrowRecordDTO;
import edu.seu.vcampus.common.library.AdminReservationDTO;
import edu.seu.vcampus.common.library.AdminReservationQueryRequest;
import edu.seu.vcampus.common.library.BookCategoryDTO;
import edu.seu.vcampus.common.library.BookCopyDTO;
import edu.seu.vcampus.common.library.BookCopyIdRequest;
import edu.seu.vcampus.common.library.BookDTO;
import edu.seu.vcampus.common.library.BookSearchRequest;
import edu.seu.vcampus.common.library.BookSearchResult;
import edu.seu.vcampus.common.library.CategoryStatisticDTO;
import edu.seu.vcampus.common.library.LibraryActions;
import edu.seu.vcampus.common.library.LibraryStatisticsDTO;
import edu.seu.vcampus.common.library.PopularBookDTO;
import edu.seu.vcampus.common.library.ListBookCopiesRequest;
import edu.seu.vcampus.common.library.SetBookStatusRequest;
import edu.seu.vcampus.common.library.UpdateBookCopyRequest;
import edu.seu.vcampus.common.library.UpdateBookRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Library administrator workspace for catalog, physical copies and circulation records. */
public final class LibraryAdminPanel extends JPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String RESERVED = "RESERVED";
    private static final String LOANED = "LOANED";
    private static final String WAITING_SHELVING = "WAITING_SHELVING";
    private static final String WITHDRAWN = "WITHDRAWN";

    private final ClientContext context;
    private final JTabbedPane areas = new JTabbedPane();
    private final JTextField keyword = new JTextField(20);
    private final JButton refreshBooks = new JButton("查询 / 刷新");
    private final JButton newBook = primaryAction("＋ 新建书目");
    private final JButton addCategory = new JButton("＋ 新增分类");
    private final JButton saveBook = new JButton("保存书目信息");
    private final JButton activateBook = new JButton("开放借阅");
    private final JButton deactivateBook = new JButton("停止借阅");
    private final JTextField isbn = new JTextField(18);
    private final JTextField title = new JTextField(18);
    private final JTextField author = new JTextField(18);
    private final JComboBox<BookCategoryDTO> category = new JComboBox<>();
    private final JTextField publisher = new JTextField(18);
    private final JTextField year = new JTextField(8);
    private final JTextField price = new JTextField(8);
    private final JTextField language = new JTextField(12);
    private final JLabel selectedBook = new JLabel("请选择书目，或点击“新建书目”");
    private final DefaultTableModel bookModel = readOnlyModel(new String[]{
            "书目编号", "ISBN", "书名", "作者", "分类", "出版社", "年份", "语种", "状态", "馆藏", "可借"
    });
    private final JTable bookTable = new JTable(bookModel);

    private final JLabel copyBook = new JLabel("请先在书目维护中选择一本书");
    private final DefaultTableModel copyModel = readOnlyModel(new String[]{
            "单册编号", "馆藏条码", "馆藏地", "索书号", "单册状态", "当前可借性"
    });
    private final JTable copyTable = new JTable(copyModel);
    private final JTextField barcode = new JTextField(18);
    private final JTextField location = new JTextField(18);
    private final JTextField callNumber = new JTextField(18);
    private final JButton newCopy = primaryAction("＋ 登记新单册");
    private final JButton addCopy = new JButton("确认登记");
    private final JButton updateCopy = new JButton("保存位置与索书号");
    private final JButton shelfCopy = new JButton("确认归架");
    private final JButton restoreCopy = new JButton("恢复单册");
    private final JButton withdrawCopy = new JButton("注销单册");
    private final JButton refreshCopies = new JButton("刷新单册");

    private final JComboBox<String> borrowScope = new JComboBox<>(new String[]{"当前借阅", "借阅历史", "逾期未还"});
    private final JTextField borrowUser = new JTextField(18);
    private final JButton refreshBorrows = new JButton("刷新借阅记录");
    private final DefaultTableModel borrowModel = readOnlyModel(new String[]{
            "用户编号", "书名", "馆藏条码", "借出时间", "应还时间", "续借", "归还时间", "状态"
    });
    private final JTable borrowTable = new JTable(borrowModel);

    private final JComboBox<String> reservationScope =
            new JComboBox<>(new String[]{"排队中", "待取书", "全部"});
    private final JButton refreshReservations = new JButton("刷新预约记录");
    private final DefaultTableModel reservationModel = readOnlyModel(new String[]{
            "用户编号", "书名", "取书馆藏地", "预约状态", "排队位次", "馆藏条码", "取书截止时间"
    });
    private final JTable reservationTable = new JTable(reservationModel);

    private final JButton refreshStatistics = new JButton("刷新统计");
    private final JButton exportStatistics = new JButton("导出 CSV");
    private final JTextArea statisticsSummary = new JTextArea(10, 40);
    private final DefaultTableModel categoryModel =
            readOnlyModel(new String[]{"分类", "书目数", "馆藏数"});
    private final DefaultTableModel popularModel =
            readOnlyModel(new String[]{"书目编号", "书名", "借阅次数"});
    private final JTable categoryTable = new JTable(categoryModel);
    private final JTable popularTable = new JTable(popularModel);
    private LibraryStatisticsDTO statistics;

    private final JLabel outcome = new JLabel("管理员操作均由服务器再次校验权限");
    private final JLabel status = new JLabel("正在等待加载数据");
    private List<BookDTO> books = List.of();
    private List<BookCopyDTO> copies = List.of();
    private String selectedBookId;
    private String selectedCopyId;
    private boolean addingBook;
    private boolean addingCopy;
    private boolean working;
    private boolean uncertainWrite;

    /** @param context authenticated client context */
    public LibraryAdminPanel(ClientContext context) {
        this.context = context;
        setName("library.admin");
        setLayout(new BorderLayout(12, 12));
        LibraryUiTheme.installPage(this);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        areas.setName("library.admin.tabs");
        LibraryUiTheme.styleTabbedPane(areas);
        areas.addTab("书目维护", createCatalogArea());
        areas.addTab("实体单册", createCopyArea());
        areas.addTab("借阅查询", createBorrowArea());
        areas.addTab("预约查询", createReservationQueueArea());
        areas.addTab("统计", createStatisticsArea());
        add(areas, BorderLayout.CENTER);

        outcome.setName("library.admin.outcome");
        status.setName("library.admin.status");
        JPanel footer = new JPanel(new GridLayout(1, 2, 8, 0));
        footer.setOpaque(false);
        LibraryUiTheme.styleStatusLabel(outcome);
        LibraryUiTheme.styleStatusLabel(status);
        footer.add(outcome);
        footer.add(status);
        add(footer, BorderLayout.SOUTH);

        applyVisualTheme();
        wireEvents();
        updateControls();
    }

    private JPanel createCatalogArea() {
        keyword.setName("library.admin.keyword");
        isbn.setName("library.admin.isbn");
        title.setName("library.admin.title");
        author.setName("library.admin.author");
        category.setName("library.admin.category");
        publisher.setName("library.admin.publisher");
        year.setName("library.admin.year");
        language.setName("library.admin.language");
        LibraryCategories.render(category);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        LibraryUiTheme.installPage(panel);
        JPanel search = new JPanel(new BorderLayout(8, 0));
        LibraryUiTheme.styleCard(search);
        search.setBorder(LibraryUiTheme.cardBorder(10, 12));
        search.add(new JLabel("书名 / 作者 / ISBN"), BorderLayout.WEST);
        search.add(keyword, BorderLayout.CENTER);
        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchActions.setOpaque(false);
        searchActions.add(refreshBooks);
        searchActions.add(newBook);
        search.add(searchActions, BorderLayout.EAST);
        panel.add(search, BorderLayout.NORTH);

        configureTable(bookTable, "library.admin.books");
        JPanel editor = new JPanel(new GridBagLayout());
        editor.setName("library.admin.bookEditor");
        LibraryUiTheme.styleCard(editor);
        editor.setPreferredSize(new Dimension(330, 500));
        editor.setMinimumSize(new Dimension(300, 0));
        editor.setBorder(BorderFactory.createTitledBorder(
                LibraryUiTheme.cardBorder(10, 10),
                "书目元数据（馆藏数量由实体单册统计）"));
        GridBagConstraints c = formConstraints();
        row(editor, c, 0, "当前书目", selectedBook);
        row(editor, c, 1, "ISBN", isbn);
        row(editor, c, 2, "书名", title);
        row(editor, c, 3, "作者", author);
        JPanel categoryEditor = new JPanel(new BorderLayout(5, 0));
        categoryEditor.setOpaque(false);
        categoryEditor.add(category, BorderLayout.CENTER);
        categoryEditor.add(addCategory, BorderLayout.EAST);
        row(editor, c, 4, "分类", categoryEditor);
        row(editor, c, 5, "出版社", publisher);
        row(editor, c, 6, "出版年", year);
        row(editor, c, 7, "定价（元）", price);
        row(editor, c, 8, "语种", language);
        JPanel metadataActions = new JPanel(new GridLayout(0, 1, 0, 6));
        metadataActions.setOpaque(false);
        metadataActions.add(saveBook);
        metadataActions.add(activateBook);
        metadataActions.add(deactivateBook);
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 2;
        editor.add(metadataActions, c);

        JPanel workspace = new JPanel(new BorderLayout(10, 0));
        workspace.setOpaque(false);
        workspace.add(LibraryUiTheme.tableScrollPane(bookTable), BorderLayout.CENTER);
        LibraryUiTheme.setColumnWidths(bookTable,
                95, 125, 190, 110, 80, 130, 60, 60, 80, 55, 55);
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setBorder(null);
        editorScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        editorScroll.getViewport().setBackground(LibraryUiTheme.SURFACE);
        editorScroll.setPreferredSize(new Dimension(350, 0));
        workspace.add(editorScroll, BorderLayout.EAST);
        panel.add(workspace, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCopyArea() {
        barcode.setName("library.admin.barcode");
        location.setName("library.admin.location");
        callNumber.setName("library.admin.callNumber");
        configureTable(copyTable, "library.admin.copies");

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        LibraryUiTheme.installPage(panel);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        LibraryUiTheme.styleCard(header);
        header.setBorder(LibraryUiTheme.cardBorder(10, 12));
        header.add(copyBook, BorderLayout.CENTER);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerActions.setOpaque(false);
        headerActions.add(refreshCopies);
        headerActions.add(newCopy);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel editor = new JPanel(new GridBagLayout());
        editor.setName("library.admin.copyEditor");
        LibraryUiTheme.styleCard(editor);
        editor.setPreferredSize(new Dimension(330, 360));
        editor.setMinimumSize(new Dimension(300, 0));
        editor.setBorder(BorderFactory.createTitledBorder(
                LibraryUiTheme.cardBorder(10, 10), "单册资料与状态"));
        GridBagConstraints c = formConstraints();
        row(editor, c, 0, "馆藏条码", barcode);
        row(editor, c, 1, "馆藏地", location);
        row(editor, c, 2, "索书号", callNumber);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 6));
        actions.setOpaque(false);
        actions.add(addCopy);
        actions.add(updateCopy);
        actions.add(shelfCopy);
        actions.add(restoreCopy);
        actions.add(withdrawCopy);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        editor.add(actions, c);

        JPanel workspace = new JPanel(new BorderLayout(10, 0));
        workspace.setOpaque(false);
        workspace.add(LibraryUiTheme.tableScrollPane(copyTable), BorderLayout.CENTER);
        LibraryUiTheme.setColumnWidths(copyTable, 115, 130, 240, 110, 100, 120);
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setBorder(null);
        editorScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        editorScroll.getViewport().setBackground(LibraryUiTheme.SURFACE);
        editorScroll.setPreferredSize(new Dimension(350, 0));
        workspace.add(editorScroll, BorderLayout.EAST);
        panel.add(workspace, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBorrowArea() {
        borrowScope.setName("library.admin.borrowScope");
        configureTable(borrowTable, "library.admin.borrows");
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        LibraryUiTheme.installPage(panel);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        LibraryUiTheme.styleCard(header);
        header.setBorder(LibraryUiTheme.cardBorder(10, 12));
        header.add(new JLabel("查询范围"));
        header.add(borrowScope);
        borrowUser.setName("library.admin.borrowUser");
        borrowUser.setToolTipText("按读者编号筛选，留空表示查看全部读者");
        header.add(new JLabel("读者编号"));
        header.add(borrowUser);
        header.add(refreshBorrows);
        panel.add(header, BorderLayout.NORTH);
        panel.add(LibraryUiTheme.tableScrollPane(borrowTable), BorderLayout.CENTER);
        LibraryUiTheme.setColumnWidths(borrowTable,
                125, 190, 130, 135, 135, 60, 135, 90);
        return panel;
    }

    /** 预约队列只读视图：管理员可见全馆排队与待取情况，但不能重排或强制取消。 */
    private JPanel createReservationQueueArea() {
        reservationScope.setName("library.admin.reservationScope");
        configureTable(reservationTable, "library.admin.reservations");
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        LibraryUiTheme.installPage(panel);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        LibraryUiTheme.styleCard(header);
        header.setBorder(LibraryUiTheme.cardBorder(10, 12));
        header.add(new JLabel("查询范围"));
        header.add(reservationScope);
        header.add(refreshReservations);
        JLabel hint = LibraryUiTheme.createMutedLabel(
                "预约队列只读；取消与顺延由读者操作或系统到期清理触发");
        header.add(hint);
        panel.add(header, BorderLayout.NORTH);
        panel.add(LibraryUiTheme.tableScrollPane(reservationTable), BorderLayout.CENTER);
        LibraryUiTheme.setColumnWidths(reservationTable,
                125, 190, 210, 95, 75, 125, 145);
        return panel;
    }

    /** 全馆统计：数字来自服务器查询时派生，客户端只负责呈现与导出。 */
    private JPanel createStatisticsArea() {
        configureTable(categoryTable, "library.admin.statisticsCategories");
        configureTable(popularTable, "library.admin.statisticsPopular");
        statisticsSummary.setName("library.admin.statisticsSummary");
        statisticsSummary.setEditable(false);
        statisticsSummary.setFont(new java.awt.Font(
                java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        statisticsSummary.setBackground(LibraryUiTheme.SURFACE);
        statisticsSummary.setForeground(LibraryUiTheme.TEXT);
        statisticsSummary.setText("打开本页后加载统计");

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        LibraryUiTheme.installPage(panel);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        LibraryUiTheme.styleCard(header);
        header.setBorder(LibraryUiTheme.cardBorder(10, 12));
        header.add(refreshStatistics);
        header.add(exportStatistics);
        header.add(LibraryUiTheme.createMutedLabel(
                "统计为查询时派生，不落库；导出为当前快照"));
        panel.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(10, 10));
        body.setOpaque(false);
        body.add(statisticsSummary, BorderLayout.NORTH);
        JPanel tables = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        tables.setOpaque(false);
        tables.add(labelled("分类分布", categoryTable));
        tables.add(labelled("借阅排行", popularTable));
        body.add(tables, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel labelled(String title, JTable table) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setOpaque(false);
        wrapper.add(new JLabel(title), BorderLayout.NORTH);
        wrapper.add(LibraryUiTheme.tableScrollPane(table), BorderLayout.CENTER);
        return wrapper;
    }

    private void loadStatistics() {
        if (working) { return; }
        setWorking(true);
        status.setText("正在加载统计……");
        new SwingWorker<Response, Void>() {
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.ADMIN_STATISTICS, null);
            }
            protected void done() {
                try {
                    LibraryStatisticsDTO loaded =
                            requireData(get(), LibraryStatisticsDTO.class);
                    statistics = loaded;
                    statisticsSummary.setText(summaryText(loaded));
                    categoryModel.setRowCount(0);
                    for (CategoryStatisticDTO row : loaded.getCategories()) {
                        categoryModel.addRow(new Object[]{row.getCategoryName(),
                                row.getBookCount(), row.getCopyCount()});
                    }
                    popularModel.setRowCount(0);
                    for (PopularBookDTO row : loaded.getPopularBooks()) {
                        popularModel.addRow(new Object[]{row.getBookId(), row.getTitle(),
                                row.getBorrowCount()});
                    }
                    status.setText("统计已更新");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("统计加载已中断");
                } catch (ExecutionException | IllegalArgumentException exception) {
                    status.setText("统计加载失败，请稍后重试");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
    }

    private static String summaryText(LibraryStatisticsDTO value) {
        return "书目 " + value.getBookCount() + " 种，馆藏 " + value.getCopyCount()
                + " 册（可借 " + value.getAvailableCount()
                + "，已注销 " + value.getWithdrawnCount() + "）\n"
                + "当前在借 " + value.getActiveBorrowCount()
                + " 本，其中逾期 " + value.getOverdueCount()
                + " 本；历史借阅 " + value.getHistoryBorrowCount() + " 条\n"
                + "预约：排队中 " + value.getWaitingReservationCount()
                + " 条，待取书 " + value.getReadyReservationCount() + " 条\n"
                + "费用：未结清 " + value.getUnpaidFeeCount() + " 笔 / "
                + yuan(value.getUnpaidFeeFen()) + " 元；已结清 "
                + yuan(value.getSettledFeeFen()) + " 元";
    }

    private void exportStatisticsCsv() {
        if (statistics == null) {
            outcome.setText("请先刷新统计再导出");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("library-statistics.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.writeString(chooser.getSelectedFile().toPath(),
                    statisticsCsv(statistics), StandardCharsets.UTF_8);
            outcome.setText("已导出：" + chooser.getSelectedFile().getAbsolutePath());
        } catch (java.io.IOException exception) {
            outcome.setText("导出失败：" + exception.getMessage());
        }
    }

    /** 导出内容与界面同源：概览、分类分布、借阅排行三段，字段按 CSV 规则转义。 */
    static String statisticsCsv(LibraryStatisticsDTO value) {
        StringBuilder csv = new StringBuilder();
        csv.append("概览,数值\n");
        csv.append("书目数,").append(value.getBookCount()).append('\n');
        csv.append("馆藏数,").append(value.getCopyCount()).append('\n');
        csv.append("可借数,").append(value.getAvailableCount()).append('\n');
        csv.append("已注销,").append(value.getWithdrawnCount()).append('\n');
        csv.append("当前在借,").append(value.getActiveBorrowCount()).append('\n');
        csv.append("逾期未还,").append(value.getOverdueCount()).append('\n');
        csv.append("历史借阅,").append(value.getHistoryBorrowCount()).append('\n');
        csv.append("排队中预约,").append(value.getWaitingReservationCount()).append('\n');
        csv.append("待取书预约,").append(value.getReadyReservationCount()).append('\n');
        csv.append("未结清笔数,").append(value.getUnpaidFeeCount()).append('\n');
        csv.append("未结清金额(元),").append(yuan(value.getUnpaidFeeFen())).append('\n');
        csv.append("已结清金额(元),").append(yuan(value.getSettledFeeFen())).append('\n');

        csv.append("\n分类,书目数,馆藏数\n");
        for (CategoryStatisticDTO row : value.getCategories()) {
            csv.append(csvField(row.getCategoryName())).append(',')
                    .append(row.getBookCount()).append(',')
                    .append(row.getCopyCount()).append('\n');
        }

        csv.append("\n书目编号,书名,借阅次数\n");
        for (PopularBookDTO row : value.getPopularBooks()) {
            csv.append(csvField(row.getBookId())).append(',')
                    .append(csvField(row.getTitle())).append(',')
                    .append(row.getBorrowCount()).append('\n');
        }
        return csv.toString();
    }

    /** 含逗号、引号或换行的字段用双引号包裹并把内部引号翻倍。 */
    static String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private void loadReservations() {
        if (working) { return; }
        String scope = switch (reservationScope.getSelectedIndex()) {
            case 0 -> AdminReservationQueryRequest.WAITING;
            case 1 -> AdminReservationQueryRequest.READY;
            default -> AdminReservationQueryRequest.ALL;
        };
        setWorking(true);
        status.setText("正在加载预约队列……");
        new SwingWorker<Response, Void>() {
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.ADMIN_QUERY_RESERVATIONS,
                        new AdminReservationQueryRequest(scope));
            }
            protected void done() {
                try {
                    List<AdminReservationDTO> records =
                            requireList(get(), AdminReservationDTO.class);
                    reservationModel.setRowCount(0);
                    for (AdminReservationDTO record : records) {
                        reservationModel.addRow(new Object[]{record.getUserId(),
                                record.getBookTitle(), record.getPickupLocation(),
                                displayReservationStatus(record.getStatus()),
                                record.getQueuePosition() == null
                                        ? "—" : "第 " + record.getQueuePosition() + " 位",
                                record.getAssignedBarcode() == null || record.getAssignedBarcode().isBlank()
                                        ? "—" : record.getAssignedBarcode(),
                                format(record.getExpiresAt())});
                    }
                    status.setText("共 " + records.size() + " 条"
                            + reservationScope.getSelectedItem() + "预约");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("预约查询已中断");
                } catch (ExecutionException | IllegalArgumentException exception) {
                    status.setText("预约查询失败，请稍后重试");
                } finally {
                    setWorking(false);
                }
            }
        }.execute();
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

    private void applyVisualTheme() {
        for (JTextField field : List.of(
                keyword, isbn, title, author, publisher, year,
                language, barcode, location, callNumber)) {
            LibraryUiTheme.styleTextField(field);
        }
        LibraryUiTheme.styleComboBox(category);
        LibraryUiTheme.styleComboBox(borrowScope);

        for (JButton button : List.of(
                refreshBooks, newBook, addCategory, activateBook, newCopy,
                updateCopy, restoreCopy, refreshCopies, refreshBorrows)) {
            LibraryUiTheme.styleSecondaryButton(button);
        }
        for (JButton button : List.of(saveBook, addCopy, shelfCopy)) {
            LibraryUiTheme.stylePrimaryButton(button);
        }
        for (JButton button : List.of(deactivateBook, withdrawCopy)) {
            LibraryUiTheme.styleDangerButton(button);
        }
        selectedBook.setForeground(LibraryUiTheme.MUTED);
        copyBook.setForeground(LibraryUiTheme.MUTED);
    }

    private void wireEvents() {
        refreshBooks.addActionListener(event -> refresh());
        keyword.addActionListener(event -> refresh());
        newBook.addActionListener(event -> beginNewBook());
        addCategory.addActionListener(event -> addCategory());
        saveBook.addActionListener(event -> saveBook());
        activateBook.addActionListener(event -> changeBookStatus(ACTIVE));
        deactivateBook.addActionListener(event -> changeBookStatus(INACTIVE));
        bookTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !working) { selectBook(); }
        });
        newCopy.addActionListener(event -> beginNewCopy());
        addCopy.addActionListener(event -> addCopy());
        updateCopy.addActionListener(event -> updateCopy());
        shelfCopy.addActionListener(event -> mutateCopy(LibraryActions.SHELVE_BOOK_COPY, "上架"));
        restoreCopy.addActionListener(event -> mutateCopy(LibraryActions.RESTORE_BOOK_COPY, "恢复"));
        withdrawCopy.addActionListener(event -> mutateCopy(LibraryActions.WITHDRAW_BOOK_COPY, "注销"));
        refreshCopies.addActionListener(event -> loadCopies());
        copyTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !working) { selectCopy(); }
        });
        refreshBorrows.addActionListener(event -> loadBorrows());
        borrowScope.addActionListener(event -> {
            if (!working && areas.getSelectedIndex() == 2) { loadBorrows(); }
        });
        refreshReservations.addActionListener(event -> loadReservations());
        reservationScope.addActionListener(event -> {
            if (!working && areas.getSelectedIndex() == 3) { loadReservations(); }
        });
        areas.addChangeListener(event -> {
            if (working) { return; }
            if (areas.getSelectedIndex() == 1 && selectedBookId != null) {
                loadCopies();
            } else if (areas.getSelectedIndex() == 2) {
                loadBorrows();
            } else if (areas.getSelectedIndex() == 3) {
                loadReservations();
            } else if (areas.getSelectedIndex() == 4) {
                loadStatistics();
            }
        });
        refreshStatistics.addActionListener(event -> loadStatistics());
        exportStatistics.addActionListener(event -> exportStatisticsCsv());
    }

    /** Reloads authoritative categories and all catalog snapshots. */
    void refresh() {
        if (working) { return; }
        String query = keyword.getText().strip();
        setWorking(true);
        status.setText("正在加载书目与分类……");
        new SwingWorker<CatalogLoad, Void>() {
            protected CatalogLoad doInBackground() throws Exception {
                List<BookCategoryDTO> categories = LibraryCategories.read(
                        context.send(LibraryActions.LIST_CATEGORIES, null));
                return new CatalogLoad(categories, context.send(LibraryActions.ADMIN_SEARCH_BOOKS,
                        new BookSearchRequest(query, null)));
            }
            protected void done() {
                try {
                    CatalogLoad loaded = get();
                    BookSearchResult result = requireData(loaded.books(), BookSearchResult.class);
                    category.removeAllItems();
                    loaded.categories().forEach(category::addItem);
                    books = result.getBooks();
                    fillBookTable();
                    clearBookForm();
                    uncertainWrite = false;
                    status.setText("共 " + books.size() + " 条书目；馆藏与可借数量来自实体单册实时汇总");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("刷新已中断，可重新查询");
                } catch (ExecutionException | IllegalArgumentException exception) {
                    status.setText("刷新失败：" + message(exception));
                } finally { setWorking(false); }
            }
        }.execute();
    }

    private void fillBookTable() {
        bookModel.setRowCount(0);
        for (BookDTO book : books) {
            bookModel.addRow(new Object[]{book.getBookId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                    book.getCategoryName(), book.getPublisher(), book.getPublicationYear(), book.getLanguage(),
                    displayBookStatus(book.getStatus()), book.getTotalCount(), book.getAvailableCount()});
        }
    }

    private void beginNewBook() {
        if (working || uncertainWrite || category.getItemCount() == 0) { return; }
        bookTable.clearSelection();
        clearBookForm();
        addingBook = true;
        category.setSelectedIndex(0);
        selectedBook.setText("新书目（保存后再登记实体单册）");
        outcome.setText("请填写书目元数据；新增书目初始馆藏为 0");
        updateControls();
        isbn.requestFocusInWindow();
    }

    private void addCategory() {
        if (working || uncertainWrite) return;
        String name = JOptionPane.showInputDialog(this,
                "输入新分类名称（新增后可被所有书目复用）", "新增图书分类",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        if (name.isBlank()) {
            outcome.setText("分类名称不能为空");
            return;
        }
        submit(LibraryActions.ADD_BOOK_CATEGORY, new AddBookCategoryRequest(name),
                "新增分类", BookCategoryDTO.class, added -> {
                    category.addItem(added);
                    category.setSelectedItem(added);
                    outcome.setText("分类“" + added.getCategoryName() + "”已新增，可用于新书目");
                });
    }

    private void selectBook() {
        int viewRow = bookTable.getSelectedRow();
        if (viewRow < 0) { clearBookForm(); updateControls(); return; }
        BookDTO book = books.get(bookTable.convertRowIndexToModel(viewRow));
        selectedBookId = book.getBookId();
        addingBook = false;
        selectedBook.setText(book.getBookId() + " · " + displayBookStatus(book.getStatus()));
        isbn.setText(book.getIsbn());
        title.setText(book.getTitle());
        author.setText(book.getAuthor());
        publisher.setText(book.getPublisher());
        year.setText(book.getPublicationYear() == null ? "" : book.getPublicationYear().toString());
        price.setText(yuan(book.getPriceFen()));
        language.setText(book.getLanguage());
        selectCategory(book.getCategoryId());
        copyBook.setText("当前书目：《" + book.getTitle() + "》（" + book.getBookId() + "）· "
                + displayBookStatus(book.getStatus()));
        outcome.setText("已选择《" + book.getTitle() + "》；可切换到“实体单册”维护馆藏");
        clearCopyForm();
        updateControls();
    }

    private void saveBook() {
        if (working || uncertainWrite || (!addingBook && selectedBookId == null)) { return; }
        BookCategoryDTO choice = (BookCategoryDTO) category.getSelectedItem();
        Integer publicationYear = readYear();
        if (publicationYear == Integer.MIN_VALUE) { return; }
        int priceFen = readPriceFen();
        if (priceFen == Integer.MIN_VALUE) { return; }
        if (choice == null || isbn.getText().isBlank() || title.getText().isBlank() || author.getText().isBlank()) {
            outcome.setText("请填写 ISBN、书名、作者并选择分类");
            return;
        }
        Serializable request = addingBook
                ? new AddBookRequest(isbn.getText(), title.getText(), author.getText(), choice.getCategoryId(),
                        publisher.getText(), publicationYear, language.getText(), priceFen)
                : new UpdateBookRequest(selectedBookId, isbn.getText(), title.getText(), author.getText(),
                        choice.getCategoryId(), publisher.getText(), publicationYear, language.getText(),
                        priceFen);
        submitBook(addingBook ? LibraryActions.ADD_BOOK : LibraryActions.UPDATE_BOOK,
                request, addingBook ? "新增书目" : "修改书目");
    }

    private void changeBookStatus(String target) {
        BookDTO book = selectedBook();
        if (working || book == null || target.equals(book.getStatus())) { return; }
        submitBook(LibraryActions.SET_BOOK_STATUS,
                new SetBookStatusRequest(book.getBookId(), target),
                ACTIVE.equals(target) ? "开放借阅" : "停止借阅");
    }

    private void submitBook(String action, Serializable request, String operation) {
        submit(action, request, operation, BookDTO.class, book -> {
            outcome.setText(operation + "成功：《" + book.getTitle() + "》；馆藏 "
                    + book.getTotalCount() + " 本，可借 " + book.getAvailableCount() + " 本");
            keyword.setText(book.getIsbn());
            SwingUtilities.invokeLater(this::refresh);
        });
    }

    private void loadCopies() {
        if (working || selectedBookId == null) { return; }
        setWorking(true);
        status.setText("正在加载实体单册……");
        new SwingWorker<Response, Void>() {
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.LIST_BOOK_COPIES, new ListBookCopiesRequest(selectedBookId));
            }
            protected void done() {
                try {
                    copies = requireList(get(), BookCopyDTO.class);
                    fillCopyTable();
                    clearCopyForm();
                    status.setText("当前书目共有 " + copies.size() + " 个实体单册（含已注销单册）");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("单册刷新已中断");
                } catch (ExecutionException | IllegalArgumentException exception) {
                    status.setText("单册刷新失败：" + message(exception));
                } finally { setWorking(false); }
            }
        }.execute();
    }

    private void fillCopyTable() {
        copyModel.setRowCount(0);
        BookDTO book = selectedBook();
        for (BookCopyDTO copy : copies) {
            copyModel.addRow(new Object[]{copy.getCopyId(), copy.getBarcode(), copy.getLocation(),
                    copy.getCallNumber(), displayCopyStatus(copy.getStatus()),
                    displayCopyAvailability(book, copy)});
        }
    }

    private void beginNewCopy() {
        if (working || uncertainWrite || selectedBookId == null) { return; }
        copyTable.clearSelection();
        clearCopyForm();
        addingCopy = true;
        outcome.setText("请输入全馆唯一的馆藏条码、馆藏地和索书号");
        updateControls();
        barcode.requestFocusInWindow();
    }

    private void selectCopy() {
        int viewRow = copyTable.getSelectedRow();
        if (viewRow < 0) { clearCopyForm(); updateControls(); return; }
        BookCopyDTO copy = copies.get(copyTable.convertRowIndexToModel(viewRow));
        selectedCopyId = copy.getCopyId();
        addingCopy = false;
        barcode.setText(copy.getBarcode());
        location.setText(copy.getLocation());
        callNumber.setText(copy.getCallNumber());
        outcome.setText("已选择单册 " + copy.getBarcode() + " · " + displayCopyStatus(copy.getStatus()));
        updateControls();
    }

    private void addCopy() {
        if (working || !addingCopy || selectedBookId == null) { return; }
        if (barcode.getText().isBlank() || location.getText().isBlank() || callNumber.getText().isBlank()) {
            outcome.setText("请填写馆藏条码、馆藏地和索书号");
            return;
        }
        submitCopy(LibraryActions.ADD_BOOK_COPY,
                new AddBookCopyRequest(selectedBookId, barcode.getText(), location.getText(), callNumber.getText()),
                "登记单册");
    }

    private void updateCopy() {
        if (working || addingCopy || selectedCopyId == null) { return; }
        if (location.getText().isBlank() || callNumber.getText().isBlank()) {
            outcome.setText("馆藏地和索书号不能为空");
            return;
        }
        submitCopy(LibraryActions.UPDATE_BOOK_COPY,
                new UpdateBookCopyRequest(selectedCopyId, location.getText(), callNumber.getText()),
                "修改单册资料");
    }

    private void mutateCopy(String action, String operation) {
        BookCopyDTO copy = selectedCopy();
        if (working || copy == null) { return; }
        if (LibraryActions.SHELVE_BOOK_COPY.equals(action) && !WAITING_SHELVING.equals(copy.getStatus())) { return; }
        if (LibraryActions.RESTORE_BOOK_COPY.equals(action) && !WITHDRAWN.equals(copy.getStatus())) { return; }
        if (LibraryActions.WITHDRAW_BOOK_COPY.equals(action)
                && (RESERVED.equals(copy.getStatus()) || LOANED.equals(copy.getStatus())
                || WITHDRAWN.equals(copy.getStatus()))) { return; }
        if (LibraryActions.WITHDRAW_BOOK_COPY.equals(action) && !confirmWithdrawal(copy)) { return; }
        submitCopy(action, new BookCopyIdRequest(copy.getCopyId()), operation);
    }

    private boolean confirmWithdrawal(BookCopyDTO copy) {
        return JOptionPane.showConfirmDialog(this,
                "确定注销馆藏条码 “" + copy.getBarcode() + "” 吗？\n"
                        + "注销后不计入馆藏，但会保留历史记录，也可由管理员恢复。",
                "确认注销单册", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    private void submitCopy(String action, Serializable request, String operation) {
        submit(action, request, operation, BookCopyDTO.class, copy -> {
            outcome.setText(operation + "成功：" + copy.getBarcode() + " · " + displayCopyStatus(copy.getStatus()));
            SwingUtilities.invokeLater(this::loadCopies);
        });
    }

    private void loadBorrows() {
        if (working) { return; }
        String scope = switch (borrowScope.getSelectedIndex()) {
            case 1 -> AdminBorrowQueryRequest.HISTORY;
            case 2 -> AdminBorrowQueryRequest.OVERDUE;
            default -> AdminBorrowQueryRequest.CURRENT;
        };
        setWorking(true);
        status.setText("正在加载借阅记录……");
        new SwingWorker<Response, Void>() {
            protected Response doInBackground() throws Exception {
                return context.send(LibraryActions.ADMIN_QUERY_BORROWS,
                        new AdminBorrowQueryRequest(scope, borrowUser.getText().strip()));
            }
            protected void done() {
                try {
                    List<AdminBorrowRecordDTO> records = requireList(get(), AdminBorrowRecordDTO.class);
                    borrowModel.setRowCount(0);
                    for (AdminBorrowRecordDTO record : records) {
                        borrowModel.addRow(new Object[]{record.getUserId(), record.getBookTitle(), record.getBarcode(),
                                format(record.getBorrowTime()), format(record.getDueTime()), record.getRenewalCount(),
                                format(record.getReturnTime()), record.isOverdue()
                                ? "已逾期" : displayBorrowStatus(record.getStatus())});
                    }
                    status.setText("共 " + records.size() + " 条" + borrowScope.getSelectedItem() + "记录");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    status.setText("借阅查询已中断");
                } catch (ExecutionException | IllegalArgumentException exception) {
                    status.setText("借阅查询失败：" + message(exception));
                } finally { setWorking(false); }
            }
        }.execute();
    }

    private <T> void submit(String action, Serializable request, String operation,
            Class<T> type, Consumer<T> success) {
        if (working || uncertainWrite) { return; }
        setWorking(true);
        outcome.setText("正在" + operation + "……");
        new SwingWorker<Response, Void>() {
            protected Response doInBackground() throws Exception { return context.send(action, request); }
            protected void done() {
                try {
                    Response response = get();
                    if (response == null || response.isSuccess() && !type.isInstance(response.getData())) {
                        unknownWrite(operation);
                        return;
                    }
                    if (!response.isSuccess()) {
                        outcome.setText(operation + "失败："
                                + (ErrorCodes.COMMON_INVALID_ARGUMENT.equals(response.getCode())
                                ? response.getMessage() : LibraryMessages.failure(response)));
                        return;
                    }
                    success.accept(type.cast(response.getData()));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    unknownWrite(operation);
                } catch (ExecutionException exception) {
                    unknownWrite(operation);
                } finally { setWorking(false); }
            }
        }.execute();
    }

    private <T> T requireData(Response response, Class<T> type) {
        if (response == null) { throw new IllegalArgumentException("服务器未返回结果"); }
        if (!response.isSuccess()) {
            throw new IllegalArgumentException(ErrorCodes.COMMON_INVALID_ARGUMENT.equals(response.getCode())
                    ? response.getMessage() : LibraryMessages.failure(response));
        }
        if (!type.isInstance(response.getData())) {
            throw new IllegalArgumentException("服务器返回的数据格式不正确");
        }
        return type.cast(response.getData());
    }

    private <T> List<T> requireList(Response response, Class<T> type) {
        if (response == null) { throw new IllegalArgumentException("服务器未返回结果"); }
        if (!response.isSuccess()) { throw new IllegalArgumentException(LibraryMessages.failure(response)); }
        if (!(response.getData() instanceof List<?> values)
                || values.stream().anyMatch(value -> !type.isInstance(value))) {
            throw new IllegalArgumentException("服务器返回的数据格式不正确");
        }
        List<T> result = new ArrayList<>(values.size());
        values.forEach(value -> result.add(type.cast(value)));
        return result;
    }

    private void unknownWrite(String operation) {
        uncertainWrite = true;
        outcome.setText(operation + "结果未确认，请先查询 / 刷新核对，勿重复提交");
        updateControls();
    }

    private Integer readYear() {
        String value = year.getText().strip();
        if (value.isEmpty()) { return null; }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1000 || parsed > 9999) { throw new NumberFormatException(); }
            return parsed;
        } catch (NumberFormatException exception) {
            outcome.setText("出版年须为空或 1000 至 9999 的整数");
            return Integer.MIN_VALUE;
        }
    }

    /** 读取定价（元，最多两位小数）并转换为分；非法输入返回 {@code Integer.MIN_VALUE}。 */
    private int readPriceFen() {
        String value = price.getText().strip();
        try {
            int fen = new java.math.BigDecimal(value).movePointRight(2).intValueExact();
            if (fen < 1 || fen > 999_999) {
                throw new ArithmeticException("out of range");
            }
            return fen;
        } catch (NumberFormatException | ArithmeticException exception) {
            outcome.setText("定价须为 0.01 至 9999.99 元，最多两位小数");
            return Integer.MIN_VALUE;
        }
    }

    private static String yuan(int fen) {
        return java.math.BigDecimal.valueOf(fen, 2).toPlainString();
    }

    private void clearBookForm() {
        selectedBookId = null;
        addingBook = false;
        selectedBook.setText("请选择书目，或点击“新建书目”");
        isbn.setText(""); title.setText(""); author.setText(""); publisher.setText("");
        year.setText(""); price.setText(""); language.setText(""); category.setSelectedIndex(-1);
        copyBook.setText("请先在书目维护中选择一本书");
        copies = List.of();
        copyModel.setRowCount(0);
        clearCopyForm();
    }

    private void clearCopyForm() {
        selectedCopyId = null;
        addingCopy = false;
        barcode.setText(""); location.setText(""); callNumber.setText("");
        copyTable.clearSelection();
    }

    private void selectCategory(String categoryId) {
        category.setSelectedIndex(-1);
        for (int i = 0; i < category.getItemCount(); i++) {
            if (category.getItemAt(i).getCategoryId().equals(categoryId)) {
                category.setSelectedIndex(i);
                return;
            }
        }
    }

    private BookDTO selectedBook() {
        return books.stream().filter(book -> book.getBookId().equals(selectedBookId)).findFirst().orElse(null);
    }

    private BookCopyDTO selectedCopy() {
        return copies.stream().filter(copy -> copy.getCopyId().equals(selectedCopyId)).findFirst().orElse(null);
    }

    private void setWorking(boolean value) {
        working = value;
        updateControls();
    }

    private void updateControls() {
        BookDTO book = selectedBook();
        BookCopyDTO copy = selectedCopy();
        boolean ready = !working && !uncertainWrite;
        boolean bookEditable = ready && (addingBook || book != null);
        refreshBooks.setEnabled(!working);
        keyword.setEnabled(!working);
        bookTable.setEnabled(!working);
        newBook.setEnabled(ready && category.getItemCount() > 0);
        addCategory.setEnabled(ready);
        isbn.setEnabled(bookEditable);
        title.setEnabled(bookEditable);
        author.setEnabled(bookEditable);
        category.setEnabled(bookEditable);
        publisher.setEnabled(bookEditable);
        year.setEnabled(bookEditable);
        language.setEnabled(bookEditable);
        saveBook.setEnabled(bookEditable && category.getSelectedItem() != null);
        saveBook.setText(addingBook ? "确认新增书目" : "保存书目信息");
        activateBook.setEnabled(ready && book != null && !ACTIVE.equals(book.getStatus()));
        deactivateBook.setEnabled(ready && book != null && !INACTIVE.equals(book.getStatus()));

        refreshCopies.setEnabled(!working && selectedBookId != null);
        copyTable.setEnabled(!working);
        newCopy.setEnabled(ready && selectedBookId != null);
        barcode.setEnabled(ready && addingCopy);
        boolean copyEditable = ready && (addingCopy
                || copy != null && !RESERVED.equals(copy.getStatus())
                && !WITHDRAWN.equals(copy.getStatus()));
        location.setEnabled(copyEditable);
        callNumber.setEnabled(copyEditable);
        addCopy.setEnabled(ready && addingCopy);
        updateCopy.setEnabled(ready && !addingCopy && copy != null
                && !RESERVED.equals(copy.getStatus()) && !WITHDRAWN.equals(copy.getStatus()));
        shelfCopy.setEnabled(ready && copy != null && WAITING_SHELVING.equals(copy.getStatus()));
        restoreCopy.setEnabled(ready && copy != null && WITHDRAWN.equals(copy.getStatus()));
        withdrawCopy.setEnabled(ready && copy != null
                && !RESERVED.equals(copy.getStatus()) && !LOANED.equals(copy.getStatus())
                && !WITHDRAWN.equals(copy.getStatus()));
        borrowScope.setEnabled(!working);
        refreshBorrows.setEnabled(!working);
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static JButton primaryAction(String text) {
        return new JButton(text);
    }

    private static void configureTable(JTable table, String name) {
        table.setName(name);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        LibraryUiTheme.styleTable(table);
    }

    private static GridBagConstraints formConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 8, 5, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        return c;
    }

    private static void row(JPanel panel, GridBagConstraints c, int row, String label, Component value) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(value, c);
    }

    private static String displayBookStatus(String value) {
        return ACTIVE.equals(value) ? "开放借阅" : INACTIVE.equals(value) ? "停止借阅" : value;
    }

    private static String displayCopyStatus(String value) {
        return switch (value) {
            case AVAILABLE -> "在架";
            case RESERVED -> "预约待取";
            case LOANED -> "已借出";
            case WAITING_SHELVING -> "待上架";
            case WITHDRAWN -> "已注销";
            default -> value;
        };
    }

    private static String displayCopyAvailability(BookDTO book, BookCopyDTO copy) {
        return switch (copy.getStatus()) {
            case WITHDRAWN -> "不参与馆藏";
            case RESERVED -> "不可借（预约待取）";
            case LOANED -> "不可借（已借出）";
            case WAITING_SHELVING -> "不可借（待上架）";
            case AVAILABLE -> book != null && ACTIVE.equals(book.getStatus())
                    ? "可借" : "书目已停止借阅";
            default -> "不可借";
        };
    }

    private static String displayBorrowStatus(String value) {
        return "BORROWED".equals(value) ? "借阅中" : "RETURNED".equals(value) ? "已归还" : value;
    }

    private static String format(LocalDateTime value) {
        return value == null ? "—" : TIME.format(value);
    }

    private static String message(Exception exception) {
        Throwable cause = exception instanceof ExecutionException ? exception.getCause() : exception;
        return cause == null || cause.getMessage() == null ? "请检查连接后重试" : cause.getMessage();
    }

    private record CatalogLoad(List<BookCategoryDTO> categories, Response books) { }
}
