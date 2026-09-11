package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.common.hospital.AdminDepartmentView;
import edu.seu.vcampus.common.hospital.AdminDepartmentWorkspaceView;
import edu.seu.vcampus.common.hospital.CreateDepartmentRequest;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.UpdateDepartmentRequest;
import edu.seu.vcampus.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Hierarchical department editor owned by the hospital administrator workflow. */
final class AdminDepartmentPanel extends JPanel {
    private final ClientContext context;
    private final Runnable back;
    private final JPanel list = verticalList();
    private final JTextField search = new JTextField();
    private final JLabel status = new JLabel("正在读取科室……");
    private final JButton retry = HospitalTheme.quietButton("重新加载");
    private final JTextField name = new JTextField();
    private final JComboBox<ParentOption> parent = new JComboBox<>();
    private final JCheckBox bookable = new JCheckBox("允许患者直接挂号");
    private final JCheckBox active = new JCheckBox("科室启用");
    private final JButton save = HospitalTheme.primaryButton("建立科室");
    private final JButton clear = HospitalTheme.quietButton("新建另一科室");
    private final JLabel formStatus = new JLabel(" ");

    private List<AdminDepartmentView> departments = List.of();
    private AdminDepartmentView selected;
    private boolean busy;

    AdminDepartmentPanel(ClientContext context, Runnable back) {
        this.context = context;
        this.back = back;
        setLayout(new BorderLayout(0, 18));
        setBackground(HospitalTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(HospitalResponsiveLayout.constrainWidth(header()), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        configureActions();
    }

    void activate() {
        clearEditor();
        load();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);
        JPanel copy = verticalList();
        JLabel title = new JLabel("科室目录");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 28F));
        title.setForeground(HospitalTheme.TEXT);
        JLabel subtitle = new JLabel("维护科室层级和挂号入口；历史业务数据不会被删除");
        subtitle.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(subtitle);
        JButton backButton = HospitalTheme.quietButton("‹ 返回管理首页");
        backButton.addActionListener(event -> back.run());
        panel.add(copy, BorderLayout.CENTER);
        panel.add(backButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel content() {
        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setOpaque(false);
        content.add(catalog(), BorderLayout.CENTER);
        JScrollPane editorScroll = HospitalResponsiveLayout.verticalScroll(editor());
        editorScroll.setPreferredSize(new Dimension(390, 0));
        content.add(editorScroll, BorderLayout.EAST);
        return content;
    }

    private JPanel catalog() {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JLabel title = new JLabel("当前科室");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        search.setName("adminDepartmentSearch");
        search.setToolTipText("按科室名称搜索");
        search.setPreferredSize(new Dimension(230, 36));
        top.add(title, BorderLayout.WEST);
        top.add(search, BorderLayout.EAST);
        JScrollPane scroll = HospitalResponsiveLayout.verticalScroll(list);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footer.setOpaque(false);
        retry.setVisible(false);
        footer.add(status);
        footer.add(retry);
        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel editor() {
        HospitalTheme.SurfacePanel panel = new HospitalTheme.SurfacePanel(
                HospitalTheme.PRIMARY_LIGHT, 14);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
        panel.setPreferredSize(new Dimension(370, 560));
        JLabel title = new JLabel("科室资料");
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        title.setForeground(HospitalTheme.PRIMARY_DARK);
        name.setName("adminDepartmentName");
        parent.setName("adminDepartmentParent");
        bookable.setOpaque(false);
        active.setOpaque(false);
        formStatus.setForeground(HospitalTheme.WARNING);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(note("分类科室用于组织层级；只有没有下级的具体科室才能开放挂号。"));
        panel.add(Box.createVerticalStrut(20));
        panel.add(field("科室名称", name));
        panel.add(Box.createVerticalStrut(14));
        panel.add(field("上级科室", parent));
        panel.add(Box.createVerticalStrut(18));
        panel.add(bookable);
        panel.add(Box.createVerticalStrut(8));
        panel.add(active);
        panel.add(Box.createVerticalGlue());
        panel.add(formStatus);
        panel.add(Box.createVerticalStrut(8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        save.setName("saveDepartmentButton");
        clear.setVisible(false);
        actions.add(save);
        actions.add(clear);
        panel.add(actions);
        return panel;
    }

    private void configureActions() {
        retry.addActionListener(event -> load());
        clear.addActionListener(event -> clearEditor());
        save.addActionListener(event -> save());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { render(); }
            @Override public void removeUpdate(DocumentEvent event) { render(); }
            @Override public void changedUpdate(DocumentEvent event) { render(); }
        });
    }

    private void load() {
        if (busy) return;
        setBusy(true);
        status.setText("正在读取科室……");
        retry.setVisible(false);
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(HospitalActions.GET_ADMIN_DEPARTMENT_WORKSPACE, null);
            }

            @Override protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()
                            && response.getData() instanceof AdminDepartmentWorkspaceView data) {
                        departments = data.getDepartments();
                        rebuildParents();
                        render();
                        status.setText("共 " + departments.size() + " 个科室；修改后自动更新");
                    } else {
                        loadFailed(response.getMessage());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    loadFailed("读取已中断");
                } catch (ExecutionException exception) {
                    loadFailed("无法连接服务器");
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void render() {
        list.removeAll();
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        departments.stream()
                .filter(item -> query.isEmpty()
                        || item.getDepartmentName().toLowerCase(Locale.ROOT).contains(query))
                .forEach(item -> {
                    list.add(departmentCard(item));
                    list.add(Box.createVerticalStrut(9));
                });
        list.revalidate();
        list.repaint();
    }

    private JPanel departmentCard(AdminDepartmentView item) {
        HospitalTheme.SurfacePanel card = new HospitalTheme.SurfacePanel(
                HospitalTheme.SURFACE, 10, HospitalTheme.BORDER);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(BorderFactory.createEmptyBorder(13, 15, 13, 15));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        JPanel copy = verticalList();
        JLabel title = new JLabel(item.getDepartmentName()
                + (item.isActive() ? "" : "  ·  已停用"));
        title.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        title.setForeground(item.isActive() ? HospitalTheme.TEXT : HospitalTheme.MUTED);
        JLabel facts = new JLabel((item.getParentDepartmentName() == null
                ? "一级科室" : "上级：" + item.getParentDepartmentName())
                + "  ·  " + (item.isBookable() ? "可挂号" : "分类节点")
                + "  ·  医生 " + item.getDoctorCount()
                + "  ·  未来排班 " + item.getFutureScheduleCount());
        facts.setForeground(HospitalTheme.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(6));
        copy.add(facts);
        JButton edit = HospitalTheme.quietButton("编辑资料");
        edit.setName("adminDepartmentButton");
        edit.addActionListener(event -> select(item));
        card.add(copy, BorderLayout.CENTER);
        card.add(edit, BorderLayout.EAST);
        return card;
    }

    private void select(AdminDepartmentView item) {
        selected = item;
        name.setText(item.getDepartmentName());
        rebuildParents();
        selectParent(item.getParentDepartmentId());
        bookable.setSelected(item.isBookable());
        active.setSelected(item.isActive());
        active.setEnabled(true);
        save.setText("保存科室资料");
        clear.setVisible(true);
        formStatus.setText("正在编辑“" + item.getDepartmentName() + "”");
    }

    private void clearEditor() {
        selected = null;
        name.setText("");
        rebuildParents();
        bookable.setSelected(false);
        active.setSelected(true);
        active.setEnabled(false);
        save.setText("建立科室");
        clear.setVisible(false);
        formStatus.setText(" ");
    }

    private void rebuildParents() {
        String selectedParent = parent.getSelectedItem() instanceof ParentOption option
                ? option.id() : null;
        parent.removeAllItems();
        parent.addItem(new ParentOption(null, "无上级（一级科室）"));
        departments.stream()
                .filter(AdminDepartmentView::isActive)
                .filter(item -> !item.isBookable())
                .filter(item -> selected == null
                        || !item.getDepartmentId().equals(selected.getDepartmentId()))
                .forEach(item -> parent.addItem(new ParentOption(
                        item.getDepartmentId(), item.getDepartmentName())));
        selectParent(selectedParent);
    }

    private void selectParent(String id) {
        for (int index = 0; index < parent.getItemCount(); index++) {
            if (java.util.Objects.equals(parent.getItemAt(index).id(), id)) {
                parent.setSelectedIndex(index);
                return;
            }
        }
        parent.setSelectedIndex(0);
    }

    private void save() {
        if (busy) return;
        ParentOption option = (ParentOption) parent.getSelectedItem();
        Object request = selected == null
                ? new CreateDepartmentRequest(name.getText(), option.id(), bookable.isSelected())
                : new UpdateDepartmentRequest(
                        selected.getDepartmentId(), name.getText(), option.id(),
                        bookable.isSelected(), active.isSelected());
        String action = selected == null
                ? HospitalActions.CREATE_DEPARTMENT : HospitalActions.UPDATE_DEPARTMENT;
        setBusy(true);
        formStatus.setForeground(HospitalTheme.MUTED);
        formStatus.setText("正在保存科室资料……");
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return context.send(action, (java.io.Serializable) request);
            }

            @Override protected void done() {
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        clearEditor();
                        formStatus.setForeground(HospitalTheme.SUCCESS);
                        formStatus.setText("科室资料已保存，正在更新目录……");
                        setBusy(false);
                        load();
                        return;
                    }
                    formStatus.setForeground(HospitalTheme.WARNING);
                    formStatus.setText(failureText(response));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    formStatus.setText("保存已中断");
                } catch (ExecutionException exception) {
                    formStatus.setText("无法连接服务器");
                } finally {
                    if (busy) setBusy(false);
                }
            }
        }.execute();
    }

    private void loadFailed(String message) {
        departments = List.of();
        render();
        status.setText("读取失败：" + message);
        retry.setVisible(true);
    }

    private void setBusy(boolean value) {
        busy = value;
        save.setEnabled(!value);
        clear.setEnabled(!value);
        retry.setEnabled(!value);
    }

    private static String failureText(Response response) {
        return switch (response.getCode()) {
            case "HOSPITAL_DEPARTMENT_IN_USE" -> "请先处理下级科室、医生或已发布排班。";
            case "HOSPITAL_DEPARTMENT_CONFLICT" -> "科室层级或名称冲突，请调整后重试。";
            default -> "保存失败：" + response.getMessage();
        };
    }

    private static JPanel field(String labelText, javax.swing.JComponent component) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(HospitalTheme.uiFont(Font.BOLD, 13F));
        field.add(label, BorderLayout.NORTH);
        field.add(component, BorderLayout.CENTER);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        return field;
    }

    private static JLabel note(String text) {
        JLabel label = new JLabel("<html><body style='width:300px'>" + text + "</body></html>");
        label.setForeground(HospitalTheme.MUTED);
        return label;
    }

    private static JPanel verticalList() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private record ParentOption(String id, String label) {
        @Override public String toString() { return label; }
    }
}
