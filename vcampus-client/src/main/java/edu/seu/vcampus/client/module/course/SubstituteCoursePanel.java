package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 方案外课程页面。
 */
final class SubstituteCoursePanel
    extends JPanel {

    private final ClientContext context;

    private final SelectionBatchInfo batch;

    private final JPanel coursePanel =
        new JPanel();

    private final JLabel statusLabel =
        new JLabel(" ");

    SubstituteCoursePanel(
        ClientContext context,
        SelectionBatchInfo batch) {

        this.context = context;
        this.batch = batch;

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

        JLabel title =
            new JLabel(
                "方案外课程");

        title.setFont(
            title.getFont()
                .deriveFont(
                    Font.BOLD,
                    20F));

        add(
            title,
            BorderLayout.NORTH);

        coursePanel.setLayout(
            new BoxLayout(
                coursePanel,
                BoxLayout.Y_AXIS));

        JScrollPane scrollPane =
            new JScrollPane(
                coursePanel);

        scrollPane.setBorder(
            BorderFactory.createEmptyBorder());

        scrollPane
            .getVerticalScrollBar()
            .setUnitIncrement(12);

        add(
            scrollPane,
            BorderLayout.CENTER);

        add(
            statusLabel,
            BorderLayout.SOUTH);
    }

    /**
     * 标签页重新进入时刷新。
     */
    void reload() {

        loadCourses();
    }

    /**
     * 加载方案外课程。
     */
    private void loadCourses() {

        if (context.currentSession().isEmpty()) {

            showError(
                "登录状态已失效，请重新登录。");

            return;
        }

        statusLabel.setText(
            "正在加载方案外课程...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.LIST_SUBSTITUTE_COURSES,
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

                        List<CourseInfo> courses =
                            readCourses(
                                response);

                        renderCourses(
                            courses);

                        statusLabel.setText(
                            "共加载 "
                                + courses.size()
                                + " 门方案外课程");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载方案外课程被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载方案外课程："
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

    private List<CourseInfo> readCourses(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的方案外课程数据格式错误。");
        }

        List<CourseInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof CourseInfo course)) {

                throw new IllegalStateException(
                    "服务器返回的方案外课程数据格式错误。");
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
                    "当前没有方案外课程。"));

        } else {

            for (CourseInfo course : courses) {

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
     * 方案外课程对应的培养方案要求
     * 是否已经被其他课程满足。
     */


    /**
     * 创建课程卡片。
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

        /*
         * 这里只负责设置边框。
         */
        if (hasTimeConflict(course)) {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        new Color(
                            190,
                            45,
                            45),
                        2),
                    BorderFactory.createEmptyBorder(
                        14,
                        16,
                        14,
                        16)));

        } else {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(
                        14,
                        16,
                        14,
                        16)));
        }

        JPanel summaryPanel =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

        /*
         * 到这里 information 才创建。
         */
        JPanel information =
            new JPanel();

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel nameLabel =
            new JLabel(
                course.getCourseName());

        nameLabel.setFont(
            nameLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    17F));

        information.add(
            nameLabel);

        information.add(
            Box.createVerticalStrut(
                6));

        information.add(
            new JLabel(
                "课程号："
                    + course.getCourseCode()));

        information.add(
            Box.createVerticalStrut(
                3));

        information.add(
            new JLabel(
                "学分："
                    + course.getCredits()
                    + "    类型："
                    + course.getCourseType()));

        /*
         * =========================
         * 时间冲突提示
         * =========================
         */
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

        /*
         * =========================
         * 培养方案要求已满足提示
         * =========================
         *
         * 注意：
         * 必须放在 information 创建之后。
         */
        JLabel satisfiedLabel =
            createRequirementSatisfiedLabel(
                course);

        if (satisfiedLabel != null) {

            information.add(
                Box.createVerticalStrut(
                    6));

            information.add(
                satisfiedLabel);
        }

        /*
         * 当前课程状态。
         */
        information.add(
            Box.createVerticalStrut(
                3));

        information.add(
            new JLabel(
                course.isSelected()
                    ? "状态：已选"
                    : "状态：未选"));

        summaryPanel.add(
            information,
            BorderLayout.CENTER);

        JButton expandButton =
            new JButton(
                "展开教学班");

        summaryPanel.add(
            expandButton,
            BorderLayout.EAST);

        card.add(
            summaryPanel,
            BorderLayout.NORTH);

        JPanel offeringPanel =
            new JPanel();

        offeringPanel.setLayout(
            new BoxLayout(
                offeringPanel,
                BoxLayout.Y_AXIS));

        offeringPanel.setBorder(
            BorderFactory.createEmptyBorder(
                8,
                12,
                0,
                12));

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

        offeringPanel.setVisible(
            false);

        card.add(
            offeringPanel,
            BorderLayout.CENTER);

        expandButton.addActionListener(
            event -> {

                boolean expanded =
                    !offeringPanel.isVisible();

                offeringPanel.setVisible(
                    expanded);

                expandButton.setText(
                    expanded
                        ? "收起教学班"
                        : "展开教学班");

                card.revalidate();
                card.repaint();

                coursePanel.revalidate();
                coursePanel.repaint();
            });

        return card;
    }

    /**
     * 方案外课程对应的培养方案要求
     * 是否已经被其他课程满足。
     */
    private JLabel createRequirementSatisfiedLabel(
        CourseInfo course) {

        boolean satisfied =
            course.getOfferings()
                .stream()
                .anyMatch(offering ->
                    "REQUIREMENT_SATISFIED".equals(
                        offering.getAvailabilityStatus()));

        if (!satisfied) {
            return null;
        }

        JLabel label =
            new JLabel(
                "✓ 对应培养方案要求已满足");

        label.setFont(
            label.getFont()
                .deriveFont(
                    Font.BOLD));

        return label;
    }
    /**
     * 创建教学班卡片。
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
            offering.getAvailabilityStatus())) {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        new Color(
                            190,
                            45,
                            45)),
                    BorderFactory.createEmptyBorder(
                        10,
                        12,
                        10,
                        12)));

        } else {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(
                        10,
                        12,
                        10,
                        12)));
        }

        JPanel information =
            new JPanel();

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel classLabel =
            new JLabel(
                "教学班："
                    + offering.getClassNo());

        classLabel.setFont(
            classLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    15F));

        JLabel status =
            new JLabel(
                "状态："
                    + availabilityText(
                    offering));

        if ("TIME_CONFLICT".equals(
            offering.getAvailabilityStatus())) {

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

        information.add(
            classLabel);

        information.add(
            Box.createVerticalStrut(5));

        information.add(
            new JLabel(
                "教师："
                    + teacherText(
                    offering)));

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            new JLabel(
                "上课时间："
                    + scheduleText(
                    offering)));

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            new JLabel(
                "地点："
                    + nullableText(
                    offering.getLocationName())
                    + "    校区："
                    + nullableText(
                    offering.getCampusName())));

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            new JLabel(
                "人数："
                    + offering.getSelectedCount()
                    + " / "
                    + offering.getCapacity()
                    + "    剩余："
                    + offering.getRemainingCount()));

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            status);

        String language =
            offering.getTeachingLanguage();

        if (language != null
            && !language.isBlank()
            && !"中文".equals(language)) {

            information.add(
                Box.createVerticalStrut(3));

            information.add(
                new JLabel(
                    "授课语言："
                        + language));
        }

        card.add(
            information,
            BorderLayout.CENTER);

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
     * 选课确认。
     */
    private void confirmAndSelect(
        CourseInfo course,
        OfferingInfo offering,
        JButton button) {

        String message =
            "请确认选择以下方案外课程教学班：\n\n"
                + "课程："
                + course.getCourseName()
                + "（"
                + course.getCourseCode()
                + "）\n"
                + "教学班："
                + offering.getClassNo()
                + "\n"
                + "教师："
                + teacherText(offering)
                + "\n"
                + "时间："
                + scheduleText(offering)
                + "\n"
                + "地点："
                + nullableText(
                offering.getLocationName())
                + "\n"
                + "校区："
                + nullableText(
                offering.getCampusName());

        int result =
            JOptionPane.showConfirmDialog(
                this,
                message,
                "确认选课",
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
     * 提交方案外选课。
     */
    private void submitSelection(
        OfferingInfo offering,
        JButton button) {

        button.setEnabled(
            false);

        button.setText(
            "提交中...");

        statusLabel.setText(
            "正在提交选课请求...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.SELECT_COURSE,
                        new SelectCourseRequest(
                            batch.getBatchId(),
                            offering.getOfferingId()));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (response.isSuccess()) {

                            JOptionPane.showMessageDialog(
                                SubstituteCoursePanel.this,
                                response.getMessage(),
                                "选课成功",
                                JOptionPane.INFORMATION_MESSAGE);

                        } else {

                            JOptionPane.showMessageDialog(
                                SubstituteCoursePanel.this,
                                response.getMessage(),
                                "选课失败",
                                JOptionPane.WARNING_MESSAGE);
                        }

                        loadCourses();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        JOptionPane.showMessageDialog(
                            SubstituteCoursePanel.this,
                            "选课请求被中断。",
                            "选课失败",
                            JOptionPane.ERROR_MESSAGE);

                        loadCourses();

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        JOptionPane.showMessageDialog(
                            SubstituteCoursePanel.this,
                            "无法提交选课请求："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()),
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
            offering.getAvailabilityStatus());
    }

    private String selectButtonText(
        OfferingInfo offering) {

        if (batch.getStatus()
            != SelectionBatchStatus.OPEN
            || !batch.isAllowSelect()) {

            return "当前批次不可选";
        }

        return switch (
            offering.getAvailabilityStatus()) {
            case "REQUIREMENT_SATISFIED" ->
                "培养方案要求已满足";
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

    private boolean hasTimeConflict(
        CourseInfo course) {

        return course.getOfferings()
            .stream()
            .anyMatch(offering ->
                "TIME_CONFLICT".equals(
                    offering.getAvailabilityStatus()));
    }

    private JLabel createConflictLabel(
        CourseInfo course) {

        int total =
            course.getOfferings().size();

        long conflictCount =
            course.getOfferings()
                .stream()
                .filter(offering ->
                    "TIME_CONFLICT".equals(
                        offering.getAvailabilityStatus()))
                .count();

        if (conflictCount == 0) {
            return null;
        }

        String text =
            conflictCount == total
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

            text.append(
                "（单周）");

        } else if ("EVEN".equals(
            schedule.getWeekPattern())) {

            text.append(
                "（双周）");
        }

        return text.toString();
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

    private String availabilityText(
        OfferingInfo offering) {

        if (offering.isSelected()) {
            return "已选";
        }

        return switch (
            offering.getAvailabilityStatus()) {
            case "REQUIREMENT_SATISFIED" ->
                "培养方案要求已满足";
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

            case "OFFERING_CLOSED" ->
                "当前教学班不可选";

            default ->
                "不可选";
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
                "方案外课程加载失败。"));

        coursePanel.revalidate();
        coursePanel.repaint();

        statusLabel.setText(
            message);
    }
}
