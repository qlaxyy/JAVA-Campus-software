package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseAdminAuditInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Font;
/**
 * 教务强制操作日志页面。
 */
final class CourseAdminAuditPanel
    extends JPanel {

    private static final DateTimeFormatter
        TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss");

    private final ClientContext context;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "编号",
                "操作时间",
                "操作人",
                "学生学号",
                "操作类型",
                "批次 ID",
                "教学班 ID",
                "选课记录 ID",
                "操作原因"
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
        CourseTheme.primaryButton(
            "刷新日志");

    private final JLabel statusLabel =
        new JLabel(" ");

    CourseAdminAuditPanel(
        ClientContext context) {

        this.context =
            context;

        initialiseView();

        loadLogs();
    }

    private void initialiseView() {

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
         * 标题
         * =========================
         */
        JPanel header =
            new JPanel();

        header.setOpaque(false);

        header.setLayout(
            new BoxLayout(
                header,
                BoxLayout.Y_AXIS));

        JPanel titleRow =
            new JPanel(
                new BorderLayout());

        titleRow.setOpaque(false);

        JPanel titleText =
            new JPanel();

        titleText.setOpaque(false);

        titleText.setLayout(
            new BoxLayout(
                titleText,
                BoxLayout.Y_AXIS));

        titleText.add(
            CourseTheme.title(
                "教务操作日志"));

        titleText.add(
            Box.createVerticalStrut(
                5));

        titleText.add(
            CourseTheme.subtitle(
                "查看教务老师执行的强制选课和强制退课记录"));

        titleRow.add(
            titleText,
            BorderLayout.CENTER);

        titleRow.add(
            reloadButton,
            BorderLayout.EAST);

        header.add(
            titleRow);

        add(
            header,
            BorderLayout.NORTH);

        /*
         * =========================
         * 日志表格
         * =========================
         */
        table.setRowHeight(
            30);

        table.setFillsViewportHeight(
            true);

        table.setAutoCreateRowSorter(
            true);

        table.setBackground(
            Color.WHITE);

        table.setForeground(
            CourseTheme.TEXT);

        table.setGridColor(
            CourseTheme.BORDER);

        table.setSelectionBackground(
            CourseTheme.PRIMARY_LIGHT);

        table.setSelectionForeground(
            CourseTheme.TEXT);

        table.getTableHeader()
            .setOpaque(
                true);

        table.getTableHeader()
            .setBackground(
                CourseTheme.NAVY);

        table.getTableHeader()
            .setForeground(
                Color.WHITE);

        table.getTableHeader()
            .setPreferredSize(
                new Dimension(
                    0,
                    34));
        DefaultTableCellRenderer headerRenderer =
            new DefaultTableCellRenderer();

        headerRenderer.setOpaque(
            true);

        headerRenderer.setBackground(
            CourseTheme.NAVY);

        headerRenderer.setForeground(
            Color.WHITE);

        headerRenderer.setHorizontalAlignment(
            SwingConstants.CENTER);

        headerRenderer.setFont(
            table.getTableHeader()
                .getFont()
                .deriveFont(
                    Font.BOLD));

        for (int column = 0;
             column < table.getColumnCount();
             column++) {

            table.getColumnModel()
                .getColumn(column)
                .setHeaderRenderer(
                    headerRenderer);
        }

        table.getColumnModel()
            .getColumn(0)
            .setPreferredWidth(55);

        table.getColumnModel()
            .getColumn(1)
            .setPreferredWidth(145);

        table.getColumnModel()
            .getColumn(8)
            .setPreferredWidth(260);

        JScrollPane scrollPane =
            new JScrollPane(
                table);

        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                CourseTheme.BORDER));

        scrollPane
            .getViewport()
            .setBackground(
                Color.WHITE);

        add(
            scrollPane,
            BorderLayout.CENTER);

        /*
         * =========================
         * 状态栏
         * =========================
         */
        statusLabel.setForeground(
            CourseTheme.MUTED);

        add(
            statusLabel,
            BorderLayout.SOUTH);

        reloadButton.addActionListener(
            event ->
                loadLogs());
    }

    /**
     * 从服务器加载日志。
     */
    private void loadLogs() {

        reloadButton.setEnabled(
            false);

        statusLabel.setText(
            "正在加载教务操作日志...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_LIST_AUDIT_LOGS,
                        null);
                }

                @Override
                protected void done() {

                    reloadButton.setEnabled(
                        true);

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        List<CourseAdminAuditInfo> logs =
                            readLogs(
                                response);

                        renderLogs(
                            logs);

                        statusLabel.setText(
                            "共 "
                                + logs.size()
                                + " 条教务操作记录。");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "日志加载被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载操作日志："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()));

                    } catch (IllegalStateException exception) {

                        showError(
                            exception.getMessage());
                    }
                }
            };

        worker.execute();
    }

    private List<CourseAdminAuditInfo> readLogs(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的日志数据格式错误。");
        }

        List<CourseAdminAuditInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof CourseAdminAuditInfo log)) {

                throw new IllegalStateException(
                    "服务器返回的日志数据格式错误。");
            }

            result.add(
                log);
        }

        return result;
    }

    private void renderLogs(
        List<CourseAdminAuditInfo> logs) {

        tableModel.setRowCount(
            0);

        /*
         * 最新记录显示在最上方。
         */
        for (int index =
             logs.size() - 1;
             index >= 0;
             index--) {

            CourseAdminAuditInfo log =
                logs.get(
                    index);

            tableModel.addRow(
                new Object[]{
                    log.getOperationId(),
                    TIME_FORMATTER.format(
                        log.getOperatedAt()),
                    log.getOperatorUsername(),
                    log.getStudentId(),
                    operationText(
                        log.getOperationType()),
                    nullableId(
                        log.getBatchId()),
                    nullableId(
                        log.getOfferingId()),
                    nullableId(
                        log.getEnrollmentId()),
                    log.getReason()
                });
        }
    }

    private String operationText(
        String operationType) {

        return switch (operationType) {

            case "FORCE_SELECT" ->
                "强制选课";

            case "FORCE_DROP" ->
                "强制退课";
            case "UPDATE_OFFERING" ->
                "修改教学班";
            case "UPDATE_COURSE" ->
                "修改课程";
            case "UPDATE_BATCH" ->
                "修改选课批次";
            default ->
                operationType;
        };
    }

    private Object nullableId(
        Long value) {

        return value == null
            ? "-"
            : value;
    }

    private void showError(
        String message) {

        tableModel.setRowCount(
            0);

        statusLabel.setText(
            message);
    }
}
