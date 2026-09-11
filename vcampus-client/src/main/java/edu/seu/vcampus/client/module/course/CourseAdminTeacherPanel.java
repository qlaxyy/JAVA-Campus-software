package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.AdminTeacherAssignmentRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.OfferingTeacherRequest;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.TeacherProfileView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 教务端任课教师管理页面。
 */
final class CourseAdminTeacherPanel
    extends JPanel {

    private final ClientContext context;

    private final JTextField offeringIdField =
        new JTextField(10);

    private final JComboBox<TeacherChoice> teacherBox =
        new JComboBox<>();

    private final DefaultListModel<String>
        assignmentModel =
        new DefaultListModel<>();

    private final JList<String> assignmentList =
        new JList<>(
            assignmentModel);

    private final JButton loadButton =
        CourseTheme.quietButton(
            "查询任课教师");

    private final JButton assignButton =
        CourseTheme.primaryButton(
            "分配教师");

    private final JButton removeButton =
        CourseTheme.quietButton(
            "移除选中教师");

    private final JButton reloadTeachersButton =
        CourseTheme.quietButton(
            "刷新教师名单");

    private final JLabel statusLabel =
        new JLabel(" ");

    CourseAdminTeacherPanel(
        ClientContext context) {

        this.context =
            context;

        initialiseView();

        loadActiveTeachers();
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

        JPanel header =
            new JPanel();

        header.setOpaque(false);

        header.setLayout(
            new BoxLayout(
                header,
                BoxLayout.Y_AXIS));

        header.add(
            CourseTheme.title(
                "任课教师管理"));

        header.add(
            Box.createVerticalStrut(
                5));

        header.add(
            CourseTheme.subtitle(
                "从公共有效教师名单中选择教师，并分配至指定教学班"));

        add(
            header,
            BorderLayout.NORTH);

        CourseTheme.SurfacePanel content =
            new CourseTheme.SurfacePanel();

        content.setLayout(
            new BorderLayout(
                0,
                12));

        content.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    14,
                    14,
                    14,
                    14)));

        content.add(
            createToolbar(),
            BorderLayout.NORTH);

        assignmentList.setBackground(
            java.awt.Color.WHITE);

        assignmentList.setForeground(
            CourseTheme.TEXT);

        assignmentList.setFixedCellHeight(
            30);

        JScrollPane scrollPane =
            new JScrollPane(
                assignmentList);

        scrollPane.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                "当前教学班任课教师 userId"));

        content.add(
            scrollPane,
            BorderLayout.CENTER);

        content.add(
            createActionPanel(),
            BorderLayout.SOUTH);

        add(
            content,
            BorderLayout.CENTER);

        statusLabel.setForeground(
            CourseTheme.MUTED);

        add(
            statusLabel,
            BorderLayout.SOUTH);

        loadButton.addActionListener(
            event ->
                loadAssignments());

        offeringIdField.addActionListener(
            event ->
                loadAssignments());

        reloadTeachersButton.addActionListener(
            event ->
                loadActiveTeachers());

        assignButton.addActionListener(
            event ->
                assignTeacher());

        removeButton.addActionListener(
            event ->
                removeTeacher());
    }

    private JPanel createToolbar() {

        JPanel panel =
            new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT,
                    10,
                    4));

        panel.setOpaque(false);

        offeringIdField.setPreferredSize(
            new Dimension(
                120,
                34));

        teacherBox.setPreferredSize(
            new Dimension(
                360,
                34));

        panel.add(
            new JLabel(
                "教学班 ID："));

        panel.add(
            offeringIdField);

        panel.add(
            loadButton);

        panel.add(
            new JLabel(
                "有效教师："));

        panel.add(
            teacherBox);

        panel.add(
            reloadTeachersButton);

        return panel;
    }

    private JPanel createActionPanel() {

        JPanel panel =
            new JPanel(
                new FlowLayout(
                    FlowLayout.RIGHT,
                    10,
                    4));

        panel.setOpaque(false);

        panel.add(
            removeButton);

        panel.add(
            assignButton);

        return panel;
    }

    /**
     * 从公共教师接口加载有效教师。
     */
    private void loadActiveTeachers() {

        setBusy(
            true);

        statusLabel.setText(
            "正在加载公共教师名单...");

        new SwingWorker<Response, Void>() {

            @Override
            protected Response doInBackground()
                throws Exception {

                return context.send(
                    CourseActions
                        .ADMIN_LIST_ACTIVE_TEACHERS,
                    null);
            }

            @Override
            protected void done() {

                setBusy(
                    false);

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
                            "服务器返回的教师名单格式错误。");

                        return;
                    }

                    teacherBox.removeAllItems();

                    for (Object value : values) {

                        if (!(value
                            instanceof TeacherProfileView
                            teacher)) {

                            showError(
                                "服务器返回的教师名单格式错误。");

                            return;
                        }

                        teacherBox.addItem(
                            new TeacherChoice(
                                teacher));
                    }

                    statusLabel.setText(
                        "已加载 "
                            + teacherBox.getItemCount()
                            + " 名有效教师。");

                } catch (InterruptedException exception) {

                    Thread.currentThread()
                        .interrupt();

                    showError(
                        "教师名单加载已中断。");

                } catch (ExecutionException exception) {

                    showError(
                        failureMessage(
                            "无法加载教师名单",
                            exception));
                }
            }
        }.execute();
    }

    /**
     * 加载指定教学班的任课关系。
     */
    private void loadAssignments() {

        Long offeringId =
            readOfferingId();

        if (offeringId == null) {

            return;
        }

        setBusy(
            true);

        statusLabel.setText(
            "正在加载任课关系...");

        new SwingWorker<Response, Void>() {

            @Override
            protected Response doInBackground()
                throws Exception {

                return context.send(
                    CourseActions
                        .ADMIN_LIST_OFFERING_TEACHERS,
                    new OfferingTeacherRequest(
                        offeringId));
            }

            @Override
            protected void done() {

                setBusy(
                    false);

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
                            "服务器返回的任课关系格式错误。");

                        return;
                    }

                    assignmentModel.clear();

                    for (Object value : values) {

                        if (!(value
                            instanceof String userId)) {

                            showError(
                                "服务器返回的任课关系格式错误。");

                            return;
                        }

                        assignmentModel.addElement(
                            userId);
                    }

                    statusLabel.setText(
                        "教学班 "
                            + offeringId
                            + " 共分配 "
                            + assignmentModel.getSize()
                            + " 名教师。");

                } catch (InterruptedException exception) {

                    Thread.currentThread()
                        .interrupt();

                    showError(
                        "任课关系加载已中断。");

                } catch (ExecutionException exception) {

                    showError(
                        failureMessage(
                            "无法加载任课关系",
                            exception));
                }
            }
        }.execute();
    }

    private void assignTeacher() {

        Long offeringId =
            readOfferingId();

        if (offeringId == null) {

            return;
        }

        TeacherChoice choice =
            (TeacherChoice)
                teacherBox.getSelectedItem();

        if (choice == null) {

            showError(
                "请先选择一名有效教师。");

            return;
        }

        changeAssignment(
            CourseActions.ADMIN_ASSIGN_TEACHER,
            new AdminTeacherAssignmentRequest(
                offeringId,
                choice.teacher()
                    .getUserId()),
            "正在分配任课教师...",
            "任课教师分配成功。");
    }

    private void removeTeacher() {

        Long offeringId =
            readOfferingId();

        if (offeringId == null) {

            return;
        }

        String teacherUserId =
            assignmentList.getSelectedValue();

        if (teacherUserId == null) {

            showError(
                "请先从列表中选择需要移除的教师。");

            return;
        }

        int result =
            JOptionPane.showConfirmDialog(
                this,
                "确定从教学班 "
                    + offeringId
                    + " 移除教师 "
                    + teacherUserId
                    + " 吗？",
                "确认移除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result
            != JOptionPane.YES_OPTION) {

            return;
        }

        changeAssignment(
            CourseActions.ADMIN_REMOVE_TEACHER,
            new AdminTeacherAssignmentRequest(
                offeringId,
                teacherUserId),
            "正在移除任课教师...",
            "任课教师移除成功。");
    }

    private void changeAssignment(
        String action,
        AdminTeacherAssignmentRequest request,
        String loadingMessage,
        String successMessage) {

        setBusy(
            true);

        statusLabel.setText(
            loadingMessage);

        new SwingWorker<Response, Void>() {

            @Override
            protected Response doInBackground()
                throws Exception {

                return context.send(
                    action,
                    request);
            }

            @Override
            protected void done() {

                setBusy(
                    false);

                try {

                    Response response =
                        get();

                    if (!response.isSuccess()) {

                        showError(
                            response.getMessage());

                        return;
                    }

                    statusLabel.setText(
                        successMessage);

                    loadAssignments();

                } catch (InterruptedException exception) {

                    Thread.currentThread()
                        .interrupt();

                    showError(
                        "任课关系修改已中断。");

                } catch (ExecutionException exception) {

                    showError(
                        failureMessage(
                            "无法修改任课关系",
                            exception));
                }
            }
        }.execute();
    }

    private Long readOfferingId() {

        String text =
            offeringIdField
                .getText()
                .trim();

        if (text.isEmpty()) {

            showError(
                "请输入教学班 ID。");

            return null;
        }

        try {

            long offeringId =
                Long.parseLong(
                    text);

            if (offeringId <= 0) {

                throw new NumberFormatException();
            }

            return offeringId;

        } catch (NumberFormatException exception) {

            showError(
                "教学班 ID 必须是正整数。");

            return null;
        }
    }

    private void setBusy(
        boolean busy) {

        loadButton.setEnabled(
            !busy);

        assignButton.setEnabled(
            !busy);

        removeButton.setEnabled(
            !busy);

        reloadTeachersButton.setEnabled(
            !busy);

        offeringIdField.setEnabled(
            !busy);

        teacherBox.setEnabled(
            !busy);
    }

    private void showError(
        String message) {

        String actualMessage =
            message == null
                || message.isBlank()
                ? "操作失败。"
                : message;

        statusLabel.setText(
            actualMessage);

        JOptionPane.showMessageDialog(
            this,
            actualMessage,
            "操作失败",
            JOptionPane.ERROR_MESSAGE);
    }

    private String failureMessage(
        String prefix,
        ExecutionException exception) {

        Throwable cause =
            exception.getCause();

        String detail =
            cause == null
                ? exception.getMessage()
                : cause.getMessage();

        return prefix
            + "："
            + (detail == null
            ? "未知错误"
            : detail);
    }

    private record TeacherChoice(
        TeacherProfileView teacher) {

        @Override
        public String toString() {

            return teacher.getDisplayName()
                + "（"
                + teacher.getCampusCardNumber()
                + "，"
                + teacher.getDepartment()
                + "）";
        }
    }
}
