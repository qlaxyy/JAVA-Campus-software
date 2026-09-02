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
import java.awt.Component;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

/**
 * 一个具体选课批次的主页面。
 */
final class CourseBatchPanel extends JPanel {

    /**
     * 批次开放时间显示格式。
     */
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm");

    private final ClientContext context;

    private final SelectionBatchInfo batch;

    CourseBatchPanel(
        ClientContext context,
        SelectionBatchInfo batch,
        Runnable goBack) {

        this.context = context;
        this.batch = batch;

        initializeView(
            goBack);
    }

    /**
     * 初始化整个批次页面。
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

        /*
         * 顶部批次信息。
         */
        add(
            createHeader(
                goBack),
            BorderLayout.NORTH);

        /*
         * 中间功能标签页。
         */
        add(
            createTabs(),
            BorderLayout.CENTER);
    }

    /**
     * 创建顶部批次信息。
     */
    private JPanel createHeader(
        Runnable goBack) {

        JPanel header =
            new JPanel(
                new BorderLayout(
                    16,
                    0));

        /*
         * =========================
         * 返回按钮
         * =========================
         */
        JButton backButton =
            new JButton(
                "返回选课中心");

        backButton.addActionListener(
            event ->
                goBack.run());

        header.add(
            backButton,
            BorderLayout.WEST);

        /*
         * =========================
         * 批次信息
         * =========================
         */
        JPanel information =
            new JPanel();

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        /*
         * 批次名称。
         */
        JLabel nameLabel =
            new JLabel(
                batch.getBatchName());

        nameLabel.setFont(
            nameLabel
                .getFont()
                .deriveFont(
                    Font.BOLD,
                    22F));

        /*
         * 学期。
         */
        JLabel semesterLabel =
            new JLabel(
                "学期："
                    + batch.getSemester());

        /*
         * 开放时间。
         */
        JLabel timeLabel =
            new JLabel(
                "开放时间："
                    + TIME_FORMAT.format(
                    batch.getStartTime())
                    + " ～ "
                    + TIME_FORMAT.format(
                    batch.getEndTime()));

        /*
         * 当前状态。
         */
        JLabel statusLabel =
            new JLabel(
                "状态："
                    + statusText(
                    batch.getStatus()));

        information.add(
            nameLabel);

        information.add(
            Box.createVerticalStrut(
                5));

        information.add(
            semesterLabel);

        information.add(
            Box.createVerticalStrut(
                3));

        information.add(
            timeLabel);

        information.add(
            Box.createVerticalStrut(
                3));

        information.add(
            statusLabel);

        header.add(
            information,
            BorderLayout.CENTER);

        return header;
    }

    /**
     * 创建批次内部所有功能标签页。
     */
    private JTabbedPane createTabs() {

        JTabbedPane tabs =
            new JTabbedPane();

        /*
         * =========================
         * 1. 方案内课程
         * =========================
         */
        PlanCoursePanel planPanel =
            new PlanCoursePanel(
                context,
                batch);

        /*
         * =========================
         * 2. 方案外课程
         * =========================
         */
        SubstituteCoursePanel substitutePanel =
            new SubstituteCoursePanel(
                context,
                batch);

        /*
         * =========================
         * 3. 体育课
         * =========================
         *
         * 重修批次不显示体育课。
         */
        final PeCoursePanel pePanel;

        if (batch.getBatchType()
            != SelectionBatchType.RETAKE) {

            pePanel =
                new PeCoursePanel(
                    context,
                    batch);

        } else {

            pePanel =
                null;
        }

        /*
         * =========================
         * 4. 通选课
         * =========================
         *
         * 与体育课一样：
         *
         * PRE_SELECTION 显示
         * ADD_DROP 显示
         * RETAKE 不显示
         */
        final GeneralCoursePanel
            generalPanel;

        if (batch.getBatchType()
            != SelectionBatchType.RETAKE) {

            generalPanel =
                new GeneralCoursePanel(
                    context,
                    batch);

        } else {

            generalPanel =
                null;
        }

        /*
         * =========================
         * 5. 已选课程
         * =========================
         */
        SelectedCoursePanel selectedPanel =
            new SelectedCoursePanel(
                context,
                batch);

        /*
         * =========================
         * 加入方案内
         * =========================
         */
        tabs.addTab(
            "方案内课程",
            planPanel);

        /*
         * =========================
         * 加入方案外
         * =========================
         */
        tabs.addTab(
            "方案外课程",
            substitutePanel);

        /*
         * =========================
         * 体育课 + 通选课
         * =========================
         *
         * 重修批次都不显示。
         *
         * 预选和退改补完全一致。
         */
        if (batch.getBatchType()
            != SelectionBatchType.RETAKE) {

            tabs.addTab(
                "体育课",
                pePanel);

            tabs.addTab(
                "通选课",
                generalPanel);
        }

        /*
         * =========================
         * 已选课程
         * =========================
         */
        tabs.addTab(
            "已选课程",
            selectedPanel);

        /*
         * =========================
         * 全校课程查询
         * =========================
         */
        tabs.addTab(
            "全校课程查询",
            createPlaceholder(
                "全校课程查询"));

        /*
         * =========================
         * 标签切换刷新
         * =========================
         */
        tabs.addChangeListener(
            event -> {

                Component selected =
                    tabs.getSelectedComponent();

                /*
                 * 方案内。
                 */
                if (selected
                    == planPanel) {

                    planPanel.reload();

                    /*
                     * 方案外。
                     */
                } else if (selected
                    == substitutePanel) {

                    substitutePanel.reload();

                    /*
                     * 体育课。
                     */
                } else if (pePanel != null
                    && selected
                    == pePanel) {

                    pePanel.reload();

                    /*
                     * 通选课。
                     */
                } else if (generalPanel != null
                    && selected
                    == generalPanel) {

                    generalPanel.reload();

                    /*
                     * 已选。
                     */
                } else if (selected
                    == selectedPanel) {

                    selectedPanel.reload();
                }
            });

        return tabs;
    }


    /**
     * 当前尚未实现页面使用的占位面板。
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
     * 批次状态转换成中文。
     */
    private String statusText(
        SelectionBatchStatus status) {

        return switch (status) {

            case NOT_STARTED ->
                "未开始";

            case OPEN ->
                "进行中";

            case ENDED ->
                "已结束";
        };
    }
}
