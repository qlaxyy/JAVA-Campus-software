package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
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

    // 常用调色板与字体（与校医院保持一致）
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
    private final JLabel valDept = new JLabel("-");
    private final JLabel valMajor = new JLabel("-");
    private final JLabel valClass = new JLabel("-");
    private final JLabel valYear = new JLabel("-");
    private final JLabel valLevel = new JLabel("-");
    private final JLabel valStatus = new JLabel("-");

    // 选课模块联调核心字段
    private final JLabel valPlanId = new JLabel("-");
    private final JLabel valCurrentTerm = new JLabel("-");
    private final JLabel valCampus = new JLabel("-");

    // 补充信息组件
    private final JComboBox<String> cmbPolitical = new JComboBox<>(new String[]{"群众", "共青团员", "中共预备党员", "中共党员"});
    private final JTextField txtPhone = new JTextField(14);
    private final JTextField txtEmail = new JTextField(14);
    private final JTextField txtHomeAddress = new JTextField(14);
    private final JTextField txtEmergencyContact = new JTextField(14);
    private final JTextField txtEmergencyPhone = new JTextField(14);

    private StudentProfileDto currentProfile;

    public StudentView(ClientContext context) {
        this.context = context;
        initUI();
        setEditableState(false);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 249, 250));

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(new Color(248, 249, 250));
        mainContainer.setBorder(new EmptyBorder(20, 25, 20, 25));

        // 1. 顶部标题
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("学生学籍");
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(TEXT_MAIN);

        JLabel lblSubtitle = new JLabel("校园学籍管理服务  ·  支持档案全景查阅，非关键联络信息自主维护");
        lblSubtitle.setFont(FONT_SUB);
        lblSubtitle.setForeground(TEXT_MUTED);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSubtitle);
        mainContainer.add(headerPanel);
        mainContainer.add(Box.createVerticalStrut(15));

        // 2. 墨绿色深色 Banner
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

        JPanel bannerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        bannerTextPanel.setOpaque(false);
        JLabel lblBannerTitle = new JLabel("在校学籍全周期管理");
        lblBannerTitle.setFont(new Font("微软雅黑", Font.BOLD, 17));
        lblBannerTitle.setForeground(Color.WHITE);

        JLabel lblBannerDesc = new JLabel("学生仅可维护个人联络信息；教师可查阅；学籍管理员拥有完全管理权限。");
        lblBannerDesc.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        lblBannerDesc.setForeground(new Color(220, 240, 235));

        bannerTextPanel.add(lblBannerTitle);
        bannerTextPanel.add(lblBannerDesc);
        bannerPanel.add(bannerTextPanel, BorderLayout.CENTER);

        // Banner 右侧查询交互
        JPanel bannerRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        bannerRightPanel.setOpaque(false);
        txtSearchId.setPreferredSize(new Dimension(110, 32));
        txtSearchId.setText("student001");
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

        // 3. 状态提示
        lblStatus.setFont(FONT_SUB);
        lblStatus.setForeground(TEXT_MUTED);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContainer.add(lblStatus);
        mainContainer.add(Box.createVerticalStrut(10));

        // 4. 卡片区（六宫格）
        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        cardsGrid.setOpaque(false);
        cardsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 卡片 1：基本身份信息
        JPanel cardIdentity = createCardPanel("基本身份信息", "身份证登记与户籍档案");
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
        cardsGrid.add(cardIdentity);

        // 卡片 2：在读学业状态
        JPanel cardStudy = createCardPanel("在读学业信息", "院系、专业、学期与选课基准");
        JPanel studyBody = new JPanel(new GridLayout(5, 2, 8, 5));
        studyBody.setOpaque(false);
        addField(studyBody, "所在院系", valDept);
        addField(studyBody, "所学专业", valMajor);
        addField(studyBody, "行政班级", valClass);
        addField(studyBody, "培养层次", valLevel);
        addField(studyBody, "入学年份", valYear);
        addField(studyBody, "学籍状态", valStatus);
        addField(studyBody, "培养方案", valPlanId);
        addField(studyBody, "建议学期", valCurrentTerm);
        addField(studyBody, "就读校区", valCampus);
        cardStudy.add(studyBody, BorderLayout.CENTER);
        cardsGrid.add(cardStudy);

        // 卡片 3：联络补充（支持修改）
        JPanel cardContact = createCardPanel("联络与补充信息", "学生本人维护与紧急联系");
        JPanel contactBody = new JPanel(new GridLayout(6, 1, 0, 4));
        contactBody.setOpaque(false);
        addFormWidget(contactBody, "政治面貌", cmbPolitical);
        addFormWidget(contactBody, "联系电话", txtPhone);
        addFormWidget(contactBody, "电子邮箱", txtEmail);
        addFormWidget(contactBody, "家庭住址", txtHomeAddress);
        addFormWidget(contactBody, "紧急联系人", txtEmergencyContact);
        addFormWidget(contactBody, "紧急电话", txtEmergencyPhone);
        cardContact.add(contactBody, BorderLayout.CENTER);

        // 卡片 3 底部按钮
        JPanel contactAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        contactAction.setOpaque(false);
        styleButton(btnEdit, false);
        styleButton(btnSave, true);
        styleButton(btnCancel, false);
        btnEdit.setEnabled(false);
        contactAction.add(btnEdit);
        contactAction.add(btnSave);
        contactAction.add(btnCancel);
        cardContact.add(contactAction, BorderLayout.SOUTH);
        cardsGrid.add(cardContact);

        // 卡片 4：学籍异动申请卡片（基于当前会话精准鉴权分发）
        JPanel cardChange = createCardPanel("学籍异动申请", "申请转专业、休学与复学流程");
        JButton btnOpenChange = new JButton("办理/查看异动");
        btnOpenChange.setFont(FONT_SUB);
        btnOpenChange.setBackground(new Color(224, 231, 255));
        btnOpenChange.setForeground(Color.BLACK);
        btnOpenChange.setFocusPainted(false);
        btnOpenChange.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(199, 210, 254), 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        btnOpenChange.addActionListener(e -> {
            String rawId = context.currentSession()
                .map(s -> s.getUserId() != null ? s.getUserId().trim().toLowerCase() : "")
                .orElse("");

            if (rawId.startsWith("u-")) {
                rawId = rawId.substring(2);
            }
            rawId = rawId.replace("-", "");

            boolean isTeacher = rawId.contains("teacher");
            boolean isAdmin = rawId.contains("admin");

            // 1. 教师拦截：无权进入学籍异动
            if (isTeacher) {
                JOptionPane.showMessageDialog(this, "权限不足：普通教师无权访问学籍异动管理模块！", "权限受限", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. 管理员模式：进入全局审批工作台
            if (isAdmin) {
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

            // 3. 学生端模式：仅限申请与查看本人异动
            String targetStudentId = rawId;
            if (currentProfile != null && !rawId.equals(currentProfile.getStudentId())) {
                JOptionPane.showMessageDialog(this, "权限不足：学生仅能查看与办理本人的学籍异动！", "权限受限", JOptionPane.WARNING_MESSAGE);
                return;
            }

            StatusChangeDialog dlg = new StatusChangeDialog(
                SwingUtilities.getWindowAncestor(this),
                context,
                targetStudentId,
                false,
                () -> {
                    if (currentProfile != null) executeQuery();
                }
            );
            dlg.setVisible(true);
        });

        cardChange.add(btnOpenChange, BorderLayout.SOUTH);
        cardsGrid.add(cardChange);

        // 卡片 5~6：占位
        cardsGrid.add(createPlaceholderCard("学籍证明下载", "在线开具并打印中英文在读证明", "后续开放"));
        cardsGrid.add(createPlaceholderCard("学业毕业审核", "培养方案完成度与学分绩点核算", "后续开放"));

        mainContainer.add(cardsGrid);

        // 外层装入滚动面板
        JScrollPane scrollPane = new JScrollPane(mainContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 事件监听
        btnSearch.addActionListener(e -> executeQuery());
        txtSearchId.addActionListener(e -> executeQuery());
        btnEdit.addActionListener(e -> setEditableState(true));
        btnCancel.addActionListener(e -> {
            setEditableState(false);
            if (currentProfile != null) renderProfile(currentProfile);
        });
        btnSave.addActionListener(e -> executeUpdate());
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

        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        JLabel lblT = new JLabel(title);
        lblT.setFont(FONT_CARD_TITLE);
        lblT.setForeground(TEXT_MAIN);

        JLabel lblD = new JLabel(desc);
        lblD.setFont(FONT_SUB);
        lblD.setForeground(TEXT_MUTED);

        head.add(lblT);
        head.add(lblD);
        card.add(head, BorderLayout.NORTH);
        return card;
    }

    private JPanel createPlaceholderCard(String title, String desc, String btnText) {
        JPanel card = createCardPanel(title, desc);
        JButton btn = new JButton(btnText);
        btn.setFont(FONT_SUB);
        btn.setEnabled(false);
        btn.setForeground(new Color(148, 163, 184));
        btn.setPreferredSize(new Dimension(0, 28));
        btn.setBackground(new Color(245, 245, 245));
        card.add(btn, BorderLayout.SOUTH);
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
        cmbPolitical.setEnabled(editing);
        txtPhone.setEditable(editing);
        txtEmail.setEditable(editing);
        txtHomeAddress.setEditable(editing);
        txtEmergencyContact.setEditable(editing);
        txtEmergencyPhone.setEditable(editing);

        btnEdit.setVisible(!editing);
        btnSave.setVisible(editing);
        btnCancel.setVisible(editing);
    }

    private void executeQuery() {
        String studentId = txtSearchId.getText().trim();
        if (studentId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入学号！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSearch.setEnabled(false);
        lblStatus.setText("正在校验权限并获取学籍档案...");
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
                        btnEdit.setEnabled(false);
                        return;
                    }

                    StudentProfileResponse profileRes = (StudentProfileResponse) res.getData();
                    if (!profileRes.isFound()) {
                        lblStatus.setText(profileRes.getMessage());
                        lblStatus.setForeground(Color.RED);
                        clearFormValues();
                        btnEdit.setEnabled(false);
                    } else {
                        lblStatus.setText("● 学籍档案校验通过，当前已完成全量信息同步");
                        lblStatus.setForeground(new Color(13, 120, 90));
                        currentProfile = profileRes.getProfile();
                        renderProfile(currentProfile);
                        btnEdit.setEnabled(true);
                        setEditableState(false);
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

        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtHomeAddress.getText().trim();
        String contact = txtEmergencyContact.getText().trim();
        String contactPhone = txtEmergencyPhone.getText().trim();
        String political = (String) cmbPolitical.getSelectedItem();

        StudentUpdateProfileRequest req = new StudentUpdateProfileRequest(
            currentProfile.getStudentId(), political, phone, email, address, contact, contactPhone
        );

        btnSave.setEnabled(false);
        lblStatus.setText("正在提交档案更新并进行权限校验...");
        lblStatus.setForeground(ACCENT_BLUE);

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    return context.send(StudentActions.UPDATE_PROFILE, req);
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

                    JOptionPane.showMessageDialog(StudentView.this, "个人学籍补充信息已成功更新！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    lblStatus.setText("● 个人补充档案更新成功！");
                    lblStatus.setForeground(new Color(13, 120, 90));

                    currentProfile.setPoliticalStatus(political);
                    currentProfile.setPhone(phone);
                    currentProfile.setEmail(email);
                    currentProfile.setHomeAddress(address);
                    currentProfile.setEmergencyContact(contact);
                    currentProfile.setEmergencyPhone(contactPhone);

                    setEditableState(false);
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
        valDept.setText(p.getDepartment() != null ? p.getDepartment() : "-");
        valMajor.setText(p.getMajor() != null ? p.getMajor() : "-");
        valClass.setText(p.getClassName() != null ? p.getClassName() : "-");
        valYear.setText(p.getEnrollmentYear() != null ? String.valueOf(p.getEnrollmentYear()) : "-");
        valLevel.setText(p.getSchoolingLength() != null ? (p.getSchoolingLength() + "年制本科") : "本科生");
        valStatus.setText(p.getAcademicStatus() != null ? p.getAcademicStatus() : "在读");

        valPlanId.setText(p.getPlanId() != null ? "方案 #" + p.getPlanId() : "-");
        valCurrentTerm.setText(p.getCurrentTerm() != null ? "第 " + p.getCurrentTerm() + " 学期" : "-");
        String campusName = "-";
        if (p.getCampusId() != null) {
            campusName = (p.getCampusId() == 1L) ? "九龙湖校区" : (p.getCampusId() == 2L ? "四牌楼校区" : "丁家桥校区");
        }
        valCampus.setText(campusName);

        cmbPolitical.setSelectedItem(p.getPoliticalStatus() != null ? p.getPoliticalStatus() : "群众");
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
        valDept.setText("-");
        valMajor.setText("-");
        valClass.setText("-");
        valYear.setText("-");
        valLevel.setText("-");
        valStatus.setText("-");

        valPlanId.setText("-");
        valCurrentTerm.setText("-");
        valCampus.setText("-");

        txtPhone.setText("");
        txtEmail.setText("");
        txtHomeAddress.setText("");
        txtEmergencyContact.setText("");
        txtEmergencyPhone.setText("");
        currentProfile = null;
    }
}
