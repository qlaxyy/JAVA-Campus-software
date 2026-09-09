package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** First-visit and ordinary follow-up schedule booking backed by the socket service. */
final class SlotSearchPanel extends JPanel {

    private static final String DEPARTMENT_PAGE = "department-page";
    private static final String SCHEDULE_PAGE = "schedule-page";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M/d");

    private final ClientContext context;
    private final CardLayout pageCards = new CardLayout();
    private final JPanel pages = new JPanel(pageCards);
    private final JPanel majorDepartmentPanel = new JPanel();
    private final JPanel departmentDetailPanel = new JPanel();
    private final JLabel departmentDetailTitle = new JLabel("请选择科室");
    private final JTextField departmentSearchField = new JTextField();
    private final JLabel selectedDepartmentLabel = new JLabel("尚未选择细分科室");
    private final JLabel scheduleSummaryLabel = new JLabel("选择日期查看排班");
    private final JTextField doctorSearchField = new JTextField();
    private final JPanel datePanel = HospitalResponsiveLayout.grid(7, 74, 7, 7);
    private final JPanel resultPanel = new JPanel();
    private final JLabel statusLabel = new JLabel("请选择或搜索科室开始挂号");
    private final JLabel pageTitle = new JLabel("预约挂号");
    private final JLabel pageSubtitle = new JLabel("先找到具体科室，再选择日期与医生");
    private final JButton firstVisitButton = HospitalTheme.quietButton("首诊挂号");
    private final JButton followUpButton = HospitalTheme.quietButton("普通复诊");
    private final JButton retryButton = HospitalTheme.quietButton("重新加载");
    private final JButton chooseDepartmentButton =
            HospitalTheme.quietButton("‹ 重新选择科室");
    private final List<JToggleButton> dateButtons = new ArrayList<>();
    private final List<AbstractButton> departmentButtons = new ArrayList<>();
    private final Map<String, DepartmentView> departmentsById = new LinkedHashMap<>();
    private final Map<String, List<DepartmentView>> childrenByParent =
            new LinkedHashMap<>();

    private List<SlotView> loadedSlots = List.of();
    private DepartmentView selectedMajorDepartment;
    private DepartmentView selectedDepartment;
    private VisitType visitType = VisitType.FIRST_VISIT;
    private ConsultationRecordView sourceConsultation;
    private LocalDate selectedDate = LocalDate.now();
    private boolean busy;
    private boolean departmentsLoaded;
    private String pendingDepartmentId;

    SlotSearchPanel(ClientContext context, Runnable goBack) {
        this(context, goBack, () -> { });
    }

    SlotSearchPanel(
            ClientContext context,
            Runnable goBack,
            Runnable openFollowUpSelection) {
        this.context = context;
        initializeView(goBack, openFollowUpSelection);
    }

    void activate() {
        if (busy) {
            return;
        }
        visitType = VisitType.FIRST_VISIT;
        sourceConsultation = null;
        pendingDepartmentId = null;
        chooseDepartmentButton.setVisible(true);
        updateVisitModeHeader();
        buildDateButtons();
        showDepartmentPage();
        if (context.currentSession().isEmpty()) {
            showState("登录状态已失效，请返回并重新登录。", HospitalTheme.WARNING);
            return;
        }
        if (!departmentsLoaded) {
            loadDepartments();
        } else {
            showState("请选择左侧科室，或搜索具体科室。", HospitalTheme.MUTED);
        }
    }

    void activateForFollowUp(ConsultationRecordView source) {
        if (busy) {
            return;
        }
        if (source == null || source.getOutcome() != ConsultationOutcome.COMPLETED) {
            showState("这条诊疗记录尚不能用于普通复诊。", HospitalTheme.WARNING);
            return;
        }
        visitType = VisitType.FOLLOW_UP;
        sourceConsultation = source;
        pendingDepartmentId = null;
        selectedDepartment = null;
        loadedSlots = List.of();
        selectedDate = LocalDate.now();
        doctorSearchField.setText("");
        chooseDepartmentButton.setVisible(false);
        selectedDepartmentLabel.setText("复诊科室  ›  " + source.getDepartmentName()
                + "  ·  原接诊医生 " + source.getDoctorName());
        scheduleSummaryLabel.setText("正在加载复诊排班");
        updateVisitModeHeader();
        buildDateButtons();
        renderEmptyState("正在加载复诊排班", "请稍候");
        pageCards.show(pages, SCHEDULE_PAGE);
        searchSlots();
    }

    void activateForDepartment(String departmentId) {
        if (busy) {
            return;
        }
        visitType = VisitType.FIRST_VISIT;
        sourceConsultation = null;
        selectedDepartment = null;
        selectedMajorDepartment = null;
        loadedSlots = List.of();
        selectedDate = LocalDate.now();
        doctorSearchField.setText("");
        chooseDepartmentButton.setVisible(true);
        updateVisitModeHeader();
        buildDateButtons();
        if (context.currentSession().isEmpty()) {
            showDepartmentPage();
            showState("登录状态已失效，请返回并重新登录。", HospitalTheme.WARNING);
            return;
        }
        if (!departmentsLoaded) {
            pendingDepartmentId = departmentId;
            showDepartmentPage();
            loadDepartments();
        } else {
            pendingDepartmentId = null;
            openRequestedDepartment(departmentId);
        }
    }

