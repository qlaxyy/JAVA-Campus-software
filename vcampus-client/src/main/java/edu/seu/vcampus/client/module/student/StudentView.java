package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.ApplyStatusChangeRequest;
import edu.seu.vcampus.common.student.AuditStatusChangeRequest;
import edu.seu.vcampus.common.student.StatusChangeDto;
import edu.seu.vcampus.common.student.StudentActions;
import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentProfileRequest;
import edu.seu.vcampus.common.student.StudentProfileResponse;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class StudentView extends JPanel {
    private final ClientContext context;

    private static final Color THEME_BANNER_BG = new Color(13, 94, 76);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER_COLOR = new Color(225, 230, 235);
    private static final Color TEXT_MUTED = new Color(120, 130, 140);
    private static final Color TEXT_MAIN = new Color(40, 45, 50);
    private static final Color ACCENT_BLUE = new Color(24, 100, 190);

    private static final Font FONT_HEADER = new Font("微软雅黑", Font.BOLD, 22);
    private static final Font FONT_SUB = new Font("微软雅黑", Font.PLAIN, 12);
    private static final Font FONT_CARD_TITLE = new Font("微软雅黑", Font.BOLD, 15);
    private static final Font FONT_BODY = new Font("微软雅黑", Font.PLAIN, 13);
    private static final Font FONT_BOLD_BODY = new Font("微软雅黑", Font.BOLD, 13);

    // 顶部交互
    private final JTextField txtSearchId = new JTextField(10);
    private final JButton btnSearch = new JButton("查询档案");

    // 操作按钮与提示
    private final JButton btnEdit = new JButton("编辑修改");
    private final JButton btnSave = new JButton("保存提交");
    private final JButton btnCancel = new JButton("取消");
    private final JLabel lblStatus = new JLabel("请输入学号开始检索");

    // 核心信息 Label
    private final JLabel valId = new JLabel("-");
    private final JLabel valName = new JLabel("-");
    private final JLabel valGender = new JLabel("-");
    private final JLabel valIdCard = new JLabel("-");
    private final JLabel valBirth = new JLabel("-");
    private final JLabel valEthnicity = new JLabel("-");
    private final JLabel valNative = new JLabel("-");
    private final JLabel valYear = new JLabel("-");
    private final JLabel valLevel = new JLabel("-");

    // 选课模块联调核心字段
    private final JLabel valPlanId = new JLabel("-");
    private final JLabel valCurrentTerm = new JLabel("-");
    private final JLabel valCampus = new JLabel("-");

    // 学业信息组件（管理员可编辑）
    private final JTextField txtDept = new JTextField(12);
    private final JLabel valDeptLabel = new JLabel("-");
    private final JTextField txtMajor = new JTextField(12);
    private final JLabel valMajorLabel = new JLabel("-");
    private final JTextField txtClass = new JTextField(12);
    private final JLabel valClassLabel = new JLabel("-");
    private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"在读", "休学", "退学", "毕业"});
    private final JLabel valStatusLabel = new JLabel("-");

    private final JPanel deptContainer = new JPanel(new CardLayout());
    private final JPanel majorContainer = new JPanel(new CardLayout());
    private final JPanel classContainer = new JPanel(new CardLayout());
    private final JPanel statusContainer = new JPanel(new CardLayout());

    // 补充信息组件
    private final JComboBox<String> cmbPolitical = new JComboBox<>(new String[]{"-", "群众", "共青团员", "中共预备党员", "中共党员"});
    private final JTextField txtPhone = new JTextField(14);
    private final JTextField txtEmail = new JTextField(14);
    private final JTextField txtHomeAddress = new JTextField(14);
    private final JTextField txtEmergencyContact = new JTextField(14);
    private final JTextField txtEmergencyPhone = new JTextField(14);

    // 动态显隐控制按钮
    private final JButton btnApplyModify = new JButton();
    private final JButton btnOpenChange = new JButton();
    private final JButton btnDownloadCert = new JButton("开具证明");
    private final JButton btnOpenAudit = new JButton("查看毕业审核");

    private StudentProfileDto currentProfile;
    private long initialSearchGeneration;

    public StudentView(ClientContext context) {
        this.context = context;
        initUI();
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && isShowing()) {
                initializeOwnStudentSearch();
            }
        });
        clearFormValues();
        setEditableState(false);
        updateButtonVisibility(null);
    }

    private boolean isStudentAdmin() {
        return context.currentSession()
            .map(session -> session.canAdminister(ModuleNames.STUDENT))
            .orElse(false);
    }

    /** The server has already authorized the returned student profile. Never infer identity from ID prefixes. */
    private boolean isCurrentSelfStudent(String targetStudentId) {
        if (targetStudentId == null || targetStudentId.isBlank()) {
            return false;
        }
        return context.currentSession()
                .map(session -> targetStudentId.trim().equals(session.getUsername().trim()))
                .orElse(false);
    }

    /**
     * 权限动态收敛：管理员全开放，学生本人在查阅自己档案时开放，老师及无权角色彻底隐藏
     */
    private void updateButtonVisibility(StudentProfileDto profile) {
        boolean admin = isStudentAdmin();

        if (admin) {
            btnEdit.setVisible(profile != null);
            btnEdit.setEnabled(profile != null);
            btnApplyModify.setVisible(true);
            btnOpenChange.setVisible(true);
            btnDownloadCert.setVisible(profile != null);
            btnOpenAudit.setVisible(profile != null); // 管理员可查任意学生毕业审核
            return;
        }

        boolean isSelf = profile != null && isCurrentSelfStudent(profile.getStudentId());

        btnEdit.setVisible(isSelf);
        btnEdit.setEnabled(isSelf);
        btnApplyModify.setVisible(isSelf);
        btnOpenChange.setVisible(isSelf);
        btnDownloadCert.setVisible(isSelf);
        btnOpenAudit.setVisible(isSelf); // 学生仅可查阅自己的毕业审核
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 249, 250));

        boolean admin = isStudentAdmin();

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(new Color(248, 249, 250));
        mainContainer.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("学生学籍");
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(TEXT_MAIN);

        JLabel lblSubtitle = new JLabel(admin
            ? "校园学籍管理服务  ·  学籍管理员审批工作台，支持全校档案查阅与学业学籍修改"
            : "校园学籍管理服务  ·  支持档案全景查阅，关键信息申请更正，联络信息自主维护");
        lblSubtitle.setFont(FONT_SUB);
        lblSubtitle.setForeground(TEXT_MUTED);

        headerPanel.add(lblTitle);
        mainContainer.add(headerPanel);
        mainContainer.add(Box.createVerticalStrut(15));

        JPanel bannerPanel = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(THEME_BANNER_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        bannerPanel.setOpaque(false);
        bannerPanel.setBorder(new EmptyBorder(18, 22, 18, 22));
        bannerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        bannerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bannerTextPanel = new JPanel(new GridLayout(1, 1));
        bannerTextPanel.setOpaque(false);
        JLabel lblBannerTitle = new JLabel("学籍查询");
        lblBannerTitle.setFont(new Font("微软雅黑", Font.BOLD, 17));
        lblBannerTitle.setForeground(Color.WHITE);

        JLabel lblBannerDesc = new JLabel(admin
            ? "当前以【学籍管理员】身份运行：点击编辑后可直接修改学生的院系、专业、班级及学籍状态。"
            : "关键法定身份变更须提交申请与材料；教师可查阅学生学籍；学籍管理员拥有审批与管理权限。");
        lblBannerDesc.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        lblBannerDesc.setForeground(new Color(220, 240, 235));

        bannerTextPanel.add(lblBannerTitle);
        bannerPanel.add(bannerTextPanel, BorderLayout.CENTER);

        JPanel bannerRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bannerRightPanel.setOpaque(false);
        txtSearchId.setPreferredSize(new Dimension(110, 32));
        txtSearchId.setName("student.searchId");
        txtSearchId.setText("");
        txtSearchId.setFont(FONT_BODY);

        btnSearch.setPreferredSize(new Dimension(95, 32));
        btnSearch.setBackground(new Color(241, 245, 249));
        btnSearch.setForeground(Color.BLACK);
        btnSearch.setFont(FONT_BOLD_BODY);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        bannerRightPanel.add(new JLabel("<html><font color='#ffffff'>学号:</font></html>"));
        bannerRightPanel.add(txtSearchId);
        bannerRightPanel.add(btnSearch);
        bannerPanel.add(bannerRightPanel, BorderLayout.EAST);

        mainContainer.add(bannerPanel);
        mainContainer.add(Box.createVerticalStrut(15));

        lblStatus.setFont(FONT_SUB);
        lblStatus.setForeground(TEXT_MUTED);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContainer.add(lblStatus);
        mainContainer.add(Box.createVerticalStrut(10));

        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        cardsGrid.setOpaque(false);
        cardsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 卡片 1：基本身份信息
        JPanel cardIdentity = createCardPanel("基本身份信息", "身份证登记与法定户籍信息");
        JPanel idBody = new JPanel(new GridLayout(4, 2, 8, 6));
        idBody.setOpaque(false);
        addField(idBody, "学  号", valId);
        addField(idBody, "姓  名", valName);
        addField(idBody, "性  别", valGender);
        addField(idBody, "民  族", valEthnicity);
        addField(idBody, "出生日期", valBirth);
        addField(idBody, "籍  贯", valNative);
        addField(idBody, "身份证号", valIdCard);
        cardIdentity.add(idBody, BorderLayout.CENTER);

        btnApplyModify.setText(admin ? "查看信息更正审批" : "申请更正基本信息");
        btnApplyModify.setFont(FONT_SUB);
        btnApplyModify.setBackground(new Color(241, 245, 249));
        btnApplyModify.setFocusPainted(false);
        btnApplyModify.addActionListener(e -> openStatusOrModifyDialog());
        cardIdentity.add(btnApplyModify, BorderLayout.SOUTH);
        cardsGrid.add(cardIdentity);

        // 卡片 2：在读学业状态
        JPanel cardStudy = createCardPanel("在读学业信息", "院系, 专业, 学期与选课基准");
        JPanel studyBody = new JPanel(new GridLayout(5, 2, 8, 5));
        studyBody.setOpaque(false);

        setupContainer(deptContainer, valDeptLabel, txtDept);
        setupContainer(majorContainer, valMajorLabel, txtMajor);
        setupContainer(classContainer, valClassLabel, txtClass);
        setupContainer(statusContainer, valStatusLabel, cmbStatus);

        addCustomFieldWidget(studyBody, "所在院系", deptContainer);
        addCustomFieldWidget(studyBody, "所学专业", majorContainer);
        addCustomFieldWidget(studyBody, "行政班级", classContainer);
        addField(studyBody, "培养层次", valLevel);
        addField(studyBody, "入学年份", valYear);
        addCustomFieldWidget(studyBody, "学籍状态", statusContainer);
        addField(studyBody, "培养方案", valPlanId);
        addField(studyBody, "建议学期", valCurrentTerm);
        addField(studyBody, "就读校区", valCampus);
        cardStudy.add(studyBody, BorderLayout.CENTER);
        cardsGrid.add(cardStudy);

        // 卡片 3：联络补充
        JPanel cardContact = createCardPanel("联络与补充信息", "学生本人自主维护非关键联系方式");
        JPanel contactBody = new JPanel(new GridLayout(6, 1, 0, 4));
        contactBody.setOpaque(false);
        addFormWidget(contactBody, "政治面貌", cmbPolitical);
        addFormWidget(contactBody, "联系电话", txtPhone);
        addFormWidget(contactBody, "电子邮箱", txtEmail);
        addFormWidget(contactBody, "家庭住址", txtHomeAddress);
        addFormWidget(contactBody, "紧急联系人", txtEmergencyContact);
        addFormWidget(contactBody, "紧急电话", txtEmergencyPhone);
        cardContact.add(contactBody, BorderLayout.CENTER);

        JPanel contactAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        contactAction.setOpaque(false);
        styleButton(btnEdit, false);
        styleButton(btnSave, true);
        styleButton(btnCancel, false);
        contactAction.add(btnEdit);
        contactAction.add(btnSave);
        contactAction.add(btnCancel);
        cardContact.add(contactAction, BorderLayout.SOUTH);
        cardsGrid.add(cardContact);

        // 卡片 4：学籍异动与更正审批卡片
        JPanel cardChange = createCardPanel(
            admin ? "学籍异动与更正审批" : "学籍异动与信息更正",
            admin ? "转专业、休复学及信息变更全校审核" : "转专业、休复学及关键信息更正"
        );
        btnOpenChange.setText(admin ? "全校异动与更正审批" : "办理/查看我的申请");
        btnOpenChange.setFont(FONT_SUB);
        btnOpenChange.setBackground(new Color(224, 231, 255));
        btnOpenChange.setForeground(Color.BLACK);
        btnOpenChange.setFocusPainted(false);
        btnOpenChange.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(199, 210, 254), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnOpenChange.addActionListener(e -> openStatusOrModifyDialog());
        cardChange.add(btnOpenChange, BorderLayout.SOUTH);
        cardsGrid.add(cardChange);

        // 卡片 5：学籍证明下载
        JPanel cardCert = createCardPanel("学籍证明开具", "在线开具并打印中英文在读证明");
        btnDownloadCert.setFont(FONT_SUB);
        btnDownloadCert.setBackground(new Color(224, 231, 255));
        btnDownloadCert.setForeground(Color.BLACK);
        btnDownloadCert.setFocusPainted(false);
        btnDownloadCert.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(199, 210, 254), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnDownloadCert.addActionListener(e -> {
            if (currentProfile == null) return;
            StudentCertificateDialog dlg = new StudentCertificateDialog(
                SwingUtilities.getWindowAncestor(this),
                currentProfile
            );
            dlg.setVisible(true);
        });
        cardCert.add(btnDownloadCert, BorderLayout.SOUTH);
        cardsGrid.add(cardCert);

        // 卡片 6：学业毕业审核（带权限控制）
        JPanel cardAudit = createCardPanel("学业毕业审核", "培养方案完成度与学分绩点核算");
        btnOpenAudit.setFont(FONT_SUB);
        btnOpenAudit.setBackground(new Color(224, 231, 255));
        btnOpenAudit.setForeground(Color.BLACK);
        btnOpenAudit.setFocusPainted(false);
        btnOpenAudit.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(199, 210, 254), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnOpenAudit.addActionListener(e -> {
            if (currentProfile == null) return;
            GraduationAuditDialog auditDialog = new GraduationAuditDialog(
                SwingUtilities.getWindowAncestor(this),
                context,
                currentProfile,
                isStudentAdmin()
            );
            auditDialog.setVisible(true);
        });
        cardAudit.add(btnOpenAudit, BorderLayout.SOUTH);
        cardsGrid.add(cardAudit);

        mainContainer.add(cardsGrid);

        JScrollPane scrollPane = new JScrollPane(mainContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        btnSearch.addActionListener(e -> executeQuery());
        txtSearchId.addActionListener(e -> executeQuery());
        btnEdit.addActionListener(e -> setEditableState(true));
        btnCancel.addActionListener(e -> {
            setEditableState(false);
            if (currentProfile != null) renderProfile(currentProfile);
        });
        btnSave.addActionListener(e -> executeUpdate());
    }

    /** Membership comes from the server's student profile, not a userId/name heuristic. */
    SwingWorker<Response, Void> initializeOwnStudentSearch() {
        long generation = ++initialSearchGeneration;
        txtSearchId.setText("");
        clearFormValues();
        lblStatus.setText("请输入学号开始检索");
        lblStatus.setForeground(TEXT_MUTED);
        btnEdit.setEnabled(false);
        setEditableState(false);
        var session = context.currentSession();
        if (session.isEmpty()) {
            return null;
        }
        String token = session.get().getToken();
        String number = session.get().getUsername();
        SwingWorker<Response, Void> worker = new SwingWorker<>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(StudentActions.GET_PROFILE, new StudentProfileRequest(number));
            }
            @Override protected void done() {
                if (generation != initialSearchGeneration
                        || !context.currentSession().map(s -> token.equals(s.getToken())).orElse(false)
                        || !txtSearchId.getText().isBlank()) {
                    return;
                }
                try {
                    Response result = get();
                    if (result.isSuccess() && result.getData() instanceof StudentProfileResponse data
                            && data.isFound()
                            && data.getProfile() != null) {
                        txtSearchId.setText(data.getProfile().getStudentId());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException exception) {
                    // No automatic query error dialog: leave the search field blank for manual use.
                }
            }
        };
        worker.execute();
        return worker;
    }

    private void setupContainer(JPanel container, JComponent viewComp, JComponent editComp) {
        container.setOpaque(false);
        container.setLayout(new CardLayout());
        viewComp.setFont(FONT_BODY);
        if (viewComp instanceof JLabel) {
            ((JLabel) viewComp).setForeground(ACCENT_BLUE);
        }
        container.add(viewComp, "VIEW");
        container.add(editComp, "EDIT");
    }

    private void addCustomFieldWidget(JPanel parent, String label, JComponent comp) {
        JPanel box = new JPanel(new BorderLayout(5, 0));
        box.setOpaque(false);
        JLabel l = new JLabel(label + "：");
        l.setFont(FONT_SUB);
        l.setForeground(TEXT_MUTED);
        box.add(l, BorderLayout.WEST);
        box.add(comp, BorderLayout.CENTER);
        parent.add(box);
    }

    private void openStatusOrModifyDialog() {
        boolean hasAdminScope = isStudentAdmin();

        if (hasAdminScope) {
            StatusChangeDialog dlg = new StatusChangeDialog(
                SwingUtilities.getWindowAncestor(this),
                context,
                null,
                true,
                () -> {
                    if (currentProfile != null) executeQuery();
                }
            );
            dlg.setVisible(true);
            return;
        }

        if (currentProfile != null && isCurrentSelfStudent(currentProfile.getStudentId())) {
            StatusChangeDialog dlg = new StatusChangeDialog(
                SwingUtilities.getWindowAncestor(this),
                context,
                currentProfile.getStudentId(),
                false,
                () -> executeQuery()
            );
            dlg.setVisible(true);
        }
    }

    private JPanel createCardPanel(String title, String desc) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(CARD_BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel head = new JPanel(new GridLayout(1, 1));
        head.setOpaque(false);
        JLabel lblT = new JLabel(title);
        lblT.setFont(FONT_CARD_TITLE);
        lblT.setForeground(TEXT_MAIN);

        JLabel lblD = new JLabel(desc);
        lblD.setFont(FONT_SUB);
        lblD.setForeground(TEXT_MUTED);

        head.add(lblT);
        card.add(head, BorderLayout.NORTH);
        return card;
    }

    private void addField(JPanel parent, String label, JLabel val) {
        JPanel box = new JPanel(new BorderLayout(5, 0));
        box.setOpaque(false);
        JLabel l = new JLabel(label + "：");
        l.setFont(FONT_SUB);
        l.setForeground(TEXT_MUTED);
        val.setFont(FONT_BODY);
        val.setForeground(ACCENT_BLUE);
        box.add(l, BorderLayout.WEST);
        box.add(val, BorderLayout.CENTER);
        parent.add(box);
    }

    private void addFormWidget(JPanel parent, String label, JComponent comp) {
        JPanel box = new JPanel(new BorderLayout(6, 0));
        box.setOpaque(false);
        JLabel l = new JLabel(label + "：");
        l.setPreferredSize(new Dimension(65, 22));
        l.setFont(FONT_SUB);
        l.setForeground(TEXT_MUTED);
        comp.setFont(FONT_BODY);
        box.add(l, BorderLayout.WEST);
        box.add(comp, BorderLayout.CENTER);
        parent.add(box);
    }

    private void styleButton(JButton btn, boolean isPrimary) {
        btn.setFont(FONT_SUB);
        btn.setFocusPainted(false);
        btn.setForeground(Color.BLACK);
        if (isPrimary) {
            btn.setBackground(new Color(187, 247, 208));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(134, 239, 172), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        } else {
            btn.setBackground(new Color(241, 245, 249));
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        }
    }

    private void setEditableState(boolean editing) {
        boolean admin = isStudentAdmin();

        CardLayout cl1 = (CardLayout) deptContainer.getLayout();
        CardLayout cl2 = (CardLayout) majorContainer.getLayout();
        CardLayout cl3 = (CardLayout) classContainer.getLayout();
        CardLayout cl4 = (CardLayout) statusContainer.getLayout();

        if (editing && admin) {
            cl1.show(deptContainer, "EDIT");
            cl2.show(majorContainer, "EDIT");
            cl3.show(classContainer, "EDIT");
            cl4.show(statusContainer, "EDIT");
        } else {
            cl1.show(deptContainer, "VIEW");
            cl2.show(majorContainer, "VIEW");
            cl3.show(classContainer, "VIEW");
            cl4.show(statusContainer, "VIEW");
        }

        cmbPolitical.setEnabled(editing);
        txtPhone.setEditable(editing);
        txtEmail.setEditable(editing);
        txtHomeAddress.setEditable(editing);
        txtEmergencyContact.setEditable(editing);
        txtEmergencyPhone.setEditable(editing);

        if (editing) {
            btnEdit.setVisible(false);
            btnSave.setVisible(true);
            btnCancel.setVisible(true);
        } else {
            btnSave.setVisible(false);
            btnCancel.setVisible(false);
            updateButtonVisibility(currentProfile);
        }
    }

    private void executeQuery() {
        String studentId = txtSearchId.getText().trim();
        if (studentId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入学号！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSearch.setEnabled(false);
        lblStatus.setText("正在查询……");
        lblStatus.setForeground(ACCENT_BLUE);

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    return context.send(StudentActions.GET_PROFILE, new StudentProfileRequest(studentId));
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                btnSearch.setEnabled(true);
                try {
                    Response res = get();
                    if (res == null || !res.isSuccess()) {
                        lblStatus.setText("查询受阻: " + (res != null ? res.getMessage() : "网络或服务器异常"));
                        lblStatus.setForeground(Color.RED);
                        clearFormValues();
                        updateButtonVisibility(null);
                        return;
                    }

                    StudentProfileResponse profileRes = (StudentProfileResponse) res.getData();
                    if (!profileRes.isFound()) {
                        lblStatus.setText(profileRes.getMessage());
                        lblStatus.setForeground(Color.RED);
                        clearFormValues();
                        updateButtonVisibility(null);
                    } else {
                        lblStatus.setText("查询成功");
                        lblStatus.setForeground(new Color(13, 120, 90));
                        currentProfile = profileRes.getProfile();
                        renderProfile(currentProfile);

                        setEditableState(false);
                        updateButtonVisibility(currentProfile);
                    }
                } catch (Exception ex) {
                    lblStatus.setText("处理异常: " + ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                }
            }
        }.execute();
    }

    private void executeUpdate() {
        if (currentProfile == null) return;

        boolean hasAdminScope = isStudentAdmin();
        boolean isSelf = isCurrentSelfStudent(currentProfile.getStudentId());
        if (!hasAdminScope && !isSelf) {
            JOptionPane.showMessageDialog(this, "权限不足：当前账号无权修改该学生档案！", "拒绝访问", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtHomeAddress.getText().trim();
        String contact = txtEmergencyContact.getText().trim();
        String contactPhone = txtEmergencyPhone.getText().trim();

        String selectedPolitical = (String) cmbPolitical.getSelectedItem();
        final String political = "-".equals(selectedPolitical) ? "群众" : selectedPolitical;

        StudentUpdateProfileRequest req = new StudentUpdateProfileRequest(
            currentProfile.getStudentId(), political, phone, email, address, contact, contactPhone
        );

        btnSave.setEnabled(false);
        lblStatus.setText("正在保存……");
        lblStatus.setForeground(ACCENT_BLUE);

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    Response resp1 = context.send(StudentActions.UPDATE_PROFILE, req);
                    if (resp1 == null || !resp1.isSuccess()) {
                        return resp1;
                    }

                    if (hasAdminScope) {
                        String newMajor = txtMajor.getText().trim();
                        String newStatus = (String) cmbStatus.getSelectedItem();

                        if (!newMajor.equals(currentProfile.getMajor())) {
                            ApplyStatusChangeRequest appReq = new ApplyStatusChangeRequest(
                                currentProfile.getStudentId(), "转专业", "[变更: 专业 -> " + newMajor + "] 管理员后台直接调整"
                            );
                            Response rApply = context.send(StudentActions.APPLY_STATUS_CHANGE, appReq);
                            if (rApply != null && rApply.isSuccess() && rApply.getData() instanceof StatusChangeDto sc) {
                                context.send(StudentActions.AUDIT_STATUS_CHANGE, new AuditStatusChangeRequest(sc.getChangeId(), true));
                            }
                        }
                        if (!newStatus.equals(currentProfile.getAcademicStatus())) {
                            ApplyStatusChangeRequest appReq = new ApplyStatusChangeRequest(
                                currentProfile.getStudentId(), newStatus, "管理员后台直接调整学籍状态"
                            );
                            Response rApply = context.send(StudentActions.APPLY_STATUS_CHANGE, appReq);
                            if (rApply != null && rApply.isSuccess() && rApply.getData() instanceof StatusChangeDto sc) {
                                context.send(StudentActions.AUDIT_STATUS_CHANGE, new AuditStatusChangeRequest(sc.getChangeId(), true));
                            }
                        }
                    }
                    return resp1;
                } catch (IOException ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    Response res = get();
                    if (res == null || !res.isSuccess()) {
                        JOptionPane.showMessageDialog(StudentView.this,
                            res != null ? res.getMessage() : "通信失败", "保存受阻", JOptionPane.ERROR_MESSAGE);
                        lblStatus.setText("保存失败: " + (res != null ? res.getMessage() : ""));
                        lblStatus.setForeground(Color.RED);
                        return;
                    }

                    JOptionPane.showMessageDialog(StudentView.this, "学生学籍档案已成功更新！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    lblStatus.setText("● 学籍档案更新成功！");
                    lblStatus.setForeground(new Color(13, 120, 90));

                    currentProfile.setPoliticalStatus(political);
                    currentProfile.setPhone(phone);
                    currentProfile.setEmail(email);
                    currentProfile.setHomeAddress(address);
                    currentProfile.setEmergencyContact(contact);
                    currentProfile.setEmergencyPhone(contactPhone);
                    if (hasAdminScope) {
                        currentProfile.setDepartment(txtDept.getText().trim());
                        currentProfile.setMajor(txtMajor.getText().trim());
                        currentProfile.setClassName(txtClass.getText().trim());
                        currentProfile.setAcademicStatus((String) cmbStatus.getSelectedItem());
                    }

                    setEditableState(false);
                    renderProfile(currentProfile);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentView.this, "异常: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renderProfile(StudentProfileDto p) {
        valId.setText(p.getStudentId());
        valName.setText(p.getName());
        valGender.setText(p.getGender());
        valIdCard.setText(p.getIdCardNumber() != null ? p.getIdCardNumber() : "-");
        valBirth.setText(p.getBirthDate() != null ? p.getBirthDate() : "-");
        valEthnicity.setText(p.getEthnicity() != null ? p.getEthnicity() : "-");
        valNative.setText(p.getNativePlace() != null ? p.getNativePlace() : "-");

        valDeptLabel.setText(p.getDepartment() != null ? p.getDepartment() : "-");
        txtDept.setText(p.getDepartment() != null ? p.getDepartment() : "");

        valMajorLabel.setText(p.getMajor() != null ? p.getMajor() : "-");
        txtMajor.setText(p.getMajor() != null ? p.getMajor() : "");

        valClassLabel.setText(p.getClassName() != null ? p.getClassName() : "-");
        txtClass.setText(p.getClassName() != null ? p.getClassName() : "");

        valYear.setText(p.getEnrollmentYear() != null ? String.valueOf(p.getEnrollmentYear()) : "-");
        valLevel.setText(p.getSchoolingLength() != null ? (p.getSchoolingLength() + "年制本科") : "本科生");

        String status = p.getAcademicStatus() != null ? p.getAcademicStatus() : "在读";
        valStatusLabel.setText(status);
        cmbStatus.setSelectedItem(status);

        valPlanId.setText(p.getPlanId() != null ? "方案 #" + p.getPlanId() : "-");
        valCurrentTerm.setText(p.getCurrentTerm() != null ? "第 " + p.getCurrentTerm() + " 学期" : "-");
        String campusName = "-";
        if (p.getCampusId() != null) {
            campusName = (p.getCampusId() == 1L) ? "九龙湖校区" : (p.getCampusId() == 2L ? "四牌楼校区" : "丁家桥校区");
        }
        valCampus.setText(campusName);

        boolean canViewPolitical = currentProfile != null;
        if (canViewPolitical && p.getPoliticalStatus() != null && !p.getPoliticalStatus().isBlank()) {
            cmbPolitical.setSelectedItem(p.getPoliticalStatus());
        } else {
            cmbPolitical.setSelectedItem("-");
        }

        txtPhone.setText(p.getPhone() != null ? p.getPhone() : "");
        txtEmail.setText(p.getEmail() != null ? p.getEmail() : "");
        txtHomeAddress.setText(p.getHomeAddress() != null ? p.getHomeAddress() : "");
        txtEmergencyContact.setText(p.getEmergencyContact() != null ? p.getEmergencyContact() : "");
        txtEmergencyPhone.setText(p.getEmergencyPhone() != null ? p.getEmergencyPhone() : "");
    }

    private void clearFormValues() {
        valId.setText("-");
        valName.setText("-");
        valGender.setText("-");
        valIdCard.setText("-");
        valBirth.setText("-");
        valEthnicity.setText("-");
        valNative.setText("-");

        valDeptLabel.setText("-");
        txtDept.setText("");
        valMajorLabel.setText("-");
        txtMajor.setText("");
        valClassLabel.setText("-");
        txtClass.setText("");
        valStatusLabel.setText("-");

        valYear.setText("-");
        valLevel.setText("-");

        valPlanId.setText("-");
        valCurrentTerm.setText("-");
        valCampus.setText("-");

        cmbPolitical.setSelectedItem("-");
        txtPhone.setText("");
        txtEmail.setText("");
        txtHomeAddress.setText("");
        txtEmergencyContact.setText("");
        txtEmergencyPhone.setText("");
        currentProfile = null;
        updateButtonVisibility(null);
    }
}
