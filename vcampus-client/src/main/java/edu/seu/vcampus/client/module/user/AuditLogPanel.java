package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.UserAuditLogEntry;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/** Resizable audit browser with wrapped table text and complete, selectable details. */
final class AuditLogPanel extends JPanel {
    AuditLogPanel(List<UserAuditLogEntry> entries) {
        super(new BorderLayout(10, 12));
        setBackground(UserUiTheme.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JTextField search = new JTextField();
        search.setName("audit.search");
        UserUiTheme.styleSearchField(search);
        JPanel filters = new JPanel(new BorderLayout(12, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("搜索"), BorderLayout.WEST);
        search.setToolTipText("操作者、业务动作、表名或说明");
        filters.add(search, BorderLayout.CENTER);
        add(filters, BorderLayout.NORTH);

        DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        Object[][] rows = new Object[entries.size()][6];
        for (int index = 0; index < entries.size(); index++) {
            UserAuditLogEntry entry = entries.get(index);
            rows[index] = new Object[]{time.format(Instant.ofEpochMilli(entry.getOccurredAtEpochMillis())),
                    entry.getActorDisplayName() + "（" + entry.getActorUsername() + "）",
                    actionText(entry.getActionCode()), entry.getTarget(),
                    entry.isSuccessful() ? "成功" : "失败", entry.getDetail()};
        }
        DefaultTableModel model = new DefaultTableModel(rows,
                new String[]{"时间", "操作者", "操作", "目标表或对象", "结果", "说明"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model) {
            @Override public String getToolTipText(MouseEvent event) {
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                return row < 0 || column < 0 ? null : String.valueOf(getValueAt(row, column));
            }
        };
        table.setName("audit.table");
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = UserUiTheme.createTableScrollPane(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        int[] widths = {155, 170, 120, 175, 65, 340};
        int[] minimums = {145, 100, 80, 100, 50, 180};
        for (int column = 0; column < widths.length; column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
            table.getColumnModel().getColumn(column).setMinWidth(minimums[column]);
            table.getColumnModel().getColumn(column).setWidth(widths[column]);
        }
        table.getColumnModel().getColumn(5).setCellRenderer(new WrappedDetailRenderer());

        JTextArea detail = new JTextArea("未选择记录");
        detail.setName("audit.detail");
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(table.getFont().deriveFont(13F));
        detail.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setOpaque(false);
        detailPanel.add(new JLabel("完整记录"), BorderLayout.NORTH);
        detailPanel.add(new JScrollPane(detail), BorderLayout.CENTER);
        detailPanel.setMinimumSize(new Dimension(0, 130));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, detailPanel);
        split.setBorder(null);
        split.setResizeWeight(0.7);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(event -> {
            int selected = table.getSelectedRow();
            if (selected < 0) {
                detail.setText("未选择记录");
                return;
            }
            UserAuditLogEntry entry = entries.get(table.convertRowIndexToModel(selected));
            detail.setText("时间：" + time.format(Instant.ofEpochMilli(entry.getOccurredAtEpochMillis()))
                    + "\n操作者：" + entry.getActorDisplayName() + "（" + entry.getActorUsername() + "）"
                    + "\n操作：" + actionText(entry.getActionCode()) + " [" + entry.getActionCode() + "]"
                    + "\n目标：" + entry.getTarget() + "\n结果：" + (entry.isSuccessful() ? "成功" : "失败")
                    + "\n说明：" + entry.getDetail());
            detail.setCaretPosition(0);
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String query = search.getText().trim();
                sorter.setRowFilter(query.isEmpty() ? null
                        : RowFilter.regexFilter("(?i)" + Pattern.quote(query)));
            }
            @Override public void insertUpdate(DocumentEvent event) { update(); }
            @Override public void removeUpdate(DocumentEvent event) { update(); }
            @Override public void changedUpdate(DocumentEvent event) { update(); }
        });
        if (!entries.isEmpty()) { table.setRowSelectionInterval(0, 0); }
    }

    private static String actionText(String action) {
        return switch (action) {
            case "DATABASE.INSERT" -> "数据库新增";
            case "DATABASE.UPDATE" -> "数据库修改";
            case "DATABASE.DELETE" -> "数据库删除";
            case "DATABASE.CREATE" -> "建立数据表";
            case "DATABASE.ALTER" -> "调整数据表";
            case "DATABASE.DROP" -> "删除数据表";
            case "USER.ADMIN_CREATE_ACCOUNT", "USER.ADMIN_CREATE_GENERATED_ACCOUNT" -> "新增账号";
            case "USER.ADMIN_UPDATE_ACCOUNT" -> "编辑账号";
            case "USER.ADMIN_UPDATE_STATUS" -> "启用/禁用账号";
            case "USER.ADMIN_RESET_PASSWORD" -> "重置密码";
            case "USER.ADMIN_BATCH_CREATE_ACCOUNTS" -> "批量导入账号";
            case "USER.ADMIN_SAVE_TEACHER_PROFILE" -> "维护教师资格";
            case "USER.ADMIN_BATCH_SAVE_TEACHERS" -> "批量导入教师";
            case "USER.LOCAL_RESET_SUPER_ADMIN_PASSWORD" -> "本机紧急改密";
            case "DEMO_DATABASE_INITIALIZED" -> "初始化演示库";
            default -> action;
        };
    }

    private static final class WrappedDetailRenderer extends JTextArea implements TableCellRenderer {
        WrappedDetailRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            setText(String.valueOf(value));
            setFont(table.getFont());
            setForeground(UserUiTheme.TEXT);
            setBackground(selected ? UserUiTheme.PRIMARY_LIGHT : row % 2 == 0
                    ? UserUiTheme.SURFACE : new java.awt.Color(249, 251, 250));
            setSize(Math.max(1, table.getColumnModel().getColumn(column).getWidth()), Short.MAX_VALUE);
            int height = Math.max(36, getPreferredSize().height);
            if (table.getRowHeight(row) != height) { table.setRowHeight(row, height); }
            return this;
        }
    }
}
