package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.GeneralCourseListRequest;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.course.SelectCourseRequest;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
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
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 通选课页面。
 */
final class GeneralCoursePanel
    extends JPanel {

    private final ClientContext context;

    private final SelectionBatchInfo batch;

    private final JPanel coursePanel =
        new JPanel();

    private final JLabel statusLabel =
        new JLabel(" ");

    private final JComboBox<String>
        categoryBox =
        new JComboBox<>(
            new String[]{
                "全部",
                "自然科学",
                "人文社科",
                "创新创业",
                "心理健康",
                "美育"
            });

    GeneralCoursePanel(
        ClientContext context,
        SelectionBatchInfo batch) {

        this.context =
            context;

        this.batch =
            batch;

        initializeView();

        loadCourses();
    }

    private void initializeView() {

        setLayout(
            new BorderLayout(
                0,
                12));

        setBorder(
            BorderFactory.createEmptyBorder(
                16,
                16,
                16,
                16));

        JPanel top =
            new JPanel(
                new BorderLayout());

        JLabel title =
            new JLabel(
                "通选课");

        title.setFont(
            title.getFont()
                .deriveFont(
                    Font.BOLD,
                    20F));

        top.add(
            title,
            BorderLayout.WEST);

        JPanel filter =
            new JPanel(
                new FlowLayout(
                    FlowLayout.RIGHT));

        filter.add(
            new JLabel(
                "通选类别："));

        filter.add(
            categoryBox);

        top.add(
            filter,
            BorderLayout.EAST);

        add(
            top,
            BorderLayout.NORTH);

        coursePanel.setLayout(
            new BoxLayout(
                coursePanel,
                BoxLayout.Y_AXIS));

        JScrollPane scroll =
            new JScrollPane(
                coursePanel);

        scroll.setBorder(
            BorderFactory
                .createEmptyBorder());

        scroll.getVerticalScrollBar()
            .setUnitIncrement(
                12);

        add(
            scroll,
            BorderLayout.CENTER);

        add(
            statusLabel,
            BorderLayout.SOUTH);

        categoryBox.addActionListener(
            event ->
                loadCourses());
    }

    void reload() {

        loadCourses();
    }

    private void loadCourses() {

        if (context.currentSession()
            .isEmpty()) {

            showError(
                "登录状态已失效，请重新登录。");

            return;
        }

        String category =
            selectedCategory();

        statusLabel.setText(
            "正在加载通选课程...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions
                            .LIST_GENERAL_COURSES,
                        new GeneralCourseListRequest(
                            batch.getBatchId(),
                            category));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (!response.isSuccess()) {

                            showError(
                                response
                                    .getMessage());

                            return;
                        }

                        List<CourseInfo> courses =
                            readCourses(
                                response);

                        renderCourses(
                            courses);

                        statusLabel.setText(
                            "共加载 "
                                + courses.size()
                                + " 门通选课程");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载通选课程被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            cause == null
                                ? exception.getMessage()
                                : cause.getMessage());

                    } catch (IllegalStateException exception) {

                        showError(
                            exception.getMessage());
                    }
                }
            };

        worker.execute();
    }

    private String selectedCategory() {

        Object value =
            categoryBox.getSelectedItem();

        if (value == null
            || "全部".equals(value)) {

            return null;
        }

        return value.toString();
    }

    private List<CourseInfo> readCourses(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的通选课程数据格式错误。");
        }

        List<CourseInfo> result =
            new ArrayList<>();

        for (Object value
            : values) {

            if (!(value
                instanceof CourseInfo course)) {

                throw new IllegalStateException(
                    "服务器返回的通选课程数据格式错误。");
            }

            result.add(
                course);
        }

        return result;
    }

    private void renderCourses(
        List<CourseInfo> courses) {

        coursePanel.removeAll();

        if (courses.isEmpty()) {

            coursePanel.add(
                new JLabel(
                    "当前没有符合条件的通选课程。"));

        } else {

            for (CourseInfo course
                : courses) {

                coursePanel.add(
                    createCourseCard(
                        course));

                coursePanel.add(
                    Box.createVerticalStrut(
                        12));
            }
        }

        coursePanel.revalidate();

        coursePanel.repaint();
    }

    /**
     * 一门通选课程。
     */
    private JPanel createCourseCard(
        CourseInfo course) {

        JPanel card =
            new JPanel(
                new BorderLayout(
                    0,
                    10));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        if (hasTimeConflict(
            course)) {

            card.setBorder(
                BorderFactory
                    .createCompoundBorder(
                        BorderFactory
                            .createLineBorder(
                                new Color(
                                    190,
                                    45,
                                    45),
                                2),
                        BorderFactory
                            .createEmptyBorder(
                                14,
                                16,
                                14,
                                16)));

        } else {

            card.setBorder(
                BorderFactory
                    .createCompoundBorder(
                        BorderFactory
                            .createEtchedBorder(),
                        BorderFactory
                            .createEmptyBorder(
                                14,
                                16,
                                14,
                                16)));
        }

        JPanel information =
            new JPanel();

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel name =
            new JLabel(
                course.getCourseName());

        name.setFont(
            name.getFont()
                .deriveFont(
                    Font.BOLD,
                    17F));

        information.add(
            name);

        information.add(
            Box.createVerticalStrut(
                6));

        information.add(
            new JLabel(
                "课程号："
                    + course
                    .getCourseCode()));

        information.add(
            Box.createVerticalStrut(
                3));

        information.add(
            new JLabel(
                "学分："
                    + course.getCredits()
                    + "    类型："
                    + course
                    .getCourseType()));

        JLabel conflictLabel =
            createConflictLabel(
                course);

        if (conflictLabel != null) {

            information.add(
                Box.createVerticalStrut(
                    6));

            information.add(
                conflictLabel);
        }

        information.add(
            Box.createVerticalStrut(
                3));

        information.add(
            new JLabel(
                course.isSelected()
                    ? "状态：已选"
                    : "状态：未选"));

        card.add(
            information,
            BorderLayout.NORTH);

        JPanel offeringPanel =
            new JPanel();

        offeringPanel.setLayout(
            new BoxLayout(
                offeringPanel,
                BoxLayout.Y_AXIS));

        for (OfferingInfo offering
            : course.getOfferings()) {

            offeringPanel.add(
                createOfferingCard(
                    course,
                    offering));

            offeringPanel.add(
                Box.createVerticalStrut(
                    8));
        }

        card.add(
            offeringPanel,
            BorderLayout.CENTER);

        return card;
    }

    /**
     * 教学班卡片。
     */
    private JPanel createOfferingCard(
        CourseInfo course,
        OfferingInfo offering) {

        JPanel card =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        if ("TIME_CONFLICT".equals(
            offering
                .getAvailabilityStatus())) {

            card.setBorder(
                BorderFactory
                    .createCompoundBorder(
                        BorderFactory
                            .createLineBorder(
                                new Color(
                                    190,
                                    45,
                                    45)),
                        BorderFactory
                            .createEmptyBorder(
                                10,
                                12,
                                10,
                                12)));

        } else {

            card.setBorder(
                BorderFactory
                    .createCompoundBorder(
                        BorderFactory
                            .createEtchedBorder(),
                        BorderFactory
                            .createEmptyBorder(
                                10,
                                12,
                                10,
                                12)));
        }

        JPanel info =
            new JPanel();

        info.setLayout(
            new BoxLayout(
                info,
                BoxLayout.Y_AXIS));

        JLabel classLabel =
            new JLabel(
                "教学班："
                    + offering
                    .getClassNo());

        classLabel.setFont(
            classLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    15F));

        info.add(
            classLabel);

        info.add(
            Box.createVerticalStrut(
                5));

        info.add(
            new JLabel(
                "教师："
                    + teacherText(
                    offering)));

        info.add(
            Box.createVerticalStrut(
                3));

        info.add(
            new JLabel(
                "上课时间："
                    + scheduleText(
                    offering)));

        info.add(
            Box.createVerticalStrut(
                3));

        info.add(
            new JLabel(
                "地点："
                    + nullableText(
                    offering
                        .getLocationName())
                    + "    校区："
                    + nullableText(
                    offering
                        .getCampusName())));

        info.add(
            Box.createVerticalStrut(
                3));

        info.add(
            new JLabel(
                "人数："
                    + offering
                    .getSelectedCount()
                    + " / "
                    + offering
                    .getCapacity()
                    + "    剩余："
                    + offering
                    .getRemainingCount()));

        info.add(
            Box.createVerticalStrut(
                3));

        JLabel status =
            new JLabel(
                "状态："
                    + availabilityText(
                    offering));

        if ("TIME_CONFLICT".equals(
            offering
                .getAvailabilityStatus())) {

            status.setForeground(
                new Color(
                    190,
                    45,
                    45));

            status.setFont(
                status.getFont()
                    .deriveFont(
                        Font.BOLD));
        }

        info.add(
            status);

        card.add(
            info,
            BorderLayout.CENTER);

        /*
         * =========================
         * 选择按钮
         * =========================
         */
        JButton selectButton =
            new JButton(
                selectButtonText(
                    offering));

        selectButton.setEnabled(
            canSelect(
                offering));

        selectButton.addActionListener(
            event ->
                confirmAndSelect(
                    course,
                    offering,
                    selectButton));

        card.add(
            selectButton,
            BorderLayout.EAST);

        return card;
    }

    /**
     * 确认选课。
     */
    private void confirmAndSelect(
        CourseInfo course,
        OfferingInfo offering,
        JButton button) {

        String message =
            "请确认选择以下通选教学班：\n\n"
                + "课程："
                + course.getCourseName()
                + "（"
                + course.getCourseCode()
                + "）\n"
                + "教学班："
                + offering.getClassNo()
                + "\n"
                + "教师："
                + teacherText(
                offering)
                + "\n"
                + "时间："
                + scheduleText(
                offering)
                + "\n"
                + "地点："
                + nullableText(
                offering
                    .getLocationName());

        int result =
            JOptionPane.showConfirmDialog(
                this,
                message,
                "确认通选课选课",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result
            != JOptionPane.YES_OPTION) {

            return;
        }

        submitSelection(
            offering,
            button);
    }

    /**
     * 提交选课。
     */
    private void submitSelection(
        OfferingInfo offering,
        JButton button) {

        button.setEnabled(
            false);

        button.setText(
            "提交中...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.SELECT_COURSE,
                        new SelectCourseRequest(
                            batch.getBatchId(),
                            offering
                                .getOfferingId()));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            GeneralCoursePanel.this,
                            response.getMessage(),
                            response.isSuccess()
                                ? "选课成功"
                                : "选课失败",
                            response.isSuccess()
                                ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE);

                        /*
                         * 无论成功失败都刷新，
                         * Server 状态是最终权威。
                         */
                        loadCourses();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        loadCourses();

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        JOptionPane.showMessageDialog(
                            GeneralCoursePanel.this,
                            cause == null
                                ? exception.getMessage()
                                : cause.getMessage(),
                            "选课失败",
                            JOptionPane.ERROR_MESSAGE);

                        loadCourses();
                    }
                }
            };

        worker.execute();
    }

    private boolean canSelect(
        OfferingInfo offering) {

        return batch.getStatus()
            == SelectionBatchStatus.OPEN
            && batch.isAllowSelect()
            && "AVAILABLE".equals(
            offering
                .getAvailabilityStatus());
    }

    private String selectButtonText(
        OfferingInfo offering) {

        if (batch.getStatus()
            != SelectionBatchStatus.OPEN
            || !batch.isAllowSelect()) {

            return "当前批次不可选";
        }

        return switch (
            offering
                .getAvailabilityStatus()) {

            case "AVAILABLE" ->
                "选择";

            case "SELECTED" ->
                "已选";

            case "COURSE_ALREADY_SELECTED" ->
                "已选其他教学班";

            case "FULL" ->
                "人数已满";

            case "TIME_CONFLICT" ->
                "时间冲突";

            default ->
                "不可选";
        };
    }

    private String availabilityText(
        OfferingInfo offering) {

        return switch (
            offering
                .getAvailabilityStatus()) {

            case "AVAILABLE" ->
                "可选";

            case "SELECTED" ->
                "已选";

            case "COURSE_ALREADY_SELECTED" ->
                "已经选择该课程";

            case "FULL" ->
                "人数已满";

            case "TIME_CONFLICT" ->
                "时间冲突";

            case "NOT_ELIGIBLE" ->
                "不符合选课条件";

            default ->
                "不可选";
        };
    }

    private boolean hasTimeConflict(
        CourseInfo course) {

        return course.getOfferings()
            .stream()
            .anyMatch(offering ->
                "TIME_CONFLICT".equals(
                    offering
                        .getAvailabilityStatus()));
    }

    private JLabel createConflictLabel(
        CourseInfo course) {

        long conflictCount =
            course.getOfferings()
                .stream()
                .filter(offering ->
                    "TIME_CONFLICT".equals(
                        offering
                            .getAvailabilityStatus()))
                .count();

        if (conflictCount == 0) {

            return null;
        }

        String text =
            conflictCount
                == course.getOfferings()
                .size()
                ? "⚠ 时间冲突：所有教学班均与已选课程冲突"
                : "⚠ 时间冲突：部分教学班与已选课程冲突";

        JLabel label =
            new JLabel(
                text);

        label.setForeground(
            new Color(
                190,
                45,
                45));

        label.setFont(
            label.getFont()
                .deriveFont(
                    Font.BOLD));

        return label;
    }

    private String teacherText(
        OfferingInfo offering) {

        if (offering.getTeacherNames()
            .isEmpty()) {

            return "未安排";
        }

        return String.join(
            "、",
            offering.getTeacherNames());
    }

    private String scheduleText(
        OfferingInfo offering) {

        if (offering.getSchedules()
            .isEmpty()) {

            return "未安排";
        }

        List<String> values =
            new ArrayList<>();

        for (ScheduleInfo schedule
            : offering.getSchedules()) {

            values.add(
                scheduleText(
                    schedule));
        }

        return String.join(
            "；",
            values);
    }

    private String scheduleText(
        ScheduleInfo schedule) {

        String weekPattern =
            switch (
                schedule.getWeekPattern()) {

                case "ODD" ->
                    "（单周）";

                case "EVEN" ->
                    "（双周）";

                default ->
                    "";
            };

        return dayText(
            schedule.getDayOfWeek())
            + " 第"
            + schedule.getStartPeriod()
            + "-"
            + schedule.getEndPeriod()
            + "节 第"
            + schedule.getStartWeek()
            + "-"
            + schedule.getEndWeek()
            + "周"
            + weekPattern;
    }

    private String dayText(
        int day) {

        return switch (day) {

            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";

            default ->
                "未知星期";
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

        coursePanel.removeAll();

        coursePanel.add(
            new JLabel(
                "通选课程加载失败。"));

        coursePanel.revalidate();

        coursePanel.repaint();

        statusLabel.setText(
            message);
    }
}
