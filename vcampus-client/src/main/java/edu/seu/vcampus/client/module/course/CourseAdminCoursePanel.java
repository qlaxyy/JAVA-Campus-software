package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.AdminUpdateCourseRequest;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 教务课程管理页面。
 */
final class CourseAdminCoursePanel
    extends JPanel {

    private final ClientContext context;

    private final JComboBox<BatchChoice>
        batchBox =
        new JComboBox<>();

    private final JTextField keywordField =
        new JTextField(18);

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "课程 ID",
                "课程代码",
                "课程名称",
                "学分",
                "课程类型",
                "教学班数量"
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
        new JTable(tableModel);

    private final JLabel statusLabel =
        new JLabel("正在加载选课批次……");

    private final List<CourseInfo> courses =
        new ArrayList<>();

    CourseAdminCoursePanel(
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

        header.setOpaque(false);

        JPanel titlePanel =
            new JPanel(
                new GridLayout(
                    0,
                    1,
                    0,
                    4));

        titlePanel.setOpaque(false);

        titlePanel.add(
            CourseTheme.title(
                "课程管理"));

        titlePanel.add(
            CourseTheme.subtitle(
                "查询并修改课程代码、名称、学分和课程类型"));

        header.add(
            titlePanel,
            BorderLayout.NORTH);

        JPanel toolbar =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    10,
                    0));

        toolbar.setOpaque(false);

        toolbar.add(
            new JLabel(
                "选课批次："));

        batchBox.setPrototypeDisplayValue(
            new BatchChoice(
                -1,
                "2026-2027-1 第一轮选课"));

        toolbar.add(
            batchBox);

        toolbar.add(
            new JLabel(
                "关键词："));

        toolbar.add(
            keywordField);

        JButton searchButton =
            CourseTheme.quietButton(
                "查询");

        JButton refreshButton =
            CourseTheme.quietButton(
                "刷新");

        JButton editButton =
            CourseTheme.primaryButton(
                "修改选中课程");

        toolbar.add(
            searchButton);

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

        batchBox.addActionListener(
            event ->
                loadCourses());

        searchButton.addActionListener(
            event ->
                renderCourses());

        keywordField.addActionListener(
            event ->
                renderCourses());

        refreshButton.addActionListener(
            event ->
                loadCourses());

        editButton.addActionListener(
            event ->
                editSelectedCourse());
    }

    /**
     * 加载选课批次。
     */
    private void loadBatches() {

        statusLabel.setText(
            "正在加载选课批次……");

        batchBox.setEnabled(
            false);

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

                        List<SelectionBatchInfo> batches =
                            readBatches(
                                response);

                        batchBox.removeAllItems();

                        for (SelectionBatchInfo batch
                            : batches) {

                            batchBox.addItem(
                                new BatchChoice(
                                    batch.getBatchId(),
                                    batch.getSemester()
                                        + " "
                                        + batch.getBatchName()));
                        }

                        batchBox.setEnabled(
                            true);

                        if (batches.isEmpty()) {

                            statusLabel.setText(
                                "当前没有选课批次。");

                        } else {

                            batchBox.setSelectedIndex(
                                0);

                            loadCourses();
                        }

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
     * 加载指定批次全部课程。
     */
    private void loadCourses() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null
            || batch.batchId() < 0) {

            return;
        }

        statusLabel.setText(
            "正在加载课程……");

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

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        List<CourseInfo> loaded =
                            readCourses(
                                response);

                        /*
                         * 按课程 ID 去重，避免一门课程
                         * 因多个来源或教学班重复显示。
                         */
                        Map<Long, CourseInfo> unique =
                            new LinkedHashMap<>();

                        for (CourseInfo course
                            : loaded) {

                            unique.putIfAbsent(
                                course.getCourseId(),
                                course);
                        }

                        courses.clear();
                        courses.addAll(
                            unique.values());

                        renderCourses();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载课程被中断。");

                    } catch (ExecutionException
                             | IllegalStateException exception) {

                        Throwable cause =
                            exception instanceof
                                ExecutionException
                                ? exception.getCause()
                                : exception;

                        showError(
                            "无法加载课程："
                                + messageOf(
                                cause));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 根据关键词显示课程。
     */
    private void renderCourses() {

        String keyword =
            keywordField.getText()
                .trim()
                .toLowerCase();

        tableModel.setRowCount(
            0);

        int count = 0;

        for (CourseInfo course
            : courses) {

            if (!matches(
                course,
                keyword)) {

                continue;
            }

            tableModel.addRow(
                new Object[]{
                    course.getCourseId(),
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getCredits(),
                    course.getCourseType(),
                    course.getOfferings().size()
                });

            count++;
        }

        statusLabel.setText(
            "共显示 "
                + count
                + " 门课程");
    }

    /**
     * 编辑当前选中课程。
     */
    private void editSelectedCourse() {

        int selectedRow =
            table.getSelectedRow();

        if (selectedRow < 0) {

            JOptionPane.showMessageDialog(
                this,
                "请先选择一门课程。",
                "未选择课程",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        long courseId =
            ((Number)
                tableModel.getValueAt(
                    selectedRow,
                    0))
                .longValue();

        CourseInfo course =
            findCourse(
                courseId);

        if (course == null) {

            showError(
                "未找到选中的课程，请刷新后重试。");

            return;
        }

        JTextField codeField =
            new JTextField(
                course.getCourseCode());

        JTextField nameField =
            new JTextField(
                course.getCourseName());

        JSpinner creditsSpinner =
            new JSpinner(
                new SpinnerNumberModel(
                    course.getCredits(),
                    0.5,
                    30.0,
                    0.5));

        JComboBox<String> typeBox =
            new JComboBox<>(
                new String[]{
                    "必修",
                    "限选",
                    "任选",
                    "体育",
                    "通选"
                });

        typeBox.setEditable(
            true);

        typeBox.setSelectedItem(
            course.getCourseType());

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
                "课程 ID："));

        form.add(
            new JLabel(
                String.valueOf(
                    course.getCourseId())));

        form.add(
            new JLabel(
                "课程代码："));

        form.add(
            codeField);

        form.add(
            new JLabel(
                "课程名称："));

        form.add(
            nameField);

        form.add(
            new JLabel(
                "学分："));

        form.add(
            creditsSpinner);

        form.add(
            new JLabel(
                "课程类型："));

        form.add(
            typeBox);

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
                "修改课程信息",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option
            != JOptionPane.OK_OPTION) {

            return;
        }

        String courseCode =
            codeField.getText().trim();

        String courseName =
            nameField.getText().trim();

        String courseType =
            String.valueOf(
                    typeBox.getSelectedItem())
                .trim();

        String reason =
            reasonArea.getText().trim();

        if (courseCode.isEmpty()
            || courseName.isEmpty()
            || courseType.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "课程代码、名称和类型不能为空。",
                "输入错误",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null) {

            return;
        }

        submitUpdate(
            new AdminUpdateCourseRequest(
                batch.batchId(),
                course.getCourseId(),
                courseCode,
                courseName,
                ((Number)
                    creditsSpinner.getValue())
                    .doubleValue(),
                courseType,
                reason));
    }

    /**
     * 提交课程修改。
     */
    private void submitUpdate(
        AdminUpdateCourseRequest request) {

        statusLabel.setText(
            "正在保存课程信息……");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_UPDATE_COURSE,
                        request);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            CourseAdminCoursePanel.this,
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

                            loadCourses();
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "保存课程信息被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            "无法保存课程信息："
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

    private List<CourseInfo> readCourses(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的课程数据格式错误。");
        }

        List<CourseInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof CourseInfo course)) {

                throw new IllegalStateException(
                    "服务器返回的课程数据格式错误。");
            }

            result.add(
                course);
        }

        return result;
    }

    private CourseInfo findCourse(
        long courseId) {

        for (CourseInfo course
            : courses) {

            if (course.getCourseId()
                == courseId) {

                return course;
            }
        }

        return null;
    }

    private boolean matches(
        CourseInfo course,
        String keyword) {

        if (keyword.isEmpty()) {

            return true;
        }

        return String.valueOf(
                course.getCourseId())
            .contains(keyword)
            || course.getCourseCode()
            .toLowerCase()
            .contains(keyword)
            || course.getCourseName()
            .toLowerCase()
            .contains(keyword)
            || course.getCourseType()
            .toLowerCase()
            .contains(keyword);
    }

    private void showError(
        String message) {

        statusLabel.setText(
            message);

        JOptionPane.showMessageDialog(
            this,
            message,
            "课程管理",
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

    /**
     * 批次下拉框显示对象。
     */
    private record BatchChoice(
        long batchId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }
}
