package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * 教师端“我的教学班”页面。
 */
final class CourseTeacherOfferingPanel
    extends JPanel {

    private final ClientContext context;

    private final JComboBox<BatchChoice> batchBox =
        new JComboBox<>();

    private final JButton reloadButton =
        CourseTheme.primaryButton(
            "刷新");

    private final JLabel statusLabel =
        new JLabel(
            " ");

    private boolean updatingBatches;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "教学班 ID",
                "课程编号",
                "课程名称",
                "教学班",
                "教师",
                "上课时间",
                "地点",
                "已选人数",
                "容量",
                "剩余",
                "状态"
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

    CourseTeacherOfferingPanel(
        ClientContext context) {

        this.context =
            Objects.requireNonNull(
                context);

        initialiseView();

        loadBatches();
    }

    /**
     * 初始化页面。
     */
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

        JPanel topPanel =
            new JPanel();

        topPanel.setOpaque(
            false);

        topPanel.setLayout(
            new BoxLayout(
                topPanel,
                BoxLayout.Y_AXIS));

        topPanel.add(
            CourseTheme.title(
                "我的教学班"));

        topPanel.add(
            Box.createVerticalStrut(
                5));

        topPanel.add(
            CourseTheme.subtitle(
                "查看本人负责的课程、上课安排和选课人数"));

        topPanel.add(
            Box.createVerticalStrut(
                14));

        topPanel.add(
            createToolbar());

        add(
            topPanel,
            BorderLayout.NORTH);

        initialiseTable();

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

        statusLabel.setForeground(
            CourseTheme.MUTED);

        add(
            statusLabel,
            BorderLayout.SOUTH);

        batchBox.addActionListener(
            event -> {

                if (!updatingBatches) {

                    loadOfferings();
                }
            });

        reloadButton.addActionListener(
            event ->
                loadOfferings());
    }

    /**
     * 创建批次选择工具栏。
     */
    private JPanel createToolbar() {

        CourseTheme.SurfacePanel panel =
            new CourseTheme.SurfacePanel();

        panel.setLayout(
            new FlowLayout(
                FlowLayout.LEFT,
                10,
                10));

        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    4,
                    8,
                    4,
                    8)));

        batchBox.setPreferredSize(
            new Dimension(
                320,
                34));

        panel.add(
            new JLabel(
                "选课批次："));

        panel.add(
            batchBox);

        panel.add(
            reloadButton);

        return panel;
    }

    /**
     * 初始化表格样式。
     */
    private void initialiseTable() {

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
            .setPreferredWidth(
                75);

        table.getColumnModel()
            .getColumn(2)
            .setPreferredWidth(
                140);

        table.getColumnModel()
            .getColumn(4)
            .setPreferredWidth(
                100);

        table.getColumnModel()
            .getColumn(5)
            .setPreferredWidth(
                190);

        table.getColumnModel()
            .getColumn(6)
            .setPreferredWidth(
                120);
    }

    /**
     * 加载选课批次。
     */
    private void loadBatches() {

        batchBox.setEnabled(
            false);

        reloadButton.setEnabled(
            false);

        statusLabel.setText(
            "正在加载选课批次...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.LIST_BATCHES,
                        null);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        if (!(response.getData()
                            instanceof List<?> values)) {

                            showError(
                                "服务器返回的批次数据格式错误。");

                            return;
                        }

                        updatingBatches =
                            true;

                        batchBox.removeAllItems();

                        for (Object value : values) {

                            if (!(value
                                instanceof SelectionBatchInfo batch)) {

                                showError(
                                    "服务器返回的批次数据格式错误。");

                                return;
                            }

                            batchBox.addItem(
                                new BatchChoice(
                                    batch.getBatchId(),
                                    batch.getBatchName()
                                        + "（"
                                        + batch.getStatus()
                                        + "）"));
                        }

                        if (batchBox.getItemCount()
                            > 0) {

                            batchBox.setSelectedIndex(
                                0);
                        }

                        updatingBatches =
                            false;

                        boolean hasBatches =
                            batchBox.getItemCount()
                                > 0;

                        batchBox.setEnabled(
                            hasBatches);

                        reloadButton.setEnabled(
                            hasBatches);

                        if (hasBatches) {

                            loadOfferings();

                        } else {

                            statusLabel.setText(
                                "当前没有选课批次。");
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载选课批次被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            errorMessage(
                                "无法加载选课批次：",
                                exception));

                    } finally {

                        updatingBatches =
                            false;
                    }
                }
            };

        worker.execute();
    }

    /**
     * 加载教师本人负责的教学班。
     */
    private void loadOfferings() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null) {

            return;
        }

        reloadButton.setEnabled(
            false);

        statusLabel.setText(
            "正在加载本人教学班...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .TEACHER_LIST_OFFERINGS,
                        new BatchRequest(
                            batch.batchId()));
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

                        readOfferings(
                            response);

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载教学班被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            errorMessage(
                                "无法加载教学班：",
                                exception));

                    } catch (IllegalStateException exception) {

                        showError(
                            exception.getMessage());
                    }
                }
            };

        worker.execute();
    }

    /**
     * 读取服务器返回的课程和教学班。
     */
    private void readOfferings(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的教学班数据格式错误。");
        }

        tableModel.setRowCount(
            0);

        int offeringCount =
            0;

        for (Object value : values) {

            if (!(value
                instanceof CourseInfo course)) {

                throw new IllegalStateException(
                    "服务器返回的教学班数据格式错误。");
            }

            for (OfferingInfo offering
                : course.getOfferings()) {

                tableModel.addRow(
                    new Object[]{
                        offering.getOfferingId(),
                        course.getCourseCode(),
                        course.getCourseName(),
                        offering.getClassNo(),
                        teacherText(
                            offering),
                        scheduleText(
                            offering),
                        nullableText(
                            offering.getLocationName()),
                        offering.getSelectedCount(),
                        offering.getCapacity(),
                        offering.getRemainingCount(),
                        availabilityText(
                            offering
                                .getAvailabilityStatus())
                    });

                offeringCount++;
            }
        }

        statusLabel.setText(
            offeringCount == 0
                ? "当前批次没有分配给你的教学班。"
                : "共加载 "
                + offeringCount
                + " 个教学班。");
    }

    private String teacherText(
        OfferingInfo offering) {

        if (offering
            .getTeacherNames()
            .isEmpty()) {

            return "未安排";
        }

        return String.join(
            "、",
            offering.getTeacherNames());
    }

    private String scheduleText(
        OfferingInfo offering) {

        if (offering
            .getSchedules()
            .isEmpty()) {

            return "未安排";
        }

        List<String> values =
            new ArrayList<>();

        for (ScheduleInfo schedule
            : offering.getSchedules()) {

            values.add(
                dayText(
                    schedule.getDayOfWeek())
                    + " 第"
                    + schedule.getStartPeriod()
                    + "-"
                    + schedule.getEndPeriod()
                    + "节，第"
                    + schedule.getStartWeek()
                    + "-"
                    + schedule.getEndWeek()
                    + "周"
                    + weekPatternText(
                    schedule.getWeekPattern()));
        }

        return String.join(
            "；",
            values);
    }

    private String dayText(
        int dayOfWeek) {

        return switch (dayOfWeek) {

            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";

            default -> "未知";
        };
    }

    private String weekPatternText(
        String pattern) {

        if ("ODD".equals(
            pattern)) {

            return "（单周）";
        }

        if ("EVEN".equals(
            pattern)) {

            return "（双周）";
        }

        return "";
    }

    private String availabilityText(
        String status) {

        return switch (status) {

            case "AVAILABLE" -> "开放";
            case "FULL" -> "人数已满";
            case "OFFERING_CLOSED" -> "已关闭";
            case "SELECTED" -> "已选";
            case "TIME_CONFLICT" -> "时间冲突";
            case "NOT_ELIGIBLE" -> "不符合条件";

            default -> status;
        };
    }

    private String nullableText(
        String value) {

        return value == null
            || value.isBlank()
            ? "未安排"
            : value;
    }

    private String errorMessage(
        String prefix,
        ExecutionException exception) {

        Throwable cause =
            exception.getCause();

        String message =
            cause == null
                ? exception.getMessage()
                : cause.getMessage();

        return prefix
            + message;
    }

    private void showError(
        String message) {

        tableModel.setRowCount(
            0);

        statusLabel.setText(
            message);
    }

    private record BatchChoice(
        long batchId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }
}
