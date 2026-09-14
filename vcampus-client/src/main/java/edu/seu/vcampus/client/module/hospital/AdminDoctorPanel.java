package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.DoctorApplicationView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.SubmitDoctorDeactivationRequest;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Page-oriented doctor onboarding, deactivation and application history. */
final class AdminDoctorPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ClientContext context;
    private final Runnable back;
    private final JPanel body = verticalList();
    private final JPanel history = verticalList();
    private final JLabel status = new JLabel("正在读取医生资料……");
    private final JButton retry = HospitalTheme.quietButton("重新加载");

    private final JComboBox<ApplicationKind> applicationKind =
            new JComboBox<>(ApplicationKind.values());
    private final JTextField username = new JTextField();
    private final JTextField displayName = new JTextField();
    private final JComboBox<DepartmentOption> department = new JComboBox<>();
    private final JTextField doctorTitle = new JTextField();
    private final JButton submitOnboarding = HospitalTheme.primaryButton("提交新增申请");
    private final JLabel onboardingStatus = new JLabel(" ");

    private final JComboBox<DoctorOption> doctor = new JComboBox<>();
    private final JTextArea deactivationReason = new JTextArea(4, 20);
    private final JButton submitDeactivation = HospitalTheme.primaryButton("提交停用申请");
    private final JLabel deactivationStatus = new JLabel(" ");

    private boolean busy;

    AdminDoctorPanel(ClientContext context, Runnable back) {
        this(context, back, () -> { });
    }

    AdminDoctorPanel(ClientContext context, Runnable back, Runnable openDirectory) {
        this.context = context;
        this.back = back;
        setLayout(new BorderLayout(0, 16));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(HospitalResponsiveLayout.constrainWidth(header()), BorderLayout.NORTH);
        add(HospitalResponsiveLayout.verticalScroll(body), BorderLayout.CENTER);
        configureInputs();
        renderSkeleton(openDirectory);
    }

    void activate() {
        load();
    }

    private JPanel header() {
        JPanel header = HospitalPageHeader.create(
                "医生申请管理",
                "新增或停用医生均由医院管理员发起，由总管理员审核",
                "管理首页",
                back,
                null);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footer.setOpaque(false);
        retry.setVisible(false);
        retry.addActionListener(event -> load());
        footer.add(status);
        footer.add(retry);
        header.add(footer, BorderLayout.SOUTH);
        return header;
    }

    private void renderSkeleton(Runnable openDirectory) {
        body.removeAll();
        body.add(directoryEntry(openDirectory));
        body.add(Box.createVerticalStrut(14));
        JPanel forms = HospitalResponsiveLayout.grid(2, 380, 18, 18);
        forms.setName("adminDoctorResponsiveForms");
        forms.setOpaque(false);
        forms.add(onboardingForm());
        forms.add(deactivationForm());
        body.add(forms);
        body.add(Box.createVerticalStrut(18));
        body.add(historySection());
    }

    private JPanel directoryEntry(Runnable openDirectory) {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel(
                HospitalTheme.SUCCESS_LIGHT, 12, HospitalTheme.BORDER);
        panel.setLayout(new BorderLayout(18, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        JPanel copy = verticalList();
        copy.add(sectionTitle("医生名单"));
        copy.add(Box.createVerticalStrut(4));
        JButton open = HospitalTheme.primaryButton("查看医生名单");
        open.setName("openAdminDoctorDirectoryButton");
        open.addActionListener(event -> openDirectory.run());
        panel.add(copy, BorderLayout.CENTER);
        panel.add(open, BorderLayout.EAST);
        return panel;
    }

    private JPanel onboardingForm() {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        panel.add(sectionTitle("新增医生"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(note("可关联已有校园账号，也可为外来医生申请新账号。"));
        panel.add(Box.createVerticalStrut(15));
        panel.add(field("申请类型", applicationKind));
        panel.add(Box.createVerticalStrut(10));
        username.setName("doctorApplicationUsername");
        panel.add(field("已有一卡通号", username));
        panel.add(Box.createVerticalStrut(10));
        displayName.setName("doctorApplicationDisplayName");
        panel.add(field("外来医生姓名", displayName));
        panel.add(Box.createVerticalStrut(10));
        department.setName("doctorApplicationDepartment");
        panel.add(field("所属科室", department));
        panel.add(Box.createVerticalStrut(10));
        doctorTitle.setName("doctorApplicationTitle");
        panel.add(field("职称", doctorTitle));
        panel.add(Box.createVerticalStrut(12));
        onboardingStatus.setForeground(HospitalTheme.MUTED);
        panel.add(onboardingStatus);
        panel.add(Box.createVerticalStrut(8));
        submitOnboarding.setName("submitDoctorApplicationButton");
        submitOnboarding.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(submitOnboarding);
        return panel;
    }

    private JPanel deactivationForm() {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel(
                HospitalTheme.WARNING_LIGHT, 14, new Color(232, 204, 169));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        panel.add(sectionTitle("停用医生"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(note("停用后不再允许进入医生模式或发布新排班，既往诊疗记录会完整保留。"));
        panel.add(Box.createVerticalStrut(15));
        doctor.setName("doctorDeactivationTarget");
        panel.add(field("在岗医生", doctor));
        panel.add(Box.createVerticalStrut(10));
        deactivationReason.setName("doctorDeactivationReason");
        deactivationReason.setLineWrap(true);
        deactivationReason.setWrapStyleWord(true);
        deactivationReason.setFont(HospitalTheme.uiFont(Font.PLAIN, 14F));
        JScrollPane reasonScroll = new JScrollPane(deactivationReason);
        reasonScroll.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
        panel.add(field("停用原因", reasonScroll));
        panel.add(Box.createVerticalStrut(8));
        panel.add(note("若该医生还有未开始的已发布排班或待接诊预约，审批会被阻止。"));
        panel.add(Box.createVerticalStrut(12));
        deactivationStatus.setForeground(HospitalTheme.MUTED);
        panel.add(deactivationStatus);
        panel.add(Box.createVerticalStrut(8));
        submitDeactivation.setName("submitDoctorDeactivationButton");
        submitDeactivation.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(submitDeactivation);
        return panel;
    }

    private JPanel historySection() {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel();
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(sectionTitle("申请记录"), BorderLayout.WEST);
        JLabel hint = new JLabel("最新申请优先");
        hint.setForeground(HospitalTheme.MUTED);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(history, BorderLayout.CENTER);
        return panel;
    }

    private void configureInputs() {
        applicationKind.addActionListener(event -> updateKindFields());
        submitOnboarding.addActionListener(event -> submitOnboarding());
        submitDeactivation.addActionListener(event -> submitDeactivation());
        updateKindFields();
    }

    private void updateKindFields() {
        boolean existing = applicationKind.getSelectedItem() == ApplicationKind.EXISTING;
        username.setEnabled(existing);
        displayName.setEnabled(!existing);
    }

    private void load() {
        if (busy) return;
        setBusy(true);
        status.setForeground(HospitalTheme.MUTED);
        status.setText("正在读取医生与申请记录……");
        retry.setVisible(false);
        new SwingWorker<Workspace, Void>() {
            @Override protected Workspace doInBackground() throws Exception {
                Response departments = context.send(HospitalActions.LIST_DEPARTMENTS, null);
                Response doctors = context.send(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null);
                Response applications = context.send(HospitalActions.LIST_DOCTOR_APPLICATIONS, null);
                if (!departments.isSuccess()) throw new LoadException(departments.getMessage());
                if (!doctors.isSuccess()) throw new LoadException(doctors.getMessage());
                if (!applications.isSuccess()) throw new LoadException(applications.getMessage());
                return new Workspace(
                        ((DepartmentListResponse) departments.getData()).getDepartments(),
                        ((AdminScheduleWorkspaceView) doctors.getData()).getDoctors(),
                        ((DoctorApplicationListResponse) applications.getData()).getApplications());
            }

            @Override protected void done() {
                try {
                    applyWorkspace(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    loadFailed("读取已中断");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    loadFailed(cause instanceof LoadException
                            ? cause.getMessage() : "无法连接服务器");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void applyWorkspace(Workspace workspace) {
        department.removeAllItems();
        workspace.departments().stream()
                .filter(DepartmentView::isBookable)
                .forEach(item -> department.addItem(new DepartmentOption(
                        item.getDepartmentId(), item.getDepartmentName())));
        doctor.removeAllItems();
        workspace.doctors().stream()
                .filter(AdminDoctorView::isActive)
                .forEach(item -> doctor.addItem(new DoctorOption(
                        item.getDoctorId(), item.toString())));
        renderHistory(workspace.applications());
        long activeDoctors = workspace.doctors().stream()
                .filter(AdminDoctorView::isActive)
                .count();
        status.setForeground(HospitalTheme.SUCCESS);
        status.setText("可申请停用的在岗医生 " + activeDoctors
                + " 人；申请记录 " + workspace.applications().size() + " 条");
        submitDeactivation.setEnabled(activeDoctors > 0);
    }

    private void renderHistory(List<DoctorApplicationView> applications) {
        history.removeAll();
        if (applications.isEmpty()) {
            history.add(note("暂无申请记录。"));
        } else {
            applications.forEach(item -> {
                history.add(applicationCard(item));
                history.add(Box.createVerticalStrut(9));
            });
        }
        history.revalidate();
        history.repaint();
    }

    private JPanel applicationCard(DoctorApplicationView application) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                statusFill(application.getStatus()), 10, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(14, 4));
        card.setBorder(BorderFactory.createEmptyBorder(13, 15, 13, 15));
        JPanel copy = verticalList();
        JLabel title = new JLabel(typeText(application) + " · "
                + application.getDisplayName() + " · " + application.getDepartmentName());
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 15F));
        JLabel detail = new JLabel(application.getDoctorTitle() + " · "
                + application.getCreatedAt().format(DATE_TIME)
                + deactivationReasonText(application));
        detail.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(detail);
        JLabel badge = new JLabel(statusText(application.getStatus()));
        badge.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        badge.setForeground(statusColor(application.getStatus()));
        card.add(copy, BorderLayout.CENTER);
        card.add(badge, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        return card;
    }

    private void submitOnboarding() {
        if (busy) return;
        DepartmentOption selectedDepartment = (DepartmentOption) department.getSelectedItem();
        if (selectedDepartment == null) {
            showOnboardingError("没有可用的具体科室，请先建立或启用科室。");
            return;
        }
        try {
            SubmitDoctorApplicationRequest request =
                    applicationKind.getSelectedItem() == ApplicationKind.EXISTING
                            ? SubmitDoctorApplicationRequest.forExistingAccount(
                                    username.getText(), selectedDepartment.id(), doctorTitle.getText())
                            : SubmitDoctorApplicationRequest.forExternalDoctor(
                                    displayName.getText(), selectedDepartment.id(), doctorTitle.getText());
            mutate(HospitalActions.SUBMIT_DOCTOR_APPLICATION, request, onboardingStatus,
                    "新增医生申请已提交。");
        } catch (IllegalArgumentException exception) {
            showOnboardingError("请完整填写账号或姓名、科室和职称。");
        }
    }

    private void submitDeactivation() {
        if (busy) return;
        DoctorOption selectedDoctor = (DoctorOption) doctor.getSelectedItem();
        if (selectedDoctor == null) {
            showDeactivationError("当前没有可申请停用的在岗医生。");
            return;
        }
        try {
            SubmitDoctorDeactivationRequest request = new SubmitDoctorDeactivationRequest(
                    selectedDoctor.id(), deactivationReason.getText());
            mutate(HospitalActions.SUBMIT_DOCTOR_DEACTIVATION, request, deactivationStatus,
                    "停用申请已提交，医生在审核通过前仍然在岗。");
        } catch (IllegalArgumentException exception) {
            showDeactivationError("请填写停用原因。");
        }
    }

    private void mutate(String action, java.io.Serializable request, JLabel message,
                        String successText) {
        setBusy(true);
        message.setForeground(HospitalTheme.MUTED);
        message.setText("正在提交……");
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(action, request);
            }

            @Override protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        message.setForeground(HospitalTheme.SUCCESS);
                        message.setText(successText);
                        deactivationReason.setText("");
                        setBusy(false);
                        load();
                        return;
                    }
                    message.setForeground(HospitalTheme.WARNING);
                    message.setText("提交失败：" + response.getMessage());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    message.setText("提交已中断。");
                } catch (ExecutionException exception) {
                    message.setText("无法连接服务器。");
                } finally {
                    if (busy) setBusy(false);
                }
            }
        }.execute();
    }

    private void loadFailed(String message) {
        status.setForeground(HospitalTheme.WARNING);
        status.setText("读取失败：" + message);
        retry.setVisible(true);
    }

    private void setBusy(boolean value) {
        busy = value;
        submitOnboarding.setEnabled(!value);
        submitDeactivation.setEnabled(!value && doctor.getItemCount() > 0);
        retry.setEnabled(!value);
    }

    private void showOnboardingError(String text) {
        onboardingStatus.setForeground(HospitalTheme.WARNING);
        onboardingStatus.setText(text);
    }

    private void showDeactivationError(String text) {
        deactivationStatus.setForeground(HospitalTheme.WARNING);
        deactivationStatus.setText(text);
    }

    private static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        label.setForeground(HospitalTheme.PRIMARY_DARK);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea note(String text) {
        return HospitalResponsiveLayout.wrappingText(
                text, HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.MUTED);
    }

    private static JPanel field(String labelText, JComponent component) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        field.add(label, BorderLayout.NORTH);
        field.add(component, BorderLayout.CENTER);
        int height = component instanceof JScrollPane ? 112 : 66;
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        field.setAlignmentX(LEFT_ALIGNMENT);
        return field;
    }

    private static JPanel verticalList() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static String typeText(DoctorApplicationView application) {
        return switch (application.getApplicationType()) {
            case EXISTING_ACCOUNT -> "关联校园账号";
            case EXTERNAL_DOCTOR -> "新增外来医生";
            case DEACTIVATE_DOCTOR -> "停用医生";
        };
    }

    private static String statusText(DoctorApplicationStatus value) {
        return switch (value) {
            case PENDING -> "待审核";
            case APPROVED -> "已通过";
            case REJECTED -> "已拒绝";
        };
    }

    private static Color statusColor(DoctorApplicationStatus value) {
        return switch (value) {
            case PENDING -> HospitalTheme.WARNING;
            case APPROVED -> HospitalTheme.SUCCESS;
            case REJECTED -> HospitalTheme.MUTED;
        };
    }

    private static Color statusFill(DoctorApplicationStatus value) {
        return switch (value) {
            case PENDING -> HospitalTheme.WARNING_LIGHT;
            case APPROVED -> HospitalTheme.SUCCESS_LIGHT;
            case REJECTED -> HospitalTheme.SURFACE;
        };
    }

    private static String deactivationReasonText(DoctorApplicationView application) {
        return application.getApplicationType() == DoctorApplicationType.DEACTIVATE_DOCTOR
                ? " · 原因：" + application.getRequestReason() : "";
    }

    private enum ApplicationKind {
        EXISTING("关联已有校园账号"),
        EXTERNAL("为外来医生申请新账号");

        private final String label;
        ApplicationKind(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private record DepartmentOption(String id, String label) {
        @Override public String toString() { return label; }
    }

    private record DoctorOption(String id, String label) {
        @Override public String toString() { return label; }
    }

    private record Workspace(
            List<DepartmentView> departments,
            List<AdminDoctorView> doctors,
            List<DoctorApplicationView> applications) {
    }

    private static final class LoadException extends Exception {
        LoadException(String message) { super(message); }
    }
}
