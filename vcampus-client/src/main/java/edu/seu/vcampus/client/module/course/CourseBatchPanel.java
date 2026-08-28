package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.course.SelectionBatchStatus;
import edu.seu.vcampus.common.course.SelectionBatchType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

/**
 * 一个具体选课批次的主页面。
 */
final class CourseBatchPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClientContext context;
    private final SelectionBatchInfo batch;

    CourseBatchPanel(
        ClientContext context,
        SelectionBatchInfo batch,
        Runnable goBack) {

        this.context = context;
        this.batch = batch;

        initializeView(goBack);
    }

    /**
     * 初始化批次页面。
     */
    private void initializeView(
        Runnable goBack) {

        setLayout(
            new BorderLayout(
                0,
                16));

        setBorder(
            BorderFactory.createEmptyBorder(
                20,
                24,
                20,
                24));

        add(
            createHeader(goBack),
            BorderLayout.NORTH);

        add(
            createTabs(),
            BorderLayout.CENTER);
    }

    /**
     * 创建批次顶部信息。
     */
    private JPanel createHeader(
        Runnable goBack) {

        JPanel header =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

        JButton backButton =
            new JButton(
                "返回选课中心");

        backButton.addActionListener(
            event -> goBack.run());

        header.add(
            backButton,
            BorderLayout.WEST);

        JPanel information =
            new JPanel();

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel nameLabel =
            new JLabel(
                batch.getBatchName());

        nameLabel.setFont(
            nameLabel
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    22F));

        JLabel semesterLabel =
            new JLabel(
                "学期："
                    + batch.getSemester());

        JLabel timeLabel =
            new JLabel(
                "开放时间："
                    + TIME_FORMAT.format(
                    batch.getStartTime())
                    + " ～ "
                    + TIME_FORMAT.format(
                    batch.getEndTime()));

        JLabel statusLabel =
            new JLabel(
                "状态："
                    + statusText(
                    batch.getStatus()));

        information.add(nameLabel);

        information.add(
            Box.createVerticalStrut(5));

        information.add(semesterLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(timeLabel);

        information.add(
            Box.createVerticalStrut(3));

        information.add(statusLabel);

        header.add(
            information,
            BorderLayout.CENTER);

        return header;
    }

    /**
     * 创建批次内部功能标签。
     */
    private JTabbedPane createTabs() {

        JTabbedPane tabs =
            new JTabbedPane();

        /*
         * 方案内课程已经接入真实服务器请求。
         */
        tabs.addTab(
            "方案内课程",
            new PlanCoursePanel(
                context,
                batch));

        /*
         * 其余页面暂时还是占位页面。
         */
        tabs.addTab(
            "方案外课程",
            createPlaceholder(
                "方案外课程"));

        /*
         * 体育课和通选课不能重修，
         * 所以重修批次不显示这两个标签。
         */
        if (batch.getBatchType()
            != SelectionBatchType.RETAKE) {

            tabs.addTab(
                "体育课",
                createPlaceholder(
                    "体育课"));

            tabs.addTab(
                "通选课",
                createPlaceholder(
                    "通选课"));
        }

        tabs.addTab(
            "已选课程",
            createPlaceholder(
                "已选课程"));

        tabs.addTab(
            "全校课程查询",
            createPlaceholder(
                "全校课程查询"));

        return tabs;
    }

    /**
     * 当前阶段暂时使用的占位页面。
     */
    private JPanel createPlaceholder(
        String name) {

        JPanel panel =
            new JPanel(
                new BorderLayout());

        JLabel label =
            new JLabel(
                name
                    + "页面将在后续实现。",
                JLabel.CENTER);

        label.setFont(
            label
                .getFont()
                .deriveFont(
                    Font.PLAIN,
                    18F));

        panel.add(
            label,
            BorderLayout.CENTER);

        return panel;
    }

    /**
     * 将批次状态转换成中文。
     */
    private String statusText(
        SelectionBatchStatus status) {

        return switch (status) {
            case NOT_STARTED -> "未开始";
            case OPEN -> "进行中";
            case ENDED -> "已结束";
        };
    }
}
