package edu.seu.vcampus.client.module.student;

import edu.seu.vcampus.common.student.StudentProfileDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class StudentCertificateDialog extends JDialog {

    private final StudentProfileDto profile;
    private final JComboBox<String> cmbLanguage = new JComboBox<>(new String[]{"中文证明 (Chinese)", "英文证明 (English)"});
    private final JTextPane txtPreview = new JTextPane();
    private final String certNo;
    private final String issueDate;

    public StudentCertificateDialog(Window owner, StudentProfileDto profile) {
        super(owner, "学籍在读证明开具与打印", ModalityType.APPLICATION_MODAL);
        this.profile = profile;
        this.issueDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        this.certNo = "SEU-STU-" + profile.getStudentId() + "-" + System.currentTimeMillis() % 100000;

        initUI();
        refreshCertificateContent();
    }

    private void initUI() {
        setSize(780, 640);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        // 1. 顶部控制栏
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setBackground(new Color(248, 249, 250));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 235)));

        topBar.add(new JLabel("证明语言格式:"));
        topBar.add(cmbLanguage);
        topBar.add(new JLabel(" |  开具编号: " + certNo));

        add(topBar, BorderLayout.NORTH);

        // 2. 中间证明预览面板（A4 纸张效果）
        txtPreview.setEditable(false);
        txtPreview.setContentType("text/html");
        txtPreview.setMargin(new Insets(20, 25, 20, 25));

        JPanel paperContainer = new JPanel(new BorderLayout());
        paperContainer.setBackground(new Color(240, 242, 245));
        paperContainer.setBorder(new EmptyBorder(15, 40, 15, 40));

        JScrollPane scrollPane = new JScrollPane(txtPreview);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        paperContainer.add(scrollPane, BorderLayout.CENTER);
        add(paperContainer, BorderLayout.CENTER);

        // 3. 底部功能按钮
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomBar.setBackground(new Color(248, 249, 250));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 230, 235)));

        JButton btnSaveTxt = createStyledButton("导出文本凭单", new Color(241, 245, 249), new Color(203, 213, 225));
        JButton btnPrint = createStyledButton("立即调用打印 (Print)", new Color(187, 247, 208), new Color(134, 239, 172));
        JButton btnClose = createStyledButton("关闭", new Color(241, 245, 249), new Color(203, 213, 225));

        btnSaveTxt.addActionListener(e -> exportToFile());
        btnPrint.addActionListener(e -> printCertificate());
        btnClose.addActionListener(e -> dispose());
        cmbLanguage.addActionListener(e -> refreshCertificateContent());

        bottomBar.add(btnSaveTxt);
        bottomBar.add(btnPrint);
        bottomBar.add(btnClose);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bg, Color border) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        return btn;
    }

    private void refreshCertificateContent() {
        boolean isEnglish = cmbLanguage.getSelectedIndex() == 1;
        String html;
        if (isEnglish) {
            html = buildEnglishHtml();
        } else {
            html = buildChineseHtml();
        }
        txtPreview.setText(html);
        txtPreview.setCaretPosition(0);
    }

    private String buildChineseHtml() {
        String schooling = profile.getSchoolingLength() != null ? String.valueOf(profile.getSchoolingLength()) : "4";
        String status = profile.getAcademicStatus() != null ? profile.getAcademicStatus() : "在读";

        return "<html>"
            + "<body style='font-family: Microsoft YaHei, sans-serif; padding: 25px; line-height: 1.8; color: #1e293b;'>"
            + "<div style='text-align: center; margin-bottom: 20px;'>"
            + "<h2 style='margin: 0; color: #0d5e4c; font-size: 24px; letter-spacing: 2px;'>东南大学学籍在读证明</h2>"
            + "<p style='color: #64748b; font-size: 11px; margin-top: 4px;'>Southeast University Certificate of Student Status</p>"
            + "<div style='height: 2px; background-color: #0d5e4c; margin: 15px 0 25px 0;'></div>"
            + "</div>"
            + "<p style='text-align: right; color: #64748b; font-size: 12px;'>证明编号: <b>" + certNo + "</b></p>"
            + "<div style='font-size: 14px; text-indent: 2em; margin: 25px 0;'>"
            + "学生 <b>" + profile.getName() + "</b>（性别：<b>" + profile.getGender() + "</b>，学号：<b>" + profile.getStudentId() + "</b>，"
            + "身份证号：<b>" + (profile.getIdCardNumber() != null ? profile.getIdCardNumber() : "-") + "</b>），"
            + "系我校 <b>" + (profile.getDepartment() != null ? profile.getDepartment() : "计算机科学与工程学院") + "</b> "
            + "<b>" + (profile.getMajor() != null ? profile.getMajor() : "计算机科学与技术") + "</b> 专业 "
            + "<b>" + (profile.getEnrollmentYear() != null ? profile.getEnrollmentYear() : "2024") + "</b> 级全日制本科生（" + schooling + "年制）。"
            + "</div>"
            + "<div style='font-size: 14px; text-indent: 2em; margin-bottom: 30px;'>"
            + "该生当前学籍状态为：<b style='color: #0d5e4c;'>" + status + "</b>。该生在校遵守校纪校规，表现良好，特此证明。"
            + "</div>"
            + "<div style='margin-top: 60px; float: right; text-align: center;'>"
            + "<p style='font-size: 14px; margin: 0;'>东南大学教务处（学籍管理办公室）</p>"
            + "<p style='font-size: 12px; color: #64748b; margin-top: 5px;'>签发日期：" + issueDate + "</p>"
            + "<div style='display: inline-block; border: 2px dashed #dc2626; color: #dc2626; border-radius: 50%; "
            + "width: 90px; height: 90px; line-height: 85px; font-weight: bold; font-size: 12px; transform: rotate(-15deg); margin-top: 10px;'>"
            + "★电子验证章★"
            + "</div>"
            + "</div>"
            + "</body>"
            + "</html>";
    }

    private String buildEnglishHtml() {
        String schooling = profile.getSchoolingLength() != null ? String.valueOf(profile.getSchoolingLength()) : "4";
        String status = profile.getAcademicStatus() != null ? profile.getAcademicStatus() : "Enrolled";

        return "<html>"
            + "<body style='font-family: Arial, sans-serif; padding: 25px; line-height: 1.6; color: #1e293b;'>"
            + "<div style='text-align: center; margin-bottom: 20px;'>"
            + "<h2 style='margin: 0; color: #0d5e4c; font-size: 22px;'>SOUTHEAST UNIVERSITY</h2>"
            + "<h3 style='margin: 4px 0 0 0; color: #334155; font-size: 16px;'>Certificate of Enrollment</h3>"
            + "<div style='height: 2px; background-color: #0d5e4c; margin: 15px 0 25px 0;'></div>"
            + "</div>"
            + "<p style='text-align: right; color: #64748b; font-size: 11px;'>Ref No: <b>" + certNo + "</b></p>"
            + "<div style='font-size: 13px; margin: 25px 0; text-align: justify;'>"
            + "This is to certify that <b>" + profile.getName() + "</b> (Gender: <b>" + profile.getGender() + "</b>, "
            + "Student ID: <b>" + profile.getStudentId() + "</b>) is currently enrolled as a full-time undergraduate student "
            + "in the Department of <b>" + (profile.getDepartment() != null ? profile.getDepartment() : "Computer Science") + "</b>, "
            + "majoring in <b>" + (profile.getMajor() != null ? profile.getMajor() : "Computer Science and Technology") + "</b> (" + schooling + "-year program) "
            + "since September, <b>" + (profile.getEnrollmentYear() != null ? profile.getEnrollmentYear() : "2024") + "</b>."
            + "</div>"
            + "<div style='font-size: 13px; margin-bottom: 30px;'>"
            + "Current Academic Status: <b style='color: #0d5e4c;'>" + status + "</b>."
            + "</div>"
            + "<div style='margin-top: 50px; float: right; text-align: center;'>"
            + "<p style='font-size: 13px; margin: 0; font-weight: bold;'>Academic Affairs Office</p>"
            + "<p style='font-size: 11px; color: #64748b;'>Southeast University, P.R.China</p>"
            + "<p style='font-size: 11px; color: #64748b;'>Date: " + LocalDate.now().toString() + "</p>"
            + "</div>"
            + "</body>"
            + "</html>";
    }

    private void exportToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("东南大学学籍证明_" + profile.getStudentId() + ".txt"));
        int res = fileChooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(dest)) {
                writer.write("====================================================\n");
                writer.write("              东南大学学籍在读证明凭证               \n");
                writer.write("====================================================\n");
                writer.write("证明编号: " + certNo + "\n");
                writer.write("学生学号: " + profile.getStudentId() + "\n");
                writer.write("学生姓名: " + profile.getName() + "\n");
                writer.write("性别: " + profile.getGender() + "\n");
                writer.write("所在院系: " + profile.getDepartment() + "\n");
                writer.write("所学专业: " + profile.getMajor() + "\n");
                writer.write("学籍状态: " + (profile.getAcademicStatus() != null ? profile.getAcademicStatus() : "在读") + "\n");
                writer.write("签发机构: 东南大学教务处 (学籍办公室)\n");
                writer.write("签发日期: " + issueDate + "\n");
                writer.write("====================================================\n");
                JOptionPane.showMessageDialog(this, "学籍证明凭证导出成功！\n文件路径: " + dest.getAbsolutePath(), "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "导出文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printCertificate() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                txtPreview.printAll(g2d);
                return PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "打印任务已提交！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, "打印失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
