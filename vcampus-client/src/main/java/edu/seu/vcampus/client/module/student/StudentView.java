package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.StudentActions;
import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentProfileRequest;
import edu.seu.vcampus.common.student.StudentProfileResponse;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;

/**
 * 学生学籍信息查询面板（标准表单卡片式布局）
 */
public class StudentView extends JPanel {
    private final ClientContext context;

    private final JTextField txtSearchId = new JTextField(12);
    private final JButton btnSearch = new JButton("查询学籍");
    private final JButton btnClear = new JButton("清空");
    private final JLabel lblTip = new JLabel("请输入学号（如 student001）进行检索");

    // 表单展示字段
    private final JLabel valId = new JLabel("-");
    private final JLabel valName = new JLabel("-");
    private final JLabel valGender = new JLabel("-");
    private final JLabel valIdCard = new JLabel("-");
    private final JLabel valBirth = new JLabel("-");
    private final JLabel valEthnicity = new JLabel("-");
    private final JLabel valNative = new JLabel("-");
    private final JLabel valPolitical = new JLabel("-");
    private final JLabel valDept = new JLabel("-");
    private final JLabel valMajor = new JLabel("-");
    private final JLabel valClass = new JLabel("-");
    private final JLabel valYear = new JLabel("-");
    private final JLabel valLevel = new JLabel("-");
    private final JLabel valStatus = new JLabel("-");

    public StudentView(ClientContext context) {
        this.context = context;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. 顶部检索栏
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        searchPanel.setBorder(BorderFactory.createTitledBorder("学籍检索"));
        searchPanel.add(new JLabel("学生学号 / 一卡通:"));
        searchPanel.add(txtSearchId);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClear);
        lblTip.setForeground(Color.GRAY);
        searchPanel.add(lblTip);
        add(searchPanel, BorderLayout.NORTH);

        // 2. 中间卡片式档案表单
        JPanel cardPanel = new JPanel(new GridLayout(7, 2, 15, 12));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "学籍档案详情",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 14)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        addFormField(cardPanel, "学  号：", valId);
        addFormField(cardPanel, "姓  名：", valName);
        addFormField(cardPanel, "性  别：", valGender);
        addFormField(cardPanel, "身份证号：", valIdCard);
        addFormField(cardPanel, "出生日期：", valBirth);
        addFormField(cardPanel, "民  族：", valEthnicity);
        addFormField(cardPanel, "籍  贯：", valNative);
        addFormField(cardPanel, "政治面貌：", valPolitical);
        addFormField(cardPanel, "所在院系：", valDept);
        addFormField(cardPanel, "所学专业：", valMajor);
        addFormField(cardPanel, "行政班级：", valClass);
        addFormField(cardPanel, "入学年份：", valYear);
        addFormField(cardPanel, "培养层次：", valLevel);
        addFormField(cardPanel, "学籍状态：", valStatus);

        add(cardPanel, BorderLayout.CENTER);

        // 绑定事件
        btnSearch.addActionListener(e -> executeQuery());
        txtSearchId.addActionListener(e -> executeQuery());
        btnClear.addActionListener(e -> clearForm());
    }

    private void addFormField(JPanel parent, String labelText, JLabel valLabel) {
        JPanel fieldBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        lbl.setPreferredSize(new Dimension(85, 25));
        valLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        valLabel.setForeground(new Color(30, 70, 140));
        fieldBox.add(lbl);
        fieldBox.add(valLabel);
        parent.add(fieldBox);
    }

    private void executeQuery() {
        String studentId = txtSearchId.getText().trim();
        if (studentId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入学号！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSearch.setEnabled(false);
        lblTip.setText("正在查询服务器档案，请稍候...");
        lblTip.setForeground(Color.BLUE);

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
                    if (res == null) {
                        lblTip.setText("网络通信失败，请检查服务端是否已启动");
                        lblTip.setForeground(Color.RED);
                        clearFormValues();
                        return;
                    }

                    if (!res.isSuccess()) {
                        lblTip.setText("查询受阻: " + res.getMessage());
                        lblTip.setForeground(Color.RED);
                        clearFormValues();
                        return;
                    }

                    StudentProfileResponse profileRes = (StudentProfileResponse) res.getData();
                    if (!profileRes.isFound()) {
                        lblTip.setText(profileRes.getMessage());
                        lblTip.setForeground(Color.RED);
                        clearFormValues();
                    } else {
                        lblTip.setText("学籍档案获取成功！");
                        lblTip.setForeground(new Color(0, 128, 0));
                        renderProfile(profileRes.getProfile());
                    }
                } catch (Exception ex) {
                    lblTip.setText("处理异常: " + ex.getMessage());
                    lblTip.setForeground(Color.RED);
                }
            }
        }.execute();
    }

    private void renderProfile(StudentProfileDto p) {
        valId.setText(p.getStudentId());
        valName.setText(p.getName());
        valGender.setText(p.getGender());
        valIdCard.setText(p.getIdCard());
        valBirth.setText(p.getBirthDate());
        valEthnicity.setText(p.getEthnicity());
        valNative.setText(p.getNativePlace());
        valPolitical.setText(p.getPoliticalStatus());
        valDept.setText(p.getDepartment());
        valMajor.setText(p.getMajor());
        valClass.setText(p.getClassName());
        valYear.setText(p.getEnrollmentYear());
        valLevel.setText(p.getEducationLevel());
        valStatus.setText(p.getStatus());
    }

    private void clearFormValues() {
        valId.setText("-");
        valName.setText("-");
        valGender.setText("-");
        valIdCard.setText("-");
        valBirth.setText("-");
        valEthnicity.setText("-");
        valNative.setText("-");
        valPolitical.setText("-");
        valDept.setText("-");
        valMajor.setText("-");
        valClass.setText("-");
        valYear.setText("-");
        valLevel.setText("-");
        valStatus.setText("-");
    }

    private void clearForm() {
        txtSearchId.setText("");
        lblTip.setText("请输入学号（如 student001）进行检索");
        lblTip.setForeground(Color.GRAY);
        clearFormValues();
    }
}
