package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.Objects;

/**
 * 教务老师和超级管理员共用的选课管理视图。
 *
 * 两种管理员使用相同的页面结构，
 * 但只有超级管理员可以修改成绩。
 */
final class CourseAdminView extends JPanel {

    private final ClientContext context;

    private final boolean canEditGrades;

    CourseAdminView(
        ClientContext context,
        boolean canEditGrades) {

        this.context =
            Objects.requireNonNull(
                context);

        this.canEditGrades =
            canEditGrades;

        initializeView();
    }

    /**
     * 初始化管理员页面。
     */
    private void initializeView() {

        setLayout(
            new BorderLayout(
                0,
                18));

        setBackground(
            CourseTheme.BACKGROUND);

        setBorder(
            BorderFactory.createEmptyBorder(
                22,
                26,
                22,
                26));

        add(
            createHeader(),
            BorderLayout.NORTH);

        add(
            createTabs(),
            BorderLayout.CENTER);
    }

    /**
     * 创建顶部身份信息区域。
     */
    private JPanel createHeader() {

        CourseTheme.SurfacePanel header =
            new CourseTheme.SurfacePanel();

        header.setLayout(
            new BorderLayout());

        header.setBorder(
            BorderFactory.createEmptyBorder(
                18,
                20,
                18,
                20));

        JPanel information =
            new JPanel();

        information.setOpaque(
            false);

        information.setLayout(
            new BoxLayout(
                information,
                BoxLayout.Y_AXIS));

        JLabel title =
            CourseTheme.title(
                "选课管理");

        title.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        JLabel subtitle =
            CourseTheme.subtitle(
                administratorDescription());

        subtitle.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        information.add(
            title);

        information.add(
            Box.createVerticalStrut(
                6));

        information.add(
            subtitle);

        header.add(
            information,
            BorderLayout.CENTER);

        return header;
    }

    /**
     * 创建管理员功能标签页。
     */
    private JTabbedPane createTabs() {

        JTabbedPane tabs =
            new JTabbedPane();

        tabs.setFont(
            tabs.getFont()
                .deriveFont(
                    Font.BOLD,
                    14F));

        tabs.setBackground(
            CourseTheme.SURFACE);

        tabs.addTab(
            "学生选课管理",
            new CourseAdminStudentPanel(
                context));
        tabs.addTab(
            "操作日志",
            new CourseAdminAuditPanel(
                context));
        tabs.addTab(
            "教学班管理",
            new CourseAdminOfferingPanel(
                context));

        tabs.addTab(
            "课程管理",
            new CourseAdminCoursePanel(
                context));

        tabs.addTab(
            "选课批次管理",
            new CourseAdminBatchPanel(
                context));
        tabs.addTab(
            "培养方案管理",
            createPlaceholder(
                "培养方案管理",
                "维护方案内课程和方案外课程替代关系。"));

        tabs.addTab(
            "成绩管理",
            createPlaceholder(
                "成绩管理",
                canEditGrades
                    ? "查询、录入和修改学生成绩。"
                    : "查询学生成绩。当前账号没有成绩修改权限。"));

        tabs.addTab(
            "数据统计",
            createPlaceholder(
                "数据统计",
                "查看教学班人数、剩余容量和选退课统计。"));

        return tabs;
    }

    /**
     * 创建尚未接入具体功能的占位页面。
     */
    private JPanel createPlaceholder(
        String titleText,
        String descriptionText) {

        CourseTheme.SurfacePanel panel =
            new CourseTheme.SurfacePanel();

        panel.setLayout(
            new BorderLayout());

        panel.setBorder(
            BorderFactory.createEmptyBorder(
                30,
                30,
                30,
                30));

        JPanel content =
            new JPanel();

        content.setOpaque(
            false);

        content.setLayout(
            new BoxLayout(
                content,
                BoxLayout.Y_AXIS));

        JLabel title =
            new JLabel(
                titleText);

        title.setForeground(
            CourseTheme.TEXT);

        title.setFont(
            title.getFont()
                .deriveFont(
                    Font.BOLD,
                    22F));

        title.setAlignmentX(
            Component.CENTER_ALIGNMENT);

        JLabel description =
            new JLabel(
                descriptionText);

        description.setForeground(
            CourseTheme.MUTED);

        description.setFont(
            description.getFont()
                .deriveFont(
                    14F));

        description.setAlignmentX(
            Component.CENTER_ALIGNMENT);

        JLabel status =
            new JLabel(
                "功能将在后续步骤中接入");

        status.setForeground(
            CourseTheme.PRIMARY);

        status.setFont(
            status.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        status.setAlignmentX(
            Component.CENTER_ALIGNMENT);

        content.add(
            Box.createVerticalGlue());

        content.add(
            title);

        content.add(
            Box.createVerticalStrut(
                12));

        content.add(
            description);

        content.add(
            Box.createVerticalStrut(
                18));

        content.add(
            status);

        content.add(
            Box.createVerticalGlue());

        panel.add(
            content,
            BorderLayout.CENTER);

        return panel;
    }

    /**
     * 根据当前账号生成管理员身份说明。
     */
    private String administratorDescription() {

        String displayName =
            context.currentSession()
                .map(
                    SessionInfo::getDisplayName)
                .orElse(
                    "当前管理员");

        String authority =
            canEditGrades
                ? "超级管理员，可修改成绩"
                : "教务老师，成绩仅供查看";

        return displayName
            + " · "
            + authority;
    }
}
