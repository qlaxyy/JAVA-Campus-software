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
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 方案内课程页面。
 */
final class PlanCoursePanel extends JPanel {

    private final ClientContext context;
    private final SelectionBatchInfo batch;

    private final JPanel coursePanel =
        new JPanel();

    private final JLabel statusLabel =
        new JLabel("正在加载方案内课程...");

    PlanCoursePanel(
        ClientContext context,
        SelectionBatchInfo batch) {

        this.context = context;
        this.batch = batch;

        initializeView();
        loadCourses();
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

        JLabel title =
            new JLabel("方案内课程");

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
     * 从服务器加载方案内课程。
     */
    private void loadCourses() {

        if (context.currentSession().isEmpty()) {

            showError(
                "登录状态已失效，请重新登录。");

            return;
        }

        statusLabel.setText(
            "正在加载方案内课程...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.LIST_PLAN_COURSES,
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
                                + " 门方案内课程");

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showError(
                            "加载方案内课程被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showError(
                            "无法加载方案内课程："
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
     * 从 Response 中读取课程列表。
     */
    private List<CourseInfo> readCourses(
        Response response) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的方案内课程数据格式错误。");
        }

        List<CourseInfo> result =
            new ArrayList<>();

        for (Object value : values) {

            if (!(value
                instanceof CourseInfo course)) {

                throw new IllegalStateException(
                    "服务器返回的方案内课程数据格式错误。");
            }

            result.add(course);
        }

        return result;
    }

    /**
     * 显示所有课程。
     */
    private void renderCourses(
        List<CourseInfo> courses) {

        coursePanel.removeAll();

        if (courses.isEmpty()) {

            coursePanel.add(
                new JLabel(
                    "当前没有可显示的方案内课程。"));

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
            LEFT_ALIGNMENT);

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory
                    .createEtchedBorder(),
                BorderFactory
                    .createEmptyBorder(
                        14,
                        16,
                        14,
                        16)));

        /*
         * =====================
         * 课程一级信息
         * =====================
         */
        JPanel summaryPanel =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

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

        JLabel codeLabel =
            new JLabel(
                "课程号："
                    + course.getCourseCode());

        JLabel detailLabel =
            new JLabel(
                "学分："
                    + course.getCredits()
                    + "    类型："
                    + course.getCourseType());

        JLabel selectedLabel =
            new JLabel(
                course.isSelected()
                    ? "状态：已选"
                    : "状态：未选");

        information.add(
            nameLabel);

        information.add(
            Box.createVerticalStrut(6));

        information.add(
            codeLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            detailLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            selectedLabel);

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

        /*
         * =====================
         * 教学班区域
         * =====================
         */
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

        if (course.getOfferings().isEmpty()) {

            offeringPanel.add(
                new JLabel(
                    "本课程当前没有教学班。"));

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

        offeringPanel.setVisible(
            false);

        card.add(
            offeringPanel,
            BorderLayout.CENTER);

        /*
         * 展开 / 收起。
         */
        expandButton.addActionListener(
            event -> {

                boolean expanded =
                    !offeringPanel
                        .isVisible();

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
            LEFT_ALIGNMENT);

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory
                    .createEtchedBorder(),
                BorderFactory
                    .createEmptyBorder(
                        10,
                        12,
                        10,
                        12)));

        /*
         * 左侧教学班信息。
         */
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

        JLabel teacherLabel =
            new JLabel(
                "教师："
                    + teacherText(
                    offering));

        JLabel scheduleLabel =
            new JLabel(
                "上课时间："
                    + scheduleText(
                    offering));

        JLabel locationLabel =
            new JLabel(
                "地点："
                    + nullableText(
                    offering
                        .getLocationName())
                    + "    校区："
                    + nullableText(
                    offering
                        .getCampusName()));

        JLabel capacityLabel =
            new JLabel(
                "人数："
                    + offering.getSelectedCount()
                    + " / "
                    + offering.getCapacity()
                    + "    剩余："
                    + offering.getRemainingCount());

        JLabel availabilityLabel =
            new JLabel(
                "状态："
                    + availabilityText(
                    offering));

        information.add(
            classLabel);

        information.add(
            Box.createVerticalStrut(5));

