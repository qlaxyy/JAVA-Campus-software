package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.course.AdminUpdateOfferingRequest;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * 教务端教学班管理页面。
 */
final class CourseAdminOfferingPanel
    extends JPanel {

    private final ClientContext context;

    private final JComboBox<BatchChoice> batchBox =
        new JComboBox<>();

    private final JTextField keywordField =
        new JTextField(18);

    private final JButton searchButton =
        CourseTheme.quietButton(
            "查询");

    private final JButton reloadButton =
        CourseTheme.primaryButton(
            "刷新");
    private final JButton editButton =
        CourseTheme.primaryButton(
            "修改选中教学班");
    private final JLabel statusLabel =
        new JLabel(" ");

    private final List<OfferingRow> allRows =
        new ArrayList<>();

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

    CourseAdminOfferingPanel(
        ClientContext context) {

        this.context =
            context;

        initialiseView();

        loadBatches();
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
         * 顶部区域
         * =========================
         */
        JPanel topPanel =
            new JPanel();

        topPanel.setOpaque(false);

        topPanel.setLayout(
            new BoxLayout(
                topPanel,
                BoxLayout.Y_AXIS));

        topPanel.add(
            CourseTheme.title(
                "教学班管理"));

        topPanel.add(
            Box.createVerticalStrut(
                5));

        topPanel.add(
            CourseTheme.subtitle(
                "按选课批次查看课程教学班、容量和当前状态"));

        topPanel.add(
            Box.createVerticalStrut(
                14));

        topPanel.add(
            createToolbar());

        add(
            topPanel,
            BorderLayout.NORTH);

        /*
         * =========================
         * 表格
         * =========================
         */
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

        keywordField.addActionListener(
            event ->
                applyFilter());

        searchButton.addActionListener(
            event ->
                applyFilter());

        reloadButton.addActionListener(
            event ->
                loadOfferings());

        editButton.addActionListener(
            event ->
                editSelectedOffering());
    }

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
                300,
                34));

        panel.add(
            new JLabel(
                "选课批次："));

        panel.add(
            batchBox);

        panel.add(
            new JLabel(
                "关键词："));

        panel.add(
            keywordField);

        panel.add(
            searchButton);

        panel.add(
            reloadButton);
        panel.add(
            editButton);
        return panel;
    }

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
            .setPreferredWidth(75);

        table.getColumnModel()
            .getColumn(2)
            .setPreferredWidth(135);

        table.getColumnModel()
            .getColumn(4)
            .setPreferredWidth(110);

        table.getColumnModel()
            .getColumn(5)
            .setPreferredWidth(190);

        table.getColumnModel()
            .getColumn(6)
            .setPreferredWidth(110);
    }

    /**
     * 加载选课批次。
     */
    private void loadBatches() {

        batchBox.setEnabled(false);
        reloadButton.setEnabled(false);

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
                            batchBox.getItemCount() > 0;

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

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载选课批次："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()));

                    } finally {

                        updatingBatches =
                            false;
                    }
                }
            };

        worker.execute();
    }

    /**
     * 加载当前批次教学班。
     */
    private void loadOfferings() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null) {

            return;
        }

        reloadButton.setEnabled(false);
        searchButton.setEnabled(false);

        statusLabel.setText(
            "正在加载教学班...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_LIST_OFFERINGS,
                        new BatchRequest(
                            batch.batchId()));
                }

                @Override
                protected void done() {

                    reloadButton.setEnabled(true);
                    searchButton.setEnabled(true);

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

                        applyFilter();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载教学班被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载教学班："
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

    private void readOfferings(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的教学班数据格式错误。");
        }

        allRows.clear();

        for (Object value : values) {

            if (!(value
                instanceof CourseInfo course)) {

                throw new IllegalStateException(
                    "服务器返回的教学班数据格式错误。");
            }

            for (OfferingInfo offering
                : course.getOfferings()) {

                allRows.add(
                    new OfferingRow(
                        offering.getOfferingId(),
                        course.getCourseCode(),
                        course.getCourseName(),
                        offering.getClassNo(),
                        teacherText(offering),
                        scheduleText(offering),
                        nullableText(
                            offering.getLocationName()),
                        offering.getSelectedCount(),
                        offering.getCapacity(),
                        offering.getRemainingCount(),
                        availabilityText(
                            offering
                                .getAvailabilityStatus())));
            }
        }
    }

    /**
     * 根据关键词过滤。
     */
    private void applyFilter() {

        String keyword =
            keywordField
                .getText()
                .trim()
                .toLowerCase(
                    Locale.ROOT);

        tableModel.setRowCount(
            0);

        int visibleCount =
            0;

        for (OfferingRow row : allRows) {

            if (!keyword.isBlank()
                && !row.searchText()
                .contains(keyword)) {

                continue;
            }

            tableModel.addRow(
                new Object[]{
                    row.offeringId(),
                    row.courseCode(),
                    row.courseName(),
                    row.classNo(),
                    row.teacherNames(),
                    row.schedule(),
                    row.location(),
                    row.selectedCount(),
                    row.capacity(),
                    row.remainingCount(),
                    row.status()
                });

            visibleCount++;
        }

        statusLabel.setText(
            "共显示 "
                + visibleCount
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
                    + "节");
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

    private String availabilityText(
        String status) {

        return switch (status) {

            case "AVAILABLE" -> "可选";
            case "FULL" -> "人数已满";
            case "OFFERING_CLOSED" -> "已关闭";
            case "SELECTED" -> "已选";
            case "COURSE_ALREADY_SELECTED" ->
                "该课程已选";
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
    /**
     * 打开教学班修改窗口。
     */
    private void editSelectedOffering() {

        int selectedViewRow =
            table.getSelectedRow();

        if (selectedViewRow < 0) {

            JOptionPane.showMessageDialog(
                this,
                "请先在表格中选择一个教学班。",
                "未选择教学班",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null) {

            return;
        }

        int selectedModelRow =
            table.convertRowIndexToModel(
                selectedViewRow);

        long offeringId =
            ((Number)
                tableModel.getValueAt(
                    selectedModelRow,
                    0))
                .longValue();

        String courseName =
            String.valueOf(
                tableModel.getValueAt(
                    selectedModelRow,
                    2));

        String classNo =
            String.valueOf(
                tableModel.getValueAt(
                    selectedModelRow,
                    3));

        int selectedCount =
            ((Number)
                tableModel.getValueAt(
                    selectedModelRow,
                    7))
                .intValue();

        int currentCapacity =
            ((Number)
                tableModel.getValueAt(
                    selectedModelRow,
                    8))
                .intValue();

        String currentStatus =
            String.valueOf(
                tableModel.getValueAt(
                    selectedModelRow,
                    10));

        JSpinner capacitySpinner =
            new JSpinner(
                new SpinnerNumberModel(
                    currentCapacity,
                    selectedCount,
                    10000,
                    1));

        JCheckBox openCheckBox =
            new JCheckBox(
                "允许学生选课",
                !"已关闭".equals(
                    currentStatus));

        JTextField reasonField =
            new JTextField();

        JPanel form =
            new JPanel(
                new GridLayout(
                    0,
                    2,
                    10,
                    10));

        form.add(
            new JLabel(
                "课程："));

        form.add(
            new JLabel(
                courseName
                    + "（"
                    + classNo
                    + "班）"));

        form.add(
            new JLabel(
                "教学班 ID："));

        form.add(
            new JLabel(
                String.valueOf(
                    offeringId)));

        form.add(
            new JLabel(
                "当前已选人数："));

        form.add(
            new JLabel(
                String.valueOf(
                    selectedCount)));

        form.add(
            new JLabel(
                "教学班容量："));

        form.add(
            capacitySpinner);

        form.add(
            new JLabel(
                "开放状态："));

        form.add(
            openCheckBox);

        form.add(
            new JLabel(
                "修改原因："));

        form.add(
            reasonField);

        int result =
            JOptionPane.showConfirmDialog(
                this,
                form,
                "修改教学班设置",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result
            != JOptionPane.OK_OPTION) {

            return;
        }

        String reason =
            reasonField
                .getText()
                .trim();

        if (reason.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "请输入修改原因。",
                "输入不完整",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        int capacity =
            ((Number)
                capacitySpinner.getValue())
                .intValue();

        submitOfferingUpdate(
            batch.batchId(),
            offeringId,
            capacity,
            openCheckBox.isSelected(),
            reason);
    }

    /**
     * 提交教学班修改。
     */
    private void submitOfferingUpdate(
        long batchId,
        long offeringId,
        int capacity,
        boolean open,
        String reason) {

        editButton.setEnabled(
            false);

        statusLabel.setText(
            "正在修改教学班设置...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_UPDATE_OFFERING,
                        new AdminUpdateOfferingRequest(
                            batchId,
                            offeringId,
                            capacity,
                            open,
                            reason));
                }

                @Override
                protected void done() {

                    editButton.setEnabled(
                        true);

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            CourseAdminOfferingPanel.this,
                            response.getMessage(),
                            response.isSuccess()
                                ? "修改成功"
                                : "修改失败",
                            response.isSuccess()
                                ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE);

                        if (response.isSuccess()) {

                            loadOfferings();
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "教学班修改被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法修改教学班："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()));
                    }
                }
            };

        worker.execute();
    }
    private void showError(
        String message) {

        allRows.clear();

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

    private record OfferingRow(
        long offeringId,
        String courseCode,
        String courseName,
        String classNo,
        String teacherNames,
        String schedule,
        String location,
        int selectedCount,
        int capacity,
        int remainingCount,
        String status) {

        String searchText() {

            return (
                courseCode
                    + " "
                    + courseName
                    + " "
                    + classNo
                    + " "
                    + teacherNames
                    + " "
                    + offeringId)
                .toLowerCase(
                    Locale.ROOT);
        }
    }
}
