package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.TemporaryCourseTeacherDirectory;
import edu.seu.vcampus.common.course.TemporaryCourseTeacherDirectory.TeacherAssignment;
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
 * 普通教师课程视图。
 */
final class CourseTeacherView
    extends JPanel {

    private final ClientContext context;

    private final TeacherAssignment teacher;

    CourseTeacherView(
        ClientContext context) {

        this.context =
            Objects.requireNonNull(
                context);

        SessionInfo session =
            context.currentSession()
                .orElseThrow(() ->
                    new IllegalStateException(
                        "当前用户尚未登录。"));

        this.teacher =
            TemporaryCourseTeacherDirectory.findTeacher(
                    session.getUserId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "当前账号不在教师名单中。"));

        initialiseView();
    }

    /**
     * 初始化教师端页面。
     */
    private void initialiseView() {

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
     * 创建教师身份信息区域。
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
                "教师教学管理");

        title.setAlignmentX(
            Component.LEFT_ALIGNMENT);

        JLabel subtitle =
            CourseTheme.subtitle(
                teacherDescription());

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
     * 创建教师功能标签页。
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
            "我的教学班",
            new CourseTeacherOfferingPanel(
                context));

        tabs.addTab(
            "学生名单",
            new CourseTeacherStudentPanel(
                context));
        tabs.addTab(
            "成绩管理",
            new CourseTeacherGradePanel(
                context));

        return tabs;
    }

    /**
     * 创建暂未接入业务接口的页面。
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

        JLabel assignment =
            new JLabel(
                "当前教师："
                    + teacher.teacherName()
                    + "；负责教学班："
                    + teacher.offeringIds());

        assignment.setForeground(
            CourseTheme.PRIMARY);

        assignment.setFont(
            assignment.getFont()
                .deriveFont(
                    Font.BOLD,
                    13F));

        assignment.setAlignmentX(
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
            assignment);

        content.add(
            Box.createVerticalGlue());

        panel.add(
            content,
            BorderLayout.CENTER);

        return panel;
    }

    /**
     * 生成教师身份说明。
     */
    private String teacherDescription() {

        String displayName =
            context.currentSession()
                .map(
                    SessionInfo::getDisplayName)
                .orElse(
                    teacher.teacherName());

        return displayName
            + " · "
            + teacher.teacherName()
            + " · 普通教师";
    }
}
