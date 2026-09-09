package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.AdminUpdateBatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教务选课批次管理页面。
 */
final class CourseAdminBatchPanel
    extends JPanel {

    private static final DateTimeFormatter
        TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "批次 ID",
                "学期",
                "批次名称",
                "批次类型",
                "开始时间",
                "结束时间",
                "状态",
                "允许选课",
                "允许退课"
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

    private final JLabel statusLabel =
        new JLabel(
            "正在加载选课批次……");

    private final List<SelectionBatchInfo> batches =
        new ArrayList<>();

    CourseAdminBatchPanel(
        ClientContext context) {

        this.context =
            context;

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

        setBorder(
            BorderFactory.createEmptyBorder(
                18,
                18,
                18,
                18));

        setBackground(
            CourseTheme.BACKGROUND);

        JPanel header =
            new JPanel(
                new BorderLayout(
                    0,
                    12));

        header.setOpaque(
            false);

        JPanel titlePanel =
            new JPanel(
                new GridLayout(
                    0,
                    1,
                    0,
                    4));

        titlePanel.setOpaque(
            false);

        titlePanel.add(
            CourseTheme.title(
                "选课批次管理"));

        titlePanel.add(
            CourseTheme.subtitle(
                "修改批次时间、状态以及学生选课和退课权限"));

        header.add(
            titlePanel,
            BorderLayout.NORTH);

        JPanel toolbar =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    10,
                    0));

        toolbar.setOpaque(
            false);

        JButton refreshButton =
            CourseTheme.quietButton(
                "刷新");

        JButton editButton =
            CourseTheme.primaryButton(
                "修改选中批次");

        toolbar.add(
            refreshButton);

        toolbar.add(
            editButton);

        header.add(
            toolbar,
            BorderLayout.CENTER);

        add(
            header,
            BorderLayout.NORTH);

        table.setRowHeight(
            30);

        table.setSelectionMode(
            javax.swing.ListSelectionModel
                .SINGLE_SELECTION);

        table.setFillsViewportHeight(
            true);

        DefaultTableCellRenderer
            headerRenderer =
            new DefaultTableCellRenderer();

        headerRenderer.setBackground(
            CourseTheme.NAVY);

        headerRenderer.setForeground(
            Color.WHITE);

        headerRenderer.setOpaque(
            true);

        headerRenderer.setHorizontalAlignment(
            SwingConstants.CENTER);

        headerRenderer.setFont(
            table.getTableHeader()
                .getFont()
                .deriveFont(
                    Font.BOLD));

        for (int column = 0;
             column < table
                 .getColumnModel()
                 .getColumnCount();
             column++) {

            table.getColumnModel()
                .getColumn(column)
                .setHeaderRenderer(
                    headerRenderer);
        }

        JScrollPane scrollPane =
            new JScrollPane(
                table);

        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                CourseTheme.BORDER));

        add(
            scrollPane,
            BorderLayout.CENTER);

        statusLabel.setForeground(
            CourseTheme.MUTED);

        add(
            statusLabel,
            BorderLayout.SOUTH);

        refreshButton.addActionListener(
            event ->
                loadBatches());

        editButton.addActionListener(
            event ->
                editSelectedBatch());
    }

    /**
     * 从服务器加载批次。
     */
    private void loadBatches() {

        statusLabel.setText(
            "正在加载选课批次……");

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

                        batches.clear();
                        batches.addAll(
                            readBatches(
                                response));

                        renderBatches();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载选课批次被中断。");

                    } catch (ExecutionException
                             | IllegalStateException exception) {

                        Throwable cause =
                            exception instanceof
                                ExecutionException
                                ? exception.getCause()
                                : exception;

                        showError(
                            "无法加载选课批次："
                                + messageOf(
                                cause));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 显示批次列表。
     */
    private void renderBatches() {

        tableModel.setRowCount(
            0);

        for (SelectionBatchInfo batch
            : batches) {

            tableModel.addRow(
                new Object[]{
                    batch.getBatchId(),
                    batch.getSemester(),
                    batch.getBatchName(),
                    batchTypeText(
                        batch.getBatchType()),
                    batch.getStartTime()
                        .format(
                        TIME_FORMATTER),
                    batch.getEndTime()
                        .format(
                        TIME_FORMATTER),
                    statusText(
                        batch.getStatus()),
                    batch.isAllowSelect()
                        ? "是"
                        : "否",
                    batch.isAllowDrop()
                        ? "是"
                        : "否"
                });
        }

        statusLabel.setText(
            "共加载 "
                + batches.size()
                + " 个选课批次");
    }

    /**
     * 修改选中的批次。
     */
    private void editSelectedBatch() {

        int selectedRow =
            table.getSelectedRow();

        if (selectedRow < 0) {

            JOptionPane.showMessageDialog(
                this,
                "请先选择一个选课批次。",
                "未选择批次",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        long batchId =
            ((Number)
                tableModel.getValueAt(
                    selectedRow,
                    0))
                .longValue();

        SelectionBatchInfo batch =
            findBatch(
                batchId);

        if (batch == null) {

            showError(
                "未找到选中的批次，请刷新后重试。");

            return;
        }

        JTextField semesterField =
            new JTextField(
                batch.getSemester());

        JTextField nameField =
            new JTextField(
                batch.getBatchName());

        JComboBox<SelectionBatchType>
            typeBox =
            new JComboBox<>(
                SelectionBatchType.values());

        typeBox.setSelectedItem(
            batch.getBatchType());

        JTextField startTimeField =
            new JTextField(
                batch.getStartTime()
                    .format(
                        TIME_FORMATTER));

        JTextField endTimeField =
            new JTextField(
                batch.getEndTime()
                    .format(
                        TIME_FORMATTER));

        JComboBox<SelectionBatchStatus>
            statusBox =
            new JComboBox<>(
                SelectionBatchStatus.values());

        statusBox.setSelectedItem(
            batch.getStatus());

        JCheckBox allowSelectBox =
            new JCheckBox(
                "允许学生选课",
                batch.isAllowSelect());

        JCheckBox allowDropBox =
            new JCheckBox(
                "允许学生退课",
                batch.isAllowDrop());

        JTextArea reasonArea =
            new JTextArea(
                3,
                24);

        reasonArea.setLineWrap(
            true);

        reasonArea.setWrapStyleWord(
            true);

        JPanel form =
            new JPanel(
                new GridLayout(
                    0,
                    2,
                    10,
                    8));

        form.add(
            new JLabel(
                "批次 ID："));

        form.add(
            new JLabel(
                String.valueOf(
                    batch.getBatchId())));

        form.add(
            new JLabel(
                "学期："));

        form.add(
            semesterField);

        form.add(
            new JLabel(
                "批次名称："));

        form.add(
            nameField);

        form.add(
            new JLabel(
                "批次类型："));

        form.add(
            typeBox);

        form.add(
            new JLabel(
                "开始时间："));

        form.add(
            startTimeField);

        form.add(
            new JLabel(
                "结束时间："));

        form.add(
            endTimeField);

        form.add(
            new JLabel(
                "批次状态："));

        form.add(
            statusBox);

        form.add(
            new JLabel(
                "选课权限："));

        form.add(
            allowSelectBox);

        form.add(
            new JLabel(
                "退课权限："));

        form.add(
            allowDropBox);

        form.add(
            new JLabel(
                "修改原因："));

        form.add(
            new JScrollPane(
                reasonArea));

        int option =
            JOptionPane.showConfirmDialog(
                this,
                form,
                "修改选课批次",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option
            != JOptionPane.OK_OPTION) {

            return;
        }

        LocalDateTime startTime;
        LocalDateTime endTime;

        try {

            startTime =
                LocalDateTime.parse(
                    startTimeField
                        .getText()
                        .trim(),
                    TIME_FORMATTER);

            endTime =
                LocalDateTime.parse(
                    endTimeField
                        .getText()
                        .trim(),
                    TIME_FORMATTER);

        } catch (DateTimeParseException exception) {

            JOptionPane.showMessageDialog(
                this,
                "时间格式必须为 yyyy-MM-dd HH:mm，"
                    + "例如 2026-09-05 08:00。",
                "时间格式错误",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        if (!startTime.isBefore(
            endTime)) {

            JOptionPane.showMessageDialog(
                this,
                "开始时间必须早于结束时间。",
                "时间设置错误",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        String semester =
            semesterField.getText()
                .trim();

        String batchName =
            nameField.getText()
                .trim();

        if (semester.isEmpty()
            || batchName.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "学期和批次名称不能为空。",
                "输入错误",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        submitUpdate(
            new AdminUpdateBatchRequest(
                batch.getBatchId(),
                semester,
                batchName,
                (SelectionBatchType)
                    typeBox.getSelectedItem(),
                startTime,
                endTime,
                (SelectionBatchStatus)
                    statusBox.getSelectedItem(),
                allowSelectBox.isSelected(),
                allowDropBox.isSelected(),
                reasonArea.getText()
                    .trim()));
    }

    /**
     * 提交批次修改。
     */
    private void submitUpdate(
        AdminUpdateBatchRequest request) {

        statusLabel.setText(
            "正在保存选课批次……");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_UPDATE_BATCH,
                        request);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            CourseAdminBatchPanel.this,
                            response.getMessage(),
                            response.isSuccess()
                                ? "修改成功"
                                : "修改失败",
                            response.isSuccess()
                                ? JOptionPane
                                .INFORMATION_MESSAGE
                                : JOptionPane
                                .WARNING_MESSAGE);

                        if (response.isSuccess()) {

                            loadBatches();
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "保存选课批次被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            "无法保存选课批次："
                                + messageOf(
                                exception.getCause()));
                    }
                }
            };

        worker.execute();
    }

    private List<SelectionBatchInfo> readBatches(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的批次数据格式错误。");
        }

        List<SelectionBatchInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof SelectionBatchInfo batch)) {

                throw new IllegalStateException(
                    "服务器返回的批次数据格式错误。");
            }

            result.add(
                batch);
        }

        return result;
    }

    private SelectionBatchInfo findBatch(
        long batchId) {

        for (SelectionBatchInfo batch
            : batches) {

            if (batch.getBatchId()
                == batchId) {

                return batch;
            }
        }

        return null;
    }

    private String batchTypeText(
        SelectionBatchType type) {

        return switch (type) {

            case PRE_SELECTION ->
                "预选";

            case ADD_DROP ->
                "补退选";

            case RETAKE ->
                "重修";
        };
    }

    private String statusText(
        SelectionBatchStatus status) {

        return switch (status) {

            case NOT_STARTED ->
                "未开始";

            case OPEN ->
                "进行中";

            case ENDED ->
                "已结束";
        };
    }

    private void showError(
        String message) {

        statusLabel.setText(
            message);

        JOptionPane.showMessageDialog(
            this,
            message,
            "选课批次管理",
            JOptionPane.ERROR_MESSAGE);
    }

    private String messageOf(
        Throwable throwable) {

        if (throwable == null
            || throwable.getMessage() == null) {

            return "未知错误";
        }

        return throwable.getMessage();
    }
}
