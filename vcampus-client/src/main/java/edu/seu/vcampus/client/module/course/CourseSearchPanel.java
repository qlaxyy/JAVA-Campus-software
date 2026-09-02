package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseSearchItem;
import edu.seu.vcampus.common.course.CourseSearchRequest;
import edu.seu.vcampus.common.course.CourseSearchResult;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.protocol.Response;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;



/**
 * 全校课程查询页面。
 *
 * 只负责查询，不负责选课。
 */
final class CourseSearchPanel
    extends JPanel {

    /**
     * 服务端固定每页 20 条。
     */
    private static final int PAGE_SIZE = 20;

    private final ClientContext context;

    /*
     * =========================
     * 查询条件
     * =========================
     */
    private final JTextField courseCodeField =
        new JTextField(10);

    private final JTextField courseNameField =
        new JTextField(12);

    private final JTextField teacherField =
        new JTextField(10);

    private final JComboBox<String> departmentBox =
        new JComboBox<>(
            new String[]{
                "全部院系",
                "数学学院",
                "计算机科学与工程学院",
                "电子科学与工程学院",
                "物理学院",
                "外国语学院",
                "人文学院",
                "艺术学院",
                "心理健康教育中心",
                "创新创业学院"
            });

    private final JComboBox<String> availabilityBox =
        new JComboBox<>(
            new String[]{
                "全部",
                "有余量",
                "已满"
            });

    /*
     * =========================
     * 表格
     * =========================
     */
    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "课程号",
                "课程名",
                "教学班",
                "开课院系",
                "教师",
                "学分",
                "类型",
                "上课时间",
                "地点",
                "校区",
                "人数",
                "余量"
            },
            0) {

            @Override
            public boolean isCellEditable(
                int row,
                int column) {

                return false;
            }
        };

    private final JTable table =
        new JTable(
            tableModel);

    /*
     * =========================
     * 分页
     * =========================
     */
    private final JButton previousButton =
        new JButton("上一页");

    private final JButton nextButton =
        new JButton("下一页");

    private final JLabel pageLabel =
        new JLabel("第 1 页");

    private final JLabel countLabel =
        new JLabel("共 0 条");

    private final JLabel statusLabel =
        new JLabel(" ");

    /**
     * 当前页。
     */
    private int currentPage = 1;

    /**
     * 当前总页数。
     */
    private int totalPages = 0;

    CourseSearchPanel(
        ClientContext context) {

        this.context =
            context;

        initializeView();

        loadPage(
            1);
    }

    /**
     * 初始化页面。
     */
    private void initializeView() {

        setLayout(
            new BorderLayout(
                0,
                14));

        setBackground(
            CourseTheme.BACKGROUND);

        setBorder(
            BorderFactory.createEmptyBorder(
                18,
                18,
                18,
                18));

        /*
         * =========================
         * 顶部标题 + 查询条件
         * =========================
         */
        JPanel north =
            new JPanel(
                new BorderLayout(
                    0,
                    14));

        north.setOpaque(
            false);

        JPanel titleArea =
            new JPanel();

        titleArea.setOpaque(
            false);

        titleArea.setLayout(
            new BoxLayout(
                titleArea,
                BoxLayout.Y_AXIS));

        JLabel title =
            CourseTheme.title(
                "全校课程查询");

        JLabel subtitle =
            CourseTheme.subtitle(
                "查询当前学期全校开设的课程与教学班");

        titleArea.add(
            title);

        titleArea.add(
            Box.createVerticalStrut(
                5));

        titleArea.add(
            subtitle);

        north.add(
            titleArea,
            BorderLayout.NORTH);

        /*
         * =========================
         * 白色搜索区域
         * =========================
         */
        CourseTheme.SurfacePanel filters =
            new CourseTheme.SurfacePanel();

        filters.setLayout(
            new GridLayout(
                2,
                1,
                0,
                6));

        filters.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    10,
                    14,
                    10,
                    14)));

        /*
         * 第一行
         */
        JPanel firstRow =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    8,
                    4));

        firstRow.setOpaque(
            false);

        firstRow.add(
            new JLabel(
                "课程号"));

        firstRow.add(
            courseCodeField);

        firstRow.add(
            new JLabel(
                "课程名"));

        firstRow.add(
            courseNameField);

        firstRow.add(
            new JLabel(
                "教师"));

        firstRow.add(
            teacherField);

        filters.add(
            firstRow);

        /*
         * 第二行
         */
        JPanel secondRow =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    8,
                    4));

        secondRow.setOpaque(
            false);

        secondRow.add(
            new JLabel(
                "开课院系"));

        secondRow.add(
            departmentBox);

        secondRow.add(
            new JLabel(
                "余量"));

        secondRow.add(
            availabilityBox);

        JButton searchButton =
            new JButton(
                "查询");

        JButton resetButton =
            new JButton(
                "重置");

        CourseTheme.stylePrimaryButton(
            searchButton);

        CourseTheme.styleQuietButton(
            resetButton);

        secondRow.add(
            searchButton);

        secondRow.add(
            resetButton);

        filters.add(
            secondRow);

        north.add(
            filters,
            BorderLayout.CENTER);

        add(
            north,
            BorderLayout.NORTH);

        /*
         * =========================
         * 表格
         * =========================
         */
        table.setSelectionMode(
            ListSelectionModel
                .SINGLE_SELECTION);

        table.setAutoResizeMode(
            JTable.AUTO_RESIZE_OFF);

        table.setRowHeight(
            30);

        table.setBackground(
            CourseTheme.SURFACE);

        table.setForeground(
            CourseTheme.TEXT);

        table.setGridColor(
            CourseTheme.BORDER);

        /*
         * 不显示密集的竖线，
         * 看起来更接近现代数据表。
         */
        table.setShowVerticalLines(
            false);

        table.setShowHorizontalLines(
            true);

        table.setSelectionBackground(
            CourseTheme.PRIMARY_LIGHT);

        table.setSelectionForeground(
            CourseTheme.TEXT);
        /*
         * =========================
         * 表头统一样式
         * =========================
         *
         * 不直接依赖系统 LookAndFeel 的表头背景，
         * 否则 Windows 下可能出现：
         *
         * 白色文字 + 白色背景。
         */
        DefaultTableCellRenderer headerRenderer =
            new DefaultTableCellRenderer();

        headerRenderer.setBackground(
            CourseTheme.NAVY);

        headerRenderer.setForeground(
            Color.WHITE);

        headerRenderer.setFont(
            table
                .getTableHeader()
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        headerRenderer.setHorizontalAlignment(
            JLabel.CENTER);

        headerRenderer.setOpaque(
            true);

        headerRenderer.setBorder(
            BorderFactory.createEmptyBorder(
                8,
                6,
                8,
                6));

        table
            .getTableHeader()
            .setDefaultRenderer(
                headerRenderer);

        table
            .getTableHeader()
            .setBackground(
                CourseTheme.NAVY);

        table
            .getTableHeader()
            .setForeground(
                Color.WHITE);

        table
            .getTableHeader()
            .setReorderingAllowed(
                false);

        configureColumnWidths();

        JScrollPane scrollPane =
            new JScrollPane(
                table);

        scrollPane.setBorder(
            BorderFactory
                .createEmptyBorder());

        /*
         * 再用一层白色 Surface
         * 包住整个表格。
         */
        CourseTheme.SurfacePanel tableCard =
            new CourseTheme.SurfacePanel();

        tableCard.setLayout(
            new BorderLayout());

        tableCard.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    1,
                    1,
                    1,
                    1)));

        tableCard.add(
            scrollPane,
            BorderLayout.CENTER);

        add(
            tableCard,
            BorderLayout.CENTER);

        /*
         * =========================
         * 分页区域
         * =========================
         */
        JPanel south =
            new JPanel(
                new BorderLayout());

        south.setOpaque(
            false);

        JPanel pagePanel =
            new JPanel(
                new FlowLayout(
                    FlowLayout.CENTER,
                    10,
                    4));

        pagePanel.setOpaque(
            false);

        CourseTheme.styleQuietButton(
            previousButton);

        CourseTheme.styleQuietButton(
            nextButton);

        pageLabel.setForeground(
            CourseTheme.TEXT);

        pageLabel.setFont(
            pageLabel
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        countLabel.setForeground(
            CourseTheme.MUTED);

        pagePanel.add(
            previousButton);

        pagePanel.add(
            pageLabel);

        pagePanel.add(
            nextButton);

        pagePanel.add(
            countLabel);

        south.add(
            pagePanel,
            BorderLayout.CENTER);

        statusLabel.setForeground(
            CourseTheme.MUTED);

        statusLabel.setFont(
            statusLabel
                .getFont()
                .deriveFont(
                    13F));

        south.add(
            statusLabel,
            BorderLayout.SOUTH);

        add(
            south,
            BorderLayout.SOUTH);

        /*
         * =========================
         * 查询事件
         * =========================
         */
        searchButton.addActionListener(
            event ->
                loadPage(
                    1));

        courseCodeField.addActionListener(
            event ->
                loadPage(
                    1));

        courseNameField.addActionListener(
            event ->
                loadPage(
                    1));

        teacherField.addActionListener(
            event ->
                loadPage(
                    1));

        resetButton.addActionListener(
            event ->
                resetFilters());

        previousButton.addActionListener(
            event -> {

                if (currentPage > 1) {

                    loadPage(
                        currentPage - 1);
                }
            });

        nextButton.addActionListener(
            event -> {

                if (currentPage
                    < totalPages) {

                    loadPage(
                        currentPage + 1);
                }
            });

        updatePageButtons();
    }

    /**
     * 外部切换到本标签页时刷新。
     */
    void reload() {

        loadPage(
            currentPage);
    }

    /**
     * 加载某一页。
     */
    private void loadPage(
        int page) {

        if (context.currentSession()
            .isEmpty()) {

            showError(
                "登录状态已失效，请重新登录。");

            return;
        }

        statusLabel.setText(
            "正在查询课程...");

        previousButton.setEnabled(
            false);

        nextButton.setEnabled(
            false);

        CourseSearchRequest request =
            new CourseSearchRequest(
                normalize(
                    courseCodeField
                        .getText()),
                normalize(
                    courseNameField
                        .getText()),
                normalize(
                    teacherField
                        .getText()),
                selectedDepartment(),
                selectedAvailability(),
                page,
                PAGE_SIZE);

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .SEARCH_OFFERINGS,
                        request);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response
                                    .getMessage());

                            return;
                        }

                        if (!(response.getData()
                            instanceof CourseSearchResult
                            result)) {

                            showError(
                                "服务器返回的课程查询数据格式错误。");

                            return;
                        }

                        renderResult(
                            result);

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "课程查询被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            cause == null
                                ? exception.getMessage()
                                : cause.getMessage());
                    }
                }
            };

        worker.execute();
    }

    /**
     * 展示搜索结果。
     */
    private void renderResult(
        CourseSearchResult result) {

        tableModel.setRowCount(
            0);

        for (CourseSearchItem item
            : result.getItems()) {

            tableModel.addRow(
                new Object[]{
                    item.getCourseCode(),
                    item.getCourseName(),
                    item.getClassNo(),
                    item.getDepartmentName(),
                    teacherText(
                        item),
                    item.getCredits(),
                    item.getCourseType(),
                    scheduleText(
                        item),
                    nullableText(
                        item.getLocationName()),
                    nullableText(
                        item.getCampusName()),
                    item.getSelectedCount()
                        + " / "
                        + item.getCapacity(),
                    item.getRemainingCount()
                });
        }

        currentPage =
            result.getPage();

        totalPages =
            result.getTotalPages();

        /*
         * 0 条记录时总页数是 0，
         * 界面显示“第 0 / 0 页”不自然，
         * 所以单独显示。
         */
        if (totalPages == 0) {

            pageLabel.setText(
                "第 0 / 0 页");

        } else {

            pageLabel.setText(
                "第 "
                    + currentPage
                    + " / "
                    + totalPages
                    + " 页");
        }

        countLabel.setText(
            "共 "
                + result.getTotalCount()
                + " 条");

        statusLabel.setText(
            result.getItems()
                .isEmpty()
                ? "当前条件下没有课程。"
                : "查询完成。");
        statusLabel.setForeground(
            CourseTheme.MUTED);
        updatePageButtons();
    }

    /**
     * 重置所有筛选。
     */
    private void resetFilters() {

        courseCodeField.setText(
            "");

        courseNameField.setText(
            "");

        teacherField.setText(
            "");

        departmentBox.setSelectedIndex(
            0);

        availabilityBox.setSelectedIndex(
            0);

        loadPage(
            1);
    }

    /**
     * 院系查询条件。
     */
    private String selectedDepartment() {

        Object value =
            departmentBox
                .getSelectedItem();

        if (value == null
            || "全部院系".equals(
            value)) {

            return null;
        }

        return value.toString();
    }

    /**
     * 余量条件。
     */
    private String selectedAvailability() {

        Object value =
            availabilityBox
                .getSelectedItem();

        if ("有余量".equals(
            value)) {

            return "AVAILABLE";
        }

        if ("已满".equals(
            value)) {

            return "FULL";
        }

        return "ALL";
    }

    /**
     * 更新翻页按钮。
     */
    private void updatePageButtons() {

        previousButton.setEnabled(
            totalPages > 0
                && currentPage > 1);

        nextButton.setEnabled(
            totalPages > 0
                && currentPage
                < totalPages);
    }

    /**
     * 表格列宽。
     */
    private void configureColumnWidths() {

        int[] widths = {
            90,   // 课程号
            150,  // 课程名
            65,   // 教学班
            170,  // 院系
            100,  // 教师
            55,   // 学分
            60,   // 类型
            190,  // 时间
            100,  // 地点
            100,  // 校区
            80,   // 人数
            55    // 余量
        };

        for (int i = 0;
             i < widths.length;
             i++) {

            table.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(
                    widths[i]);
        }
    }

    /**
     * 教师文本。
     */
    private String teacherText(
        CourseSearchItem item) {

        if (item.getTeacherNames()
            .isEmpty()) {

            return "未安排";
        }

        return String.join(
            "、",
            item.getTeacherNames());
    }

    /**
     * 上课时间文本。
     */
    private String scheduleText(
        CourseSearchItem item) {

        if (item.getSchedules()
            .isEmpty()) {

            return "未安排";
        }

        List<String> values =
            new ArrayList<>();

        for (ScheduleInfo schedule
            : item.getSchedules()) {

            values.add(
                scheduleText(
                    schedule));
        }

        return String.join(
            "；",
            values);
    }

    private String scheduleText(
        ScheduleInfo schedule) {

        String pattern =
            switch (
                schedule.getWeekPattern()) {

                case "ODD" ->
                    " 单周";

                case "EVEN" ->
                    " 双周";

                default ->
                    "";
            };

        return dayText(
            schedule.getDayOfWeek())
            + " "
            + schedule.getStartPeriod()
            + "-"
            + schedule.getEndPeriod()
            + "节 "
            + schedule.getStartWeek()
            + "-"
            + schedule.getEndWeek()
            + "周"
            + pattern;
    }

    private String dayText(
        int day) {

        return switch (day) {

            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";

            default ->
                "未知";
        };
    }

    private String normalize(
        String value) {

        if (value == null
            || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private String nullableText(
        String value) {

        if (value == null
            || value.isBlank()) {

            return "未安排";
        }

        return value;
    }

    /**
     * 查询失败。
     */
    private void showError(
        String message) {

        tableModel.setRowCount(
            0);

        statusLabel.setText(
            message == null
                ? "课程查询失败。"
                : message);
        statusLabel.setForeground(
            CourseTheme.DANGER);
        updatePageButtons();
    }
}
