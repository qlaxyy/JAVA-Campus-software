package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.DropCourseRequest;
import edu.seu.vcampus.common.course.EnrollmentInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 已选课程页面。
 */
final class SelectedCoursePanel extends JPanel {

    private final ClientContext context;
    private final Runnable onEnrollmentChanged;
    private final SelectionBatchInfo batch;

    private final JPanel enrollmentPanel =
        new JPanel();

    private final JLabel statusLabel =
        new JLabel(" ");

    SelectedCoursePanel(
        ClientContext context,
        SelectionBatchInfo batch,
        Runnable onEnrollmentChanged) {

        this.context = context;
        this.batch = batch;
        this.onEnrollmentChanged = onEnrollmentChanged;

        initializeView();
    }

    private void initializeView() {

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

        header.setOpaque(
            false);

        header.setLayout(
            new BoxLayout(
                header,
                BoxLayout.Y_AXIS));

        JLabel title =
            CourseTheme.title(
                "已选课程");

        JLabel subtitle =
            CourseTheme.subtitle(
                "查看当前学期已经选择的教学班，并在允许时退课");

        header.add(
            title);

        header.add(
            Box.createVerticalStrut(
                5));

        header.add(
            subtitle);

        add(
            header,
            BorderLayout.NORTH);

        /*
         * =========================
         * 已选课程列表
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
            BorderFactory
                .createEmptyBorder());

        scrollPane.setBackground(
            CourseTheme.BACKGROUND);

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

        statusLabel.setFont(
            statusLabel
                .getFont()
                .deriveFont(
                    13F));

        statusLabel.setBorder(
            BorderFactory.createEmptyBorder(
                2,
                2,
                0,
                2));

        add(
            statusLabel,
            BorderLayout.SOUTH);
    }

    /**
     * 供 CourseBatchPanel 在切换到本标签时刷新。
     */
    void reload() {

        loadEnrollments();
    }

    /**
     * 从服务器获取已选课程。
     */
    private void loadEnrollments() {

        statusLabel.setText(
            "正在加载已选课程...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.LIST_ENROLLMENTS,
                        new BatchRequest(
                            batch.getBatchId()));
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

                        List<EnrollmentInfo> enrollments =
                            readEnrollments(
                                response);

                        renderEnrollments(
                            enrollments);

                        statusLabel.setText(
                            "当前已选 "
                                + enrollments.size()
                                + " 个教学班");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载已选课程被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载已选课程："
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
                "服务器返回的已选课程数据格式错误。");
        }

