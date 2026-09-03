package edu.seu.vcampus.client.module.course;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.CourseInfo;
import edu.seu.vcampus.common.course.DropCourseRequest;
import edu.seu.vcampus.common.course.EnrollmentInfo;
import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.SelectionBatchInfo;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 选课列表中的退课交互。
 *
 * 课程列表只携带 offeringId，退课接口需要 enrollmentId，
 * 因此先读取当前已选课程并定位对应选课记录，再提交退课。
 */
final class CourseDropSupport {

    private CourseDropSupport() {
    }

    static void confirmAndDrop(
        Component parent,
        ClientContext context,
        SelectionBatchInfo batch,
        CourseInfo course,
        OfferingInfo offering,
        JButton button,
        JLabel statusLabel,
        Runnable reload) {

        button.setEnabled(
            false);

        button.setText(
            "正在检查...");

        statusLabel.setText(
            "正在读取选课记录...");

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

                            showFailure(
                                parent,
                                response.getMessage());

                            reload.run();
                            return;
                        }

                        EnrollmentInfo enrollment =
                            findEnrollment(
                                response,
                                offering.getOfferingId());

                        if (enrollment == null) {

                            showFailure(
                                parent,
                                "未找到对应的选课记录，页面将重新刷新。");

                            reload.run();
                            return;
                        }

                        if (!enrollment.isCanDrop()) {

                            showFailure(
                                parent,
                                enrollment.getDropUnavailableReason() == null
                                    ? "当前课程不可退选。"
                                    : enrollment.getDropUnavailableReason());

                            reload.run();
                            return;
                        }

                        showConfirmation(
                            parent,
                            context,
                            batch,
                            course,
                            offering,
                            enrollment,
                            button,
                            statusLabel,
                            reload);

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showFailure(
                            parent,
                            "读取选课记录被中断。");

                        reload.run();

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showFailure(
                            parent,
                            "无法读取选课记录："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()));

                        reload.run();

                    } catch (IllegalStateException exception) {

                        showFailure(
                            parent,
                            exception.getMessage());

                        reload.run();
                    }
                }
            };

        worker.execute();
    }

    /**
     * 根据教学班编号查找对应选课记录。
     */
    private static EnrollmentInfo findEnrollment(
        Response response,
        long offeringId) {

        if (!(response.getData()
            instanceof List<?> values)) {

            throw new IllegalStateException(
                "服务器返回的已选课程数据格式错误。");
        }

        for (Object value : values) {

            if (!(value
                instanceof EnrollmentInfo enrollment)) {

                throw new IllegalStateException(
                    "服务器返回的已选课程数据格式错误。");
            }

            if (enrollment.getOfferingId()
                == offeringId) {

                return enrollment;
            }
        }

        return null;
    }

    /**
     * 显示退课确认框。
     */
    private static void showConfirmation(
        Component parent,
        ClientContext context,
        SelectionBatchInfo batch,
        CourseInfo course,
        OfferingInfo offering,
        EnrollmentInfo enrollment,
        JButton button,
        JLabel statusLabel,
        Runnable reload) {

        String message =
            "确认退选以下课程吗？\n\n"
                + "课程："
                + course.getCourseName()
                + "（"
                + course.getCourseCode()
                + "）\n"
                + "教学班："
                + offering.getClassNo()
                + "\n\n"
                + "注意：退课后空出的名额可能被其他学生选择，"
                + "再次选课不保证成功。";

        int result =
            JOptionPane.showConfirmDialog(
                parent,
                message,
                "确认退课",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result
            != JOptionPane.YES_OPTION) {

            button.setText(
                "退课");

            button.setEnabled(
                true);

            statusLabel.setText(
                "已取消退课");

            return;
        }

        submitDrop(
            parent,
            context,
            batch,
            enrollment,
            button,
            statusLabel,
            reload);
    }

    /**
     * 提交退课请求。
     */
    private static void submitDrop(
        Component parent,
        ClientContext context,
        SelectionBatchInfo batch,
        EnrollmentInfo enrollment,
        JButton button,
        JLabel statusLabel,
        Runnable reload) {

        button.setText(
            "提交中...");

        statusLabel.setText(
            "正在提交退课请求...");

        SwingWorker<Response, Void> worker =
            new SwingWorker<>() {

                @Override
                protected Response doInBackground()
                    throws Exception {

                    return context.send(
                        CourseActions.DROP_COURSE,
                        new DropCourseRequest(
                            batch.getBatchId(),
                            enrollment.getEnrollmentId()));
                }

                @Override
                protected void done() {

                    try {

                        Response response =
                            get();

                        JOptionPane.showMessageDialog(
                            parent,
                            response.getMessage(),
                            response.isSuccess()
                                ? "退课成功"
                                : "退课失败",
                            response.isSuccess()
                                ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE);

                    } catch (InterruptedException exception) {

                        Thread.currentThread()
                            .interrupt();

                        showFailure(
                            parent,
                            "退课请求被中断。");

                    } catch (ExecutionException exception) {

                        Throwable cause =
                            exception.getCause();

                        showFailure(
                            parent,
                            "无法提交退课请求："
                                + (cause == null
                                ? exception.getMessage()
                                : cause.getMessage()));

                    } finally {

                        reload.run();
                    }
                }
            };

        worker.execute();
    }

    /**
     * 显示退课失败提示。
     */
    private static void showFailure(
        Component parent,
        String message) {

        JOptionPane.showMessageDialog(
            parent,
            message,
            "退课失败",
            JOptionPane.WARNING_MESSAGE);
    }
}
