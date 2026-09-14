package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.StudentProfileDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class AddStudentDialog extends JDialog {
    private final ClientContext context;
    private final Runnable onSuccess;

    private final JTextField txtStudentId = new JTextField(15);
    private final JTextField txtName = new JTextField(15);
    private final JComboBox<String> cmbGender = new JComboBox<>(new String[]{"男", "女"});
    private final JTextField txtMajor = new JTextField(15);
    private final JTextField txtClass = new JTextField(15);
    private final JTextField txtYear = new JTextField("2026", 15);
    private final JTextField txtIdCard = new JTextField(15);

    public AddStudentDialog(Window parent, ClientContext context, Runnable onSuccess) {
        super(parent, "录入新生 / 新增学生学籍档案", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.onSuccess = onSuccess;

        setSize(480, 420);
        setLocationRelativeTo(parent);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));
        root.setBackground(new Color(248, 249, 250));

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        formPanel.setOpaque(false);

        formPanel.add(new JLabel("学号 (唯一标识):"));
        formPanel.add(txtStudentId);

        formPanel.add(new JLabel("学生姓名:"));
        formPanel.add(txtName);

        formPanel.add(new JLabel("性别:"));
        formPanel.add(cmbGender);

        formPanel.add(new JLabel("所学专业:"));
        txtMajor.setText("计算机科学与技术");
        formPanel.add(txtMajor);

        formPanel.add(new JLabel("行政班级:"));
        txtClass.setText("计科2601班");
        formPanel.add(txtClass);

        formPanel.add(new JLabel("入学年份:"));
        formPanel.add(txtYear);

        formPanel.add(new JLabel("身份证号:"));
        txtIdCard.setText("320102200601011234");
        formPanel.add(txtIdCard);

        root.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnSubmit = new JButton("确认录入");
        btnSubmit.setBackground(new Color(187, 247, 208));
        btnSubmit.setFocusPainted(false);
        btnSubmit.addActionListener(event -> submitAdd());

        JButton btnCancel = new JButton("取消");
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(event -> dispose());

        btnPanel.add(btnSubmit);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void submitAdd() {
        String studentId = txtStudentId.getText().trim();
        String name = txtName.getText().trim();
        String major = txtMajor.getText().trim();
        String className = txtClass.getText().trim();
        String yearStr = txtYear.getText().trim();
        String idCard = txtIdCard.getText().trim();

        if (studentId.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "学号和姓名不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StudentProfileDto dto = new StudentProfileDto();
        dto.setStudentId(studentId);
        dto.setName(name);
        dto.setGender((String) cmbGender.getSelectedItem());
        dto.setMajor(major);
        dto.setClassName(className);
        dto.setDepartment("计算机科学与工程学院");
        try {
            dto.setEnrollmentYear(Integer.parseInt(yearStr));
        } catch (Exception e) {
            dto.setEnrollmentYear(2026);
        }
        dto.setIdCardNumber(idCard);

        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() {
                try {
                    // 直接使用通信动作字面量，避开 common 常量修改
                    return context.send("student:add", dto);
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Response res = get();
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(AddStudentDialog.this, "新生学籍档案录入成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(AddStudentDialog.this, "录入失败: " + (res != null ? res.getMessage() : "学号可能已存在"), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AddStudentDialog.this, "异常: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
