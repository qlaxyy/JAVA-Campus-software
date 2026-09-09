package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.course.TemporaryCourseTeacherDirectory;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.JComponent;

/**
 * 选课系统客户端入口。
 *
 * 根据当前登录账号身份进入：
 *
 * 1. 选课管理端
 * 2. 普通教师端
 * 3. 学生选课端
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
                .orElseThrow(() ->
                    new IllegalStateException(
                        "当前用户尚未登录。"));

        /*
         * =========================
         * 1. 选课管理员和超级管理员
         * =========================
         *
         * 管理员判断必须放在教师判断之前，
         * 避免管理员账号意外进入教师页面。
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
         * =========================
         * 2. 临时普通教师
         * =========================
         *
         * 当前通过课程模块临时名单判断。
         * 正式教师角色完成后替换此处。
         */
        if (TemporaryCourseTeacherDirectory
            .isTeacher(
                session.getUsername())) {

            return new CourseTeacherView(
                context);
        }

        /*
         * =========================
         * 3. 普通学生
         * =========================
         */
        return new CourseSelectionView(
            context);
    }
}
