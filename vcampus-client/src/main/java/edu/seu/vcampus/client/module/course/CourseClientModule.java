package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JComponent;

/**
 * 选课系统客户端入口。
 *
 * 根据当前登录账号的身份，
 * 决定进入学生选课视图还是选课管理视图。
 */
public final class CourseClientModule
    implements ClientModule {

    @Override
    public String id() {

        return ModuleNames.COURSE;
    }

    @Override
    public String displayName() {

        return "选课系统";
    }

    @Override
    public JComponent createView(
        ClientContext context) {

        SessionInfo session =
            context.currentSession()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "当前用户尚未登录。"));

        /*
         * 教务老师：
         * Role.USER + AdminScope.COURSE
         *
         * 超级管理员：
         * Role.SUPER_ADMIN，自动拥有全部管理范围。
         */
        if (session.canAdminister(
            ModuleNames.COURSE)) {

            boolean canEditGrades =
                session.getRole()
                    == Role.SUPER_ADMIN;

            return new CourseAdminView(
                context,
                canEditGrades);
        }

        /*
         * 暂时没有普通老师视图。
         *
         * 后续可以在这里增加教师身份判断：
         *
         * if (isCourseTeacher) {
         *     return new CourseTeacherView(context);
         * }
         */

        return new CourseSelectionView(
            context);
    }
}
