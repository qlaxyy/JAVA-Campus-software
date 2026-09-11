package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.TeacherProfileView;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Table model for active teachers in the super-administrator workspace. */
final class TeacherProfileTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"一卡通号", "姓名", "院系", "职称"};
    private final List<TeacherProfileView> teachers = new ArrayList<>();

    void setTeachers(List<TeacherProfileView> values) {
        teachers.clear();
        teachers.addAll(values.stream()
                .filter(TeacherProfileView::isActive)
                .sorted(Comparator.comparing(TeacherProfileView::getCampusCardNumber))
                .toList());
        fireTableDataChanged();
    }

    TeacherProfileView teacherAt(int modelRow) {
        return teachers.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return teachers.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TeacherProfileView teacher = teachers.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> teacher.getCampusCardNumber();
            case 1 -> teacher.getDisplayName();
            case 2 -> teacher.getDepartment();
            case 3 -> teacher.getTitle();
            default -> "";
        };
    }
}
