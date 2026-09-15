package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Database-backed doctor directory for hospital administrators. */
final class AdminDoctorDirectoryPanel extends JPanel {
    private final ClientContext context;
    private final JPanel directory = verticalList();
    private final JLabel status = new JLabel("正在读取医生名单……");
    private final JButton retry = HospitalTheme.quietButton("重新加载");
    private boolean busy;

    AdminDoctorDirectoryPanel(ClientContext context, Runnable back) {
        this.context = context;
        setLayout(new BorderLayout(0, 16));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(HospitalResponsiveLayout.constrainWidth(header(back)), BorderLayout.NORTH);
        add(HospitalResponsiveLayout.verticalScroll(directory), BorderLayout.CENTER);
        showLoading();
    }

    void activate() {
        load();
    }

    private JPanel header(Runnable back) {
        JPanel header = HospitalPageHeader.create(
                "医生名单",
                "名单来自当前数据库，并按所属科室自动分组",
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

    private void load() {
        if (busy) return;
        busy = true;
        showLoading();
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null);
            }

            @Override
            protected void done() {
                try {
                    Response response = get();
                    if (!response.isSuccess()
                            || !(response.getData() instanceof AdminScheduleWorkspaceView workspace)) {
                        showFailure(response.getMessage());
                        return;
                    }
                    render(workspace.getDoctors());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure("读取已中断");
                } catch (ExecutionException exception) {
                    showFailure("无法连接服务器");
                } finally {
                    busy = false;
                }
            }
        }.execute();
    }

    private void showLoading() {
        status.setForeground(HospitalTheme.MUTED);
        status.setText("正在读取医生名单……");
        retry.setVisible(false);
        directory.removeAll();
        directory.add(note("正在按科室整理医生资料……"));
        refreshDirectory();
    }

    private void render(List<AdminDoctorView> doctors) {
        directory.removeAll();
        if (doctors.isEmpty()) {
            directory.add(note("当前数据库中没有医生档案。可返回管理首页提交新增医生申请。"));
        } else {
            List<AdminDoctorView> ordered = new ArrayList<>(doctors);
            ordered.sort(Comparator
                    .comparing(AdminDoctorView::getDepartmentName)
                    .thenComparing(AdminDoctorView::getDepartmentId)
                    .thenComparing(item -> !item.isActive())
                    .thenComparing(AdminDoctorView::getDoctorName));
            int first = 0;
            while (first < ordered.size()) {
                String departmentId = ordered.get(first).getDepartmentId();
                int afterLast = first + 1;
                while (afterLast < ordered.size()
                        && departmentId.equals(ordered.get(afterLast).getDepartmentId())) {
                    afterLast++;
                }
                directory.add(departmentGroup(
                        ordered.get(first).getDepartmentName(),
                        ordered.subList(first, afterLast)));
                if (afterLast < ordered.size()) directory.add(Box.createVerticalStrut(12));
                first = afterLast;
            }
        }
        long active = doctors.stream().filter(AdminDoctorView::isActive).count();
        status.setForeground(HospitalTheme.SUCCESS);
        status.setText("共 " + doctors.size() + " 名医生，其中在岗 " + active
                + " 人、已停用 " + (doctors.size() - active) + " 人");
        refreshDirectory();
    }

    private JPanel departmentGroup(String departmentName, List<AdminDoctorView> doctors) {
        HospitalTheme.SurfacePanel group = new HospitalTheme.SurfacePanel(
                HospitalTheme.BACKGROUND, 12, HospitalTheme.BORDER) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        group.setName("adminDoctorDepartmentGroup");
        group.putClientProperty("departmentName", departmentName);
        group.setLayout(new BorderLayout(0, 11));
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, HospitalTheme.PRIMARY),
                BorderFactory.createEmptyBorder(13, 15, 14, 15)));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heading = HospitalResponsiveLayout.grid(2, 210, 8, 4);
        heading.setOpaque(false);
        JLabel name = new JLabel(departmentName);
        name.setName("adminDoctorDepartmentName");
        name.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        name.setForeground(HospitalTheme.TEXT);
        long active = doctors.stream().filter(AdminDoctorView::isActive).count();
        long stopped = doctors.size() - active;
        JLabel count = new JLabel("共 " + doctors.size() + " 人 · 在岗 " + active + " 人"
                + (stopped == 0 ? "" : " · 已停用 " + stopped + " 人"));
        count.setName("adminDoctorDepartmentCount");
        count.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
        count.setForeground(HospitalTheme.MUTED);
        count.setHorizontalAlignment(JLabel.TRAILING);
        heading.add(name);
        heading.add(count);

        JPanel cards = HospitalResponsiveLayout.grid(3, 230, 9, 9);
        cards.setName("adminDoctorDepartmentGrid");
        cards.setOpaque(false);
        doctors.forEach(item -> cards.add(doctorCard(item)));
        group.add(heading, BorderLayout.NORTH);
        group.add(cards, BorderLayout.CENTER);
        return group;
    }

    private JPanel doctorCard(AdminDoctorView item) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                item.isActive() ? Color.WHITE : HospitalTheme.DISABLED,
                10, HospitalTheme.BORDER);
        card.setName("adminDoctorDirectoryCard");
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(item.getDoctorName() + " · " + item.getDoctorTitle());
        name.setFont(HospitalTheme.uiFont(Font.BOLD, 14F));
        name.setForeground(HospitalTheme.TEXT);
        copy.add(name);
        if (!item.getAccountName().isBlank()) {
            JLabel account = new JLabel("账号 " + item.getAccountName());
            account.setName("adminDoctorAccount");
            account.setFont(HospitalTheme.uiFont(Font.PLAIN, 12F));
            account.setForeground(HospitalTheme.MUTED);
            copy.add(Box.createVerticalStrut(3));
            copy.add(account);
        }
        JLabel state = new JLabel(item.isActive() ? "在岗" : "已停用");
        state.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        state.setForeground(item.isActive() ? HospitalTheme.SUCCESS : HospitalTheme.MUTED);
        card.add(copy, BorderLayout.CENTER);
        card.add(state, BorderLayout.EAST);
        card.setPreferredSize(new Dimension(230, 54));
        return card;
    }

    private void showFailure(String message) {
        directory.removeAll();
        directory.add(note("医生名单读取失败，请检查服务器后重新加载。"));
        status.setForeground(HospitalTheme.WARNING);
        status.setText("读取失败：" + (message == null || message.isBlank() ? "未知错误" : message));
        retry.setVisible(true);
        refreshDirectory();
    }

    private void refreshDirectory() {
        directory.revalidate();
        directory.repaint();
    }

    private static javax.swing.JTextArea note(String text) {
        return HospitalResponsiveLayout.wrappingText(
                text, HospitalTheme.uiFont(Font.PLAIN, 13F), HospitalTheme.MUTED);
    }

    private static JPanel verticalList() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }
}