        List<EnrollmentInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof EnrollmentInfo enrollment)) {

                throw new IllegalStateException(
                    "服务器返回的已选课程数据格式错误。");
            }

            result.add(
                enrollment);
        }

        return result;
    }

    private void renderEnrollments(
        List<EnrollmentInfo> enrollments) {

        enrollmentPanel.removeAll();

        if (enrollments.isEmpty()) {

            JLabel emptyLabel =
                new JLabel(
                    "当前没有已选课程。");

            enrollmentPanel.add(
                emptyLabel);

        } else {

            for (EnrollmentInfo enrollment
                : enrollments) {

                enrollmentPanel.add(
                    createEnrollmentCard(
                        enrollment));

                enrollmentPanel.add(
                    Box.createVerticalStrut(
                        10));
            }
        }

        enrollmentPanel.revalidate();
        enrollmentPanel.repaint();
    }

    /**
     * 一条已选教学班。
     */
    private JPanel createEnrollmentCard(
        EnrollmentInfo enrollment) {

        CourseTheme.SurfacePanel card =
            new CourseTheme.SurfacePanel();

        card.setLayout(
            new BorderLayout(
                20,
                0));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    15,
                    18,
                    15,
                    18)));

        /*
         * =========================
         * 课程信息
         * =========================
         */
        JPanel information =
            new JPanel();

        information.setOpaque(
            false);

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

        JLabel classLabel =
            new JLabel(
                "教学班："
                    + enrollment.getClassNo());

        JLabel teacherLabel =
            new JLabel(
                "教师："
                    + teacherText(
                    enrollment));

        JLabel scheduleLabel =
            new JLabel(
                "时间："
                    + scheduleText(
                    enrollment));

        JLabel locationLabel =
            new JLabel(
                "地点："
                    + nullableText(
                    enrollment
                        .getLocationName()));

        JLabel detailLabel =
            new JLabel(
                "学分："
                    + enrollment.getCredits()
                    + "    类型："
                    + enrollment.getCourseType());

        classLabel.setForeground(
            CourseTheme.MUTED);

        teacherLabel.setForeground(
            CourseTheme.MUTED);

        scheduleLabel.setForeground(
            CourseTheme.MUTED);

        locationLabel.setForeground(
            CourseTheme.MUTED);

        detailLabel.setForeground(
            CourseTheme.MUTED);

        information.add(
            nameLabel);

        information.add(
            Box.createVerticalStrut(
                7));

        information.add(
            classLabel);

        information.add(
            Box.createVerticalStrut(
                4));

        information.add(
            teacherLabel);

        information.add(
            Box.createVerticalStrut(
                4));

        information.add(
            scheduleLabel);

        information.add(
            Box.createVerticalStrut(
                4));

        information.add(
            locationLabel);

        information.add(
            Box.createVerticalStrut(
                4));

        information.add(
            detailLabel);

        /*
         * 已选状态。
         */
        JLabel selectedLabel =
            new JLabel(
                "● 已选");

        selectedLabel.setForeground(
            CourseTheme.SUCCESS);

        selectedLabel.setFont(
            selectedLabel
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        information.add(
            Box.createVerticalStrut(
                7));

        information.add(
            selectedLabel);

        card.add(
            information,
            BorderLayout.CENTER);

        /*
         * =========================
         * 退课按钮
         * =========================
         */
        JButton dropButton =
            new JButton();

        if (enrollment.isCanDrop()) {

            dropButton.setText(
                "退课");

            CourseTheme.styleDangerButton(
                dropButton);

            dropButton.setEnabled(
                true);

        } else {

            dropButton.setText(
                enrollment
                    .getDropUnavailableReason()
                    == null
                    ? "不可退课"
                    : enrollment
                    .getDropUnavailableReason());

            CourseTheme.styleQuietButton(
                dropButton);

            dropButton.setEnabled(
                false);
        }

        dropButton.addActionListener(
            event ->
                confirmDrop(
                    enrollment,
                    dropButton));

        card.add(
            dropButton,
            BorderLayout.EAST);

        return card;
    }

    /**
     * 退课确认。
     */
    private void confirmDrop(
        EnrollmentInfo enrollment,
        JButton dropButton) {

        String message =
            "确认退选以下课程吗？\n\n"
                + "课程："
                + enrollment.getCourseName()
                + "（"
                + enrollment.getCourseCode()
                + "）\n"
                + "教学班："
                + enrollment.getClassNo()
                + "\n"
                + "教师："
                + teacherText(enrollment)
                + "\n"
                + "时间："
                + scheduleText(enrollment)
                + "\n\n"
                + "注意：退课后空出的名额可能被其他学生选择，"
                + "再次选课不保证成功。";

        int result =
            JOptionPane.showConfirmDialog(
                this,
                message,
                "确认退课",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result
            != JOptionPane.YES_OPTION) {

            return;
        }

        submitDrop(
            enrollment,
            dropButton);
    }

    /**
     * 提交退课。
     */
    private void submitDrop(
        EnrollmentInfo enrollment,
        JButton dropButton) {

        dropButton.setEnabled(
            false);

        dropButton.setText(
            "提交中...");

        statusLabel.setText(
            "正在提交退课请求...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.DROP_COURSE,
                        new DropCourseRequest(
                            batch.getBatchId(),
                            enrollment.getEnrollmentId()));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (response.isSuccess()) {
                            onEnrollmentChanged.run();
                            JOptionPane.showMessageDialog(
                                SelectedCoursePanel.this,
                                response.getMessage(),
                                "退课成功",
                                JOptionPane.INFORMATION_MESSAGE);

                        } else {

                            JOptionPane.showMessageDialog(
                                SelectedCoursePanel.this,
                                response.getMessage(),
                                "退课失败",
                                JOptionPane.WARNING_MESSAGE);
                        }

                        /*
                         * 不相信旧界面状态，
                         * 始终重新向服务器读取。
                         */
                        loadEnrollments();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "退课请求被中断。");

                        loadEnrollments();

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        JOptionPane.showMessageDialog(
                            SelectedCoursePanel.this,
                            "无法提交退课请求："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()),
                            "退课失败",
                            JOptionPane.ERROR_MESSAGE);

                        loadEnrollments();
                    }
                }
            };

        worker.execute();
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

    private String scheduleText(
        EnrollmentInfo enrollment) {

        if (enrollment
            .getSchedules()
            .isEmpty()) {

            return "未安排";
        }

        List<String> values =
            new ArrayList<>();

        for (ScheduleInfo schedule
            : enrollment.getSchedules()) {

            values.add(
                singleScheduleText(
                    schedule));
        }

        return String.join(
            "；",
            values);
    }

    private String singleScheduleText(
        ScheduleInfo schedule) {

        StringBuilder text =
            new StringBuilder();

        text.append(
            dayText(
                schedule.getDayOfWeek()));

        text.append(" 第")
            .append(
                schedule.getStartPeriod())
            .append("-")
            .append(
                schedule.getEndPeriod())
            .append("节");

        text.append(" 第")
            .append(
                schedule.getStartWeek())
            .append("-")
            .append(
                schedule.getEndWeek())
            .append("周");

        if ("ODD".equals(
            schedule.getWeekPattern())) {

            text.append("（单周）");

        } else if ("EVEN".equals(
            schedule.getWeekPattern())) {

            text.append("（双周）");
        }

        return text.toString();
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
            default -> "未知星期";
        };
    }

    private String nullableText(
        String value) {

        if (value == null
            || value.isBlank()) {

            return "未安排";
        }

        return value;
    }

    private void showError(
        String message) {

        enrollmentPanel.removeAll();

        enrollmentPanel.add(
            new JLabel(
                "已选课程加载失败。"));

        enrollmentPanel.revalidate();
        enrollmentPanel.repaint();

        statusLabel.setText(
            message);
    }
}
