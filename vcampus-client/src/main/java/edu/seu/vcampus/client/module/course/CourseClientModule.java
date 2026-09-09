package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.module.ClientModule;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.TeacherProfileView;
import edu.seu.vcampus.common.user.UserActions;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

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

        return new TeacherRoleResolvingView(context);
    }

    /** Resolves teacher qualification from the server without blocking the Swing UI. */
    private static final class TeacherRoleResolvingView extends JPanel {

        private final ClientContext context;

        private TeacherRoleResolvingView(ClientContext context) {
            super(new BorderLayout());
            this.context = context;
            resolve();
        }

        private void resolve() {
            removeAll();
            add(new JLabel("正在确认课程身份……", JLabel.CENTER), BorderLayout.CENTER);
            revalidate();
            repaint();
            new SwingWorker<Response, Void>() {
                @Override
                protected Response doInBackground() throws IOException {
                    return context.send(UserActions.CURRENT_TEACHER_PROFILE, null);
                }

                @Override
                protected void done() {
                    try {
                        Response response = get();
                        if (response.isSuccess()
                                && response.getData() instanceof TeacherProfileView teacher) {
                            show(new CourseTeacherView(context, teacher));
                        } else if (ErrorCodes.AUTH_FORBIDDEN.equals(response.getCode())) {
                            show(new CourseSelectionView(context));
                        } else {
                            showFailure(response.getMessage());
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        showFailure("身份查询已中断");
                    } catch (ExecutionException exception) {
                        showFailure("无法连接服务器");
                    }
                }
            }.execute();
        }

        private void show(JComponent view) {
            removeAll();
            add(view, BorderLayout.CENTER);
            revalidate();
            repaint();
        }

        private void showFailure(String message) {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.add(new JLabel(message, JLabel.CENTER), BorderLayout.CENTER);
            JButton retry = new JButton("重试");
            retry.addActionListener(event -> resolve());
            panel.add(retry, BorderLayout.SOUTH);
            show(panel);
        }
    }
}
