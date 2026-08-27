package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** First-visit schedule search page backed by the real socket service. */
final class SlotSearchPanel extends JPanel {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M/d");

    private final ClientContext context;
    private final DefaultListModel<DepartmentView> departmentModel = new DefaultListModel<>();
    private final JList<DepartmentView> departmentList = new JList<>(departmentModel);
    private final JComboBox<DoctorOption> doctorCombo = new JComboBox<>();
    private final JPanel datePanel = new JPanel(new GridLayout(1, 7, 7, 0));
    private final JPanel resultPanel = new JPanel();
    private final JLabel statusLabel = new JLabel("请选择科室查看号源");
    private final JButton refreshButton = HospitalTheme.primaryButton("刷新号源");
    private final List<JToggleButton> dateButtons = new ArrayList<>();

    private List<SlotView> loadedSlots = List.of();
    private LocalDate selectedDate = LocalDate.now();
    private boolean busy;
    private boolean suppressDepartmentEvents;

    SlotSearchPanel(ClientContext context, Runnable goBack) {
        this.context = context;
        initializeView(goBack);
    }

    void activate() {
        buildDateButtons();
        if (context.currentSession().isEmpty()) {
            showState("登录状态已失效，请返回并重新登录。", HospitalTheme.WARNING);
            return;
        }
        if (departmentModel.isEmpty()) {
            loadDepartments();
        } else if (departmentList.getSelectedValue() != null) {
            searchSlots();
        }
    }