    private void initializeView(Runnable goBack, Runnable openFollowUpSelection) {
        setLayout(new BorderLayout(18, 16));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        add(HospitalResponsiveLayout.constrainWidth(createTop(goBack)),
                BorderLayout.NORTH);

        pages.setOpaque(false);
        pages.add(createDepartmentPage(), DEPARTMENT_PAGE);
        pages.add(createSchedulePage(), SCHEDULE_PAGE);
        add(pages, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        pageCards.show(pages, DEPARTMENT_PAGE);

        chooseDepartmentButton.addActionListener(event -> showDepartmentPage());
        firstVisitButton.setName("firstVisitModeButton");
        followUpButton.setName("followUpModeButton");
        firstVisitButton.addActionListener(event -> activate());
        followUpButton.addActionListener(event -> openFollowUpSelection.run());
        retryButton.addActionListener(event -> retryCurrentLoad());
        retryButton.setVisible(false);
        doctorSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                renderSlots();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                renderSlots();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                renderSlots();
            }
        });
        departmentSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateDepartmentSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateDepartmentSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateDepartmentSearch();
            }
        });
        renderDepartmentPrompt(
                "请选择科室",
                "点击左侧科室查看下级分类，或直接搜索具体科室。");
        renderEmptyState("尚未选择细分科室", "请先在科室导航页完成科室选择");
    }

    private JPanel createTop(Runnable goBack) {
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JButton back = HospitalTheme.quietButton("‹ 返回医院首页");
        back.addActionListener(event -> goBack.run());
        top.add(back, BorderLayout.WEST);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        pageTitle.setFont(HospitalTheme.uiFont(Font.BOLD, 25F));
        pageTitle.setForeground(HospitalTheme.TEXT);
        pageSubtitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        pageSubtitle.setForeground(HospitalTheme.MUTED);
        titleBox.add(pageTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(pageSubtitle);
        top.add(titleBox, BorderLayout.CENTER);

        JPanel visitTypes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        visitTypes.setOpaque(false);
        visitTypes.add(firstVisitButton);
        visitTypes.add(followUpButton);
        top.add(visitTypes, BorderLayout.EAST);
        updateVisitModeHeader();
        return top;
    }

    private void updateVisitModeHeader() {
        boolean followUp = visitType == VisitType.FOLLOW_UP;
        pageTitle.setText(followUp ? "预约普通复诊" : "预约挂号");
        pageSubtitle.setText(followUp && sourceConsultation != null
                ? "基于 " + sourceConsultation.getDepartmentName()
                        + " 的既往诊疗记录选择新排班"
                : "先找到具体科室，再选择日期与医生");
        styleVisitButton(firstVisitButton, !followUp);
        styleVisitButton(followUpButton, followUp);
    }

    private static void styleVisitButton(JButton button, boolean active) {
        button.setOpaque(true);
        button.setBackground(active ? HospitalTheme.PRIMARY_LIGHT : HospitalTheme.SURFACE);
        button.setForeground(active ? HospitalTheme.PRIMARY_DARK : HospitalTheme.MUTED);
        button.setFont(HospitalTheme.uiFont(active ? Font.BOLD : Font.PLAIN, 12F));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        active ? HospitalTheme.PRIMARY : HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 11, 6, 11)));
    }

    private JComponent createDepartmentPage() {
        JPanel page = new JPanel(new BorderLayout(18, 0));
        page.setOpaque(false);

        HospitalTheme.SurfacePanel majorArea = new HospitalTheme.SurfacePanel(
                HospitalTheme.NAVIGATION, 16);
        majorArea.setLayout(new BorderLayout(0, 14));
        majorArea.setBorder(BorderFactory.createEmptyBorder(20, 14, 18, 14));
        majorArea.setPreferredSize(new Dimension(238, 0));
        JPanel majorHeading = new JPanel();
        majorHeading.setOpaque(false);
        majorHeading.setLayout(new BoxLayout(majorHeading, BoxLayout.Y_AXIS));
        majorHeading.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        JLabel majorTitle = new JLabel("科室");
        majorTitle.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        majorTitle.setForeground(Color.WHITE);
        majorTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel searchLabel = new JLabel("搜索科室");
        searchLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        searchLabel.setForeground(HospitalTheme.NAVIGATION_MUTED);
        searchLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        departmentSearchField.setName("departmentSearchField");
        departmentSearchField.setFont(HospitalTheme.uiFont(Font.PLAIN, 13F));
        departmentSearchField.setToolTipText("输入科室名称，例如“骨关节外科”");
        departmentSearchField.setPreferredSize(new Dimension(0, 36));
        departmentSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        departmentSearchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        departmentSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.NAVIGATION_HOVER),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)));
        majorHeading.add(majorTitle);
        majorHeading.add(Box.createVerticalStrut(13));
        majorHeading.add(searchLabel);
        majorHeading.add(Box.createVerticalStrut(5));
        majorHeading.add(departmentSearchField);
        majorDepartmentPanel.setOpaque(false);
        majorDepartmentPanel.setLayout(new BoxLayout(majorDepartmentPanel, BoxLayout.Y_AXIS));
        majorDepartmentPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        JScrollPane majorScroll = HospitalResponsiveLayout.verticalScroll(
                majorDepartmentPanel);
        majorArea.add(majorHeading, BorderLayout.NORTH);
        majorArea.add(majorScroll, BorderLayout.CENTER);
        page.add(majorArea, BorderLayout.WEST);

        HospitalTheme.SurfacePanel details = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 14, HospitalTheme.BORDER);
        details.setLayout(new BorderLayout(0, 16));
        details.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        JPanel detailHeading = new JPanel();
        detailHeading.setOpaque(false);
        detailHeading.setLayout(new BoxLayout(detailHeading, BoxLayout.Y_AXIS));
        departmentDetailTitle.setFont(
                HospitalTheme.uiFont(Font.BOLD, 21F));
        departmentDetailTitle.setForeground(HospitalTheme.TEXT);
        detailHeading.add(departmentDetailTitle);
        departmentDetailPanel.setOpaque(false);
        departmentDetailPanel.setLayout(
                new BoxLayout(departmentDetailPanel, BoxLayout.Y_AXIS));
        JScrollPane detailScroll = HospitalResponsiveLayout.verticalScroll(
                departmentDetailPanel);
        details.add(detailHeading, BorderLayout.NORTH);
        details.add(detailScroll, BorderLayout.CENTER);
        page.add(details, BorderLayout.CENTER);
        return page;
    }

    private JComponent createSchedulePage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        HospitalTheme.SurfacePanel departmentBar = new HospitalTheme.SurfacePanel(
                HospitalTheme.NAVIGATION, 14);
        departmentBar.setLayout(new BorderLayout(16, 0));
        departmentBar.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 16));
        JPanel pathText = new JPanel();
        pathText.setOpaque(false);
        pathText.setLayout(new BoxLayout(pathText, BoxLayout.Y_AXIS));
        JLabel pathHint = new JLabel("当前导诊路径");
        pathHint.setFont(HospitalTheme.uiFont(Font.PLAIN, 11F));
        pathHint.setForeground(HospitalTheme.NAVIGATION_MUTED);
        selectedDepartmentLabel.setForeground(Color.WHITE);
        selectedDepartmentLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 15F));
        pathText.add(pathHint);
        pathText.add(Box.createVerticalStrut(3));
        pathText.add(selectedDepartmentLabel);
        departmentBar.add(chooseDepartmentButton, BorderLayout.WEST);
        departmentBar.add(pathText, BorderLayout.CENTER);
        controls.add(departmentBar);
        controls.add(Box.createVerticalStrut(10));

        HospitalTheme.SurfacePanel dates = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 12, HospitalTheme.BORDER);
        dates.setLayout(new BorderLayout(12, 0));
        dates.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        JLabel dateLabel = new JLabel("选择日期");
        dateLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
        dateLabel.setForeground(HospitalTheme.TEXT);
        datePanel.setOpaque(false);
        dates.add(dateLabel, BorderLayout.WEST);
        dates.add(datePanel, BorderLayout.CENTER);
        controls.add(dates);
        controls.add(Box.createVerticalStrut(10));

        HospitalTheme.SurfacePanel doctorSearch = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 12, HospitalTheme.BORDER);
        doctorSearch.setLayout(new BorderLayout(14, 0));
        doctorSearch.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        JPanel searchHeading = new JPanel();
        searchHeading.setOpaque(false);
        searchHeading.setLayout(new BoxLayout(searchHeading, BoxLayout.Y_AXIS));
        JLabel doctorLabel = new JLabel("搜索医生");
        doctorLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
        doctorLabel.setForeground(HospitalTheme.TEXT);
        JLabel hint = new JLabel("按姓名或职称筛选当天排班");
        hint.setFont(HospitalTheme.uiFont(Font.PLAIN, 11F));
        hint.setForeground(HospitalTheme.MUTED);
        searchHeading.add(doctorLabel);
        searchHeading.add(Box.createVerticalStrut(2));
        searchHeading.add(hint);
        doctorSearchField.setPreferredSize(new Dimension(320, 38));
        doctorSearchField.setName("doctorSearchField");
        doctorSearchField.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        doctorSearchField.setForeground(HospitalTheme.TEXT);
        doctorSearchField.setBackground(HospitalTheme.BACKGROUND);
        doctorSearchField.setCaretColor(HospitalTheme.PRIMARY);
        doctorSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        doctorSearchField.setToolTipText("输入医生姓名或职称，例如“林医生”或“副主任医师”");
        doctorSearch.add(searchHeading, BorderLayout.WEST);
        doctorSearch.add(doctorSearchField, BorderLayout.CENTER);
        controls.add(doctorSearch);
        page.add(controls, BorderLayout.NORTH);

        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setOpaque(false);
        JScrollPane scroll = HospitalResponsiveLayout.verticalScroll(resultPanel);
        JPanel schedules = new JPanel(new BorderLayout(0, 8));
        schedules.setOpaque(false);
        JPanel scheduleHeading = new JPanel(new BorderLayout(12, 0));
        scheduleHeading.setOpaque(false);
        JLabel scheduleTitle = new JLabel("当日可预约号源");
        scheduleTitle.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        scheduleTitle.setForeground(HospitalTheme.TEXT);
        scheduleSummaryLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        scheduleSummaryLabel.setForeground(HospitalTheme.MUTED);
        scheduleHeading.add(scheduleTitle, BorderLayout.WEST);
        scheduleHeading.add(scheduleSummaryLabel, BorderLayout.EAST);
        schedules.add(scheduleHeading, BorderLayout.NORTH);
        schedules.add(scroll, BorderLayout.CENTER);
        page.add(schedules, BorderLayout.CENTER);
        return page;
    }

    private JComponent createStatusBar() {
        JPanel status = new JPanel(new BorderLayout(10, 0));
        status.setOpaque(false);
        statusLabel.setForeground(HospitalTheme.MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
        status.add(statusLabel, BorderLayout.CENTER);
        status.add(retryButton, BorderLayout.EAST);
        return status;
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
            button.setUI(new BasicButtonUI());
            button.setFocusPainted(true);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setSelected(date.equals(selectedDate));
            button.addItemListener(event -> styleDateButton(button));
            button.addActionListener(event -> {
                selectedDate = date;
                renderSlots();
            });
            styleDateButton(button);
            group.add(button);
            dateButtons.add(button);
            datePanel.add(button);
        }
        updateDateButtonState();
        datePanel.revalidate();
        datePanel.repaint();
    }

    private static void styleMajorButton(JToggleButton button) {
        if (button.isSelected()) {
            button.setBackground(Color.WHITE);
            button.setForeground(HospitalTheme.NAVIGATION);
        } else {
            button.setBackground(HospitalTheme.NAVIGATION);
            button.setForeground(Color.WHITE);
        }
    }

    private static void styleDateButton(JToggleButton button) {
        if (button.isSelected()) {
            button.setBackground(HospitalTheme.PRIMARY);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        } else {
            button.setBackground(HospitalTheme.BACKGROUND);
            button.setForeground(HospitalTheme.TEXT);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(HospitalTheme.BORDER),
                    BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        }
    }

    private void loadDepartments() {
        if (busy) {
            return;
        }
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
                        showRetryState(
                                "服务器返回了无法识别的科室数据。",
                                HospitalTheme.WARNING);
                        return;
                    }
                    rebuildDepartmentNavigation(data.getDepartments());
                    departmentsLoaded = true;
                    setBusy(false, "科室已加载，请选择或搜索科室。");
                    String requestedDepartmentId = pendingDepartmentId;
                    pendingDepartmentId = null;
                    if (requestedDepartmentId == null) {
                        showDepartmentPage();
                    } else {
                        SwingUtilities.invokeLater(
                                () -> openRequestedDepartment(requestedDepartmentId));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showRetryState("科室加载已中断。", HospitalTheme.WARNING);
                } catch (ExecutionException exception) {
                    showNetworkError();
                } finally {
                    if (busy) {
                        setBusy(false, statusLabel.getText());
                    }
                }
            }
        }.execute();
    }

    private void rebuildDepartmentNavigation(List<DepartmentView> departments) {
        departmentsById.clear();
        childrenByParent.clear();
        departments.stream()
                .sorted(Comparator.comparing(DepartmentView::getDepartmentName))
                .forEach(department -> {
                    departmentsById.put(department.getDepartmentId(), department);
                    childrenByParent.computeIfAbsent(
                            department.getParentDepartmentId(), ignored -> new ArrayList<>())
                            .add(department);
                });

        departmentButtons.clear();
        majorDepartmentPanel.removeAll();
        ButtonGroup majorGroup = new ButtonGroup();
        for (DepartmentView major : childrenOf(null)) {
            JToggleButton button = new JToggleButton(major.getDepartmentName());
            button.setUI(new BasicButtonUI());
            button.setFocusPainted(true);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setBorder(BorderFactory.createEmptyBorder(11, 9, 11, 9));
            button.addItemListener(event -> styleMajorButton(button));
            button.addActionListener(event -> {
                selectedMajorDepartment = major;
                if (!departmentSearchField.getText().isBlank()) {
                    departmentSearchField.setText("");
                }
                if (childrenOf(major.getDepartmentId()).isEmpty() && major.isBookable()) {
                    openSchedulePage(major);
                } else {
                    renderDepartmentChildren(major);
                }
            });
            majorGroup.add(button);
            departmentButtons.add(button);
            majorDepartmentPanel.add(button);
            majorDepartmentPanel.add(Box.createVerticalStrut(5));
            styleMajorButton(button);
        }
        majorDepartmentPanel.revalidate();
        majorDepartmentPanel.repaint();
        renderDepartmentPrompt(
                "请选择科室",
                "点击左侧科室查看下级分类，或在搜索框中查找具体科室。");
    }

    private void renderDepartmentChildren(DepartmentView major) {
        departmentButtons.removeIf(button -> !(button instanceof JToggleButton));
        departmentDetailTitle.setText(major.getDepartmentName());
        departmentDetailPanel.removeAll();
        List<DepartmentView> children = childrenOf(major.getDepartmentId());
        if (children.isEmpty()) {
            renderDepartmentPrompt("暂无下级科室", "请选择其他科室。");
            return;
        }
        for (DepartmentView child : children) {
            departmentDetailPanel.add(createDepartmentEntry(child, 0));
            departmentDetailPanel.add(Box.createVerticalStrut(8));
        }
        departmentDetailPanel.revalidate();
        departmentDetailPanel.repaint();
        showState("已选择“" + major.getDepartmentName() + "”，请继续选择具体科室。",
                HospitalTheme.MUTED);
    }

    private JComponent createDepartmentEntry(DepartmentView department, int depth) {
        List<DepartmentView> children = childrenOf(department.getDepartmentId());
        if (children.isEmpty()) {
            return createBookableDepartmentCard(department, "可预约科室");
        }

        JPanel entry = new JPanel(new BorderLayout(0, 8));
        entry.setOpaque(false);
        entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 2, 12, 2)));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        entry.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton action = new JButton(department.getDepartmentName());
        action.setUI(new BasicButtonUI());
        action.setOpaque(false);
        action.setContentAreaFilled(false);
        action.setBorderPainted(false);
        action.setFocusPainted(true);
        action.setHorizontalAlignment(SwingConstants.LEFT);
        action.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        action.setForeground(HospitalTheme.TEXT);
        action.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        action.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        action.setToolTipText("展开下一级科室");
        departmentButtons.add(action);

        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JLabel cue = new JLabel("展开 " + children.size() + " 个科室  ＋");
        cue.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        cue.setForeground(HospitalTheme.MUTED);
        cue.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 6));
        heading.add(action, BorderLayout.CENTER);
        heading.add(cue, BorderLayout.EAST);
        entry.add(heading, BorderLayout.NORTH);

        HospitalTheme.SurfacePanel nested = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 10);
        nested.setLayout(new BoxLayout(nested, BoxLayout.Y_AXIS));
        nested.setBorder(BorderFactory.createEmptyBorder(
                8, 16 + depth * 8, 8, 10));
        for (DepartmentView child : children) {
            nested.add(createDepartmentEntry(child, depth + 1));
            nested.add(Box.createVerticalStrut(6));
        }
        int expandedHeight = 66 + children.size() * 76;
        nested.setMaximumSize(new Dimension(Integer.MAX_VALUE, expandedHeight - 58));
        nested.setVisible(false);
        entry.add(nested, BorderLayout.CENTER);
        action.addActionListener(event -> {
            nested.setVisible(!nested.isVisible());
            entry.setMaximumSize(new Dimension(
                    Integer.MAX_VALUE, nested.isVisible() ? expandedHeight : 58));
            cue.setText(nested.isVisible()
                    ? "收起科室  −"
                    : "展开 " + children.size() + " 个科室  ＋");
            departmentDetailPanel.revalidate();
            departmentDetailPanel.repaint();
        });
        return entry;
    }

    private JComponent createBookableDepartmentCard(
            DepartmentView department,
            String detail) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 10, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        Dimension cardSize = new Dimension(360, 68);
        card.setMinimumSize(cardSize);
        card.setPreferredSize(cardSize);
        card.setMaximumSize(cardSize);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("查看“" + department.getDepartmentName() + "”号源");

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JButton action = new JButton(department.getDepartmentName());
        action.setUI(new BasicButtonUI());
        action.setOpaque(false);
        action.setContentAreaFilled(false);
        action.setBorderPainted(false);
        action.setFocusPainted(true);
        action.setHorizontalAlignment(SwingConstants.LEFT);
        action.setFont(HospitalTheme.uiFont(Font.BOLD, 15F));
        action.setForeground(HospitalTheme.PRIMARY_DARK);
        action.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        action.setBorder(BorderFactory.createEmptyBorder());
        action.setToolTipText("进入该科室的号源页");
        action.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel detailLabel = new JLabel(detail);
        detailLabel.setFont(HospitalTheme.uiFont(Font.PLAIN, 11F));
        detailLabel.setForeground(HospitalTheme.MUTED);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(action);
        text.add(Box.createVerticalStrut(3));
        text.add(detailLabel);

        JButton cue = new JButton("查看号源 →");
        cue.setUI(new BasicButtonUI());
        cue.setOpaque(false);
        cue.setContentAreaFilled(false);
        cue.setBorderPainted(false);
        cue.setFocusPainted(true);
        cue.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        cue.setForeground(HospitalTheme.PRIMARY);
        cue.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cue.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        card.add(text, BorderLayout.CENTER);
        card.add(cue, BorderLayout.EAST);
        departmentButtons.add(action);
        departmentButtons.add(cue);
        Runnable openSchedule = () -> {
            if (department.isBookable()) {
                openSchedulePage(department);
            }
        };
        action.addActionListener(event -> openSchedule.run());
        cue.addActionListener(event -> openSchedule.run());
        MouseAdapter cardNavigation = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    openSchedule.run();
                }
            }
        };
        card.addMouseListener(cardNavigation);
        text.addMouseListener(cardNavigation);
        detailLabel.addMouseListener(cardNavigation);
        return card;
    }

    private void updateDepartmentSearch() {
        if (!departmentsLoaded) {
            return;
        }
        String query = departmentSearchField.getText().strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            if (selectedMajorDepartment == null) {
                renderDepartmentPrompt(
                        "请选择科室",
                        "点击左侧科室查看下级分类，或在搜索框中查找具体科室。");
            } else {
                renderDepartmentChildren(selectedMajorDepartment);
            }
            return;
        }

        departmentButtons.removeIf(button -> !(button instanceof JToggleButton));
        departmentDetailTitle.setText("科室搜索结果");
        departmentDetailPanel.removeAll();
        List<DepartmentView> matches = departmentsById.values().stream()
                .filter(DepartmentView::isBookable)
                .filter(department -> departmentPath(department)
                        .toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(DepartmentView::getDepartmentName))
                .toList();
        if (matches.isEmpty()) {
            JLabel empty = new JLabel("没有找到符合“" + departmentSearchField.getText().strip()
                    + "”的可预约科室");
            empty.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
            empty.setForeground(HospitalTheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(16, 2, 0, 2));
            departmentDetailPanel.add(empty);
        } else {
            for (DepartmentView match : matches) {
                departmentDetailPanel.add(createBookableDepartmentCard(
                        match, departmentPath(match)));
                departmentDetailPanel.add(Box.createVerticalStrut(8));
            }
        }
        departmentDetailPanel.revalidate();
        departmentDetailPanel.repaint();
        showState("找到 " + matches.size() + " 个可预约科室。", HospitalTheme.MUTED);
    }

    private void renderDepartmentPrompt(String title, String detail) {
        departmentButtons.removeIf(button -> !(button instanceof JToggleButton));
        departmentDetailTitle.setText(title);
        departmentDetailPanel.removeAll();
        JLabel message = new JLabel(
                "<html><body style='width:420px'>" + detail + "</body></html>");
        message.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        message.setForeground(HospitalTheme.MUTED);
        message.setBorder(BorderFactory.createEmptyBorder(18, 4, 0, 4));
        departmentDetailPanel.add(message);
        departmentDetailPanel.revalidate();
        departmentDetailPanel.repaint();
    }

    private List<DepartmentView> childrenOf(String parentDepartmentId) {
        return childrenByParent.getOrDefault(parentDepartmentId, List.of());
    }

    private void openSchedulePage(DepartmentView department) {
        if (!department.isBookable() || busy) {
            return;
        }
        selectedDepartment = department;
        loadedSlots = List.of();
        doctorSearchField.setText("");
        selectedDepartmentLabel.setText(departmentPath(department));
        scheduleSummaryLabel.setText("正在加载排班");
        renderEmptyState("正在加载排班", "请稍候");
        pageCards.show(pages, SCHEDULE_PAGE);
        searchSlots();
    }

    private void openRequestedDepartment(String departmentId) {
        DepartmentView department = departmentsById.get(departmentId);
        if (department == null || !department.isBookable()) {
            showDepartmentPage();
            showState("推荐科室当前不可挂号，请重新选择科室。", HospitalTheme.WARNING);
            return;
        }
        openSchedulePage(department);
    }

    private String departmentPath(DepartmentView department) {
        List<String> names = new ArrayList<>();
        DepartmentView current = department;
        while (current != null) {
            names.add(current.getDepartmentName());
            current = departmentsById.get(current.getParentDepartmentId());
        }
        Collections.reverse(names);
        return String.join("  ›  ", names);
    }

    private void showDepartmentPage() {
        if (visitType == VisitType.FOLLOW_UP) {
            pageCards.show(pages, SCHEDULE_PAGE);
            return;
        }
        pageCards.show(pages, DEPARTMENT_PAGE);
        if (departmentsLoaded && !busy) {
            updateDepartmentSearch();
            if (departmentSearchField.getText().isBlank()) {
                showState("请选择左侧科室，或搜索具体科室。", HospitalTheme.MUTED);
            }
        }
    }

    private void searchSlots() {
        DepartmentView department = selectedDepartment;
        boolean followUp = visitType == VisitType.FOLLOW_UP && sourceConsultation != null;
        if ((!followUp && (department == null || !department.isBookable())) || busy) {
            return;
        }
        String departmentName = followUp
                ? sourceConsultation.getDepartmentName()
                : department.getDepartmentName();
        setBusy(true, "正在查询“" + departmentName + "”的排班……");
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                SearchSlotsRequest request = followUp
                        ? SearchSlotsRequest.followUp(sourceConsultation.getConsultationId())
                        : SearchSlotsRequest.firstVisit(department.getDepartmentId(), null);
                return context.send(
                        HospitalActions.SEARCH_SLOTS,
                        request);
            }

            @Override
            protected void done() {
                boolean searchSucceeded = false;
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        loadedSlots = List.of();
                        renderEmptyState("排班加载失败", "请使用下方的重新加载按钮重试");
                        showResponseError(response);
                        return;
                    }
                    SlotListResponse data = response.getData()
                            instanceof SlotListResponse slots ? slots : null;
                    if (data == null) {
                        loadedSlots = List.of();
                        renderEmptyState("排班数据无法识别", "请重新加载或联系系统管理员");
                        showRetryState(
                                "服务器返回了无法识别的排班数据。",
                                HospitalTheme.WARNING);
                        return;
                    }
                    loadedSlots = data.getSlots();
                    selectEarliestAvailableDate();
                    searchSucceeded = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    loadedSlots = List.of();
                    renderEmptyState("排班查询已中断", "可以重新加载当前细分科室");
                    showRetryState("排班查询已中断。", HospitalTheme.WARNING);
                } catch (ExecutionException exception) {
                    loadedSlots = List.of();
                    renderEmptyState("无法连接服务器", "请确认服务器启动后重新加载");
                    showNetworkError();
                } finally {
                    setBusy(false, statusLabel.getText());
                    if (searchSucceeded) {
                        updateDateButtonState();
                        renderSlots();
                    }
                }
            }
        }.execute();
    }

    private void selectEarliestAvailableDate() {
        if (loadedSlots.isEmpty()) {
            syncDateButtonSelection();
            return;
        }
        boolean selectedDateHasBookableSlot = loadedSlots.stream()
                .filter(slot -> slot.getStartTime().toLocalDate().equals(selectedDate))
                .anyMatch(slot -> slot.getAvailability() == SlotAvailability.AVAILABLE
                        && !slot.isBookedByCurrentUser());
        if (!selectedDateHasBookableSlot) {
            selectedDate = loadedSlots.stream()
                    .filter(slot -> slot.getAvailability() == SlotAvailability.AVAILABLE)
                    .filter(slot -> !slot.isBookedByCurrentUser())
                    .map(slot -> slot.getStartTime().toLocalDate())
                    .findFirst()
                    .orElse(loadedSlots.getFirst().getStartTime().toLocalDate());
        }
        syncDateButtonSelection();
    }

    private void syncDateButtonSelection() {
        for (int index = 0; index < dateButtons.size(); index++) {
            LocalDate buttonDate = LocalDate.now().plusDays(index);
            dateButtons.get(index).setSelected(buttonDate.equals(selectedDate));
        }
    }

    private void updateDateButtonState() {
        boolean enabled = !busy && hasScheduleContext();
        dateButtons.forEach(button -> button.setEnabled(enabled));
    }

    private boolean hasScheduleContext() {
        return selectedDepartment != null
                || visitType == VisitType.FOLLOW_UP && sourceConsultation != null;
    }

    private void renderSlots() {
        if (resultPanel == null) {
            return;
        }
        resultPanel.removeAll();
        if (!hasScheduleContext()) {
            scheduleSummaryLabel.setText("尚未选择科室");
            renderEmptyState("尚未选择细分科室", "请返回科室导航页完成选择");
            return;
        }

        String doctorQuery = doctorSearchField.getText().strip().toLowerCase(Locale.ROOT);
        List<SlotView> visible = loadedSlots.stream()
                .filter(slot -> slot.getStartTime().toLocalDate().equals(selectedDate))
                .filter(slot -> doctorQuery.isEmpty()
                        || slot.getDoctorName().toLowerCase(Locale.ROOT).contains(doctorQuery)
                        || slot.getDoctorTitle().toLowerCase(Locale.ROOT).contains(doctorQuery))
                .toList();

        if (visible.isEmpty()) {
            if (loadedSlots.isEmpty()) {
                scheduleSummaryLabel.setText("未来 7 天暂无排班");
                renderEmptyState(
                        "未来 7 天暂无排班",
                        visitType == VisitType.FOLLOW_UP
                                ? "原科室暂时没有可预约排班，请稍后再试"
                                : "请选择其他细分科室或稍后再试");
                showState("该科室未来 7 天暂无开放排班。", HospitalTheme.MUTED);
            } else if (!doctorQuery.isEmpty()) {
                scheduleSummaryLabel.setText("当前搜索结果 0 个");
                renderEmptyState("没有匹配的医生排班", "请修改医生姓名或职称后再试");
                showState("当天没有符合“" + doctorSearchField.getText().strip()
                        + "”的医生排班。", HospitalTheme.MUTED);
            } else {
                scheduleSummaryLabel.setText(MONTH_DAY.format(selectedDate) + " · 暂无排班");
                renderEmptyState("当天没有排班", "请切换上方日期");
                showState("所选日期暂无排班，可切换其他日期。", HospitalTheme.MUTED);
            }
            return;
        }

        for (SlotView slot : visible) {
            resultPanel.add(createSlotCard(slot));
            resultPanel.add(Box.createVerticalStrut(10));
        }
        scheduleSummaryLabel.setText(MONTH_DAY.format(selectedDate)
                + " · " + visible.size() + " 个排班");
        showState("共显示 " + visible.size() + " 个当日排班；数据会自动更新。",
                HospitalTheme.MUTED);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JComponent createSlotCard(SlotView slot) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 12, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 124));

        HospitalTheme.SurfacePanel time = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 10);
        time.setLayout(new BoxLayout(time, BoxLayout.Y_AXIS));
        time.setBorder(BorderFactory.createEmptyBorder(10, 13, 9, 13));
        time.setPreferredSize(new Dimension(132, 92));
        JLabel timeHint = new JLabel("就诊时段");
        timeHint.setFont(HospitalTheme.uiFont(Font.PLAIN, 11F));
        timeHint.setForeground(HospitalTheme.MUTED);
        JLabel startTime = new JLabel(TIME.format(slot.getStartTime()));
        startTime.setFont(HospitalTheme.dataFont(Font.BOLD, 24F));
        startTime.setForeground(HospitalTheme.PRIMARY_DARK);
        JLabel endTime = new JLabel("至 " + TIME.format(slot.getEndTime()));
        endTime.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        endTime.setForeground(HospitalTheme.MUTED);
        time.add(timeHint);
        time.add(Box.createVerticalStrut(2));
        time.add(startTime);
        time.add(Box.createVerticalStrut(1));
        time.add(endTime);
        card.add(time, BorderLayout.WEST);

        JPanel doctor = new JPanel();
        doctor.setOpaque(false);
        doctor.setLayout(new BoxLayout(doctor, BoxLayout.Y_AXIS));
        boolean originalDoctor = visitType == VisitType.FOLLOW_UP
                && sourceConsultation != null
                && slot.getDoctorId().equals(sourceConsultation.getDoctorId());
        JLabel doctorName = new JLabel(slot.getDoctorName()
                + (originalDoctor ? "  ·  原接诊医生" : ""));
        doctorName.setFont(HospitalTheme.uiFont(Font.BOLD, 18F));
        doctorName.setForeground(HospitalTheme.TEXT);
        JLabel doctorTitle = new JLabel(slot.getDoctorTitle());
        doctorTitle.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        doctorTitle.setForeground(HospitalTheme.PRIMARY);
        JLabel department = new JLabel("科室 · " + slot.getDepartmentName());
        department.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        department.setForeground(HospitalTheme.MUTED);
        doctor.add(doctorName);
        doctor.add(Box.createVerticalStrut(3));
        doctor.add(doctorTitle);
        doctor.add(Box.createVerticalStrut(8));
        doctor.add(department);
        card.add(doctor, BorderLayout.CENTER);

        boolean booked = slot.isBookedByCurrentUser();
        boolean available = slot.getAvailability() == SlotAvailability.AVAILABLE && !booked;
        JPanel action = new JPanel();
        action.setOpaque(false);
        action.setLayout(new BoxLayout(action, BoxLayout.X_AXIS));
        action.setPreferredSize(new Dimension(258, 92));

        JPanel bookingInfo = new JPanel();
        bookingInfo.setOpaque(false);
        bookingInfo.setLayout(new BoxLayout(bookingInfo, BoxLayout.Y_AXIS));
        String stateText = booked ? "已预约" : available ? "可预约" : "已满号";
        JLabel state = new JLabel(stateText, SwingConstants.CENTER);
        state.setOpaque(true);
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 11F));
        state.setForeground(booked || available ? HospitalTheme.SUCCESS : HospitalTheme.WARNING);
        state.setBackground(booked || available
                ? HospitalTheme.SUCCESS_LIGHT : HospitalTheme.WARNING_LIGHT);
        state.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        state.setAlignmentX(CENTER_ALIGNMENT);
        JLabel remaining = new JLabel("余号 " + slot.getRemaining(), SwingConstants.CENTER);
        remaining.setFont(HospitalTheme.uiFont(Font.BOLD, 17F));
        remaining.setForeground(booked || available
                ? HospitalTheme.SUCCESS : HospitalTheme.WARNING);
        remaining.setAlignmentX(CENTER_ALIGNMENT);
        JLabel price = new JLabel("挂号费  ¥" + String.format(Locale.ROOT, "%.2f",
                slot.getPriceCents() / 100.0), SwingConstants.CENTER);
        price.setFont(HospitalTheme.uiFont(Font.PLAIN, 11F));
        price.setForeground(HospitalTheme.MUTED);
        price.setAlignmentX(CENTER_ALIGNMENT);
        JButton reserve;
        if (booked) {
            reserve = HospitalTheme.quietButton("已预约");
            reserve.setEnabled(false);
            reserve.setToolTipText("你已经预约了该排班");
        } else if (available) {
            reserve = HospitalTheme.primaryButton("立即预约");
            reserve.setEnabled(!busy);
            reserve.setToolTipText("确认挂号并完成模拟支付");
        } else {
            reserve = HospitalTheme.quietButton("不可预约");
            reserve.setEnabled(false);
            reserve.setToolTipText("该排班已满");
        }
        Dimension buttonSize = new Dimension(112, 40);
        reserve.setMinimumSize(buttonSize);
        reserve.setPreferredSize(buttonSize);
        reserve.setMaximumSize(buttonSize);
        reserve.setAlignmentY(CENTER_ALIGNMENT);
        if (available) {
            reserve.addActionListener(event -> confirmBooking(slot));
        }
        bookingInfo.add(state);
        bookingInfo.add(Box.createVerticalStrut(5));
        bookingInfo.add(remaining);
        bookingInfo.add(Box.createVerticalStrut(2));
        bookingInfo.add(price);
        bookingInfo.setAlignmentY(CENTER_ALIGNMENT);
        action.add(bookingInfo);
        action.add(Box.createHorizontalStrut(14));
        action.add(reserve);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private void confirmBooking(SlotView slot) {
        if (busy) {
            return;
        }
        String amount = String.format(Locale.ROOT, "%.2f", slot.getPriceCents() / 100.0);
        String message = (visitType == VisitType.FOLLOW_UP ? "类型：普通复诊\n" : "")
                + "科室：" + slot.getDepartmentName()
                + "\n医生：" + slot.getDoctorName() + " " + slot.getDoctorTitle()
                + "\n时间：" + slot.getStartTime().toLocalDate() + " "
                + TIME.format(slot.getStartTime()) + "–" + TIME.format(slot.getEndTime())
                + "\n挂号费：¥" + amount
                + "\n\n本项目使用模拟支付，确认后将直接标记为已支付。";
        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "确认预约",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            bookSlot(slot);
        }
    }

    private void bookSlot(SlotView slot) {
        setBusy(true, "正在预约并完成模拟支付……");
        renderSlots();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                BookAppointmentRequest request = visitType == VisitType.FOLLOW_UP
                        ? BookAppointmentRequest.followUp(
                                slot.getScheduleId(), sourceConsultation.getAppointmentId())
                        : BookAppointmentRequest.firstVisit(slot.getScheduleId());
                return context.send(
                        HospitalActions.BOOK_APPOINTMENT,
                        request);
            }

            @Override
            protected void done() {
                boolean refreshAfterBooking = false;
                String terminalMessage = null;
                Color terminalColor = HospitalTheme.WARNING;
                try {
                    Response response = get();
                    if (!response.isSuccess()) {
                        terminalMessage = bookingErrorMessage(response);
                        refreshAfterBooking = shouldReloadAfterBookingFailure(response);
                        JOptionPane.showMessageDialog(
                                SlotSearchPanel.this,
                                terminalMessage,
                                "预约未完成",
                                JOptionPane.WARNING_MESSAGE);
                    } else {
                        AppointmentBookingView booking = response.getData()
                                instanceof AppointmentBookingView view ? view : null;
                        if (booking == null) {
                            terminalMessage = "服务器返回了无法识别的预约数据。";
                        } else {
                            String amount = String.format(
                                    Locale.ROOT, "%.2f", booking.getAmountCents() / 100.0);
                            JOptionPane.showMessageDialog(
                                    SlotSearchPanel.this,
                                    "预约成功！\n候诊序号：" + booking.getQueueNumber()
                                            + " 号\n医生：" + booking.getDoctorName()
                                            + "\n挂号费：¥" + amount + "（已模拟支付）",
                                    "预约成功",
                                    JOptionPane.INFORMATION_MESSAGE);
                            terminalMessage = "预约成功，候诊序号为 "
                                    + booking.getQueueNumber() + " 号。";
                            terminalColor = HospitalTheme.SUCCESS;
                            refreshAfterBooking = true;
                        }
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    terminalMessage = "预约操作已中断。";
                } catch (ExecutionException exception) {
                    terminalMessage = "无法连接服务器，请确认服务器已经启动后重试。";
                } finally {
                    setBusy(false, "预约操作已结束。");
                    renderSlots();
                }
                if (refreshAfterBooking) {
                    searchSlots();
                } else if (terminalMessage != null) {
                    showState(terminalMessage, terminalColor);
                }
            }
        }.execute();
    }

    private static String bookingErrorMessage(Response response) {
        return switch (response.getCode()) {
            case ErrorCodes.AUTH_REQUIRED -> "登录状态已失效，请返回并重新登录。";
            case ErrorCodes.HOSPITAL_DUPLICATE_APPOINTMENT -> "你已经预约过这个排班。";
            case ErrorCodes.HOSPITAL_SELF_BOOKING_FORBIDDEN ->
                    "医生不能预约自己的排班，请选择同科室其他医生。";
            case ErrorCodes.HOSPITAL_SLOT_FULL -> "该排班刚刚约满，号源将自动更新。";
            case ErrorCodes.HOSPITAL_SCHEDULE_CLOSED -> "该排班已关闭，请选择其他排班。";
            case ErrorCodes.HOSPITAL_SCHEDULE_STARTED -> "该排班已经开始，不能再预约。";
            case ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID ->
                    "来源诊疗记录的状态已经变化，请返回健康档案后重新选择。";
            default -> "预约失败：" + response.getMessage();
        };
    }

    private static boolean shouldReloadAfterBookingFailure(Response response) {
        return ErrorCodes.HOSPITAL_SLOT_FULL.equals(response.getCode())
                || ErrorCodes.HOSPITAL_SCHEDULE_CLOSED.equals(response.getCode())
                || ErrorCodes.HOSPITAL_SCHEDULE_STARTED.equals(response.getCode());
    }

    private void renderEmptyState(String title, String detail) {
        resultPanel.removeAll();
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BorderLayout());
        empty.setBorder(BorderFactory.createEmptyBorder(50, 20, 50, 20));
        JLabel text = new JLabel(
                "<html><center><b>" + title + "</b><br><br>"
                        + detail + "</center></html>",
                SwingConstants.CENTER);
        text.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        text.setForeground(HospitalTheme.MUTED);
        empty.add(text);
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        resultPanel.add(empty);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private void showResponseError(Response response) {
        if (ErrorCodes.AUTH_REQUIRED.equals(response.getCode())) {
            showRetryState("登录状态已失效，请返回并重新登录。", HospitalTheme.WARNING);
        } else {
            showRetryState("查询失败：" + response.getMessage(), HospitalTheme.WARNING);
        }
    }

    private void showNetworkError() {
        showRetryState(
                "无法连接服务器，请确认服务器已经启动后重试。",
                HospitalTheme.WARNING);
    }

    private void showState(String message, Color color) {
        retryButton.setVisible(false);
        statusLabel.setForeground(color);
        statusLabel.setText(message);
    }

    private void showRetryState(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(message);
        retryButton.setVisible(true);
    }

    private void retryCurrentLoad() {
        if (busy) {
            return;
        }
        if (!departmentsLoaded) {
            loadDepartments();
        } else if (hasScheduleContext()) {
            pageCards.show(pages, SCHEDULE_PAGE);
            searchSlots();
        } else {
            loadDepartments();
        }
    }

    private void setBusy(boolean working, String message) {
        busy = working;
        departmentButtons.forEach(button -> button.setEnabled(!working));
        departmentSearchField.setEnabled(!working);
        chooseDepartmentButton.setEnabled(!working);
        doctorSearchField.setEnabled(!working && hasScheduleContext());
        updateDateButtonState();
        if (working) {
            showState(message, HospitalTheme.MUTED);
        } else {
            statusLabel.setText(message);
        }
    }
}
