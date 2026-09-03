package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.EnrollmentInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 当前学期课表页面。
 *
 * 支持：
 *
 * 1. 学期课表；
 * 2. 周课表；
 * 3. 左右切换教学周；
 * 4. 单双周过滤；
 * 5. 分阶段课程过滤；
 * 6. 同一时间位置多课程显示。
 */
final class TimetablePanel extends JPanel {

    /**
     * 默认显示 12 节课。
     */
    private static final int DEFAULT_MAX_PERIOD = 12;

    /**
     * 如果当前没有课程，
     * 默认认为一个学期有 16 个教学周。
     */
    private static final int DEFAULT_MAX_WEEK = 16;

    /**
     * 周一到周日。
     */
    private static final int DAY_COUNT = 7;

    /**
     * 课表中的一个显示位置。
     */
    private record SlotKey(
        int dayOfWeek,
        int startPeriod,
        int endPeriod) {
    }

    /**
     * 一条等待显示的课程安排。
     */
    private record ScheduledCourse(
        EnrollmentInfo enrollment,
        ScheduleInfo schedule) {
    }

    private final ClientContext context;

    private final SelectionBatchInfo batch;

    private final JScrollPane scrollPane =
        new JScrollPane();

    private final JLabel statusLabel =
        new JLabel(" ");

    /*
     * =========================
     * 课表模式控制
     * =========================
     */

    /**
     * false：
     * 学期课表。
     *
     * true：
     * 周课表。
     */
    private boolean weeklyView = false;

    /**
     * 周课表当前显示的教学周。
     */
    private int currentWeek = 1;

    /**
     * 当前学期最大教学周。
     */
    private int maxWeek =
        DEFAULT_MAX_WEEK;

    /**
     * 最近一次从服务器读取到的选课数据。
     *
     * 切换周数时不重新请求服务器，
     * 直接使用这份数据重新绘制。
     */
    private List<EnrollmentInfo> currentEnrollments =
        List.of();

    private final JButton viewModeButton =
        new JButton(
            "切换到周课表");

    private final JButton previousWeekButton =
        new JButton(
            "←");

    private final JButton nextWeekButton =
        new JButton(
            "→");

    private final JLabel weekLabel =
        new JLabel(
            "第 1 周",
            SwingConstants.CENTER);

    /**
     * 周数切换区域。
     */
    private final JPanel weekControlPanel =
        new JPanel(
            new FlowLayout(
                FlowLayout.RIGHT,
                6,
                0));

    TimetablePanel(
        ClientContext context,
        SelectionBatchInfo batch) {

        this.context = context;
        this.batch = batch;

        initializeView();
    }

    /**
     * 初始化页面。
     */
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

        /*
         * =========================
         * 顶部区域
         * =========================
         */
        add(
            createHeader(),
            BorderLayout.NORTH);

        /*
         * =========================
         * 课表滚动区域
         * =========================
         */
        scrollPane.setBorder(
            BorderFactory.createEmptyBorder());

        scrollPane
            .getVerticalScrollBar()
            .setUnitIncrement(16);

        scrollPane
            .getHorizontalScrollBar()
            .setUnitIncrement(16);

        add(
            scrollPane,
            BorderLayout.CENTER);

        /*
         * =========================
         * 状态文字
         * =========================
         */
        add(
            statusLabel,
            BorderLayout.SOUTH);

        /*
         * 初始控制状态。
         */
        updateControlState();

