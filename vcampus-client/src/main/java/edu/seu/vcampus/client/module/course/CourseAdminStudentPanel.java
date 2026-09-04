package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.AdminForceDropCourseRequest;
import edu.seu.vcampus.common.course.AdminForceSelectCourseRequest;
import edu.seu.vcampus.common.course.AdminListStudentEnrollmentsRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.EnrollmentInfo;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教务端学生选课管理页面。
 */
final class CourseAdminStudentPanel
    extends JPanel {

    private final ClientContext context;

    private final JTextField studentIdField =
        new JTextField(14);

    private final JComboBox<BatchChoice> batchBox =
        new JComboBox<>();

    private final JComboBox<OfferingChoice> offeringBox =
        new JComboBox<>();

    private boolean updatingBatches;

    private final JTextField reasonField =
        new JTextField(18);

    private final JButton queryButton =
        CourseTheme.primaryButton(
            "查询已选课程");

    private final JButton forceSelectButton =
        CourseTheme.primaryButton(
            "强制选课");

    private final JPanel enrollmentPanel =
        new JPanel();

    private final JLabel statusLabel =
        new JLabel("请输入学生学号。");

    CourseAdminStudentPanel(
        ClientContext context) {

        this.context = context;

        initialiseView();
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
                "学生选课管理"));

        topPanel.add(
            Box.createVerticalStrut(
                5));

        topPanel.add(
            CourseTheme.subtitle(
                "查询学生已选课程，并执行强制选课或强制退课"));

        topPanel.add(
            Box.createVerticalStrut(
                14));

        topPanel.add(
            createControlPanel());

        add(
            topPanel,
            BorderLayout.NORTH);

        /*
         * =========================
         * 已选课程区域
         * =========================
         */
        enrollmentPanel.setLayout(
            new BoxLayout(
                enrollmentPanel,
                BoxLayout.Y_AXIS));

        enrollmentPanel.setBackground(
            CourseTheme.BACKGROUND);

        JScrollPane scrollPane =
            new JScrollPane(
                enrollmentPanel);

        scrollPane.setBorder(
            BorderFactory.createEmptyBorder());

        scrollPane
            .getViewport()
            .setBackground(
                CourseTheme.BACKGROUND);

        scrollPane
            .getVerticalScrollBar()
            .setUnitIncrement(
                14);

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

        statusLabel.setBorder(
            BorderFactory.createEmptyBorder(
                2,
                2,
                0,
                2));

        add(
            statusLabel,
            BorderLayout.SOUTH);

        queryButton.addActionListener(
            event ->
                loadEnrollments());

        forceSelectButton.addActionListener(
            event ->
                confirmForceSelect());
        batchBox.addActionListener(
            event -> {

                if (!updatingBatches) {

                    loadOfferings();
                }
            });

        loadBatches();
    }

    private JPanel createControlPanel() {

        CourseTheme.SurfacePanel panel =
            new CourseTheme.SurfacePanel();

        panel.setLayout(
            new BoxLayout(
                panel,
                BoxLayout.Y_AXIS));

        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    12,
                    14,
                    12,
                    14)));

        /*
         * 查询学生。
         */
        JPanel queryRow =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    10,
                    4));

        queryRow.setOpaque(false);

        queryRow.add(
            new JLabel(
                "学生学号："));

        queryRow.add(
            studentIdField);

        queryRow.add(
            queryButton);

        /*
         * 强制选课。
         */
        JPanel selectRow =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    10,
                    4));

        selectRow.setOpaque(false);

        batchBox.setPreferredSize(
            new Dimension(
                280,
                34));

        offeringBox.setPreferredSize(
            new Dimension(
                390,
                34));

        selectRow.add(
            new JLabel(
                "选课批次："));

        selectRow.add(
            batchBox);

        selectRow.add(
            new JLabel(
                "课程教学班："));

        selectRow.add(
            offeringBox);
        selectRow.add(
            new JLabel(
                "操作原因："));

        selectRow.add(
            reasonField);

        selectRow.add(
            forceSelectButton);

        panel.add(
            queryRow);

        panel.add(
            selectRow);

        return panel;
    }
    /**
     * 加载全部选课批次。
     */
    private void loadBatches() {

        batchBox.setEnabled(false);
        offeringBox.setEnabled(false);
        forceSelectButton.setEnabled(false);

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

                        batchBox.setEnabled(
                            batchBox.getItemCount() > 0);

                        if (batchBox.getItemCount()
                            == 0) {

                            statusLabel.setText(
                                "当前没有选课批次。");

                            return;
                        }

                        loadOfferings();

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
     * 加载当前批次的全部教学班。
     */
    private void loadOfferings() {

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        if (batch == null) {

            return;
        }

        offeringBox.removeAllItems();
        offeringBox.setEnabled(false);
        forceSelectButton.setEnabled(false);

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
                                "服务器返回的课程数据格式错误。");

                            return;
                        }

                        int offeringCount =
                            0;

                        for (Object value : values) {

                            if (!(value
                                instanceof CourseInfo course)) {

                                showError(
                                    "服务器返回的课程数据格式错误。");

                                return;
                            }

                            for (OfferingInfo offering
                                : course.getOfferings()) {

                                String teacher =
                                    offering
                                        .getTeacherNames()
                                        .isEmpty()
                                        ? "未安排教师"
                                        : String.join(
                                        "、",
                                        offering
                                            .getTeacherNames());

                                offeringBox.addItem(
                                    new OfferingChoice(
                                        offering
                                            .getOfferingId(),
                                        course
                                            .getCourseCode()
                                            + " "
                                            + course
                                            .getCourseName()
                                            + " - "
                                            + offering
                                            .getClassNo()
                                            + "班 - "
                                            + teacher
                                            + "（"
                                            + offering
                                            .getSelectedCount()
                                            + "/"
                                            + offering
                                            .getCapacity()
                                            + "）"));

                                offeringCount++;
                            }
                        }

                        boolean hasOfferings =
                            offeringCount > 0;

                        offeringBox.setEnabled(
                            hasOfferings);

                        forceSelectButton.setEnabled(
                            hasOfferings);

                        statusLabel.setText(
                            hasOfferings
                                ? "已加载 "
                                + offeringCount
                                + " 个教学班。"
                                : "当前批次没有教学班。");

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
                    }
                }
            };

        worker.execute();
    }
    /**
     * 查询指定学生的已选课程。
     */
    private void loadEnrollments() {

        String studentId =
            cleanStudentId();

        if (studentId == null) {

            return;
        }

        queryButton.setEnabled(false);

        statusLabel.setText(
            "正在查询学生已选课程...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_LIST_STUDENT_ENROLLMENTS,
                        new AdminListStudentEnrollmentsRequest(
                            studentId));
                }

                @Override
                protected void done() {

                    queryButton.setEnabled(true);

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response.getMessage());

                            return;
                        }

                        List<EnrollmentInfo> enrollments =
                            readEnrollments(
                                response);

                        renderEnrollments(
                            studentId,
                            enrollments);

                        statusLabel.setText(
                            "学生 "
                                + studentId
                                + " 当前已选 "
                                + enrollments.size()
                                + " 个教学班。");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "查询被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法查询学生选课："
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

    private List<EnrollmentInfo> readEnrollments(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的选课数据格式错误。");
        }

        List<EnrollmentInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof EnrollmentInfo enrollment)) {

                throw new IllegalStateException(
                    "服务器返回的选课数据格式错误。");
            }

            result.add(
                enrollment);
        }

        return result;
    }

    private void renderEnrollments(
        String studentId,
        List<EnrollmentInfo> enrollments) {

        enrollmentPanel.removeAll();

        if (enrollments.isEmpty()) {

            JLabel emptyLabel =
                new JLabel(
                    "该学生当前没有已选课程。");

            emptyLabel.setForeground(
                CourseTheme.MUTED);

            enrollmentPanel.add(
                emptyLabel);

        } else {

            for (EnrollmentInfo enrollment
                : enrollments) {

                enrollmentPanel.add(
                    createEnrollmentCard(
                        studentId,
                        enrollment));

                enrollmentPanel.add(
                    Box.createVerticalStrut(
                        10));
            }
        }

        enrollmentPanel.revalidate();
        enrollmentPanel.repaint();
    }

    private JPanel createEnrollmentCard(
        String studentId,
        EnrollmentInfo enrollment) {

        CourseTheme.SurfacePanel card =
            new CourseTheme.SurfacePanel();

        card.setLayout(
            new BorderLayout(
                20,
                0));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        card.setMaximumSize(
            new Dimension(
                Integer.MAX_VALUE,
                120));

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    14,
                    18,
                    14,
                    18)));

        JPanel information =
            new JPanel();

        information.setOpaque(false);

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel nameLabel =
            new JLabel(
                enrollment.getCourseName()
                    + "（"
                    + enrollment.getCourseCode()
                    + "）");

        nameLabel.setForeground(
            CourseTheme.TEXT);

        nameLabel.setFont(
            nameLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    16F));

        JLabel detailLabel =
            new JLabel(
                "教学班："
                    + enrollment.getClassNo()
                    + "    教师："
                    + teacherText(enrollment)
                    + "    学分："
                    + enrollment.getCredits());

        detailLabel.setForeground(
            CourseTheme.MUTED);

        information.add(
            nameLabel);

        information.add(
            Box.createVerticalStrut(
                8));

        information.add(
            detailLabel);

        card.add(
            information,
            BorderLayout.CENTER);

        JButton dropButton =
            new JButton(
                "强制退课");

        CourseTheme.styleDangerButton(
            dropButton);

        dropButton.addActionListener(
            event ->
                confirmForceDrop(
                    studentId,
                    enrollment,
                    dropButton));

        card.add(
            dropButton,
            BorderLayout.EAST);

        return card;
    }

    /**
     * 确认并提交强制选课。
     */
    private void confirmForceSelect() {

        String studentId =
            cleanStudentId();

        if (studentId == null) {

            return;
        }

        BatchChoice batch =
            (BatchChoice)
                batchBox.getSelectedItem();

        OfferingChoice offering =
            (OfferingChoice)
                offeringBox.getSelectedItem();

        if (batch == null
            || offering == null) {

            JOptionPane.showMessageDialog(
                this,
                "请选择选课批次和课程教学班。",
                "输入不完整",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        String reason =
            reasonField.getText().trim();

        if (reason.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "请输入强制选课原因。",
                "输入不完整",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        int result =
            JOptionPane.showConfirmDialog(
                this,
                "确认给学生 "
                    + studentId
                    + " 强制选择教学班 "
                    + offering.offeringId()
                    + " 吗？",
                "确认强制选课",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result
            != JOptionPane.YES_OPTION) {

            return;
        }

        submitForceSelect(
            studentId,
            batch.batchId(),
            offering.offeringId(),
            reason);
    }

    private void submitForceSelect(
        String studentId,
        long batchId,
        long offeringId,
        String reason) {

        forceSelectButton.setEnabled(false);

        statusLabel.setText(
            "正在提交强制选课...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_FORCE_SELECT_COURSE,
                        new AdminForceSelectCourseRequest(
                            studentId,
                            batchId,
                            offeringId,
                            reason));
                }

                @Override
                protected void done() {

                    forceSelectButton.setEnabled(true);

                    handleMutationResult(
                        this,
                        "强制选课");

                    loadEnrollments();
                }
            };

        worker.execute();
    }

    /**
     * 确认并提交强制退课。
     */
    private void confirmForceDrop(
        String studentId,
        EnrollmentInfo enrollment,
        JButton dropButton) {

        String reason =
            JOptionPane.showInputDialog(
                this,
                "请输入强制退课原因：",
                "强制退课",
                JOptionPane.WARNING_MESSAGE);

        if (reason == null) {

            return;
        }

        reason =
            reason.trim();

        if (reason.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "强制退课原因不能为空。",
                "输入不完整",
                JOptionPane.WARNING_MESSAGE);

            return;
        }

        int result =
            JOptionPane.showConfirmDialog(
                this,
                "确认将学生 "
                    + studentId
                    + " 的课程“"
                    + enrollment.getCourseName()
                    + "”强制退选吗？",
                "确认强制退课",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result
            != JOptionPane.YES_OPTION) {

            return;
        }

        submitForceDrop(
            studentId,
            enrollment.getEnrollmentId(),
            reason,
            dropButton);
    }

    private void submitForceDrop(
        String studentId,
        long enrollmentId,
        String reason,
        JButton dropButton) {

        dropButton.setEnabled(false);

        statusLabel.setText(
            "正在提交强制退课...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .ADMIN_FORCE_DROP_COURSE,
                        new AdminForceDropCourseRequest(
                            studentId,
                            enrollmentId,
                            reason));
                }

                @Override
                protected void done() {

                    handleMutationResult(
                        this,
                        "强制退课");

                    loadEnrollments();
                }
            };

        worker.execute();
    }

    private void handleMutationResult(
        SwingWorker<Response, Void> worker,
        String operationName) {

        try {

            Response response =
                worker.get();

            if (response.isSuccess()) {

                JOptionPane.showMessageDialog(
                    this,
                    response.getMessage(),
                    operationName + "成功",
                    JOptionPane.INFORMATION_MESSAGE);

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    response.getMessage(),
                    operationName + "失败",
                    JOptionPane.WARNING_MESSAGE);
            }

        } catch (InterruptedException exception) {

            Thread.currentThread()
                .interrupt();

            showError(
                operationName + "被中断。");

        } catch (ExecutionException exception) {

            Throwable cause =
                exception.getCause();

            showError(
                operationName
                    + "请求失败："
                    + (cause == null
                    ? exception.getMessage()
                    : cause.getMessage()));
        }
    }

    private String cleanStudentId() {

        String studentId =
            studentIdField
                .getText()
                .trim();

        if (studentId.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "请输入学生学号。",
                "输入不完整",
                JOptionPane.WARNING_MESSAGE);

            return null;
        }

        return studentId;
    }

    private Long positiveLong(
        String value,
        String fieldName) {

        try {

            long number =
                Long.parseLong(
                    value.trim());

            if (number <= 0) {

                throw new NumberFormatException();
            }

            return number;

        } catch (NumberFormatException exception) {

            JOptionPane.showMessageDialog(
                this,
                fieldName
                    + "必须是大于 0 的整数。",
                "输入格式错误",
                JOptionPane.WARNING_MESSAGE);

            return null;
        }
    }

    private String teacherText(
        EnrollmentInfo enrollment) {

        if (enrollment
            .getTeacherNames()
            .isEmpty()) {

            return "未安排";
        }

        return String.join(
            "、",
            enrollment.getTeacherNames());
    }

    private void showError(
        String message) {

        statusLabel.setText(
            message);

        JOptionPane.showMessageDialog(
            this,
            message,
            "操作失败",
            JOptionPane.ERROR_MESSAGE);
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
