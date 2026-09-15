package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/** A scrollable, patient-owned list of every open examination and result-review task. */
final class PatientCareTasksPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private final ClientContext context;
    private final Runnable openFollowUp;
    private final Runnable openMyAppointments;
    private final JLabel summary = new JLabel("正在读取诊疗待办……");
    private final JPanel taskList = new JPanel();
    private int requestVersion;

    PatientCareTasksPanel(
            ClientContext context,
            Runnable back,
            Runnable openFollowUp,
            Runnable openMyAppointments) {
        this.context = context;
        this.openFollowUp = openFollowUp;
        this.openMyAppointments = openMyAppointments;
        setName("patientCareTasksPanel");
        setLayout(new BorderLayout(0, 16));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(HospitalResponsiveLayout.constrainWidth(HospitalPageHeader.create(
                "诊疗待办", "逐条查看检查进度与结果回诊安排", "医院首页", back, null)),
                BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        summary.setFont(HospitalTheme.uiFont(Font.BOLD, 15F));
        summary.setForeground(HospitalTheme.PRIMARY_DARK);
        body.add(summary, BorderLayout.NORTH);
        taskList.setName("patientCareTaskList");
        taskList.setOpaque(false);
        taskList.setLayout(new BoxLayout(taskList, BoxLayout.Y_AXIS));
        body.add(HospitalResponsiveLayout.verticalScroll(taskList), BorderLayout.CENTER);
        add(HospitalResponsiveLayout.constrainWidth(body), BorderLayout.CENTER);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && !isShowing()) {
                requestVersion++;
                taskList.removeAll();
                taskList.revalidate();
                taskList.repaint();
            }
        });
    }

    void activate() {
        int version = ++requestVersion;
        String requestedUserId = currentUserId();
        summary.setText("正在读取诊疗待办……");
        taskList.removeAll();
        taskList.revalidate();
        taskList.repaint();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_MY_HEALTH_RECORD, null);
            }

            @Override
            protected void done() {
                if (version != requestVersion
                        || !Objects.equals(requestedUserId, currentUserId())) {
                    return;
                }
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof PatientHealthRecordView record) {
                        showTasks(record);
                    } else {
                        showError("待办读取失败，请重新进入本页面。" + response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("待办读取已中断，请重新进入本页面。");
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    void showTasks(PatientHealthRecordView record) {
        List<ExaminationOrderView> tasks = record.getExaminations().stream()
                .filter(order -> order.getStatus() != ExaminationStatus.REVIEWED
                        && order.getStatus() != ExaminationStatus.CANCELLED)
                .sorted(Comparator.comparingInt(PatientCareTasksPanel::priority)
                        .thenComparing(ExaminationOrderView::getOrderedAt))
                .toList();
        taskList.removeAll();
        summary.setText("共 " + tasks.size() + " 项待办");
        if (tasks.isEmpty()) {
            JLabel empty = new JLabel("当前没有待处理的检查或回诊事项。");
            empty.setForeground(HospitalTheme.MUTED);
            taskList.add(empty);
        } else {
            for (ExaminationOrderView task : tasks) {
                JComponent card = taskCard(task);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
                taskList.add(card);
                taskList.add(Box.createVerticalStrut(10));
            }
        }
        taskList.revalidate();
        taskList.repaint();
    }

    private JComponent taskCard(ExaminationOrderView task) {
        boolean ready = task.getStatus() == ExaminationStatus.RESULT_READY;
        boolean booked = task.isResultReviewBooked();
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 13, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel state = new JLabel(ready
                ? booked ? "回诊已安排" : "检查结果已出 · 待安排回诊"
                : "检查中 · 等待报告");
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        state.setForeground(ready && !booked ? HospitalTheme.WARNING : HospitalTheme.PRIMARY);
        JComponent title = HospitalResponsiveLayout.wrappingText(
                task.getItemName() + " · " + task.getDepartmentName(),
                HospitalTheme.uiFont(Font.BOLD, 17F), HospitalTheme.TEXT);
        JLabel detail = new JLabel(task.getDoctorName() + " · 开单 "
                + DATE_TIME.format(task.getOrderedAt()));
        detail.setForeground(HospitalTheme.MUTED);
        copy.add(state);
        copy.add(Box.createVerticalStrut(8));
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(detail);

        JButton action = HospitalTheme.primaryButton(ready
                ? booked ? "查看预约" : "安排回诊"
                : "查看检查进度");
        action.setName("patientCareTaskActionButton");
        action.setActionCommand(task.getOrderId());
        action.addActionListener(event -> {
            if (ready && booked) {
                openMyAppointments.run();
            } else {
                openFollowUp.run();
            }
        });
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, action, 540, 12),
                BorderLayout.CENTER);
        return card;
    }

    private void showError(String message) {
        summary.setText(message);
        summary.setForeground(HospitalTheme.WARNING);
    }

    private static int priority(ExaminationOrderView task) {
        if (task.getStatus() == ExaminationStatus.RESULT_READY
                && !task.isResultReviewBooked()) {
            return 0;
        }
        return task.isResultReviewBooked() ? 1 : 2;
    }

    private String currentUserId() {
        return context.currentSession().map(SessionInfo::getUserId).orElse(null);
    }
}
