package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.CreateScheduleRequest;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SetSchedulePublicationRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Hospital administrator workflow for creating and publishing doctor schedules. */
final class AdminSchedulePanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CARD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("M月d日 EEEE");

    private final ClientContext context;
    private final JComboBox<AdminDoctorView> doctorBox = new JComboBox<>();
    private final JLabel departmentValue = new JLabel("请先选择医生");
    private final JTextField dateField = new JTextField(
            LocalDate.now().plusDays(1).format(DATE_FORMAT), 12);
    private final JTextField startField = new JTextField("08:30", 7);
    private final JTextField endField = new JTextField("09:00", 7);
    private final JSpinner capacitySpinner = new JSpinner(
            new SpinnerNumberModel(10, 1, 200, 1));
    private final JSpinner feeSpinner = new JSpinner(
            new SpinnerNumberModel(12.0, 0.0, 1000.0, 1.0));
    private final JButton createButton = HospitalTheme.primaryButton("建立未发布排班");
    private final JLabel statusLabel = new JLabel("正在读取排班……");
    private final JButton retryButton = HospitalTheme.quietButton("重试");
    private final JPanel scheduleList = new JPanel();

    private boolean busy;

    AdminSchedulePanel(ClientContext context, Runnable back) {
        this.context = context;
        setLayout(new BorderLayout(0, 20));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(HospitalResponsiveLayout.constrainWidth(createHeader(back)),
                BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);

        doctorBox.setName("adminScheduleDoctorBox");
        dateField.setName("adminScheduleDateField");
        startField.setName("adminScheduleStartField");
        endField.setName("adminScheduleEndField");
        capacitySpinner.setName("adminScheduleCapacitySpinner");
        feeSpinner.setName("adminScheduleFeeSpinner");
        createButton.setName("createAdminScheduleButton");
        scheduleList.setName("adminScheduleList");

        doctorBox.addActionListener(event -> updateSelectedDepartment());
        createButton.addActionListener(event -> createSchedule());
        retryButton.addActionListener(event -> loadWorkspace());
        retryButton.setVisible(false);
    }

    void activate() {
        loadWorkspace();
    }

    private JComponent createHeader(Runnable back) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("排班管理");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 28F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("先建立草稿，核对时间和号数后再向患者发布");
        subtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(subtitle);
        JButton backButton = HospitalTheme.quietButton("返回管理工作台");
        backButton.addActionListener(event -> back.run());
        header.add(copy, BorderLayout.CENTER);
        header.add(backButton, BorderLayout.EAST);
        return header;
    }

    private JComponent createBody() {
        JPanel body = HospitalResponsiveLayout.grid(2, 360, 18, 18);
        body.setName("adminScheduleResponsiveBody");
        body.setOpaque(false);
        JComponent form = createForm();
        JScrollPane formScroll = HospitalResponsiveLayout.verticalScroll(form);
        body.add(formScroll);
        body.add(createScheduleArea());
        return body;
    }

    private JComponent createForm() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("NEW SCHEDULE");
        eyebrow.setFont(HospitalTheme.dataFont(Font.BOLD, 11F));
        eyebrow.setForeground(HospitalTheme.PRIMARY);
        JLabel title = new JLabel("新建排班草稿");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel note = new JLabel("草稿不会出现在患者挂号和医生工作台中。");
        note.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        note.setForeground(HospitalTheme.MUTED);
        heading.add(eyebrow);
        heading.add(Box.createVerticalStrut(5));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(note);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);
        addField(fields, constraints, "出诊医生", doctorBox);
        addField(fields, constraints, "所属科室", departmentValue);
        addField(fields, constraints, "日期（年-月-日）", dateField);

        JPanel times = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        times.setOpaque(false);
        times.add(startField);
        times.add(new JLabel("至"));
        times.add(endField);
        addField(fields, constraints, "出诊时段", times);

        JPanel numbers = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        numbers.setOpaque(false);
        numbers.add(new JLabel("号数"));
        numbers.add(capacitySpinner);
        numbers.add(Box.createHorizontalStrut(12));
        numbers.add(new JLabel("挂号费（元）"));
        numbers.add(feeSpinner);
        addField(fields, constraints, "号源设置", numbers);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(createButton, BorderLayout.NORTH);
        JLabel hint = new JLabel("建立后请在右侧找到该草稿并发布");
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setForeground(HospitalTheme.MUTED);
        footer.add(hint, BorderLayout.SOUTH);

        card.add(heading, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private static void addField(
            JPanel target,
            GridBagConstraints constraints,
            String labelText,
            JComponent component) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.TEXT);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label);
        row.add(Box.createVerticalStrut(6));
        row.add(component);
        constraints.gridy++;
        target.add(row, constraints);
    }

    private JComponent createScheduleArea() {
        HospitalTheme.SurfacePanel area = new HospitalTheme.SurfacePanel();
        area.setLayout(new BorderLayout(0, 12));
        area.setBorder(BorderFactory.createEmptyBorder(20, 20, 14, 20));

        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel("近期排班");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        title.setForeground(HospitalTheme.TEXT);
        JPanel state = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        state.setOpaque(false);
        statusLabel.setForeground(HospitalTheme.MUTED);
        state.add(statusLabel);
        state.add(retryButton);
        heading.add(title, BorderLayout.WEST);
        heading.add(state, BorderLayout.EAST);

        scheduleList.setOpaque(false);
        scheduleList.setLayout(new BoxLayout(scheduleList, BoxLayout.Y_AXIS));
        JScrollPane scroll = HospitalResponsiveLayout.verticalScroll(scheduleList);

        area.add(heading, BorderLayout.NORTH);
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    private void loadWorkspace() {
        if (busy) {
            return;
        }
        setBusy(true, "正在读取排班……");
        retryButton.setVisible(false);
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof AdminScheduleWorkspaceView data) {
                        showWorkspace(data);
                    } else {
                        showLoadError(response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadError("读取已中断，请重试。");
                } catch (ExecutionException exception) {
                    showLoadError("无法连接服务器，请确认服务器已经启动。");
                } finally {
                    setBusy(false, statusLabel.getText());
                }
            }
        }.execute();
    }

    private void showWorkspace(AdminScheduleWorkspaceView data) {
        AdminDoctorView selected = (AdminDoctorView) doctorBox.getSelectedItem();
        String selectedDoctorId = selected == null ? null : selected.getDoctorId();
        doctorBox.removeAllItems();
        for (AdminDoctorView doctor : data.getDoctors()) {
            doctorBox.addItem(doctor);
            if (doctor.getDoctorId().equals(selectedDoctorId)) {
                doctorBox.setSelectedItem(doctor);
            }
        }
        updateSelectedDepartment();
        renderSchedules(data.getSchedules());
        statusLabel.setText(data.getSchedules().isEmpty()
                ? "暂无未来排班"
                : "共 " + data.getSchedules().size() + " 个未来排班");
        statusLabel.setForeground(HospitalTheme.MUTED);
        retryButton.setVisible(false);
    }

    private void renderSchedules(List<SlotView> schedules) {
        scheduleList.removeAll();
        if (schedules.isEmpty()) {
            JLabel empty = new JLabel("还没有未来排班。可先在左侧建立一条草稿。");
            empty.setForeground(HospitalTheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(26, 8, 0, 0));
            scheduleList.add(empty);
        } else {
            for (SlotView schedule : schedules) {
                JComponent card = createScheduleCard(schedule);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 184));
                scheduleList.add(card);
                scheduleList.add(Box.createVerticalStrut(10));
            }
        }
        scheduleList.revalidate();
        scheduleList.repaint();
    }

    private JComponent createScheduleCard(SlotView schedule) {
        boolean published = schedule.getAvailability() != SlotAvailability.CLOSED;
        Color stateColor = published ? HospitalTheme.SUCCESS : HospitalTheme.WARNING;
        Color stateBackground = published
                ? HospitalTheme.SUCCESS_LIGHT : HospitalTheme.WARNING_LIGHT;
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.BACKGROUND, 12, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(15, 0));
        card.setBorder(BorderFactory.createEmptyBorder(15, 16, 15, 16));

        JPanel rail = new JPanel();
        rail.setBackground(stateColor);
        rail.setPreferredSize(new Dimension(5, 70));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel time = new JLabel(schedule.getStartTime().format(CARD_DATE_FORMAT)
                + "  " + schedule.getStartTime().format(TIME_FORMAT)
                + "–" + schedule.getEndTime().format(TIME_FORMAT));
        time.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        time.setForeground(HospitalTheme.TEXT);
        JLabel doctor = new JLabel(schedule.getDepartmentName() + " · "
                + schedule.getDoctorName() + " " + schedule.getDoctorTitle());
        doctor.setForeground(HospitalTheme.MUTED);
        int booked = schedule.getCapacity() - schedule.getRemaining();
        JLabel quota = new JLabel("号数 " + schedule.getCapacity()
                + " · 已预约 " + booked
                + " · 挂号费 ¥" + String.format("%.2f", schedule.getPriceCents() / 100.0));
        quota.setFont(HospitalTheme.dataFont(Font.PLAIN, 13F));
        quota.setForeground(HospitalTheme.TEXT);
        copy.add(time);
        copy.add(Box.createVerticalStrut(5));
        copy.add(doctor);
        copy.add(Box.createVerticalStrut(8));
        copy.add(quota);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JLabel state = new JLabel(published
                ? (schedule.getAvailability() == SlotAvailability.FULL ? "已发布 · 已满" : "已发布")
                : "未发布 / 已关闭");
        state.setOpaque(true);
        state.setBackground(stateBackground);
        state.setForeground(stateColor);
        state.setHorizontalAlignment(SwingConstants.CENTER);
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        state.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        state.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JButton toggle = published
                ? HospitalTheme.quietButton("关闭排班")
                : HospitalTheme.primaryButton("发布号源");
        toggle.setName("toggleAdminScheduleButton");
        toggle.setActionCommand(schedule.getScheduleId());
        toggle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        boolean canClose = !published || booked == 0;
        boolean notStarted = LocalDateTime.now().isBefore(schedule.getStartTime());
        toggle.setEnabled(notStarted && canClose);
        if (published && booked > 0) {
            toggle.setToolTipText("已有预约，不能直接关闭该排班");
        } else if (!notStarted) {
            toggle.setToolTipText("排班开始后不能更改发布状态");
        }
        toggle.addActionListener(event -> setPublished(schedule, !published));
        actions.add(state);
        actions.add(Box.createVerticalStrut(12));
        actions.add(toggle);

        card.add(rail, BorderLayout.WEST);
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, actions, 540, 15),
                BorderLayout.CENTER);
        return card;
    }

    private void createSchedule() {
        if (busy) {
            return;
        }
        AdminDoctorView doctor = (AdminDoctorView) doctorBox.getSelectedItem();
        if (doctor == null) {
            showActionError("当前没有可排班的医生，请先完成医生资料维护。");
            return;
        }
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DATE_FORMAT);
            LocalDateTime start = LocalDateTime.of(
                    date, LocalTime.parse(startField.getText().trim(), TIME_FORMAT));
            LocalDateTime end = LocalDateTime.of(
                    date, LocalTime.parse(endField.getText().trim(), TIME_FORMAT));
            int capacity = ((Number) capacitySpinner.getValue()).intValue();
            int feeCents = (int) Math.round(
                    ((Number) feeSpinner.getValue()).doubleValue() * 100.0);
            CreateScheduleRequest request = new CreateScheduleRequest(
                    doctor.getDoctorId(), doctor.getDepartmentId(),
                    start, end, feeCents, capacity);
            runMutation(
                    HospitalActions.CREATE_SCHEDULE,
                    request,
                    "草稿已建立，正在更新排班列表……");
        } catch (DateTimeParseException exception) {
            showActionError("日期或时间格式不正确。日期示例：2026-09-08，时间示例：08:30。");
        } catch (IllegalArgumentException exception) {
            showActionError("请检查：结束时间须晚于开始时间，号数和挂号费须在合理范围内。");
        }
    }

    private void setPublished(SlotView schedule, boolean published) {
        if (busy) {
            return;
        }
        runMutation(
                HospitalActions.SET_SCHEDULE_PUBLICATION,
                new SetSchedulePublicationRequest(schedule.getScheduleId(), published),
                published ? "排班已发布，正在同步号源……" : "排班已关闭，正在同步列表……");
    }

    private void runMutation(String action, Object data, String successMessage) {
        setBusy(true, "正在保存……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(action, (java.io.Serializable) data);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        statusLabel.setText(successMessage);
                        setBusy(false, successMessage);
                        loadWorkspace();
                    } else {
                        showActionError(response.getMessage());
                        setBusy(false, statusLabel.getText());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showActionError("保存已中断，请重试。");
                    setBusy(false, statusLabel.getText());
                } catch (ExecutionException exception) {
                    showActionError("无法连接服务器，请确认服务器已经启动。");
                    setBusy(false, statusLabel.getText());
                }
            }
        }.execute();
    }

    private void updateSelectedDepartment() {
        AdminDoctorView doctor = (AdminDoctorView) doctorBox.getSelectedItem();
        departmentValue.setText(doctor == null ? "请先选择医生" : doctor.getDepartmentName());
        departmentValue.setForeground(doctor == null ? HospitalTheme.MUTED : HospitalTheme.TEXT);
    }

    private void showLoadError(String message) {
        statusLabel.setText("读取失败：" + safeMessage(message));
        statusLabel.setForeground(HospitalTheme.WARNING);
        retryButton.setVisible(true);
    }

    private void showActionError(String message) {
        statusLabel.setText("操作失败：" + safeMessage(message));
        statusLabel.setForeground(HospitalTheme.WARNING);
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        createButton.setEnabled(!value);
        doctorBox.setEnabled(!value);
        statusLabel.setText(message);
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "服务器未返回具体原因。" : message;
    }
}
