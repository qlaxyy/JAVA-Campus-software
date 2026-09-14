package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseGradeInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * 学生查看本人课程成绩的页面。
 */
final class CourseStudentGradePanel
    extends JPanel {

    private static final DateTimeFormatter
        TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[] {
                "课程代码",
                "课程名称",
                "教学班",
                "平时成绩",
                "期末成绩",
                "平时比例",
                "期末比例",
                "总成绩",
                "结果",
                "录入时间"
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

    private final JButton reloadButton =
        CourseTheme.quietButton(
            "刷新成绩");

    private final JLabel statusLabel =
        new JLabel(
            "正在加载成绩……");

    CourseStudentGradePanel(
        ClientContext context) {

        this.context =
            context;

        initialiseView();
        bindEvents();
        loadGrades();
    }

    /**
     * 初始化页面。
     */
    private void initialiseView() {

        setLayout(
            new BorderLayout(
                0,
                16));

        setBackground(
            CourseTheme.BACKGROUND);

        setBorder(
            BorderFactory.createEmptyBorder(
                24,
                24,
                24,
                24));

        add(
            createHeader(),
            BorderLayout.NORTH);

        configureTable();

        JScrollPane scrollPane =
            new JScrollPane(
                table);

        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                CourseTheme.BORDER));

        scrollPane.getViewport()
            .setBackground(
                Color.WHITE);

        add(
            scrollPane,
            BorderLayout.CENTER);

        statusLabel.setForeground(
            CourseTheme.MUTED);

        statusLabel.setBorder(
            BorderFactory.createEmptyBorder(
                4,
                4,
                0,
                4));

        add(
            statusLabel,
            BorderLayout.SOUTH);
    }

    /**
     * 创建页面标题区域。
     */
    private JPanel createHeader() {

        JPanel header =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

        header.setOpaque(false);

        JPanel titlePanel =
            new JPanel(
                new BorderLayout(
                    0,
                    6));

        titlePanel.setOpaque(false);

        titlePanel.add(
            CourseTheme.title(
                "我的成绩"),
            BorderLayout.NORTH);

        titlePanel.add(
            CourseTheme.pageSubtitle(
                "查看本人课程成绩和成绩构成"),
            BorderLayout.CENTER);

        header.add(
            titlePanel,
            BorderLayout.CENTER);

        JPanel actionPanel =
            new JPanel(
                new FlowLayout(
                    FlowLayout.RIGHT,
                    0,
                    0));

        actionPanel.setOpaque(false);



        actionPanel.add(
            reloadButton);

        header.add(
            actionPanel,
            BorderLayout.EAST);

        return header;
    }

    /**
     * 配置成绩表格。
     */
    private void configureTable() {

        table.setRowHeight(
            34);

        table.setAutoResizeMode(
            JTable.AUTO_RESIZE_OFF);

        table.setFillsViewportHeight(
            true);

        table.setSelectionMode(
            javax.swing.ListSelectionModel
                .SINGLE_SELECTION);

        table.setBackground(
            Color.WHITE);

        table.setForeground(
            Color.BLACK);

        table.setGridColor(
            CourseTheme.BORDER);

        table.setSelectionBackground(
            new Color(
                226,
                236,
                248));

        table.setSelectionForeground(
            Color.BLACK);

        table.getTableHeader()
            .setReorderingAllowed(
                false);

        DefaultTableCellRenderer
            headerRenderer =
            new DefaultTableCellRenderer();

        headerRenderer.setHorizontalAlignment(
            SwingConstants.CENTER);

        headerRenderer.setBackground(
            new Color(
                45,
                62,
                80));

        headerRenderer.setForeground(
            Color.WHITE);

        headerRenderer.setFont(
            table.getFont()
                .deriveFont(
                    Font.BOLD));

        DefaultTableCellRenderer
            centreRenderer =
            new DefaultTableCellRenderer();

        centreRenderer.setHorizontalAlignment(
            SwingConstants.CENTER);

        int columnCount =
            table.getColumnModel()
                .getColumnCount();

        for (int index = 0;
             index < columnCount;
             index++) {

            table.getColumnModel()
                .getColumn(
                    index)
                .setHeaderRenderer(
                    headerRenderer);

            table.getColumnModel()
                .getColumn(
                    index)
                .setCellRenderer(
                    centreRenderer);
        }

        setColumnWidth(
            0,
            100);

        setColumnWidth(
            1,
            170);

        setColumnWidth(
            2,
            100);

        setColumnWidth(
            3,
            90);

        setColumnWidth(
            4,
            90);

        setColumnWidth(
            5,
            90);

        setColumnWidth(
            6,
            90);

        setColumnWidth(
            7,
            90);

        setColumnWidth(
            8,
            90);

        setColumnWidth(
            9,
            150);
    }

    /**
     * 设置列宽。
     */
    private void setColumnWidth(
        int columnIndex,
        int width) {

        table.getColumnModel()
            .getColumn(
                columnIndex)
            .setPreferredWidth(
                width);
    }

    /**
     * 绑定页面事件。
     */
    private void bindEvents() {

        reloadButton.addActionListener(
            event ->
                loadGrades());
    }

    /**
     * 从服务器加载当前学生的成绩。
     */
    private void loadGrades() {

        reloadButton.setEnabled(
            false);

        statusLabel.setText(
            "正在加载成绩……");

        new SwingWorker<Response, Void>() {

            @Override
            protected Response doInBackground()
                throws Exception {

                return context.send(
                    CourseActions.STUDENT_LIST_GRADES,
                    null);
            }

            @Override
            protected void done() {

                try {

                    Response response =
                        get();

                    if (!response.isSuccess()) {

                        showError(
                            messageOf(
                                response,
                                "成绩加载失败。"));

                        return;
                    }

                    if (!(response.getData()
                        instanceof List<?> values)) {

                        showError(
                            "服务器返回的成绩数据格式错误。");

                        return;
                    }

                    updateTable(
                        values);

                } catch (InterruptedException exception) {

                    Thread.currentThread()
                        .interrupt();

                    showError(
                        "成绩加载已中断。");

                } catch (ExecutionException exception) {

                    showError(
                        "成绩加载失败："
                            + rootMessage(
                            exception));

                } finally {

                    reloadButton.setEnabled(
                        true);
                }
            }
        }.execute();
    }

    /**
     * 更新成绩表格。
     */
    private void updateTable(
        List<?> values) {

        tableModel.setRowCount(
            0);

        int recordedCount =
            0;

        for (Object value : values) {

            if (!(value
                instanceof CourseGradeInfo grade)) {

                continue;
            }

            if (grade.isRecorded()) {

                recordedCount++;
            }

            tableModel.addRow(
                new Object[] {
                    grade.getCourseCode(),
                    grade.getCourseName(),
                    grade.getClassNo(),
                    scoreText(
                        grade.getUsualScore()),
                    scoreText(
                        grade.getFinalExamScore()),
                    grade.getUsualWeightPercent()
                        + "%",
                    grade.getFinalExamWeightPercent()
                        + "%",
                    scoreText(
                        grade.getTotalScore()),
                    resultText(
                        grade),
                    timeText(
                        grade)
                });
        }

        if (tableModel.getRowCount() == 0) {

            statusLabel.setText(
                "当前没有可以查看的课程成绩。");

            return;
        }

        statusLabel.setText(
            "共 "
                + tableModel.getRowCount()
                + " 门课程，已录入 "
                + recordedCount
                + " 门。");
    }

    /**
     * 格式化成绩。
     */
    private String scoreText(
        Double score) {

        if (score == null) {

            return "暂未录入";
        }

        return String.format(
            Locale.ROOT,
            "%.2f",
            score);
    }

    /**
     * 格式化成绩结果。
     */
    private String resultText(
        CourseGradeInfo grade) {

        if (!grade.isRecorded()) {

            return "暂未录入";
        }

        if (grade.isPassed()) {

            return "通过";
        }

        return "未通过";
    }

    /**
     * 格式化录入时间。
     */
    private String timeText(
        CourseGradeInfo grade) {

        if (grade.getRecordedAt() == null) {

            return "--";
        }

        return TIME_FORMATTER.format(
            grade.getRecordedAt());
    }

    /**
     * 显示错误信息。
     */
    private void showError(
        String message) {

        statusLabel.setText(
            message);

        JOptionPane.showMessageDialog(
            this,
            message,
            "成绩查询",
            JOptionPane.ERROR_MESSAGE);
    }

    private String messageOf(
        Response response,
        String fallback) {

        String message =
            response.getMessage();

        if (message == null
            || message.isBlank()) {

            return fallback;
        }

        return message;
    }

    private String rootMessage(
        Throwable throwable) {

        Throwable current =
            throwable;

        while (current.getCause() != null) {

            current =
                current.getCause();
        }

        String message =
            current.getMessage();

        if (message == null
            || message.isBlank()) {

            return current.getClass()
                .getSimpleName();
        }

        return message;
    }
}