    private void initializeView(Runnable goBack) {
        setLayout(new BorderLayout(16, 16));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));
        add(createTop(goBack), BorderLayout.NORTH);
        add(createDepartments(), BorderLayout.WEST);
        add(createResults(), BorderLayout.CENTER);

        departmentList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !suppressDepartmentEvents && !busy) {
                doctorCombo.setModel(new DefaultComboBoxModel<>(
                        new DoctorOption[]{DoctorOption.all()}));
                searchSlots();
            }
        });
        doctorCombo.addActionListener(event -> renderSlots());
        refreshButton.addActionListener(event -> searchSlots());
    }

    private JPanel createTop(Runnable goBack) {
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JButton back = HospitalTheme.quietButton("‹ 返回医院首页");
        back.addActionListener(event -> goBack.run());
        heading.add(back, BorderLayout.WEST);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("预约挂号");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("首诊可按科室、日期和医生筛选未来 7 天号源");
        subtitle.setForeground(HospitalTheme.MUTED);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(subtitle);
        heading.add(titleBox, BorderLayout.CENTER);

        JPanel visitTypes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        visitTypes.setOpaque(false);
        JToggleButton firstVisit = new JToggleButton("首诊挂号", true);
        firstVisit.setForeground(HospitalTheme.PRIMARY_DARK);
        JToggleButton followUp = new JToggleButton("复诊挂号（后续开放）");
        followUp.setEnabled(false);
        ButtonGroup visitGroup = new ButtonGroup();
        visitGroup.add(firstVisit);
        visitGroup.add(followUp);
        visitTypes.add(firstVisit);
        visitTypes.add(followUp);
        heading.add(visitTypes, BorderLayout.EAST);
        top.add(heading, BorderLayout.NORTH);

        HospitalTheme.SurfacePanel dates = new HospitalTheme.SurfacePanel();
        dates.setLayout(new BorderLayout(12, 0));
        dates.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel label = new JLabel("就诊日期");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(HospitalTheme.TEXT);
        datePanel.setOpaque(false);
        dates.add(label, BorderLayout.WEST);
        dates.add(datePanel, BorderLayout.CENTER);
        top.add(dates, BorderLayout.SOUTH);
        return top;
    }

    private JComponent createDepartments() {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));
        panel.setPreferredSize(new Dimension(170, 0));

        JLabel title = new JLabel("选择科室");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16F));
        title.setForeground(HospitalTheme.TEXT);
        departmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        departmentList.setFixedCellHeight(42);
        departmentList.setCellRenderer(new DepartmentRenderer());
        departmentList.setBorder(BorderFactory.createEmptyBorder());
        departmentList.setBackground(HospitalTheme.SURFACE);

        JScrollPane scroll = new JScrollPane(departmentList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createResults() {
        JPanel area = new JPanel(new BorderLayout(0, 10));
        area.setOpaque(false);

        JPanel filter = new JPanel(new BorderLayout(12, 0));
        filter.setOpaque(false);
        JPanel doctor = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        doctor.setOpaque(false);
        doctor.add(new JLabel("医生"));
        doctorCombo.setPreferredSize(new Dimension(180, 34));
        doctorCombo.setModel(new DefaultComboBoxModel<>(
                new DoctorOption[]{DoctorOption.all()}));
        doctor.add(doctorCombo);
        filter.add(doctor, BorderLayout.WEST);
        filter.add(refreshButton, BorderLayout.EAST);
        area.add(filter, BorderLayout.NORTH);

        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBackground(HospitalTheme.BACKGROUND);
        JScrollPane scroll = new JScrollPane(resultPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        area.add(scroll, BorderLayout.CENTER);

        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        area.add(statusLabel, BorderLayout.SOUTH);
        return area;
    }

    private void buildDateButtons() {
        datePanel.removeAll();
        dateButtons.clear();
        ButtonGroup group = new ButtonGroup();
        LocalDate today = LocalDate.now();
        if (selectedDate.isBefore(today) || selectedDate.isAfter(today.plusDays(6))) {
            selectedDate = today;
        }
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = today.plusDays(offset);
            String dayName = offset == 0
                    ? "今天"
                    : date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA);
            JToggleButton button = new JToggleButton(
                    "<html><center>" + dayName + "<br>" + MONTH_DAY.format(date)
                            + "</center></html>");
            button.setFocusPainted(false);
            button.setSelected(date.equals(selectedDate));
            button.addActionListener(event -> {
                selectedDate = date;
                renderSlots();
            });
            group.add(button);
            dateButtons.add(button);
            datePanel.add(button);
        }
        datePanel.revalidate();
        datePanel.repaint();
    }

    private void loadDepartments() {
        setBusy(true, "正在从服务器加载科室……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.LIST_DEPARTMENTS, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        showResponseError(response);
                        return;
                    }
                    DepartmentListResponse data = response.getData()
                            instanceof DepartmentListResponse departments ? departments : null;
                    if (data == null) {
                        showState("服务器返回了无法识别的科室数据。", HospitalTheme.WARNING);
                        return;
                    }
                    suppressDepartmentEvents = true;
                    departmentModel.clear();
                    data.getDepartments().forEach(departmentModel::addElement);
                    if (!departmentModel.isEmpty()) {
                        departmentList.setSelectedIndex(0);
                    }
                    suppressDepartmentEvents = false;
                    setBusy(false, "科室已加载，正在查询号源……");
                    if (departmentList.getSelectedValue() != null) {
                        searchSlots();
                    } else {
                        showState("当前没有开放科室。", HospitalTheme.MUTED);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showState("科室加载已中断。", HospitalTheme.WARNING);
                } catch (ExecutionException exception) {
                    showNetworkError();
                } finally {
                    if (busy && departmentModel.isEmpty()) {
                        setBusy(false, statusLabel.getText());
                    }
                }
            }
        }.execute();
    }

    private void searchSlots() {
        DepartmentView department = departmentList.getSelectedValue();
        if (department == null || busy) {
            return;
        }
        setBusy(true, "正在查询“" + department.getDepartmentName() + "”的号源……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(
                        HospitalActions.SEARCH_SLOTS,
                        SearchSlotsRequest.firstVisit(department.getDepartmentId(), null));
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        loadedSlots = List.of();
                        renderSlots();
                        showResponseError(response);
                        return;
                    }
                    SlotListResponse data = response.getData()
                            instanceof SlotListResponse slots ? slots : null;
                    if (data == null) {
                        showState("服务器返回了无法识别的号源数据。", HospitalTheme.WARNING);
                        return;
                    }
                    loadedSlots = data.getSlots();
                    rebuildDoctorOptions();
                    selectEarliestAvailableDate();
                    renderSlots();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showState("号源查询已中断。", HospitalTheme.WARNING);
                } catch (ExecutionException exception) {
                    loadedSlots = List.of();
                    renderSlots();
                    showNetworkError();
                } finally {
                    setBusy(false, statusLabel.getText());
                }
            }
        }.execute();
    }

    private void rebuildDoctorOptions() {
        Map<String, DoctorOption> doctors = new LinkedHashMap<>();
        loadedSlots.forEach(slot -> doctors.putIfAbsent(
                slot.getDoctorId(),
                new DoctorOption(slot.getDoctorId(),
                        slot.getDoctorName() + " · " + slot.getDoctorTitle())));
        DefaultComboBoxModel<DoctorOption> model = new DefaultComboBoxModel<>();
        model.addElement(DoctorOption.all());
        doctors.values().forEach(model::addElement);
        doctorCombo.setModel(model);
    }

    private void selectEarliestAvailableDate() {
        boolean selectedDateHasSlots = loadedSlots.stream()
                .anyMatch(slot -> slot.getStartTime().toLocalDate().equals(selectedDate));
        if (selectedDateHasSlots || loadedSlots.isEmpty()) {
            return;
        }
        selectedDate = loadedSlots.getFirst().getStartTime().toLocalDate();
        for (int index = 0; index < dateButtons.size(); index++) {
            LocalDate buttonDate = LocalDate.now().plusDays(index);
            dateButtons.get(index).setSelected(buttonDate.equals(selectedDate));
        }
    }

    private void renderSlots() {
        resultPanel.removeAll();
        DoctorOption selectedDoctor = (DoctorOption) doctorCombo.getSelectedItem();
        List<SlotView> visible = loadedSlots.stream()
                .filter(slot -> slot.getStartTime().toLocalDate().equals(selectedDate))
                .filter(slot -> selectedDoctor == null || selectedDoctor.doctorId() == null
                        || slot.getDoctorId().equals(selectedDoctor.doctorId()))
                .toList();

        if (visible.isEmpty()) {
            addEmptyState();
            statusLabel.setText("该日期和医生条件下暂无号源，可切换日期或医生。");
        } else {
            for (SlotView slot : visible) {
                resultPanel.add(createSlotCard(slot));
                resultPanel.add(Box.createVerticalStrut(10));
            }
            statusLabel.setText("共显示 " + visible.size() + " 个号源；满号保留展示，停诊不展示。");
        }
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JComponent createSlotCard(SlotView slot) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel();
        card.setLayout(new BorderLayout(16, 8));
        card.setBorder(BorderFactory.createEmptyBorder(15, 17, 15, 17));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));

        JPanel doctor = new JPanel();
        doctor.setOpaque(false);
        doctor.setLayout(new BoxLayout(doctor, BoxLayout.Y_AXIS));
        JLabel doctorName = new JLabel(slot.getDoctorName() + "  " + slot.getDoctorTitle());
        doctorName.setFont(doctorName.getFont().deriveFont(Font.BOLD, 16F));
        doctorName.setForeground(HospitalTheme.TEXT);
        JLabel department = new JLabel(slot.getDepartmentName());
        department.setForeground(HospitalTheme.MUTED);
        doctor.add(doctorName);
        doctor.add(Box.createVerticalStrut(5));
        doctor.add(department);
        card.add(doctor, BorderLayout.WEST);

        JPanel details = new JPanel(new GridLayout(2, 1, 0, 4));
        details.setOpaque(false);
        details.add(new JLabel("就诊时间  " + TIME.format(slot.getStartTime())
                + "–" + TIME.format(slot.getEndTime())));
        details.add(new JLabel("剩余 " + slot.getRemaining() + " / " + slot.getCapacity()
                + "  ·  挂号费 ¥" + String.format(Locale.ROOT, "%.2f",
                slot.getPriceCents() / 100.0)));
        card.add(details, BorderLayout.CENTER);

        boolean available = slot.getAvailability() == SlotAvailability.AVAILABLE;
        JPanel action = new JPanel();
        action.setOpaque(false);
        action.setLayout(new BoxLayout(action, BoxLayout.Y_AXIS));
        JLabel state = new JLabel(available ? "可预约" : "已满号", SwingConstants.CENTER);
        state.setOpaque(true);
        state.setForeground(available ? HospitalTheme.SUCCESS : HospitalTheme.WARNING);
        state.setBackground(available
                ? HospitalTheme.SUCCESS_LIGHT : HospitalTheme.WARNING_LIGHT);
        state.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        JButton reserve = new JButton(available ? "预约（下一步）" : "不可预约");
        reserve.setEnabled(false);
        reserve.setToolTipText(available ? "本次提交先完成真实号源查询，预约将在下一步实现" : "该号源已满");
        action.add(state);
        action.add(Box.createVerticalStrut(7));
        action.add(reserve);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private void addEmptyState() {
        HospitalTheme.SurfacePanel empty = new HospitalTheme.SurfacePanel();
        empty.setLayout(new BorderLayout());
        empty.setBorder(BorderFactory.createEmptyBorder(50, 20, 50, 20));
        JLabel text = new JLabel(
                "<html><center><b>暂时没有符合条件的号源</b><br><br>"
                        + "请尝试切换上方日期或医生</center></html>",
                SwingConstants.CENTER);
        text.setForeground(HospitalTheme.MUTED);
        empty.add(text);
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        resultPanel.add(empty);
    }

    private void showResponseError(Response response) {
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())) {
            showState("登录状态已失效，请返回并重新登录。", HospitalTheme.WARNING);
        } else {
            showState("查询失败：" + response.getMessage(), HospitalTheme.WARNING);
        }
    }

    private void showNetworkError() {
        showState("无法连接服务器，请确认服务器已经启动后重试。", HospitalTheme.WARNING);
    }

    private void showState(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(message);
    }

    private void setBusy(boolean working, String message) {
        busy = working;
        departmentList.setEnabled(!working);
        doctorCombo.setEnabled(!working);
        refreshButton.setEnabled(!working);
        showState(message, HospitalTheme.MUTED);
    }

    private record DoctorOption(String doctorId, String label) {
        static DoctorOption all() {
            return new DoctorOption(null, "全部医生");
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class DepartmentRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean hasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, selected, hasFocus);
            if (value instanceof DepartmentView department) {
                label.setText(department.getDepartmentName());
            }
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 8));
            label.setForeground(selected ? Color.WHITE : HospitalTheme.TEXT);
            label.setBackground(selected ? HospitalTheme.PRIMARY : HospitalTheme.SURFACE);
            return label;
        }
    }
}
