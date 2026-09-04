package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.user.Role;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Table model for the super-administrator account list. */
final class UserAccountTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
        "一卡通号", "姓名（显示名称）", "状态", "全局角色", "管理范围"
    };
    private final List<UserAccountView> accounts = new ArrayList<>();

    void setAccounts(List<UserAccountView> values) {
        accounts.clear();
        accounts.addAll(values.stream()
                .sorted(Comparator.comparing(UserAccountView::getUsername))
                .toList());
        fireTableDataChanged();
    }

    UserAccountView accountAt(int modelRow) {
        return accounts.get(modelRow);
    }

    @Override
    public int getRowCount() { return accounts.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return valueAt(accounts.get(rowIndex), columnIndex);
    }

    private static Object valueAt(UserAccountView account, int columnIndex) {
        return switch (columnIndex) {
            case 0 -> account.getUsername();
            case 1 -> account.getDisplayName();
            case 2 -> account.isEnabled() ? "启用" : "禁用";
            case 3 -> account.getRole() == Role.SUPER_ADMIN ? "超级管理员" : "普通账号";
            case 4 -> scopeText(account);
            default -> "";
        };
    }

    private static String scopeText(UserAccountView account) {
        if (account.getRole() == Role.SUPER_ADMIN) {
            return "全部";
        }
        if (account.getAdminScopes().isEmpty()) {
            return "无";
        }
        return account.getAdminScopes().stream()
                .sorted()
                .map(UserAccountTableModel::scopeName)
                .collect(Collectors.joining("、"));
    }

    static String scopeName(AdminScope scope) {
        return switch (scope) {
            case STUDENT -> "学籍";
            case COURSE -> "选课";
            case LIBRARY -> "图书馆";
            case SHOP -> "商店";
            case HOSPITAL -> "医院";
        };
    }
}
