package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.user.Role;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;

/** Builds the small create/edit account dialog. */
final class UserAccountEditor {

    private UserAccountEditor() {
    }

    static UserAccountFormData show(Component parent, UserAccountView existing) {
        boolean creating = existing == null;
        JTextField username = new JTextField(creating ? "" : existing.getUsername());
        JTextField displayName = new JTextField(creating ? "" : existing.getDisplayName());
        username.setEnabled(creating);

        Map<AdminScope, JCheckBox> boxes = new EnumMap<>(AdminScope.class);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("账户名（小写字母、数字或下划线）："));
        form.add(username);
        form.add(new JLabel("显示名称："));
        form.add(displayName);
        form.add(new JLabel("子系统管理权："));
        addScopeBoxes(form, boxes, existing);
        if (creating) {
            form.add(new JLabel("初始密码统一为：123456"));
        }

        int result = JOptionPane.showConfirmDialog(
                parent, form, creating ? "新增账号" : "编辑账号",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        Set<AdminScope> scopes = EnumSet.noneOf(AdminScope.class);
        boxes.forEach((scope, box) -> {
            if (box.isSelected()) {
                scopes.add(scope);
            }
        });
        return new UserAccountFormData(username.getText(), displayName.getText(), scopes);
    }

    private static void addScopeBoxes(
            JPanel form,
            Map<AdminScope, JCheckBox> boxes,
            UserAccountView existing) {
        for (AdminScope scope : AdminScope.values()) {
            JCheckBox box = new JCheckBox(UserAccountTableModel.scopeName(scope));
            box.setSelected(existing != null && existing.getAdminScopes().contains(scope));
            box.setEnabled(existing == null || existing.getRole() != Role.SUPER_ADMIN);
            boxes.put(scope, box);
            form.add(box);
        }
    }
}