        /*
         * 初始显示空课表。
         */
        renderTimetable(
            currentEnrollments);
    }

    /**
     * 创建顶部区域。
     */
    private JPanel createHeader() {

        JPanel header =
            new JPanel(
                new BorderLayout(
                    12,
                    0));

        /*
         * =========================
         * 左侧：标题
         * =========================
         */
        JLabel title =
            new JLabel(
                "我的课表");

        title.setFont(
            title.getFont()
                .deriveFont(
                    Font.BOLD,
                    20F));

        header.add(
            title,
            BorderLayout.WEST);

        /*
         * =========================
         * 右侧：控制按钮
         * =========================
         */
        JPanel controls =
            new JPanel(
                new FlowLayout(
                    FlowLayout.RIGHT,
                    8,
                    0));

        JLabel semesterLabel =
            new JLabel(
                "学期："
                    + batch.getSemester());

        controls.add(
            semesterLabel);

        /*
         * 学期 / 周课表切换。
         */
        viewModeButton.addActionListener(
            event ->
                switchViewMode());

        controls.add(
            viewModeButton);

        /*
         * =========================
         * 周数切换
         * =========================
         */
        previousWeekButton.setToolTipText(
            "上一周");

        nextWeekButton.setToolTipText(
            "下一周");

        weekLabel.setPreferredSize(
            new Dimension(
                64,
                28));

        previousWeekButton.addActionListener(
            event ->
                showPreviousWeek());

        nextWeekButton.addActionListener(
            event ->
                showNextWeek());

        weekControlPanel.add(
            previousWeekButton);

        weekControlPanel.add(
            weekLabel);

        weekControlPanel.add(
            nextWeekButton);

        controls.add(
            weekControlPanel);

        header.add(
            controls,
            BorderLayout.EAST);

        return header;
    }

    /**
     * 学期课表 / 周课表切换。
     */
    private void switchViewMode() {

        weeklyView =
            !weeklyView;

        updateControlState();

        renderTimetable(
            currentEnrollments);

        updateStatusText();

        revalidate();
        repaint();
    }

    /**
     * 查看上一周。
     */
    private void showPreviousWeek() {

        if (!weeklyView
            || currentWeek <= 1) {

            return;
        }

        currentWeek--;

        updateControlState();

        renderTimetable(
            currentEnrollments);

        updateStatusText();
    }

    /**
     * 查看下一周。
     */
    private void showNextWeek() {

        if (!weeklyView
            || currentWeek >= maxWeek) {

            return;
        }

        currentWeek++;

        updateControlState();

        renderTimetable(
            currentEnrollments);

        updateStatusText();
    }

    /**
     * 更新顶部按钮状态。
     */
    private void updateControlState() {

        if (weeklyView) {

            viewModeButton.setText(
                "切换到学期课表");

        } else {

            viewModeButton.setText(
                "切换到周课表");
        }

        /*
         * 学期课表状态下，
         * 不显示左右切换按钮。
         */
        weekControlPanel.setVisible(
            weeklyView);

        weekLabel.setText(
            "第 "
                + currentWeek
                + " 周");

        previousWeekButton.setEnabled(
            weeklyView
                && currentWeek > 1);

        nextWeekButton.setEnabled(
            weeklyView
                && currentWeek < maxWeek);
    }

    /**
     * 重新从服务器读取当前学生已选课程。
     */
    void reload() {

        statusLabel.setText(
            "正在加载课表...");

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

                        /*
                         * 保存最近一次服务器数据。
                         */
                        currentEnrollments =
                            List.copyOf(
                                enrollments);

                        /*
                         * 根据课程安排自动计算
                         * 当前学期最大周数。
                         */
                        maxWeek =
                            calculateMaxWeek(
                                currentEnrollments);

                        /*
                         * 防止刷新数据以后，
                         * currentWeek 超出新的最大周。
                         */
                        if (currentWeek > maxWeek) {

                            currentWeek =
                                maxWeek;
                        }

                        if (currentWeek < 1) {

                            currentWeek =
                                1;
                        }

                        updateControlState();

                        renderTimetable(
                            currentEnrollments);

                        updateStatusText();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "课表加载被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载课表："
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

    /**
     * 从 Response 中读取 EnrollmentInfo。
     */
    private List<EnrollmentInfo> readEnrollments(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的课表数据格式错误。");
        }

        List<EnrollmentInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof EnrollmentInfo enrollment)) {

                throw new IllegalStateException(
                    "服务器返回的课表数据格式错误。");
            }

            result.add(
                enrollment);
        }

        return result;
    }

    /**
     * 根据已选课程重新绘制课表。
     */
    private void renderTimetable(
        List<EnrollmentInfo> enrollments) {

        int maxPeriod =
            calculateMaxPeriod(
                enrollments);

        TimetableGridPanel grid =
            new TimetableGridPanel(
                maxPeriod);

        /*
         * 创建星期标题和节次。
         */
        grid.buildBaseGrid();

        /*
         * =========================
         * 按照显示位置分组
         * =========================
         */
        Map<SlotKey, List<ScheduledCourse>> groups =
            new LinkedHashMap<>();

        for (EnrollmentInfo enrollment
            : enrollments) {

            for (ScheduleInfo schedule
                : enrollment.getSchedules()) {

                /*
                 * =========================
                 * 周课表过滤
                 * =========================
                 *
                 * 学期课表：
                 * 所有 Schedule 都显示。
                 *
                 * 周课表：
                 * 只显示 currentWeek
                 * 真正上课的 Schedule。
                 */
                if (weeklyView
                    && !isTeachingWeek(
                    schedule,
                    currentWeek)) {

                    continue;
                }

                SlotKey key =
                    new SlotKey(
                        schedule.getDayOfWeek(),
                        schedule.getStartPeriod(),
                        schedule.getEndPeriod());

                groups
                    .computeIfAbsent(
                        key,
                        ignored ->
                            new ArrayList<>())
                    .add(
                        new ScheduledCourse(
                            enrollment,
                            schedule));
            }
        }

        /*
         * 每一个时间槽只向
         * GridBagLayout 添加一个组件。
         */
        for (Map.Entry<
            SlotKey,
            List<ScheduledCourse>> entry
            : groups.entrySet()) {

            grid.addCourseGroup(
                entry.getKey(),
                entry.getValue());
        }

        scrollPane.setViewportView(
            grid);

        scrollPane.revalidate();
        scrollPane.repaint();
    }

    /**
     * 当前指定教学周是否真的有这门课。
     */
    private static boolean isTeachingWeek(
        ScheduleInfo schedule,
        int week) {

        /*
         * 不在课程教学周范围内。
         */
        if (week
            < schedule.getStartWeek()
            || week
            > schedule.getEndWeek()) {

            return false;
        }

        /*
         * 判断单双周。
         */
        return switch (
            schedule.getWeekPattern()) {

            case "ODD" ->
                week % 2 == 1;

            case "EVEN" ->
                week % 2 == 0;

            /*
             * EVERY，
             * 以及未来未知类型，
             * 默认按每周处理。
             */
            default ->
                true;
        };
    }

    /**
     * 自动计算需要显示到第几节。
     */
    private int calculateMaxPeriod(
        List<EnrollmentInfo> enrollments) {

        int maxPeriod =
            DEFAULT_MAX_PERIOD;

        for (EnrollmentInfo enrollment
            : enrollments) {

            for (ScheduleInfo schedule
                : enrollment.getSchedules()) {

                maxPeriod =
                    Math.max(
                        maxPeriod,
                        schedule.getEndPeriod());
            }
        }

        return maxPeriod;
    }

    /**
     * 自动计算当前学期最大教学周。
     */
    private int calculateMaxWeek(
        List<EnrollmentInfo> enrollments) {

        int result =
            DEFAULT_MAX_WEEK;

        for (EnrollmentInfo enrollment
            : enrollments) {

            for (ScheduleInfo schedule
                : enrollment.getSchedules()) {

                result =
                    Math.max(
                        result,
                        schedule.getEndWeek());
            }
        }

        return result;
    }

    /**
     * 更新底部状态文字。
     */
    private void updateStatusText() {

        if (!weeklyView) {

            statusLabel.setText(
                "当前课表包含 "
                    + currentEnrollments.size()
                    + " 个教学班");

            return;
        }

        int visibleCount = 0;

        /*
         * 一个教学班即使一周上两次课，
         * 这里也只计算一次。
         */
        for (EnrollmentInfo enrollment
            : currentEnrollments) {

            boolean visible = false;

            for (ScheduleInfo schedule
                : enrollment.getSchedules()) {

                if (isTeachingWeek(
                    schedule,
                    currentWeek)) {

                    visible =
                        true;

                    break;
                }
            }

            if (visible) {

                visibleCount++;
            }
        }

        statusLabel.setText(
            "第 "
                + currentWeek
                + " 周共有 "
                + visibleCount
                + " 个教学班有课");
    }

    /**
     * 课表主体。
     */
    private static final class TimetableGridPanel
        extends JPanel {

        private final int maxPeriod;

        private final JLabel cornerLabel =
            new JLabel(
                "节次",
                SwingConstants.CENTER);

        private final JLabel[] dayLabels =
            new JLabel[DAY_COUNT];

        private final JLabel[] periodLabels;

        TimetableGridPanel(
            int maxPeriod) {

            super(
                new GridBagLayout());

            this.maxPeriod =
                maxPeriod;

            this.periodLabels =
                new JLabel[maxPeriod];

            setOpaque(
                true);

            Color background =
                UIManager.getColor(
                    "Panel.background");

            if (background != null) {

                setBackground(
                    background);
            }

            setPreferredSize(
                new Dimension(
                    980,
                    42
                        + maxPeriod * 64));
        }

        /**
         * 创建星期标题和节次。
         */
        void buildBaseGrid() {

            /*
             * =========================
             * 左上角
             * =========================
             */
            GridBagConstraints corner =
                new GridBagConstraints();

            corner.gridx = 0;
            corner.gridy = 0;

            corner.fill =
                GridBagConstraints.BOTH;

            corner.weightx = 0;
            corner.weighty = 0;

            cornerLabel.setPreferredSize(
                new Dimension(
                    60,
                    42));

            cornerLabel.setFont(
                cornerLabel.getFont()
                    .deriveFont(
                        Font.BOLD));

            add(
                cornerLabel,
                corner);

            /*
             * =========================
             * 星期标题
             * =========================
             */
            for (int day = 1;
                 day <= DAY_COUNT;
                 day++) {

                JLabel label =
                    new JLabel(
                        dayText(day),
                        SwingConstants.CENTER);

                label.setFont(
                    label.getFont()
                        .deriveFont(
                            Font.BOLD));

                label.setPreferredSize(
                    new Dimension(
                        130,
                        42));

                dayLabels[day - 1] =
                    label;

                GridBagConstraints constraints =
                    new GridBagConstraints();

                constraints.gridx =
                    day;

                constraints.gridy =
                    0;

                constraints.fill =
                    GridBagConstraints.BOTH;

                constraints.weightx =
                    1;

                constraints.weighty =
                    0;

                add(
                    label,
                    constraints);
            }

            /*
             * =========================
             * 左侧节次
             * =========================
             */
            for (int period = 1;
                 period <= maxPeriod;
                 period++) {

                JLabel label =
                    new JLabel(
                        "第"
                            + period
                            + "节",
                        SwingConstants.CENTER);

                label.setPreferredSize(
                    new Dimension(
                        60,
                        64));

                periodLabels[period - 1] =
                    label;

                GridBagConstraints constraints =
                    new GridBagConstraints();

                constraints.gridx =
                    0;

                constraints.gridy =
                    period;

                constraints.fill =
                    GridBagConstraints.BOTH;

                constraints.weightx =
                    0;

                constraints.weighty =
                    1;

                add(
                    label,
                    constraints);
            }
        }

        /**
         * 加入一个时间槽。
         */
        void addCourseGroup(
            SlotKey slot,
            List<ScheduledCourse> courses) {

            int day =
                slot.dayOfWeek();

            int startPeriod =
                slot.startPeriod();

            int endPeriod =
                slot.endPeriod();

            if (day < 1
                || day > DAY_COUNT
                || startPeriod < 1
                || endPeriod < startPeriod
                || endPeriod > maxPeriod) {

                return;
            }

            /*
             * 时间槽容器。
             */
            JPanel group =
                new JPanel();

            group.setLayout(
                new BoxLayout(
                    group,
                    BoxLayout.Y_AXIS));

            group.setOpaque(
                false);

            /*
             * 同一位置的课程
             * 上下排列。
             */
            for (int index = 0;
                 index < courses.size();
                 index++) {

                ScheduledCourse scheduledCourse =
                    courses.get(index);

                JPanel card =
                    createCourseCard(
                        scheduledCourse.enrollment(),
                        scheduledCourse.schedule());

                card.setAlignmentX(
                    Component.LEFT_ALIGNMENT);

                card.setMaximumSize(
                    new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE));

                group.add(
                    card);

                if (index
                    < courses.size() - 1) {

                    group.add(
                        Box.createVerticalStrut(
                            4));
                }
            }

            GridBagConstraints constraints =
                new GridBagConstraints();

            constraints.gridx =
                day;

            constraints.gridy =
                startPeriod;

            /*
             * 多节课程合并。
             */
            constraints.gridheight =
                endPeriod
                    - startPeriod
                    + 1;

            constraints.fill =
                GridBagConstraints.BOTH;

            constraints.weightx =
                1;

            constraints.weighty =
                0;

            constraints.insets =
                new Insets(
                    3,
                    4,
                    3,
                    4);

            add(
                group,
                constraints);
        }

        /**
         * 创建单个课程块。
         */
        private JPanel createCourseCard(
            EnrollmentInfo enrollment,
            ScheduleInfo schedule) {

            JPanel card =
                new JPanel(
                    new BorderLayout());

            Color background =
                UIManager.getColor(
                    "Table.selectionBackground");

            Color foreground =
                UIManager.getColor(
                    "Table.selectionForeground");

            if (background != null) {

                card.setBackground(
                    background);
            }

            card.setOpaque(
                true);

            card.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        borderColor()),
                    BorderFactory.createEmptyBorder(
                        7,
                        8,
                        7,
                        8)));

            String teachers =
                enrollment
                    .getTeacherNames()
                    .isEmpty()
                    ? "未安排教师"
                    : String.join(
                    "、",
                    enrollment.getTeacherNames());

            String location =
                enrollment.getLocationName();

            if (location == null
                || location.isBlank()) {

                location =
                    "未安排教室";
            }

            String text =
                "<html>"
                    + "<b>"
                    + html(
                    enrollment.getCourseName())
                    + "</b>"
                    + "<br>"
                    + "第"
                    + schedule.getStartPeriod()
                    + "-"
                    + schedule.getEndPeriod()
                    + "节"
                    + " · "
                    + html(location)
                    + "<br>"
                    + html(teachers)
                    + "<br>"
                    + html(
                    weekText(schedule))
                    + "</html>";

            JLabel label =
                new JLabel(
                    text);

            label.setVerticalAlignment(
                SwingConstants.TOP);

            if (foreground != null) {

                label.setForeground(
                    foreground);
            }

            card.add(
                label,
                BorderLayout.CENTER);

            return card;
        }

        /**
         * 绘制网格线。
         */
        @Override
        protected void paintComponent(
            Graphics graphics) {

            super.paintComponent(
                graphics);

            Color line =
                UIManager.getColor(
                    "Separator.foreground");

            if (line == null) {

                line =
                    new Color(
                        210,
                        210,
                        210);
            }

            graphics.setColor(
                line);

            /*
             * =========================
             * 竖线
             * =========================
             */
            graphics.drawLine(
                cornerLabel.getX(),
                0,
                cornerLabel.getX(),
                getHeight());

            graphics.drawLine(
                cornerLabel.getX()
                    + cornerLabel.getWidth(),
                0,
                cornerLabel.getX()
                    + cornerLabel.getWidth(),
                getHeight());

            for (JLabel dayLabel
                : dayLabels) {

                if (dayLabel == null) {
                    continue;
                }

                int right =
                    dayLabel.getX()
                        + dayLabel.getWidth();

                graphics.drawLine(
                    right,
                    0,
                    right,
                    getHeight());
            }

            /*
             * =========================
             * 横线
             * =========================
             */
            int headerBottom =
                cornerLabel.getY()
                    + cornerLabel.getHeight();

            graphics.drawLine(
                0,
                headerBottom,
                getWidth(),
                headerBottom);

            for (JLabel periodLabel
                : periodLabels) {

                if (periodLabel == null) {
                    continue;
                }

                int bottom =
                    periodLabel.getY()
                        + periodLabel.getHeight();

                graphics.drawLine(
                    0,
                    bottom,
                    getWidth(),
                    bottom);
            }
        }

        /**
         * 获取课程块边框颜色。
         */
        private static Color borderColor() {

            Color color =
                UIManager.getColor(
                    "Component.borderColor");

            if (color != null) {

                return color;
            }

            return new Color(
                180,
                180,
                180);
        }
    }

    /**
     * 教学周文字。
     */
    private static String weekText(
        ScheduleInfo schedule) {

        StringBuilder value =
            new StringBuilder();

        value.append("第")
            .append(
                schedule.getStartWeek())
            .append("-")
            .append(
                schedule.getEndWeek())
            .append("周");

        if ("ODD".equals(
            schedule.getWeekPattern())) {

            value.append(
                " · 单周");

        } else if ("EVEN".equals(
            schedule.getWeekPattern())) {

            value.append(
                " · 双周");
        }

        return value.toString();
    }

    /**
     * 星期文字。
     */
    private static String dayText(
        int dayOfWeek) {

        return switch (dayOfWeek) {

            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";

            default -> "未知";
        };
    }

    /**
     * Swing HTML 简单转义。
     */
    private static String html(
        String value) {

        if (value == null) {

            return "";
        }

        return value
            .replace(
                "&",
                "&amp;")
            .replace(
                "<",
                "&lt;")
            .replace(
                ">",
                "&gt;");
    }

    /**
     * 显示加载失败。
     */
    private void showError(
        String message) {

        JPanel error =
            new JPanel(
                new BorderLayout());

        error.add(
            new JLabel(
                "课表加载失败。",
                SwingConstants.CENTER),
            BorderLayout.CENTER);

        scrollPane.setViewportView(
            error);

        statusLabel.setText(
            message);
    }
}
