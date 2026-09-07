package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.user.Role;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.EnumSet;
import java.util.Set;

/** Builds the small create/edit account dialog. */
final class UserAccountEditor {

    private UserAccountEditor() {
    }

    static UserAccountFormData showForCreate(Component parent, String generatedUsername) {
        return show(parent, null, generatedUsername);
    }

    static UserAccountFormData show(Component parent, UserAccountView existing) {
        return show(parent, existing, existing == null ? "" : existing.getUsername());
    }

    private static UserAccountFormData show(
            Component parent,
            UserAccountView existing,
            String generatedUsername) {
        boolean creating = existing == null;
        JTextField username = new JTextField(
                creating ? generatedUsername : existing.getUsername());
        JTextField displayName = new JTextField(creating ? "" : existing.getDisplayName());
        username.setEditable(false);

        Set<AdminScope> scopes = EnumSet.noneOf(AdminScope.class);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("一卡通号："));
        form.add(username);
        form.add(new JLabel("姓名："));
        form.add(displayName);
        form.add(new JLabel("子系统管理权："));
        form.add(new JLabel("请选择 0–1 个权限"));
        addScopeChoices(form, scopes, existing);
        if (creating) {
            form.add(new JLabel("初始密码统一为：123456"));
        }

        int result = JOptionPane.showConfirmDialog(
                parent, form, creating ? "新增账号" : "编辑账号",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new UserAccountFormData(username.getText(), displayName.getText(), scopes);
    }

    private static void addScopeChoices(
            JPanel form,
            Set<AdminScope> selectedScopes,
            UserAccountView existing) {
        ButtonGroup group = new ButtonGroup();
        boolean editable = existing == null || existing.getRole() != Role.SUPER_ADMIN;
        AdminScope existingScope = existing == null || existing.getRole() == Role.SUPER_ADMIN
                ? null
                : existing.getAdminScopes().stream().sorted().findFirst().orElse(null);
        JRadioButton none = new JRadioButton("无管理权限");
        none.setSelected(existingScope == null);
        none.setEnabled(editable);
        group.add(none);
        form.add(none);
        for (AdminScope scope : AdminScope.values()) {
            JRadioButton choice = new JRadioButton(UserAccountTableModel.scopeName(scope));
            choice.setSelected(scope == existingScope);
            choice.setEnabled(editable);
            choice.addActionListener(event -> {
                selectedScopes.clear();
                selectedScopes.add(scope);
            });
            group.add(choice);
            form.add(choice);
        }
        none.addActionListener(event -> selectedScopes.clear());
        if (existingScope != null) {
            selectedScopes.add(existingScope);
        }
    }
}
