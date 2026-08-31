package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
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

    /**
     * 初始化界面。
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
     * 重新加载。
     */
    void reload() {

        loadCourses();
    }

    /**
     * 从服务器获取方案外课程。
     */
    private void loadCourses() {

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

    /**
     * 读取服务器返回课程。
     */
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

    /**
     * 渲染课程。
     */
    private void renderCourses(
        List<CourseInfo> courses) {

        coursePanel.removeAll();

        if (courses.isEmpty()) {

            coursePanel.add(
                new JLabel(
                    "当前没有方案外课程。"));

        } else {

            for (CourseInfo course
                : courses) {

                coursePanel.add(
                    createCourseCard(
                        course));

                coursePanel.add(
                    Box.createVerticalStrut(
                        10));
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
            new JPanel();

        card.setLayout(
            new BoxLayout(
                card,
                BoxLayout.Y_AXIS));

        card.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(
                    12,
                    14,
                    12,
                    14)));

        JLabel nameLabel =
            new JLabel(
                course.getCourseName());

        nameLabel.setFont(
            nameLabel.getFont()
                .deriveFont(
                    Font.BOLD,
                    16F));

        card.add(
            nameLabel);

        card.add(
            Box.createVerticalStrut(
                5));

        card.add(
            new JLabel(
                "课程号："
                    + course.getCourseCode()));

        card.add(
            new JLabel(
                "学分："
                    + course.getCredits()
                    + "    类型："
                    + course.getCourseType()));

        card.add(
            Box.createVerticalStrut(
                8));

        /*
         * 当前阶段先简单显示教学班。
         * 下一步再抽取通用课程卡片并接选课按钮。
         */
        for (OfferingInfo offering
            : course.getOfferings()) {

            String teachers =
                offering
                    .getTeacherNames()
                    .isEmpty()
                    ? "未安排"
                    : String.join(
                    "、",
                    offering.getTeacherNames());

            JLabel offeringLabel =
                new JLabel(
                    "教学班 "
                        + offering.getClassNo()
                        + "    教师："
                        + teachers
                        + "    剩余："
                        + offering.getRemainingCount()
                        + "    状态："
                        + statusText(
                        offering.getAvailabilityStatus()));

            card.add(
                offeringLabel);

            card.add(
                Box.createVerticalStrut(
                    4));
        }

        return card;
    }

    /**
     * 状态转换。
     */
    private String statusText(
        String status) {

        return switch (status) {

            case "AVAILABLE" ->
                "可选";

            case "FULL" ->
                "人数已满";

            case "TIME_CONFLICT" ->
                "时间冲突";

            case "SELECTED" ->
                "已选";

            case "COURSE_ALREADY_SELECTED" ->
                "已选其他教学班";

            case "NOT_ELIGIBLE" ->
                "不符合选课条件";

            case "OFFERING_CLOSED" ->
                "当前教学班不可选";

            default ->
                "不可选";
        };
    }

    /**
     * 错误显示。
     */
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
