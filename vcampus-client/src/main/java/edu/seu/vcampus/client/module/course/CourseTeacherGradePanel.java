package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.AdminUpdateGradeRequest;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseGradeInfo;
import edu.seu.vcampus.common.course.CourseGradePolicyInfo;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.TeacherListStudentsRequest;
import edu.seu.vcampus.common.course.TeacherUpdateGradePolicyRequest;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教师端成绩管理页面。
 */
final class CourseTeacherGradePanel
    extends JPanel {

    private static final DateTimeFormatter
        TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final JComboBox<BatchChoice>
        batchBox =
        new JComboBox<>();

    private final JComboBox<OfferingChoice>
        offeringBox =
        new JComboBox<>();

    private final JButton reloadButton =
        CourseTheme.quietButton(
            "刷新");

    private final JButton policyButton =
        CourseTheme.quietButton(
            "修改成绩比例");

    private final JButton editButton =
        CourseTheme.primaryButton(
            "录入或修改成绩");

    private final JLabel policyLabel =
        new JLabel(
            "成绩比例：未加载");

    private final JLabel statusLabel =
        new JLabel(" ");

    private final List<CourseGradeInfo> grades =
        new ArrayList<>();

    private CourseGradePolicyInfo currentPolicy;

    private boolean updatingBatches;

    private boolean updatingOfferings;

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new Object[]{
                "成绩 ID",
                "选课记录 ID",
                "学生学号",
                "教学班 ID",
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

    CourseTeacherGradePanel(
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
                "成绩管理"));

        topPanel.add(
            Box.createVerticalStrut(
                5));

        topPanel.add(
            CourseTheme.subtitle(
                "分别录入平时成绩和期末成绩，并按教学班比例计算总成绩"));

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

                    refreshCurrentOffering();
                }
            });

        reloadButton.addActionListener(
            event ->
                refreshCurrentOffering());

        policyButton.addActionListener(
            event ->
                editGradePolicy());

        editButton.addActionListener(
            event ->
                editSelectedGrade());

        table.addMouseListener(
            new java.awt.event.MouseAdapter() {

                @Override
                public void mouseClicked(
                    java.awt.event.MouseEvent event) {

                    if (event.getClickCount()
                        == 2
                        && table.getSelectedRow()
                        >= 0) {

                        editSelectedGrade();
                    }
                }
            });

        setOfferingControlsEnabled(
            false);
    }

    /**
     * 创建顶部工具栏。
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
                250,
                34));

        offeringBox.setPreferredSize(
            new Dimension(
                340,
                34));

        policyLabel.setForeground(
            CourseTheme.PRIMARY_DARK);

        policyLabel.setFont(
            policyLabel.getFont()
                .deriveFont(
                    Font.BOLD));

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
            policyLabel);

        panel.add(
            policyButton);

        panel.add(
            reloadButton);

        panel.add(
            editButton);

        return panel;
    }

    /**
     * 初始化成绩表格。
     */
    private void initialiseTable() {

        table.setRowHeight(
            30);

        table.setFillsViewportHeight(
            true);

        table.setAutoCreateRowSorter(
            true);

        table.setSelectionMode(
            ListSelectionModel
                .SINGLE_SELECTION);

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

        table.getColumnModel()
            .getColumn(2)
            .setPreferredWidth(
                105);

        table.getColumnModel()
            .getColumn(5)
            .setPreferredWidth(
                135);

        table.getColumnModel()
            .getColumn(13)
            .setPreferredWidth(
                135);
    }

    /**
     * 加载选课批次。
     */
    private void loadBatches() {

        batchBox.setEnabled(
            false);

        setOfferingControlsEnabled(
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
                            "无法加载选课批次："
                                + messageOf(
                                exception.getCause()));

                    } finally {

                        updatingBatches =
                            false;
                    }
                }
            };

        worker.execute();
    }

    /**
     * 加载教师负责的教学班。
     */
    private void loadOfferings() {

        BatchChoice batch =
            selectedBatch();

        if (batch == null) {

            clearOfferingData();

            return;
        }

        clearGradeData();

        setOfferingControlsEnabled(
            false);

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
                            "无法加载教学班："
                                + messageOf(
                                exception.getCause()));

                    } catch (IllegalStateException exception) {

                        showError(
                            exception.getMessage());
                    }
                }
            };

        worker.execute();
    }

    /**
     * 读取教学班。
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

        setOfferingControlsEnabled(
            hasOfferings);

        if (hasOfferings) {

            refreshCurrentOffering();

        } else {

            statusLabel.setText(
                "当前批次没有分配给你的教学班。");
        }
    }

    /**
     * 刷新比例和成绩。
     */
    private void refreshCurrentOffering() {

        if (selectedBatch() == null
            || selectedOffering() == null) {

            clearGradeData();

            return;
        }

        loadGradePolicy();

        loadGrades();
    }

    /**
     * 加载成绩比例。
     */
    private void loadGradePolicy() {

        BatchChoice batch =
            selectedBatch();

        OfferingChoice offering =
            selectedOffering();

        if (batch == null
            || offering == null) {

            return;
        }

        long expectedOfferingId =
            offering.offeringId();

        policyButton.setEnabled(
            false);

        policyLabel.setText(
            "成绩比例：加载中...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .TEACHER_GET_GRADE_POLICY,
                        new TeacherListStudentsRequest(
                            batch.batchId(),
                            expectedOfferingId));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!isCurrentOffering(
                            expectedOfferingId)) {

                            return;
                        }

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        if (!(response.getData()
                            instanceof CourseGradePolicyInfo
                            policy)) {

                            showError(
                                "服务器返回的成绩比例格式错误。");

                            return;
                        }

                        currentPolicy =
                            policy;

                        renderPolicy();

                        policyButton.setEnabled(
                            true);

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载成绩比例被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            "无法加载成绩比例："
                                + messageOf(
                                exception.getCause()));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 加载成绩。
     */
    private void loadGrades() {

        BatchChoice batch =
            selectedBatch();

        OfferingChoice offering =
            selectedOffering();

        if (batch == null
            || offering == null) {

            clearGradeData();

            return;
        }

        long expectedOfferingId =
            offering.offeringId();

        reloadButton.setEnabled(
            false);

        editButton.setEnabled(
            false);

        statusLabel.setText(
            "正在加载成绩...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .TEACHER_LIST_GRADES,
                        new TeacherListStudentsRequest(
                            batch.batchId(),
                            expectedOfferingId));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!isCurrentOffering(
                            expectedOfferingId)) {

                            return;
                        }

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        grades.clear();

                        grades.addAll(
                            readGrades(
                                response));

                        renderGrades();

                        editButton.setEnabled(
                            !grades.isEmpty());

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载成绩被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            "无法加载成绩："
                                + messageOf(
                                exception.getCause()));

                    } catch (IllegalStateException exception) {

                        showError(
                            exception.getMessage());

                    } finally {

                        if (isCurrentOffering(
                            expectedOfferingId)) {

                            reloadButton.setEnabled(
                                true);
                        }
                    }
                }
            };

        worker.execute();
    }

    /**
     * 读取成绩数据。
     */
    private List<CourseGradeInfo> readGrades(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的成绩数据格式错误。");
        }

        List<CourseGradeInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof CourseGradeInfo grade)) {

                throw new IllegalStateException(
                    "服务器返回的成绩数据格式错误。");
            }

            result.add(
                grade);
        }

        return result;
    }

    /**
     * 显示成绩比例。
     */
    private void renderPolicy() {

        if (currentPolicy == null) {

            policyLabel.setText(
                "成绩比例：未加载");

            return;
        }

        policyLabel.setText(
            "成绩比例：平时 "
                + currentPolicy
                .getUsualWeightPercent()
                + "% / 期末 "
                + currentPolicy
                .getFinalExamWeightPercent()
                + "%");
    }

    /**
     * 显示成绩表格。
     */
    private void renderGrades() {

        tableModel.setRowCount(
            0);

        for (CourseGradeInfo grade
            : grades) {

            tableModel.addRow(
                new Object[]{
                    grade.getGradeId() == null
                        ? "未录入"
                        : grade.getGradeId(),
                    grade.getEnrollmentId(),
                    grade.getStudentId(),
                    grade.getOfferingId(),
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
                    grade.getRecordedAt() == null
                        ? "未录入"
                        : grade.getRecordedAt()
                        .format(
                            TIME_FORMATTER)
                });
        }

        statusLabel.setText(
            "该教学班共有 "
                + grades.size()
                + " 条成绩记录。");
    }

    /**
     * 修改教学班成绩比例。
     */
    private void editGradePolicy() {

        OfferingChoice offering =
            selectedOffering();

        if (offering == null
            || currentPolicy == null) {

            JOptionPane.showMessageDialog(
                this,
                "成绩比例尚未加载。",
                "无法修改",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        JSpinner usualWeightSpinner =
            new JSpinner(
                new SpinnerNumberModel(
                    currentPolicy
                        .getUsualWeightPercent(),
                    0,
                    100,
                    5));

        JLabel finalWeightLabel =
            new JLabel(
                currentPolicy
                    .getFinalExamWeightPercent()
                    + "%");

        usualWeightSpinner.addChangeListener(
            event -> {

                int usualWeight =
                    ((Number)
                        usualWeightSpinner
                            .getValue())
                        .intValue();

                finalWeightLabel.setText(
                    (100 - usualWeight)
                        + "%");
            });

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
                "平时成绩比例："));

        form.add(
            usualWeightSpinner);

        form.add(
            new JLabel(
                "期末成绩比例："));

        form.add(
            finalWeightLabel);

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
                "修改成绩比例",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option
            != JOptionPane.OK_OPTION) {

            return;
        }

        int usualWeight =
            ((Number)
                usualWeightSpinner
                    .getValue())
                .intValue();

        int finalExamWeight =
            100 - usualWeight;

        submitPolicy(
            new TeacherUpdateGradePolicyRequest(
                offering.offeringId(),
                usualWeight,
                finalExamWeight,
                reasonArea.getText()
                    .trim()));
    }

    /**
     * 提交成绩比例。
     */
    private void submitPolicy(
        TeacherUpdateGradePolicyRequest request) {

        policyButton.setEnabled(
            false);

        statusLabel.setText(
            "正在保存成绩比例...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .TEACHER_UPDATE_GRADE_POLICY,
                        request);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            CourseTeacherGradePanel.this,
                            response.getMessage(),
                            response.isSuccess()
                                ? "保存成功"
                                : "保存失败",
                            response.isSuccess()
                                ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE);

                        if (response.isSuccess()) {

                            refreshCurrentOffering();

                        } else {

                            policyButton.setEnabled(
                                true);
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "保存成绩比例被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            "无法保存成绩比例："
                                + messageOf(
                                exception.getCause()));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 编辑选中学生成绩。
     */
    private void editSelectedGrade() {

        int selectedViewRow =
            table.getSelectedRow();

        if (selectedViewRow < 0) {

            JOptionPane.showMessageDialog(
                this,
                "请先选择一名学生。",
                "未选择学生",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        int selectedModelRow =
            table.convertRowIndexToModel(
                selectedViewRow);

        long enrollmentId =
            ((Number)
                tableModel.getValueAt(
                    selectedModelRow,
                    1))
                .longValue();

        CourseGradeInfo grade =
            findGrade(
                enrollmentId);

        if (grade == null) {

            showError(
                "未找到选中的成绩记录。");

            return;
        }

        JSpinner usualScoreSpinner =
            new JSpinner(
                new SpinnerNumberModel(
                    grade.getUsualScore() == null
                        ? 0.0
                        : grade.getUsualScore(),
                    0.0,
                    100.0,
                    0.5));

        JSpinner finalExamScoreSpinner =
            new JSpinner(
                new SpinnerNumberModel(
                    grade.getFinalExamScore() == null
                        ? 0.0
                        : grade.getFinalExamScore(),
                    0.0,
                    100.0,
                    0.5));

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
                "学生学号："));

        form.add(
            new JLabel(
                grade.getStudentId()));

        form.add(
            new JLabel(
                "课程："));

        form.add(
            new JLabel(
                grade.getCourseName()));

        form.add(
            new JLabel(
                "当前比例："));

        form.add(
            new JLabel(
                "平时 "
                    + grade.getUsualWeightPercent()
                    + "% / 期末 "
                    + grade.getFinalExamWeightPercent()
                    + "%"));

        form.add(
            new JLabel(
                "平时成绩："));

        form.add(
            usualScoreSpinner);

        form.add(
            new JLabel(
                "期末成绩："));

        form.add(
            finalExamScoreSpinner);

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
                grade.isRecorded()
                    ? "修改成绩"
                    : "录入成绩",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option
            != JOptionPane.OK_OPTION) {

            return;
        }

        submitGrade(
            new AdminUpdateGradeRequest(
                grade.getStudentId(),
                grade.getEnrollmentId(),
                ((Number)
                    usualScoreSpinner
                        .getValue())
                    .doubleValue(),
                ((Number)
                    finalExamScoreSpinner
                        .getValue())
                    .doubleValue(),
                reasonArea.getText()
                    .trim()));
    }

    /**
     * 提交学生成绩。
     */
    private void submitGrade(
        AdminUpdateGradeRequest request) {

        reloadButton.setEnabled(
            false);

        editButton.setEnabled(
            false);

        statusLabel.setText(
            "正在保存成绩...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .TEACHER_UPDATE_GRADE,
                        request);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            CourseTeacherGradePanel.this,
                            response.getMessage(),
                            response.isSuccess()
                                ? "保存成功"
                                : "保存失败",
                            response.isSuccess()
                                ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE);

                        if (response.isSuccess()) {

                            loadGrades();
                        }

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "保存成绩被中断。");

                    } catch (ExecutionException exception) {

                        showError(
                            "无法保存成绩："
                                + messageOf(
                                exception.getCause()));

                    } finally {

                        reloadButton.setEnabled(
                            selectedOffering()
                                != null);

                        editButton.setEnabled(
                            !grades.isEmpty());
                    }
                }
            };

        worker.execute();
    }

    private CourseGradeInfo findGrade(
        long enrollmentId) {

        for (CourseGradeInfo grade
            : grades) {

            if (grade.getEnrollmentId()
                == enrollmentId) {

                return grade;
            }
        }

        return null;
    }

    private String scoreText(
        Double score) {

        return score == null
            ? "未录入"
            : score.toString();
    }

    private String resultText(
        CourseGradeInfo grade) {

        if (!grade.isRecorded()) {

            return "未录入";
        }

        return grade.isPassed()
            ? "及格"
            : "不及格";
    }

    private BatchChoice selectedBatch() {

        return (BatchChoice)
            batchBox.getSelectedItem();
    }

    private OfferingChoice selectedOffering() {

        return (OfferingChoice)
            offeringBox.getSelectedItem();
    }

    private boolean isCurrentOffering(
        long offeringId) {

        OfferingChoice current =
            selectedOffering();

        return current != null
            && current.offeringId()
            == offeringId;
    }

    private void setOfferingControlsEnabled(
        boolean enabled) {

        offeringBox.setEnabled(
            enabled);

        reloadButton.setEnabled(
            enabled);

        policyButton.setEnabled(
            enabled
                && currentPolicy != null);

        editButton.setEnabled(
            enabled
                && !grades.isEmpty());
    }

    private void clearGradeData() {

        grades.clear();

        currentPolicy =
            null;

        tableModel.setRowCount(
            0);

        policyLabel.setText(
            "成绩比例：未加载");

        editButton.setEnabled(
            false);
    }

    private void clearOfferingData() {

        updatingOfferings =
            true;

        offeringBox.removeAllItems();

        updatingOfferings =
            false;

        clearGradeData();

        setOfferingControlsEnabled(
            false);
    }

    private void showError(
        String message) {

        statusLabel.setText(
            message == null
                ? "操作失败。"
                : message);
    }

    private String messageOf(
        Throwable throwable) {

        if (throwable == null
            || throwable.getMessage() == null) {

            return "未知错误";
        }

        return throwable.getMessage();
    }

    private record BatchChoice(
        long batchId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }

    private record OfferingChoice(
        long offeringId,
        String text) {

        @Override
        public String toString() {

            return text;
        }
    }
}
