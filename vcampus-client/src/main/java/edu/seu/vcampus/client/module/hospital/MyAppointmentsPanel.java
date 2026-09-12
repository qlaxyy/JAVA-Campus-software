package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AppointmentListResponse;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.AppointmentView;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/** Patient page that loads appointments for the authenticated session. */
final class MyAppointmentsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE);
    private static final DateTimeFormatter WEEKDAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final String TIME_ASCENDING = "时间从近到远";
    private static final String TIME_DESCENDING = "时间从远到近";

    private final ClientContext context;
    private final Runnable openSlotSearch;
    private final Predicate<AppointmentView> confirmCancellation;
    private final JPanel appointmentList = new JPanel();
    private final JComboBox<String> sortOrder = new JComboBox<>(new String[]{
            TIME_ASCENDING,
            TIME_DESCENDING
    });
    private List<AppointmentView> loadedAppointments = List.of();
    private int requestVersion;

    MyAppointmentsPanel(
            ClientContext context,
            Runnable openPatientHome,
            Runnable openSlotSearch) {
        this(context, openPatientHome, openSlotSearch, null);
    }

    MyAppointmentsPanel(
            ClientContext context,
            Runnable openPatientHome,
            Runnable openSlotSearch,
            Predicate<AppointmentView> confirmCancellation) {
        this.context = context;
        this.openSlotSearch = openSlotSearch;
        this.confirmCancellation = confirmCancellation == null
                ? this::showCancellationConfirmation
                : confirmCancellation;
        setName("myAppointmentsPanel");
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        add(HospitalResponsiveLayout.constrainWidth(createHeader(openPatientHome)),
                BorderLayout.NORTH);

        appointmentList.setOpaque(false);
        appointmentList.setLayout(new BoxLayout(appointmentList, BoxLayout.Y_AXIS));
        add(HospitalResponsiveLayout.verticalScroll(appointmentList),
                BorderLayout.CENTER);
    }

    void activate() {
        loadAppointments();
    }

    private JPanel createHeader(Runnable openPatientHome) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);

        JButton back = HospitalTheme.quietButton("‹ 返回医院首页");
        back.addActionListener(event -> openPatientHome.run());

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("我的预约");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 26F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("预约信息由当前登录账号加载，无需输入学号或用户编号");
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);

        JPanel sorting = new JPanel(new BorderLayout(0, 5));
        sorting.setOpaque(false);
        JLabel sortingLabel = new JLabel("按就诊时间排序");
        sortingLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        sortingLabel.setForeground(HospitalTheme.MUTED);
        sortOrder.setName("appointmentSortOrder");
        sortOrder.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        sortOrder.setBackground(HospitalTheme.SURFACE);
        sortOrder.setPreferredSize(new Dimension(168, 34));
        sortOrder.addActionListener(event -> renderAppointments());
        sorting.add(sortingLabel, BorderLayout.NORTH);
        sorting.add(sortOrder, BorderLayout.CENTER);

        header.add(back, BorderLayout.WEST);
        header.add(copy, BorderLayout.CENTER);
        header.add(sorting, BorderLayout.EAST);
        return header;
    }

    private void loadAppointments() {
        int activeRequest = ++requestVersion;
        showLoading();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.SEARCH_APPOINTMENTS, null);
            }

            @Override
            protected void done() {
                if (activeRequest != requestVersion) {
                    return;
                }
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        showError("预约加载失败：" + response.getMessage());
                    } else if (response.getData()
                            instanceof AppointmentListResponse appointments) {
                        showAppointments(appointments.getAppointments());
                    } else {
                        showError("服务器返回了无法识别的预约数据。");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("预约加载已中断，请重新进入本页面。");
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动。");
                }
            }
        }.execute();
    }

    private void showLoading() {
        sortOrder.setEnabled(false);
        replaceContent(createStateCard(
                "正在加载预约……",
                "服务器正在根据当前登录账号查询预约记录。",
                null,
                null));
    }

    private void showError(String message) {
        sortOrder.setEnabled(false);
        JButton retry = HospitalTheme.quietButton("重新加载");
        retry.addActionListener(event -> loadAppointments());
        replaceContent(createStateCard(
                "暂时无法显示预约",
                message,
                retry,
                HospitalTheme.WARNING_LIGHT));
    }

    private void showAppointments(List<AppointmentView> appointments) {
        loadedAppointments = List.copyOf(appointments);
        renderAppointments();
    }

    private void renderAppointments() {
        List<AppointmentView> appointments = loadedAppointments.stream()
                .sorted(appointmentComparator())
                .toList();
        sortOrder.setEnabled(!appointments.isEmpty());
        appointmentList.removeAll();
        if (appointments.isEmpty()) {
            JButton book = HospitalTheme.primaryButton("去预约挂号");
            book.addActionListener(event -> openSlotSearch.run());
            appointmentList.add(createStateCard(
                    "还没有预约记录",
                    "完成预约后，科室、医生、时间、候诊序号和费用会显示在这里。",
                    book,
                    null));
        } else {
            JLabel summary = new JLabel("共 " + appointments.size() + " 条预约记录");
            summary.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
            summary.setForeground(HospitalTheme.MUTED);
            summary.setAlignmentX(LEFT_ALIGNMENT);
            appointmentList.add(summary);
            appointmentList.add(Box.createVerticalStrut(10));
            for (AppointmentView appointment : appointments) {
                appointmentList.add(createAppointmentCard(appointment));
                appointmentList.add(Box.createVerticalStrut(12));
            }
        }
        appointmentList.revalidate();
        appointmentList.repaint();
    }

    private Comparator<AppointmentView> appointmentComparator() {
        Comparator<AppointmentView> comparator = Comparator
                .comparing(AppointmentView::getStartTime)
                .thenComparing(AppointmentView::getAppointmentId);
        return TIME_DESCENDING.equals(sortOrder.getSelectedItem())
                ? comparator.reversed()
                : comparator;
    }

    private JPanel createAppointmentCard(AppointmentView appointment) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 16, HospitalTheme.BORDER);
        card.setName("appointmentCard");
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 20));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        HospitalTheme.SurfacePanel dateBlock = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 12);
        dateBlock.setPreferredSize(new Dimension(126, 130));
        dateBlock.setLayout(new BoxLayout(dateBlock, BoxLayout.Y_AXIS));
        dateBlock.setBorder(BorderFactory.createEmptyBorder(13, 12, 12, 12));

        JLabel date = centeredLabel(
                DATE_FORMAT.format(appointment.getStartTime()),
                HospitalTheme.uiFont(Font.BOLD, 20F),
                HospitalTheme.PRIMARY_DARK);
        JLabel weekday = centeredLabel(
                WEEKDAY_FORMAT.format(appointment.getStartTime()),
                HospitalTheme.uiFont(Font.PLAIN, 12F),
                HospitalTheme.MUTED);
        JLabel queue = centeredLabel(
                "候诊 " + appointment.getQueueNumber() + " 号",
                HospitalTheme.uiFont(Font.BOLD, 14F),
                HospitalTheme.PRIMARY);
        dateBlock.add(date);
        dateBlock.add(Box.createVerticalStrut(2));
        dateBlock.add(weekday);
        dateBlock.add(Box.createVerticalGlue());
        dateBlock.add(queue);

        JPanel details = new JPanel(new BorderLayout(0, 9));
        details.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JLabel department = new JLabel(appointment.getDepartmentName());
        department.setName("appointmentDepartment");
        department.setFont(HospitalTheme.uiFont(Font.BOLD, 19F));
        department.setForeground(HospitalTheme.TEXT);
        heading.add(department, BorderLayout.CENTER);

        JPanel facts = new JPanel();
        facts.setOpaque(false);
        facts.setLayout(new BoxLayout(facts, BoxLayout.Y_AXIS));
        facts.add(factLabel(appointment.getDoctorName() + "  ·  "
                + appointment.getDoctorTitle()));
        facts.add(Box.createVerticalStrut(5));
        facts.add(factLabel("就诊类型：" + visitTypeText(appointment.getVisitType())));
        facts.add(Box.createVerticalStrut(5));
        facts.add(factLabel(
                TIME_FORMAT.format(appointment.getStartTime()) + " – "
                        + TIME_FORMAT.format(appointment.getEndTime())));
        facts.add(Box.createVerticalStrut(5));
        facts.add(factLabel("挂号费 ¥" + formatAmount(appointment.getAmountCents())
                + "  ·  " + paymentText(appointment.getPaymentStatus())));

        details.add(heading, BorderLayout.NORTH);
        details.add(facts, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JLabel status = statusLabel(appointment.getAppointmentStatus());
        status.setAlignmentX(CENTER_ALIGNMENT);
        actions.add(status);
        if (appointment.getAppointmentStatus() == AppointmentStatus.BOOKED) {
            actions.add(Box.createVerticalGlue());
            JButton cancel = HospitalTheme.quietButton("取消预约");
            cancel.setName("cancelAppointmentButton");
            cancel.setActionCommand(appointment.getAppointmentId());
            cancel.setForeground(HospitalTheme.WARNING);
            cancel.setAlignmentX(CENTER_ALIGNMENT);
            cancel.addActionListener(event -> cancelAppointment(appointment, cancel));
            actions.add(cancel);
        }

        card.add(dateBlock, BorderLayout.WEST);
        JPanel adaptiveContent = HospitalResponsiveLayout.adaptiveRow(
                details, actions, 520, 14);
        adaptiveContent.setName("appointmentAdaptiveContent");
        card.add(adaptiveContent, BorderLayout.CENTER);
        return card;
    }

    private boolean showCancellationConfirmation(AppointmentView appointment) {
        String message = "确认取消以下预约？\n\n"
                + appointment.getDepartmentName() + " · " + appointment.getDoctorName()
                + "\n" + DATE_FORMAT.format(appointment.getStartTime()) + " "
                + TIME_FORMAT.format(appointment.getStartTime()) + "–"
                + TIME_FORMAT.format(appointment.getEndTime())
                + (appointment.getAmountCents() == 0
                        ? "\n\n本次为零费用回诊，取消后不会产生退款。"
                        : "\n\n已支付挂号费将进行模拟退款。");
        return JOptionPane.showConfirmDialog(
                this,
                message,
                "取消预约",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private static String visitTypeText(VisitType visitType) {
        return switch (visitType) {
            case FIRST_VISIT -> "初次就诊";
            case FOLLOW_UP -> "复诊";
            case RESULT_REVIEW -> "检查结果回诊（续诊）";
        };
    }

    private void cancelAppointment(AppointmentView appointment, JButton button) {
        if (!confirmCancellation.test(appointment)) {
            return;
        }
        button.setEnabled(false);
        button.setText("取消中…");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.CANCEL_APPOINTMENT,
                        new CancelAppointmentRequest(appointment.getAppointmentId()));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof AppointmentView cancelled
                            && cancelled.getAppointmentStatus()
                            == AppointmentStatus.CANCELLED) {
                        loadAppointments();
                    } else {
                        button.setEnabled(true);
                        button.setText("取消预约");
                        JOptionPane.showMessageDialog(
                                MyAppointmentsPanel.this,
                                cancellationFailureMessage(response),
                                "取消未完成",
                                JOptionPane.WARNING_MESSAGE);
                        loadAppointments();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    button.setEnabled(true);
                    button.setText("取消预约");
                } catch (ExecutionException exception) {
                    button.setEnabled(true);
                    button.setText("取消预约");
                    JOptionPane.showMessageDialog(
                            MyAppointmentsPanel.this,
                            "无法连接服务器，请稍后重试。",
                            "取消未完成",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private static String cancellationFailureMessage(Response response) {
        return switch (response.getCode()) {
            case ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND ->
                    "没有找到该预约，或者它不属于当前账号。";
            case ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CANCELLABLE ->
                    "该预约当前不能取消，可能已经取消或完成。";
            case ErrorCodes.HOSPITAL_APPOINTMENT_STARTED ->
                    "就诊时段已经开始，不能再取消预约。";
            case ErrorCodes.COMMON_UNKNOWN_ACTION ->
                    "当前服务器仍是旧版本，请重启服务器并重新登录后再试。";
            default -> "服务器未能取消该预约，请稍后重试。";
        };
    }

    private JPanel createStateCard(
            String titleText,
            String detailText,
            JButton action,
            Color fill) {
        HospitalTheme.SurfacePanel state = new HospitalTheme.SurfacePanel(
                fill == null ? HospitalTheme.SURFACE : fill,
                16,
                HospitalTheme.BORDER);
        state.setName("appointmentStateCard");
        state.setLayout(new BoxLayout(state, BoxLayout.Y_AXIS));
        state.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        state.setAlignmentX(LEFT_ALIGNMENT);
        state.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        JLabel title = new JLabel(titleText);
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.TEXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel detail = new JLabel(detailText);
        detail.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        detail.setForeground(HospitalTheme.MUTED);
        detail.setAlignmentX(LEFT_ALIGNMENT);
        state.add(title);
        state.add(Box.createVerticalStrut(8));
        state.add(detail);
        if (action != null) {
            action.setAlignmentX(LEFT_ALIGNMENT);
            state.add(Box.createVerticalStrut(20));
            state.add(action);
        }
        return state;
    }

    private void replaceContent(JPanel state) {
        appointmentList.removeAll();
        appointmentList.add(state);
        appointmentList.revalidate();
        appointmentList.repaint();
    }

    private static JLabel statusLabel(AppointmentStatus status) {
        String text = switch (status) {
            case BOOKED -> "待就诊";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case NO_SHOW -> "未就诊";
        };
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setName("appointmentStatus");
        label.setOpaque(true);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        label.setForeground(switch (status) {
            case NO_SHOW -> HospitalTheme.WARNING;
            case CANCELLED -> HospitalTheme.MUTED;
            default -> HospitalTheme.PRIMARY_DARK;
        });
        label.setBackground(switch (status) {
            case NO_SHOW -> HospitalTheme.WARNING_LIGHT;
            case CANCELLED -> HospitalTheme.DISABLED;
            case COMPLETED -> HospitalTheme.PRIMARY_LIGHT;
            case BOOKED -> HospitalTheme.SUCCESS_LIGHT;
        });
        label.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return label;
    }

    private static JLabel factLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        label.setForeground(HospitalTheme.MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel centeredLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

    private static String paymentText(PaymentStatus status) {
        return switch (status) {
            case UNPAID -> "待缴费";
            case PAID -> "已支付";
            case REFUNDED -> "已退款";
        };
    }

    private static String formatAmount(long amountCents) {
        return BigDecimal.valueOf(amountCents, 2).toPlainString();
    }
}
