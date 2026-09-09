package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AdminAppointmentListResponse;
import edu.seu.vcampus.common.hospital.AdminAppointmentView;
import edu.seu.vcampus.common.hospital.AdminCancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Non-clinical appointment ledger for hospital administrators. */
final class AdminAppointmentPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClientContext context;
    private final JPanel ledger = verticalList();
    private final JTextField search = new JTextField();
    private final JComboBox<StatusFilter> statusFilter =
            new JComboBox<>(StatusFilter.values());
    private final JLabel summary = new JLabel("正在读取预约订单……");
    private final JButton retry = HospitalTheme.quietButton("重新加载");

    private List<AdminAppointmentView> appointments = List.of();
    private boolean busy;

    AdminAppointmentPanel(ClientContext context, Runnable back) {
        this.context = context;
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(HospitalResponsiveLayout.constrainWidth(header(back)), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        configureActions();
    }

    void activate() {
        load();
    }

    private JPanel header(Runnable back) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = verticalList();
        JLabel eyebrow = new JLabel("OPERATIONS LEDGER");
        eyebrow.setFont(HospitalTheme.dataFont(Font.BOLD, 11F));
        eyebrow.setForeground(HospitalTheme.PRIMARY);
        JLabel title = new JLabel("号源与预约管理");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 28F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("查询预约流转；仅处理尚未开始的异常预约，不展示诊断和病历内容");
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(3));
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);
        JButton backButton = HospitalTheme.quietButton("‹ 返回管理首页");
        backButton.addActionListener(event -> back.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(backButton, BorderLayout.EAST);
        return header;
    }

    private JPanel body() {
        HospitalTheme.SurfacePanel surface = new HospitalTheme.SurfacePanel();
        surface.setLayout(new BorderLayout(0, 14));
        surface.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel controls = HospitalResponsiveLayout.grid(2, 280, 14, 10);
        controls.setOpaque(false);
        JPanel searchArea = new JPanel(new BorderLayout(8, 0));
        searchArea.setOpaque(false);
        searchArea.add(new JLabel("搜索"), BorderLayout.WEST);
        search.setName("adminAppointmentSearch");
        search.setToolTipText("搜索患者账号、医生、科室或预约编号");
        search.setPreferredSize(new Dimension(340, 36));
        searchArea.add(search, BorderLayout.CENTER);
        JPanel filterArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterArea.setOpaque(false);
        filterArea.add(new JLabel("预约状态"));
        statusFilter.setName("adminAppointmentStatusFilter");
        statusFilter.setPreferredSize(new Dimension(130, 36));
        filterArea.add(statusFilter);
        controls.add(searchArea);
        controls.add(filterArea);

        JScrollPane scroll = HospitalResponsiveLayout.verticalScroll(ledger);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footer.setOpaque(false);
        retry.setVisible(false);
        footer.add(summary);
        footer.add(retry);

        surface.add(controls, BorderLayout.NORTH);
        surface.add(scroll, BorderLayout.CENTER);
        surface.add(footer, BorderLayout.SOUTH);
        return surface;
    }

    private void configureActions() {
        retry.addActionListener(event -> load());
        statusFilter.addActionListener(event -> render());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { render(); }
            @Override public void removeUpdate(DocumentEvent event) { render(); }
            @Override public void changedUpdate(DocumentEvent event) { render(); }
        });
    }

    private void load() {
        if (busy) return;
        setBusy(true);
        summary.setText("正在读取预约订单……");
        retry.setVisible(false);
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_ADMIN_APPOINTMENTS, null);
            }

            @Override protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof AdminAppointmentListResponse data) {
                        appointments = data.getAppointments();
                        render();
                    } else {
                        loadFailed(response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    loadFailed("读取已中断");
                } catch (ExecutionException exception) {
                    loadFailed("无法连接服务器");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void render() {
        ledger.removeAll();
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        StatusFilter filter = (StatusFilter) statusFilter.getSelectedItem();
        List<AdminAppointmentView> visible = appointments.stream()
                .filter(item -> filter == null || filter.matches(item.getAppointmentStatus()))
                .filter(item -> matches(item, query))
                .toList();
        if (visible.isEmpty()) {
            JLabel empty = new JLabel(appointments.isEmpty()
                    ? "当前没有预约订单。" : "没有符合筛选条件的预约。 ");
            empty.setForeground(HospitalTheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(28, 8, 28, 8));
            ledger.add(empty);
        } else {
            visible.forEach(item -> {
                ledger.add(appointmentCard(item));
                ledger.add(Box.createVerticalStrut(10));
            });
        }
        summary.setText("显示 " + visible.size() + " / " + appointments.size()
                + " 条；业务变动后自动更新");
        ledger.revalidate();
        ledger.repaint();
    }

    private JPanel appointmentCard(AdminAppointmentView item) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 10, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel rail = new JLabel(statusText(item.getAppointmentStatus()));
        rail.setOpaque(true);
        rail.setHorizontalAlignment(JLabel.CENTER);
        rail.setPreferredSize(new Dimension(72, 72));
        rail.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        rail.setBackground(statusBackground(item.getAppointmentStatus()));
        rail.setForeground(statusForeground(item.getAppointmentStatus()));

        JPanel copy = verticalList();
        JLabel title = new JLabel(item.getDepartmentName() + "  ·  " + item.getDoctorName());
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel time = new JLabel(item.getStartTime().format(DATE_TIME) + "–"
                + item.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                + "  ·  " + visitTypeText(item.getVisitType())
                + "  ·  候诊号 " + item.getQueueNumber());
        time.setForeground(HospitalTheme.MUTED);
        JLabel ids = new JLabel("患者 " + item.getPatientUserId()
                + "  ·  预约 " + item.getAppointmentId()
                + "  ·  排班 " + item.getScheduleId());
        ids.setFont(HospitalTheme.dataFont(Font.PLAIN, 12F));
        ids.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(time);
        copy.add(Box.createVerticalStrut(5));
        copy.add(ids);

        JPanel money = verticalList();
        JLabel amount = new JLabel(String.format("¥%.2f", item.getAmountCents() / 100.0));
        amount.setFont(HospitalTheme.dataFont(Font.BOLD, 17F));
        amount.setForeground(HospitalTheme.TEXT);
        JLabel payment = new JLabel(paymentText(item.getPaymentStatus()));
        payment.setForeground(HospitalTheme.MUTED);
        money.add(amount);
        money.add(Box.createVerticalStrut(4));
        money.add(payment);

        JPanel right = new JPanel(new BorderLayout(14, 0));
        right.setOpaque(false);
        right.add(money, BorderLayout.CENTER);
        if (item.isCancellable()) {
            JButton cancel = HospitalTheme.quietButton("取消异常预约");
            cancel.setName("adminCancelAppointmentButton");
            cancel.addActionListener(event -> confirmCancellation(item));
            right.add(cancel, BorderLayout.EAST);
        }

        card.add(rail, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private void confirmCancellation(AdminAppointmentView item) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确认由医院管理员取消这条异常预约？\n\n"
                        + item.getDepartmentName() + " · " + item.getDoctorName() + "\n"
                        + item.getStartTime().format(DATE_TIME) + "\n"
                        + "患者：" + item.getPatientUserId() + "\n\n"
                        + "已支付挂号费将同步执行模拟退款。",
                "取消异常预约",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) cancel(item);
    }

    private void cancel(AdminAppointmentView item) {
        if (busy) return;
        setBusy(true);
        summary.setText("正在取消预约并同步退款……");
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.ADMIN_CANCEL_APPOINTMENT,
                        new AdminCancelAppointmentRequest(item.getAppointmentId()));
            }

            @Override protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        setBusy(false);
                        load();
                        return;
                    }
                    summary.setText("取消失败：" + response.getMessage());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    summary.setText("取消操作已中断");
                } catch (ExecutionException exception) {
                    summary.setText("无法连接服务器");
                } finally {
                    if (busy) setBusy(false);
                }
            }
        }.execute();
    }

    private void loadFailed(String message) {
        appointments = List.of();
        render();
        summary.setText("读取失败：" + message);
        retry.setVisible(true);
    }

    private void setBusy(boolean value) {
        busy = value;
        retry.setEnabled(!value);
    }

    private static boolean matches(AdminAppointmentView item, String query) {
        if (query.isEmpty()) return true;
        return item.getPatientUserId().toLowerCase(Locale.ROOT).contains(query)
                || item.getAppointmentId().toLowerCase(Locale.ROOT).contains(query)
                || item.getScheduleId().toLowerCase(Locale.ROOT).contains(query)
                || item.getDepartmentName().toLowerCase(Locale.ROOT).contains(query)
                || item.getDoctorName().toLowerCase(Locale.ROOT).contains(query);
    }

    private static String statusText(AppointmentStatus status) {
        return switch (status) {
            case BOOKED -> "待接诊";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case NO_SHOW -> "未到诊";
        };
    }

    private static java.awt.Color statusBackground(AppointmentStatus status) {
        return switch (status) {
            case BOOKED -> HospitalTheme.PRIMARY_LIGHT;
            case COMPLETED -> HospitalTheme.SUCCESS_LIGHT;
            case CANCELLED, NO_SHOW -> HospitalTheme.WARNING_LIGHT;
        };
    }

    private static java.awt.Color statusForeground(AppointmentStatus status) {
        return switch (status) {
            case BOOKED -> HospitalTheme.PRIMARY;
            case COMPLETED -> HospitalTheme.SUCCESS;
            case CANCELLED, NO_SHOW -> HospitalTheme.WARNING;
        };
    }

    private static String visitTypeText(VisitType type) {
        return switch (type) {
            case FIRST_VISIT -> "初诊";
            case FOLLOW_UP -> "普通复诊";
            case RESULT_REVIEW -> "检查回诊";
        };
    }

    private static String paymentText(PaymentStatus status) {
        return switch (status) {
            case PAID -> "已支付";
            case UNPAID -> "待支付";
            case REFUNDED -> "已退款";
        };
    }

    private static JPanel verticalList() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private enum StatusFilter {
        ALL("全部状态"), BOOKED("待接诊"), COMPLETED("已完成"),
        CANCELLED("已取消"), NO_SHOW("未到诊");

        private final String label;

        StatusFilter(String label) { this.label = label; }

        boolean matches(AppointmentStatus status) {
            return this == ALL || name().equals(status.name());
        }

        @Override public String toString() { return label; }
    }
}
