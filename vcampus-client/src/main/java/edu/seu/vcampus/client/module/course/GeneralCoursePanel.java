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
    private final Runnable onEnrollmentChanged;
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
        SelectionBatchInfo batch,
        Runnable onEnrollmentChanged) {

        this.context = context;
        this.batch = batch;
        this.onEnrollmentChanged = onEnrollmentChanged;

        initializeView();
        loadCourses();
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
         * 标题 + 筛选
         * =========================
         */
        JPanel header =
            new JPanel(
                new BorderLayout(
                    18,
                    0));

        header.setOpaque(
            false);

        JPanel titleArea =
            new JPanel();

        titleArea.setOpaque(
            false);

        titleArea.setLayout(
            new BoxLayout(
                titleArea,
                BoxLayout.Y_AXIS));

        JLabel title =
            CourseTheme.title(
                "通选课");

        JLabel subtitle =
            CourseTheme.subtitle(
                "按照通选类别查看当前开放课程");

        titleArea.add(
            title);

        titleArea.add(
            Box.createVerticalStrut(
                5));

        titleArea.add(
            subtitle);

        header.add(
            titleArea,
            BorderLayout.CENTER);

        CourseTheme.SurfacePanel filter =
            new CourseTheme.SurfacePanel();

        filter.setLayout(
            new FlowLayout(
                FlowLayout.RIGHT,
                10,
                9));

        filter.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    CourseTheme.BORDER),
                BorderFactory.createEmptyBorder(
                    2,
                    8,
                    2,
                    8)));

        JLabel filterLabel =
            new JLabel(
                "通选类别");

        filterLabel.setForeground(
            CourseTheme.MUTED);

        categoryBox.setBackground(
            Color.WHITE);

        filter.add(
            filterLabel);

        filter.add(
            categoryBox);

        header.add(
            filter,
            BorderLayout.EAST);

        add(
            header,
            BorderLayout.NORTH);

        /*
         * =========================
         * 课程列表
         * =========================
         */
        coursePanel.setLayout(
            new BoxLayout(
                coursePanel,
                BoxLayout.Y_AXIS));

        coursePanel.setBackground(
            CourseTheme.BACKGROUND);

        JScrollPane scroll =
            new JScrollPane(
                coursePanel);

        scroll.setBorder(
            BorderFactory
                .createEmptyBorder());

        scroll.setBackground(
            CourseTheme.BACKGROUND);

        scroll
            .getViewport()
            .setBackground(
                CourseTheme.BACKGROUND);

        scroll
            .getVerticalScrollBar()
            .setUnitIncrement(
                14);

        add(
            scroll,
            BorderLayout.CENTER);

        /*
         * 状态栏。
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

        CourseTheme.SurfacePanel card =
            new CourseTheme.SurfacePanel();

        card.setLayout(
            new BorderLayout(
                0,
                10));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        if (hasTimeConflict(
            course)) {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        CourseTheme.DANGER,
                        2),
                    BorderFactory.createEmptyBorder(
                        15,
                        18,
                        15,
                        18)));

        } else {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        CourseTheme.BORDER),
                    BorderFactory.createEmptyBorder(
                        15,
                        18,
                        15,
                        18)));
        }

        /*
         * =========================
         * 课程摘要
         * =========================
         */
        JPanel summary =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

        summary.setOpaque(
            false);

        JPanel information =
            new JPanel();

        information.setOpaque(
            false);

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel name =
            new JLabel(
                course.getCourseName());

        name.setForeground(
            CourseTheme.TEXT);

        name.setFont(
            name.getFont()
                .deriveFont(
                    Font.BOLD,
                    17F));

        JLabel code =
            new JLabel(
                "课程号："
                    + course.getCourseCode());

        code.setForeground(
            CourseTheme.MUTED);

        JLabel detail =
            new JLabel(
                "学分："
                    + course.getCredits()
                    + "    类型："
                    + course.getCourseType());

        detail.setForeground(
            CourseTheme.MUTED);

        JLabel selected =
            new JLabel(
                course.isSelected()
                    ? "● 已选"
                    : "● 未选");

        selected.setFont(
            selected.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        selected.setForeground(
            course.isSelected()
                ? CourseTheme.SUCCESS
                : CourseTheme.MUTED);

        information.add(
            name);

        information.add(
            Box.createVerticalStrut(
                7));

        information.add(
            code);

        information.add(
            Box.createVerticalStrut(
                4));

        information.add(
            detail);

        JLabel conflictLabel =
            createConflictLabel(
                course);

        if (conflictLabel != null) {

            conflictLabel.setForeground(
                CourseTheme.DANGER);

            information.add(
                Box.createVerticalStrut(
                    7));

            information.add(
                conflictLabel);
        }

        information.add(
            Box.createVerticalStrut(
                6));

        information.add(
            selected);

        summary.add(
            information,
            BorderLayout.CENTER);

        JButton expand =
            new JButton(
                "展开教学班");

        CourseTheme.styleQuietButton(
            expand);

        summary.add(
            expand,
            BorderLayout.EAST);

        card.add(
            summary,
            BorderLayout.NORTH);

        /*
         * =========================
         * 教学班区域
         * =========================
         */
        JPanel offeringPanel =
            new JPanel();

        offeringPanel.setOpaque(
            false);

        offeringPanel.setLayout(
            new BoxLayout(
                offeringPanel,
                BoxLayout.Y_AXIS));

        offeringPanel.setBorder(
            BorderFactory.createEmptyBorder(
                12,
                8,
                0,
                8));

        if (course.getOfferings()
            .isEmpty()) {

            JLabel empty =
                new JLabel(
                    "本课程当前没有教学班。");

            empty.setForeground(
                CourseTheme.MUTED);

            offeringPanel.add(
                empty);

        } else {

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
        }

        /*
         * 通选课现在也和其他课程一样：
         * 默认收起。
         */
        offeringPanel.setVisible(
            false);

        card.add(
            offeringPanel,
            BorderLayout.CENTER);

        expand.addActionListener(
            event -> {

                boolean visible =
                    !offeringPanel
                        .isVisible();

                offeringPanel.setVisible(
                    visible);

                expand.setText(
                    visible
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
     * 教学班卡片。
     */
    private JPanel createOfferingCard(
        CourseInfo course,
        OfferingInfo offering) {

        CourseTheme.SurfacePanel card =
            new CourseTheme.SurfacePanel(
                new Color(
                    249,
                    251,
                    250),
                14);

        card.setLayout(
            new BorderLayout(
                18,
                0));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        String availabilityStatus =
            offering.getAvailabilityStatus();

        if ("TIME_CONFLICT".equals(
            availabilityStatus)) {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        CourseTheme.DANGER),
                    BorderFactory.createEmptyBorder(
                        12,
                        14,
                        12,
                        14)));

        } else {

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        CourseTheme.BORDER),
                    BorderFactory.createEmptyBorder(
                        12,
                        14,
                        12,
                        14)));
        }

        JPanel info =
            new JPanel();

        info.setOpaque(
            false);

        info.setLayout(
            new BoxLayout(
                info,
                BoxLayout.Y_AXIS));

        JLabel classLabel =
            new JLabel(
                "教学班 "
                    + offering.getClassNo());

        classLabel.setForeground(
            CourseTheme.TEXT);

        classLabel.setFont(
            classLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    15F));

        JLabel teacher =
            new JLabel(
                "教师："
                    + teacherText(
                    offering));

        JLabel schedule =
            new JLabel(
                "上课时间："
                    + scheduleText(
                    offering));

        JLabel location =
            new JLabel(
                "地点："
                    + nullableText(
                    offering.getLocationName())
                    + "    校区："
                    + nullableText(
                    offering.getCampusName()));

        JLabel capacity =
            new JLabel(
                "人数："
                    + offering.getSelectedCount()
                    + " / "
                    + offering.getCapacity()
                    + "    剩余："
                    + offering.getRemainingCount());

        teacher.setForeground(
            CourseTheme.MUTED);

        schedule.setForeground(
            CourseTheme.MUTED);

        location.setForeground(
            CourseTheme.MUTED);

        capacity.setForeground(
            CourseTheme.MUTED);

        JLabel status =
            new JLabel(
                "● "
                    + availabilityText(
                    offering));

        status.setFont(
            status.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        status.setForeground(
            availabilityColor(
                availabilityStatus));

        info.add(
            classLabel);

        info.add(
            Box.createVerticalStrut(
                7));

        info.add(
            teacher);

        info.add(
            Box.createVerticalStrut(
                4));

        info.add(
            schedule);

        info.add(
            Box.createVerticalStrut(
                4));

        info.add(
            location);

        info.add(
            Box.createVerticalStrut(
                4));

        info.add(
            capacity);

        info.add(
            Box.createVerticalStrut(
                6));

        info.add(
            status);

        card.add(
            info,
            BorderLayout.CENTER);

        /*
         * =========================
         * 选课 / 退课按钮
         * =========================
         */
        JButton actionButton =
            new JButton(
                actionButtonText(
                    offering));

        boolean actionable =
            canUseAction(
                offering);

        if (actionable) {

            CourseTheme.stylePrimaryButton(
                actionButton);

        } else {

            CourseTheme.styleQuietButton(
                actionButton);
        }

        actionButton.setEnabled(
            actionable);

        actionButton.addActionListener(
            event -> {

                if (offering.isSelected()) {

                    CourseDropSupport.confirmAndDrop(
                        this,
                        context,
                        batch,
                        course,
                        offering,
                        actionButton,
                        statusLabel,
                        this::loadCourses);

                } else {

                    confirmAndSelect(
                        course,
                        offering,
                        actionButton);
                }
            });

        card.add(
            actionButton,
            BorderLayout.EAST);

        return card;
    }
    /**
     * 当前教学班是否可以执行选课或退课。
     */
    private boolean canUseAction(
        OfferingInfo offering) {

        if (offering.isSelected()) {

            return batch.getStatus()
                == SelectionBatchStatus.OPEN
                && batch.isAllowDrop();
        }

        return canSelect(
            offering);
    }

    /**
     * 教学班右侧操作按钮文字。
     */
    private String actionButtonText(
        OfferingInfo offering) {

        if (offering.isSelected()) {

            return batch.getStatus()
                == SelectionBatchStatus.OPEN
                && batch.isAllowDrop()
                ? "退课"
                : "当前批次不可退";
        }

        return selectButtonText(
            offering);
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
     * 通选教学班状态颜色。
     */
    private Color availabilityColor(
        String status) {

        if (status == null) {
            return CourseTheme.MUTED;
        }

        return switch (status) {

            case "AVAILABLE",
                 "SELECTED" ->
                CourseTheme.SUCCESS;

            case "TIME_CONFLICT" ->
                CourseTheme.DANGER;

            case "FULL",
                 "NOT_ELIGIBLE" ->
                CourseTheme.WARNING;

            case "COURSE_ALREADY_SELECTED" ->
                CourseTheme.PRIMARY_DARK;

            case "OFFERING_CLOSED" ->
                CourseTheme.MUTED;

            default ->
                CourseTheme.MUTED;
        };
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
