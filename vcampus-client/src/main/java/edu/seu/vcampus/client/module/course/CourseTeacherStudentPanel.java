package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.TeacherListStudentsRequest;
import edu.seu.vcampus.common.course.TeacherStudentInfo;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教师端学生名单页面。
 */
final class CourseTeacherStudentPanel
    extends JPanel {

    private final ClientContext context;

    private final JComboBox<BatchChoice>
        batchBox =
        new JComboBox<>();

    private final JComboBox<OfferingChoice>
        offeringBox =
        new JComboBox<>();

    private final JButton reloadButton =
        CourseTheme.primaryButton(
            "刷新名单");

    private final JLabel statusLabel =
        new JLabel(" ");

    private boolean updatingBatches;

    private boolean updatingOfferings;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "序号",
                "学号",
                "选课记录 ID",
                "批次 ID",
                "教学班 ID"
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

    CourseTeacherStudentPanel(
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
                "学生名单"));

        topPanel.add(
            Box.createVerticalStrut(
                5));

        topPanel.add(
            CourseTheme.subtitle(
                "查看本人负责教学班中的已选学生"));

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

        offeringBox.addActionListener(
            event -> {

                if (!updatingOfferings) {

                    loadStudents();
                }
            });

        reloadButton.addActionListener(
            event ->
                loadStudents());
    }

    /**
     * 创建顶部操作栏。
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
                280,
                34));

        offeringBox.setPreferredSize(
            new Dimension(
                390,
                34));

        panel.add(
            new JLabel(
                "选课批次："));

        panel.add(
            batchBox);

        panel.add(
            new JLabel(
                "教学班："));

        panel.add(
            offeringBox);

        panel.add(
            reloadButton);

        return panel;
    }

    /**
     * 初始化表格。
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

        DefaultTableCellRenderer
            headerRenderer =
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

        DefaultTableCellRenderer
            centreRenderer =
            new DefaultTableCellRenderer();

        centreRenderer.setHorizontalAlignment(
            SwingConstants.CENTER);

        for (int column = 0;
             column < table.getColumnCount();
             column++) {

            table.getColumnModel()
                .getColumn(column)
                .setCellRenderer(
                    centreRenderer);
        }

        table.getColumnModel()
            .getColumn(0)
            .setPreferredWidth(
                55);

        table.getColumnModel()
            .getColumn(1)
            .setPreferredWidth(
                140);

        table.getColumnModel()
            .getColumn(2)
            .setPreferredWidth(
                130);
    }

    /**
     * 加载选课批次。
     */
    private void loadBatches() {

        batchBox.setEnabled(
            false);

        offeringBox.setEnabled(
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
                                instanceof SelectionBatchInfo
                                batch)) {

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
                                "无法加载选课批次",
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
     * 加载教师在当前批次负责的教学班。
     */
    private void loadOfferings() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null) {

            clearOfferings();

            statusLabel.setText(
                "请选择选课批次。");

            return;
        }

        offeringBox.setEnabled(
            false);

        reloadButton.setEnabled(
            false);

        tableModel.setRowCount(
            0);

        statusLabel.setText(
            "正在加载教学班...");

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
                                "无法加载教学班",
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
     * 读取服务器返回的教学班。
     */
    private void readOfferings(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的教学班数据格式错误。");
        }

        updatingOfferings =
            true;

        offeringBox.removeAllItems();

        for (Object value : values) {

            if (!(value
                instanceof CourseInfo course)) {

                updatingOfferings =
                    false;

                throw new IllegalStateException(
                    "服务器返回的教学班数据格式错误。");
            }

            for (OfferingInfo offering
                : course.getOfferings()) {

                offeringBox.addItem(
                    new OfferingChoice(
                        offering.getOfferingId(),
                        course.getCourseCode()
                            + " "
                            + course.getCourseName()
                            + " - "
                            + offering.getClassNo()
                            + "（ID："
                            + offering.getOfferingId()
                            + "）"));
            }
        }

        if (offeringBox.getItemCount()
            > 0) {

            offeringBox.setSelectedIndex(
                0);
        }

        updatingOfferings =
            false;

        boolean hasOfferings =
            offeringBox.getItemCount()
                > 0;

        offeringBox.setEnabled(
            hasOfferings);

        reloadButton.setEnabled(
            hasOfferings);

        if (hasOfferings) {

            loadStudents();

        } else {

            tableModel.setRowCount(
                0);

            statusLabel.setText(
                "当前批次没有分配给你的教学班。");
        }
    }

    /**
     * 加载选中教学班的学生名单。
     */
    private void loadStudents() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        OfferingChoice offering =
            (OfferingChoice)
                offeringBox.getSelectedItem();

        if (batch == null
            || offering == null) {

            tableModel.setRowCount(
                0);

            reloadButton.setEnabled(
                false);

            return;
        }

        reloadButton.setEnabled(
            false);

        statusLabel.setText(
            "正在加载学生名单...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .TEACHER_LIST_STUDENTS,
                        new TeacherListStudentsRequest(
                            batch.batchId(),
                            offering.offeringId()));
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

                        readStudents(
                            response);

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载学生名单被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            errorMessage(
                                "无法加载学生名单",
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
     * 将学生名单显示到表格。
     */
    private void readStudents(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的学生名单格式错误。");
        }

        tableModel.setRowCount(
            0);

        int number =
            1;

        for (Object value : values) {

            if (!(value
                instanceof TeacherStudentInfo student)) {

                throw new IllegalStateException(
                    "服务器返回的学生名单格式错误。");
            }

            tableModel.addRow(
                new Object[]{
                    number++,
                    student.getStudentId(),
                    student.getEnrollmentId(),
                    student.getBatchId(),
                    student.getOfferingId()
                });
        }

        statusLabel.setText(
            "该教学班共有 "
                + values.size()
                + " 名学生。");
    }

    /**
     * 清空教学班选择。
     */
    private void clearOfferings() {

        updatingOfferings =
            true;

        offeringBox.removeAllItems();

        updatingOfferings =
            false;

        offeringBox.setEnabled(
            false);

        reloadButton.setEnabled(
            false);

        tableModel.setRowCount(
            0);
    }

    /**
     * 显示错误。
     */
    private void showError(
        String message) {

        tableModel.setRowCount(
            0);

        statusLabel.setText(
            message == null
                ? "操作失败。"
                : message);
    }

    /**
     * 提取异步任务异常信息。
     */
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
            + "："
            + message;
    }

    /**
     * 批次下拉框对象。
     */
    private record BatchChoice(
        long batchId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }

    /**
     * 教学班下拉框对象。
     */
    private record OfferingChoice(
        long offeringId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }
}
