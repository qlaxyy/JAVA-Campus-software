package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.AdminListGradesRequest;
import edu.seu.vcampus.common.course.AdminUpdateGradeRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseGradeInfo;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.Role;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教务成绩管理页面。
 *
 * 教务管理员可以查看成绩，
 * 超级管理员可以修改成绩。
 */
final class CourseAdminGradePanel
    extends JPanel {

    private static final DateTimeFormatter
        TIME_FORMATTER =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final JTextField studentIdField =
        new JTextField(
            16);

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
                "成绩",
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

    private final JLabel statusLabel =
        new JLabel(
            "请输入学生学号查询成绩。");

    private final List<CourseGradeInfo> grades =
        new ArrayList<>();

    private final boolean superAdmin;

    CourseAdminGradePanel(
        ClientContext context) {

        this.context =
            context;

        this.superAdmin =
            context.currentSession()
                .map(session ->
                    session.getRole()
                        == Role.SUPER_ADMIN)
                .orElse(false);

        initialiseView();
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
                "成绩管理"));

        titlePanel.add(
            CourseTheme.subtitle(
                superAdmin
                    ? "查询学生成绩并修改成绩"
                    : "查询学生课程成绩"));

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

        toolbar.add(
            new JLabel(
                "学生学号："));

        toolbar.add(
            studentIdField);

        JButton searchButton =
            CourseTheme.quietButton(
                "查询");

        JButton refreshButton =
            CourseTheme.quietButton(
                "刷新");

        JButton editButton =
            CourseTheme.primaryButton(
                "录入或修改成绩");

        editButton.setVisible(
            superAdmin);

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

        table.setFillsViewportHeight(
            true);

        table.setSelectionMode(
            javax.swing.ListSelectionModel
                .SINGLE_SELECTION);

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

        searchButton.addActionListener(
            event ->
                loadGrades());

        refreshButton.addActionListener(
            event ->
                loadGrades());

        studentIdField.addActionListener(
            event ->
                loadGrades());

        editButton.addActionListener(
            event ->
                editSelectedGrade());
    }

    /**
     * 查询学生成绩。
     */
    private void loadGrades() {

        String studentId =
            studentIdField.getText()
                .trim();

        if (studentId.isEmpty()) {

            JOptionPane.showMessageDialog(
                this,
                "请输入学生学号。",
                "缺少学生学号",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        statusLabel.setText(
            "正在加载学生成绩……");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_LIST_GRADES,
                        new AdminListGradesRequest(
                            studentId));
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

                        grades.clear();
                        grades.addAll(
                            readGrades(
                                response));

                        renderGrades();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载成绩被中断。");

                    } catch (ExecutionException
                             | IllegalStateException exception) {

                        Throwable cause =
                            exception instanceof
                                ExecutionException
                                ? exception.getCause()
                                : exception;

                        showError(
                            "无法加载成绩："
                                + messageOf(
                                cause));
                    }
                }
            };

        worker.execute();
    }

    /**
     * 显示成绩列表。
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
                    grade.getScore() == null
                        ? "未录入"
                        : grade.getScore(),
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
            "共显示 "
                + grades.size()
                + " 条课程成绩");
    }

    /**
     * 超级管理员修改成绩。
     */
    private void editSelectedGrade() {

        if (!superAdmin) {

            JOptionPane.showMessageDialog(
                this,
                "只有超级管理员可以修改成绩。",
                "没有权限",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        int selectedRow =
            table.getSelectedRow();

        if (selectedRow < 0) {

            JOptionPane.showMessageDialog(
                this,
                "请先选择一条课程记录。",
                "未选择课程",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        long enrollmentId =
            ((Number)
                tableModel.getValueAt(
                    selectedRow,
                    1))
                .longValue();

        CourseGradeInfo grade =
            findGrade(
                enrollmentId);

        if (grade == null) {

            showError(
                "未找到选中的成绩记录，请重新查询。");

            return;
        }

        double initialScore =
            grade.getScore() == null
                ? 0.0
                : grade.getScore();

        JSpinner scoreSpinner =
            new JSpinner(
                new SpinnerNumberModel(
                    initialScore,
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
                grade.getCourseName()
                    + "（"
                    + grade.getCourseCode()
                    + "）"));

        form.add(
            new JLabel(
                "教学班："));

        form.add(
            new JLabel(
                grade.getClassNo()));

        form.add(
            new JLabel(
                "成绩："));

        form.add(
            scoreSpinner);

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
                grade.getScore() == null
                    ? "录入成绩"
                    : "修改成绩",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option
            != JOptionPane.OK_OPTION) {

            return;
        }

        submitUpdate(
            new AdminUpdateGradeRequest(
                grade.getStudentId(),
                grade.getEnrollmentId(),
                ((Number)
                    scoreSpinner.getValue())
                    .doubleValue(),
                reasonArea.getText()
                    .trim()));
    }

    /**
     * 提交成绩修改。
     */
    private void submitUpdate(
        AdminUpdateGradeRequest request) {

        statusLabel.setText(
            "正在保存成绩……");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_UPDATE_GRADE,
                        request);
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            CourseAdminGradePanel.this,
                            response.getMessage(),
                            response.isSuccess()
                                ? "保存成功"
                                : "保存失败",
                            response.isSuccess()
                                ? JOptionPane
                                .INFORMATION_MESSAGE
                                : JOptionPane
                                .WARNING_MESSAGE);

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
                    }
                }
            };

        worker.execute();
    }

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

    private String resultText(
        CourseGradeInfo grade) {

        if (!grade.isRecorded()) {

            return "未录入";
        }

        return grade.isPassed()
            ? "及格"
            : "不及格";
    }

    private void showError(
        String message) {

        statusLabel.setText(
            message);

        JOptionPane.showMessageDialog(
            this,
            message,
            "成绩管理",
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
