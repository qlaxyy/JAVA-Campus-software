package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.BatchCreateSchedulesRequest;
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
import javax.swing.SpinnerNumberModel;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/** Administrator page for multi-date manual scheduling and precise schedule lookup. */
final class AdminSchedulePanel extends JPanel {

    private static final String ALL_DEPARTMENTS = "全部科室";
    private static final String ALL_DOCTORS = "全部医生";
    private static final String ALL_DATES = "全部日期";
    private static final String ALL_TIMES = "全部时段";
    private static final String ALL_STATES = "全部状态";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CARD_DATE =
            DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA);

    private final ClientContext context;
    private final JComboBox<AdminDoctorView> doctorBox = new JComboBox<>();
    private final JLabel departmentValue = new JLabel("请先选择医生");
    private final JLabel schedulePreview = new JLabel("请选择日期和时段");
    private final HospitalMultiDateCalendarPicker dateCalendar =
            new HospitalMultiDateCalendarPicker(
                    LocalDate.now(), LocalDate.now(), ignored -> updateSchedulePreview());
    private final JComboBox<String> startTimeBox =
            new JComboBox<>(timeOptions(false));
    private final JComboBox<String> endTimeBox =
            new JComboBox<>(timeOptions(true));
    private final JSpinner capacitySpinner = new JSpinner(
            new SpinnerNumberModel(10, 1, 200, 1));
    private final JSpinner feeSpinner = new JSpinner(
            new SpinnerNumberModel(12.0, 0.0, 1000.0, 1.0));
    private final JButton createButton = HospitalTheme.primaryButton("建立未发布排班");
    private final JLabel statusLabel = new JLabel("正在读取排班……");
    private final JButton retryButton = HospitalTheme.quietButton("重试");
    private final JPanel scheduleList = new JPanel();
    private final JComboBox<String> departmentFilter = new JComboBox<>();
    private final JComboBox<Object> doctorFilter = new JComboBox<>();
    private final JComboBox<Object> dateFilter = new JComboBox<>();
    private final JComboBox<String> timeFilter = new JComboBox<>();
    private final JComboBox<String> stateFilter = new JComboBox<>(
            new String[]{ALL_STATES, "已发布", "已关闭"});
    private final List<JButton> slotActionButtons = new ArrayList<>();

    private List<SlotView> loadedSchedules = List.of();
    private boolean busy;
    private boolean updatingFilters;
    private boolean filtersInitialized;

    AdminSchedulePanel(ClientContext context, Runnable back) {
        this.context = context;
        setLayout(new BorderLayout(0, 20));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(HospitalResponsiveLayout.constrainWidth(HospitalPageHeader.create(
                "排班管理",
                "选择多日期与出诊时段，建立草稿后按条件检索和发布",
                "管理首页", back, null)), BorderLayout.NORTH);
        add(HospitalResponsiveLayout.constrainWidth(createBody()), BorderLayout.CENTER);

        doctorBox.setName("adminScheduleDoctorBox");
        dateCalendar.setName("adminScheduleMultiDateCalendar");
        startTimeBox.setName("adminScheduleStartTimeBox");
        endTimeBox.setName("adminScheduleEndTimeBox");
        capacitySpinner.setName("adminScheduleCapacitySpinner");
        feeSpinner.setName("adminScheduleFeeSpinner");
        createButton.setName("createManualSchedulesButton");
        scheduleList.setName("adminScheduleList");
        departmentFilter.setName("adminScheduleDepartmentFilter");
        doctorFilter.setName("adminScheduleDoctorFilter");
        dateFilter.setName("adminScheduleDateFilter");
        timeFilter.setName("adminScheduleTimeFilter");
        stateFilter.setName("adminScheduleStatusFilter");
        startTimeBox.setSelectedItem("09:00");
        endTimeBox.setSelectedItem("18:00");
        departmentFilter.addItem(ALL_DEPARTMENTS);
        doctorFilter.addItem(ALL_DOCTORS);
        dateFilter.addItem(ALL_DATES);
        timeFilter.addItem(ALL_TIMES);

        doctorBox.addActionListener(event -> updateSelectedDepartment());
        startTimeBox.addActionListener(event -> {
            keepEndTimeAfterStart();
            updateSchedulePreview();
        });
        endTimeBox.addActionListener(event -> updateSchedulePreview());
        createButton.addActionListener(event -> createManualSchedules());
        retryButton.addActionListener(event -> loadWorkspace());
        retryButton.setVisible(false);
        departmentFilter.addActionListener(event -> renderSchedules());
        doctorFilter.addActionListener(event -> renderSchedules());
        dateFilter.addActionListener(event -> renderSchedules());
        timeFilter.addActionListener(event -> renderSchedules());
        stateFilter.addActionListener(event -> renderSchedules());
        updateSchedulePreview();
    }

    void activate() {
        loadWorkspace();
    }

    private JComponent createBody() {
        JPanel body = HospitalResponsiveLayout.grid(2, 380, 18, 18);
        body.setName("adminScheduleResponsiveBody");
        body.setOpaque(false);
        body.add(HospitalResponsiveLayout.verticalScroll(createManualForm()));
        body.add(createSearchArea());
        return body;
    }

    private JComponent createManualForm() {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        JPanel heading = vertical();
        JLabel title = new JLabel("手动批量排班");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel note = new JLabel("可为同一位医生选多个日期；每个日期按 30 分钟生成草稿。");
        note.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        note.setForeground(HospitalTheme.MUTED);
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
        addField(fields, constraints, "出诊日期（可多选）", dateCalendar);

        JPanel times = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        times.setOpaque(false);
        times.add(new JLabel("开始"));
        times.add(startTimeBox);
        times.add(new JLabel("至"));
        times.add(new JLabel("结束"));
        times.add(endTimeBox);
        addField(fields, constraints, "出诊时段", times);

        JPanel numbers = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        numbers.setOpaque(false);
        numbers.add(new JLabel("每时段号数"));
        numbers.add(capacitySpinner);
        numbers.add(Box.createHorizontalStrut(12));
        numbers.add(new JLabel("挂号费（元）"));
        numbers.add(feeSpinner);
        addField(fields, constraints, "号源设置", numbers);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        schedulePreview.setName("adminSchedulePreview");
        schedulePreview.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        schedulePreview.setForeground(HospitalTheme.PRIMARY_DARK);
        schedulePreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        footer.add(schedulePreview, BorderLayout.NORTH);
        footer.add(createButton, BorderLayout.CENTER);
        JLabel hint = new JLabel("提交前校验所有日期；任一时段冲突则整批不建立。");
        hint.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        hint.setForeground(HospitalTheme.MUTED);
        hint.setHorizontalAlignment(JLabel.CENTER);
        footer.add(hint, BorderLayout.SOUTH);

        card.add(heading, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JComponent createSearchArea() {
        HospitalTheme.SurfacePanel area = new HospitalTheme.SurfacePanel();
        area.setLayout(new BorderLayout(0, 14));
        area.setBorder(BorderFactory.createEmptyBorder(20, 20, 14, 20));
        JPanel top = vertical();
        JLabel title = new JLabel("检索具体排班");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 21F));
        title.setForeground(HospitalTheme.TEXT);
        top.add(title);
        top.add(Box.createVerticalStrut(8));
        JPanel filters = HospitalResponsiveLayout.grid(3, 140, 8, 8);
        filters.setName("adminScheduleSearchFilters");
        filters.setOpaque(false);
        filters.add(filterField("科室", departmentFilter));
        filters.add(filterField("医生", doctorFilter));
        filters.add(filterField("日期", dateFilter));
        filters.add(filterField("开始时间", timeFilter));
        filters.add(filterField("状态", stateFilter));
        top.add(filters);
        top.add(Box.createVerticalStrut(8));
        JPanel state = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        state.setOpaque(false);
        statusLabel.setForeground(HospitalTheme.MUTED);
        state.add(statusLabel);
        state.add(retryButton);
        top.add(state);
        scheduleList.setOpaque(false);
        scheduleList.setLayout(new BoxLayout(scheduleList, BoxLayout.Y_AXIS));
        area.add(top, BorderLayout.NORTH);
        area.add(HospitalResponsiveLayout.verticalScroll(scheduleList), BorderLayout.CENTER);
        return area;
    }

    private static JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static void addField(
            JPanel target, GridBagConstraints constraints,
            String text, JComponent component) {
        JPanel row = vertical();
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        label.setForeground(HospitalTheme.TEXT);
        row.add(label);
        row.add(Box.createVerticalStrut(6));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(component);
        constraints.gridy++;
        target.add(row, constraints);
    }

    private static JComponent filterField(String text, JComboBox<?> box) {
        JPanel field = vertical();
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.PLAIN, 11F));
        label.setForeground(HospitalTheme.MUTED);
        field.add(label);
        field.add(Box.createVerticalStrut(3));
        field.add(box);
        return field;
    }

    private void updateSchedulePreview() {
        int dateCount = dateCalendar.getSelectedDates().size();
        try {
            long slotsPerDate = (timeMinutes(selectedTime(endTimeBox))
                    - timeMinutes(selectedTime(startTimeBox))) / 30;
            if (slotsPerDate <= 0 || dateCount == 0) {
                schedulePreview.setText("请选择至少一个日期，并确保结束时间晚于开始时间。");
                schedulePreview.setForeground(HospitalTheme.WARNING);
                return;
            }
            schedulePreview.setText("将建立 " + dateCount + " 天 × " + slotsPerDate
                    + " 个时段，共 " + (dateCount * slotsPerDate) + " 条未发布排班");
            schedulePreview.setForeground(HospitalTheme.PRIMARY_DARK);
        } catch (RuntimeException exception) {
            schedulePreview.setText("请选择有效的出诊时段。");
            schedulePreview.setForeground(HospitalTheme.WARNING);
        }
    }

    private void createManualSchedules() {
        if (busy) {
            return;
        }
        AdminDoctorView doctor = (AdminDoctorView) doctorBox.getSelectedItem();
        if (doctor == null) {
            showError("请先选择在岗医生。");
            return;
        }
        Set<LocalDate> dates = dateCalendar.getSelectedDates();
        if (dates.isEmpty()) {
            showError("请至少选择一个出诊日期。");
            return;
        }
        try {
            LocalTime startTime = LocalTime.parse(selectedTime(startTimeBox), TIME);
            int endMinute = timeMinutes(selectedTime(endTimeBox));
            long minutes = endMinute - timeMinutes(selectedTime(startTimeBox));
            if (minutes <= 0 || minutes % 30 != 0) {
                throw new IllegalArgumentException("结束时间须晚于开始时间，且按 30 分钟对齐。");
            }
            int capacity = ((Number) capacitySpinner.getValue()).intValue();
            int feeCents = (int) Math.round(
                    ((Number) feeSpinner.getValue()).doubleValue() * 100.0);
            LocalDateTime now = LocalDateTime.now();
            List<CreateScheduleRequest> schedules = new ArrayList<>();
            for (LocalDate date : dates.stream().sorted().toList()) {
                for (int minute = timeMinutes(selectedTime(startTimeBox));
                     minute < endMinute;
                     minute += 30) {
                    LocalTime time = LocalTime.of(minute / 60, minute % 60);
                    LocalDateTime start = LocalDateTime.of(date, time);
                    if (!start.isAfter(now)) {
                        throw new IllegalArgumentException(
                                "今天只能建立当前时间之后的排班，请调整时段或取消选择今天。");
                    }
                    schedules.add(new CreateScheduleRequest(
                            doctor.getDoctorId(), doctor.getDepartmentId(),
                            start, start.plusMinutes(30), feeCents, capacity));
                }
            }
            sendManualBatch(new BatchCreateSchedulesRequest(schedules));
        } catch (IllegalArgumentException exception) {
            showError(safeMessage(exception.getMessage()));
        }
    }

    private void sendManualBatch(BatchCreateSchedulesRequest request) {
        setBusy(true, "正在校验并建立排班草稿……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.CREATE_SCHEDULES, request);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess() && response.getData() instanceof List<?> list) {
                        setBusy(false, "已建立 " + list.size()
                                + " 条未发布排班，请在右侧检索后发布。");
                        loadWorkspace();
                    } else {
                        showError("建立失败：" + safeMessage(response.getMessage()));
                        setBusy(false, statusLabel.getText());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("建立已中断，请重新读取排班核对。");
                    setBusy(false, statusLabel.getText());
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动。");
                    setBusy(false, statusLabel.getText());
                }
            }
        }.execute();
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
                        showError("读取失败：" + safeMessage(response.getMessage()));
                        retryButton.setVisible(true);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("读取已中断，请重试。");
                    retryButton.setVisible(true);
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动。");
                    retryButton.setVisible(true);
                } finally {
                    setBusy(false, statusLabel.getText());
                }
            }
        }.execute();
    }

    private void showWorkspace(AdminScheduleWorkspaceView data) {
        String selectedDoctorId = doctorBox.getSelectedItem() instanceof AdminDoctorView doctor
                ? doctor.getDoctorId() : null;
        doctorBox.removeAllItems();
        for (AdminDoctorView doctor : data.getDoctors()) {
            if (doctor.isActive()) {
                doctorBox.addItem(doctor);
                if (doctor.getDoctorId().equals(selectedDoctorId)) {
                    doctorBox.setSelectedItem(doctor);
                }
            }
        }
        updateSelectedDepartment();
        loadedSchedules = data.getSchedules();
        rebuildFilters(data.getDoctors(), loadedSchedules);
        renderSchedules();
        statusLabel.setText("共 " + loadedSchedules.size() + " 个未来排班");
        statusLabel.setForeground(HospitalTheme.MUTED);
        retryButton.setVisible(false);
    }

    private void rebuildFilters(List<AdminDoctorView> doctors, List<SlotView> slots) {
        Object department = departmentFilter.getSelectedItem();
        Object doctorChoice = doctorFilter.getSelectedItem();
        String doctorId = doctorChoice instanceof AdminDoctorView doctor
                ? doctor.getDoctorId() : null;
        Object date = dateFilter.getSelectedItem();
        Object time = timeFilter.getSelectedItem();
        updatingFilters = true;
        try {
            departmentFilter.removeAllItems();
            departmentFilter.addItem(ALL_DEPARTMENTS);
            slots.stream().map(SlotView::getDepartmentName).distinct().sorted()
                    .forEach(departmentFilter::addItem);
            departmentFilter.setSelectedItem(contains(departmentFilter, department)
                    ? department : ALL_DEPARTMENTS);
            doctorFilter.removeAllItems();
            doctorFilter.addItem(ALL_DOCTORS);
            doctors.forEach(doctorFilter::addItem);
            doctorFilter.setSelectedItem(doctors.stream()
                    .filter(doctor -> doctor.getDoctorId().equals(doctorId))
                    .findFirst().orElse(null));
            if (doctorFilter.getSelectedItem() == null) {
                doctorFilter.setSelectedItem(ALL_DOCTORS);
            }
            dateFilter.removeAllItems();
            dateFilter.addItem(ALL_DATES);
            List<LocalDate> dates = slots.stream().map(slot -> slot.getStartTime().toLocalDate())
                    .distinct().sorted().toList();
            dates.forEach(dateFilter::addItem);
            dateFilter.setSelectedItem(!filtersInitialized && !dates.isEmpty()
                    ? dates.getFirst() : contains(dateFilter, date) ? date : ALL_DATES);
            timeFilter.removeAllItems();
            timeFilter.addItem(ALL_TIMES);
            slots.stream().map(slot -> TIME.format(slot.getStartTime()))
                    .distinct().sorted().forEach(timeFilter::addItem);
            timeFilter.setSelectedItem(contains(timeFilter, time) ? time : ALL_TIMES);
        } finally {
            updatingFilters = false;
            filtersInitialized = true;
        }
    }

    private void renderSchedules() {
        if (updatingFilters) {
            return;
        }
        slotActionButtons.clear();
        scheduleList.removeAll();
        List<SlotView> matches = loadedSchedules.stream().filter(this::matchesFilters)
                .sorted(Comparator.comparing(SlotView::getStartTime)
                        .thenComparing(SlotView::getDoctorName)).toList();
        if (matches.isEmpty()) {
            JLabel empty = new JLabel(loadedSchedules.isEmpty()
                    ? "还没有未来排班。可在左侧建立手动排班。"
                    : "没有匹配排班，请调整检索条件。");
            empty.setForeground(HospitalTheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(24, 8, 0, 0));
            scheduleList.add(empty);
        } else {
            for (SlotView schedule : matches) {
                JComponent card = scheduleCard(schedule);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 164));
                scheduleList.add(card);
                scheduleList.add(Box.createVerticalStrut(10));
            }
        }
        scheduleList.revalidate();
        scheduleList.repaint();
    }

    private boolean matchesFilters(SlotView slot) {
        Object department = departmentFilter.getSelectedItem();
        if (department instanceof String text && !ALL_DEPARTMENTS.equals(text)
                && !text.equals(slot.getDepartmentName())) return false;
        Object doctor = doctorFilter.getSelectedItem();
        if (doctor instanceof AdminDoctorView selected
                && !selected.getDoctorId().equals(slot.getDoctorId())) return false;
        Object date = dateFilter.getSelectedItem();
        if (date instanceof LocalDate selected
                && !selected.equals(slot.getStartTime().toLocalDate())) return false;
        Object time = timeFilter.getSelectedItem();
        if (time instanceof String selected && !ALL_TIMES.equals(selected)
                && !selected.equals(TIME.format(slot.getStartTime()))) return false;
        boolean published = slot.getAvailability() != SlotAvailability.CLOSED;
        Object state = stateFilter.getSelectedItem();
        return !("已发布".equals(state) && !published
                || "已关闭".equals(state) && published);
    }

    private JComponent scheduleCard(SlotView schedule) {
        boolean published = schedule.getAvailability() != SlotAvailability.CLOSED;
        int booked = schedule.getCapacity() - schedule.getRemaining();
        Color accent = published ? HospitalTheme.SUCCESS : HospitalTheme.WARNING;
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.BACKGROUND, 12, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 15, 14, 15));
        JPanel rail = new JPanel();
        rail.setBackground(accent);
        rail.setPreferredSize(new Dimension(5, 70));
        card.add(rail, BorderLayout.WEST);
        JPanel copy = vertical();
        JLabel time = new JLabel(schedule.getStartTime().format(CARD_DATE) + "  "
                + TIME.format(schedule.getStartTime()) + "–" + TIME.format(schedule.getEndTime()));
        time.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        time.setForeground(HospitalTheme.TEXT);
        JLabel identity = new JLabel(schedule.getDepartmentName() + " · "
                + schedule.getDoctorName() + " " + schedule.getDoctorTitle());
        identity.setForeground(HospitalTheme.MUTED);
        JLabel facts = new JLabel("号数 " + schedule.getCapacity() + " · 已预约 "
                + booked + " · 挂号费 ¥" + String.format(Locale.ROOT,
                "%.2f", schedule.getPriceCents() / 100.0));
        facts.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        copy.add(time); copy.add(Box.createVerticalStrut(4)); copy.add(identity);
        copy.add(Box.createVerticalStrut(6)); copy.add(facts);

        JPanel action = vertical();
        JLabel state = new JLabel(published ? "已发布" : "已关闭");
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        state.setForeground(accent);
        JButton toggle = published ? HospitalTheme.quietButton("取消此排班")
                : HospitalTheme.primaryButton("发布号源");
        toggle.setName("toggleAdminScheduleButton");
        toggle.setActionCommand(schedule.getScheduleId());
        boolean eligible = LocalDateTime.now().isBefore(schedule.getStartTime())
                && (!published || booked == 0);
        toggle.putClientProperty("hospitalEligible", eligible);
        toggle.setEnabled(eligible && !busy);
        if (!eligible) {
            toggle.setText(booked > 0 ? "已有预约，不能取消" : "排班已开始，不能调整");
            HospitalTheme.applyDisabledStyle(toggle);
            toggle.setToolTipText(booked > 0 ? "请先处理患者预约和退款。" : "排班开始后不能变更状态。");
        }
        toggle.addActionListener(event -> setPublished(schedule, !published));
        slotActionButtons.add(toggle);
        action.add(state); action.add(Box.createVerticalStrut(9)); action.add(toggle);
        card.add(HospitalResponsiveLayout.adaptiveRow(copy, action, 540, 12),
                BorderLayout.CENTER);
        return card;
    }

    private void setPublished(SlotView schedule, boolean published) {
        if (busy) return;
        setBusy(true, published ? "正在发布号源……" : "正在取消排班……");
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.SET_SCHEDULE_PUBLICATION,
                        new SetSchedulePublicationRequest(schedule.getScheduleId(), published));
            }
            @Override protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        setBusy(false, published ? "排班已发布。" : "排班已取消。");
                        loadWorkspace();
                    } else {
                        showError("操作失败：" + safeMessage(response.getMessage()));
                        setBusy(false, statusLabel.getText());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("操作已中断，请重新读取排班。");
                    setBusy(false, statusLabel.getText());
                } catch (ExecutionException exception) {
                    showError("无法连接服务器，请确认服务器已经启动。");
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

    private void keepEndTimeAfterStart() {
        String start = selectedTime(startTimeBox);
        String end = selectedTime(endTimeBox);
        if ("24:00".equals(end) || end.compareTo(start) > 0) return;
        endTimeBox.setSelectedIndex(Math.min(startTimeBox.getSelectedIndex() + 1,
                endTimeBox.getItemCount() - 1));
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        createButton.setEnabled(!value);
        doctorBox.setEnabled(!value);
        startTimeBox.setEnabled(!value);
        endTimeBox.setEnabled(!value);
        slotActionButtons.forEach(button -> button.setEnabled(!value
                && Boolean.TRUE.equals(button.getClientProperty("hospitalEligible"))));
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(HospitalTheme.WARNING);
    }

    private static boolean contains(JComboBox<?> box, Object value) {
        for (int index = 0; index < box.getItemCount(); index++) {
            if (Objects.equals(box.getItemAt(index), value)) return true;
        }
        return false;
    }

    private static String[] timeOptions(boolean endOptions) {
        List<String> values = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            values.add(String.format(Locale.ROOT, "%02d:00", hour));
            values.add(String.format(Locale.ROOT, "%02d:30", hour));
        }
        if (endOptions) values.add("24:00");
        return values.toArray(String[]::new);
    }

    private static String selectedTime(JComboBox<String> box) {
        Object value = box.getSelectedItem();
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("请选择出诊时段。");
        }
        return text;
    }

    private static int timeMinutes(String value) {
        if ("24:00".equals(value)) {
            return 24 * 60;
        }
        LocalTime time = LocalTime.parse(value, TIME);
        return time.getHour() * 60 + time.getMinute();
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "服务器未返回具体原因。" : message;
    }
}