        information.add(
            teacherLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            scheduleLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            locationLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            capacityLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(
            availabilityLabel);

        /*
         * 中文为默认语言，
         * 双语 / 全英文才额外显示。
         */
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

        /*
         * =====================
         * 选择按钮
         * =====================
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
     * 确认并提交选课请求。
     */
    private void confirmAndSelect(
        CourseInfo course,
        OfferingInfo offering,
        JButton selectButton) {

        String message =
            "请确认选择以下教学班：\n\n"
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
            selectButton);
    }

    /**
     * 向服务器提交选课请求。
     */
    private void submitSelection(
        OfferingInfo offering,
        JButton selectButton) {

        selectButton.setEnabled(
            false);

        selectButton.setText(
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
                            offering
                                .getOfferingId()));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        if (response.isSuccess()) {

                            JOptionPane.showMessageDialog(
                                PlanCoursePanel.this,
                                response.getMessage(),
                                "选课成功",
                                JOptionPane.INFORMATION_MESSAGE);

                        } else {

                            JOptionPane.showMessageDialog(
                                PlanCoursePanel.this,
                                response.getMessage(),
                                "选课失败",
                                JOptionPane.WARNING_MESSAGE);
                        }

                        /*
                         * 无论成功还是失败，
                         * 都重新从服务器获取最新状态。
                         */
                        loadCourses();

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        JOptionPane.showMessageDialog(
                            PlanCoursePanel.this,
                            "选课请求被中断。",
                            "选课失败",
                            JOptionPane.ERROR_MESSAGE);

                        loadCourses();

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        JOptionPane.showMessageDialog(
                            PlanCoursePanel.this,
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

    /**
     * 当前教学班是否可以点击“选择”。
     */
    private boolean canSelect(
        OfferingInfo offering) {

        /*
         * 批次不开放时绝对不能操作。
         */
        if (batch.getStatus()
            != SelectionBatchStatus.OPEN) {

            return false;
        }

        if (!batch.isAllowSelect()) {

            return false;
        }

        /*
         * 服务器返回 AVAILABLE
         * 才允许点击。
         */
        return "AVAILABLE".equals(
            offering.getAvailabilityStatus());
    }

    /**
     * 选择按钮显示文字。
     */
    private String selectButtonText(
        OfferingInfo offering) {

        if (batch.getStatus()
            != SelectionBatchStatus.OPEN
            || !batch.isAllowSelect()) {

            return "当前批次不可选";
        }

        if (offering.isSelected()) {

            return "已选";
        }

        return switch (
            offering.getAvailabilityStatus()) {

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

            case "NOT_ELIGIBLE" ->
                "不可选";

            case "OFFERING_CLOSED" ->
                "不可选";

            default ->
                "不可选";
        };
    }

    /**
     * 教师显示。
     */
    private String teacherText(
        OfferingInfo offering) {

        if (offering
            .getTeacherNames()
            .isEmpty()) {

            return "未安排";
        }

        return String.join(
            "、",
            offering.getTeacherNames());
    }

    /**
     * 教学班所有上课时间。
     */
    private String scheduleText(
        OfferingInfo offering) {

        if (offering
            .getSchedules()
            .isEmpty()) {

            return "未安排";
        }

        List<String> values =
            new ArrayList<>();

        for (ScheduleInfo schedule
            : offering.getSchedules()) {

            values.add(
                singleScheduleText(
                    schedule));
        }

        return String.join(
            "；",
            values);
    }

    /**
     * 单条时间记录转换成中文。
     */
    private String singleScheduleText(
        ScheduleInfo schedule) {

        StringBuilder text =
            new StringBuilder();

        text.append(
            dayText(
                schedule.getDayOfWeek()));

        text.append(
                " 第")
            .append(
                schedule.getStartPeriod())
            .append("-")
            .append(
                schedule.getEndPeriod())
            .append("节");

        text.append(
                " 第")
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

    /**
     * 星期转换。
     */
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

    /**
     * 教学班状态转换成中文。
     */
    private String availabilityText(
        OfferingInfo offering) {

        if (offering.isSelected()) {

            return "已选";
        }

        return switch (
            offering.getAvailabilityStatus()) {

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

    /**
     * 空字符串显示处理。
     */
    private String nullableText(
        String value) {

        if (value == null
            || value.isBlank()) {

            return "未安排";
        }

        return value;
    }

    /**
     * 加载失败。
     */
    private void showError(
        String message) {

        coursePanel.removeAll();

        coursePanel.add(
            new JLabel(
                "方案内课程加载失败。"));

        coursePanel.revalidate();
        coursePanel.repaint();

        statusLabel.setText(
            message);
    }
}
