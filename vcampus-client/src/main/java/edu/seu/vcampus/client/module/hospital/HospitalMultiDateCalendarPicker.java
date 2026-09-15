package edu.seu.vcampus.client.module.hospital;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Month calendar that lets an administrator toggle several future work dates. */
final class HospitalMultiDateCalendarPicker extends JPanel {

    private static final Color SELECTED_FILL = new Color(198, 88, 24);
    private static final Color SELECTED_BORDER = new Color(151, 61, 13);
    private static final Color SELECTED_TINT = new Color(255, 241, 231);
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("yyyy年 M月", Locale.CHINA);
    private static final DateTimeFormatter SELECTED =
            DateTimeFormatter.ofPattern("M月d日", Locale.CHINA);
    private static final String[] WEEKDAYS = {"一", "二", "三", "四", "五", "六", "日"};

    private final LocalDate minimumDate;
    private final Consumer<Set<LocalDate>> onSelectionChanged;
    private final Set<LocalDate> selectedDates = new LinkedHashSet<>();
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel selectionLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel days = new JPanel(new GridLayout(0, 7, 4, 4));
    private YearMonth displayedMonth;

    HospitalMultiDateCalendarPicker(
            LocalDate initialDate,
            LocalDate minimumDate,
            Consumer<Set<LocalDate>> onSelectionChanged) {
        this.minimumDate = Objects.requireNonNull(minimumDate);
        LocalDate selected = Objects.requireNonNull(initialDate);
        if (!selected.isBefore(minimumDate)) {
            selectedDates.add(selected);
        }
        displayedMonth = YearMonth.from(selectedDates.isEmpty() ? minimumDate : selected);
        this.onSelectionChanged = Objects.requireNonNull(onSelectionChanged);

        setName("hospitalMultiDateCalendarPicker");
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 10, 12)));

        JPanel navigation = new JPanel(new BorderLayout(8, 0));
        navigation.setOpaque(false);
        JButton previous = navigationButton("‹");
        previous.setName("hospitalMultiCalendarPreviousMonth");
        previous.addActionListener(event -> {
            displayedMonth = displayedMonth.minusMonths(1);
            render();
        });
        JButton next = navigationButton("›");
        next.setName("hospitalMultiCalendarNextMonth");
        next.addActionListener(event -> {
            displayedMonth = displayedMonth.plusMonths(1);
            render();
        });
        monthLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 16F));
        monthLabel.setForeground(HospitalTheme.TEXT);
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(monthLabel, BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST);

        JPanel calendar = new JPanel();
        calendar.setOpaque(false);
        calendar.setLayout(new BoxLayout(calendar, BoxLayout.Y_AXIS));
        JPanel headers = new JPanel(new GridLayout(1, 7, 4, 0));
        headers.setOpaque(false);
        for (String weekday : WEEKDAYS) {
            JLabel label = new JLabel(weekday, SwingConstants.CENTER);
            label.setFont(HospitalTheme.uiFont(Font.BOLD, 11F));
            label.setForeground(HospitalTheme.MUTED);
            headers.add(label);
        }
        days.setOpaque(false);
        calendar.add(headers);
        calendar.add(days);

        selectionLabel.setName("hospitalMultiCalendarSelection");
        selectionLabel.setOpaque(true);
        selectionLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        selectionLabel.setForeground(SELECTED_BORDER);
        selectionLabel.setBackground(SELECTED_TINT);
        selectionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(238, 180, 142)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        add(navigation, BorderLayout.NORTH);
        add(calendar, BorderLayout.CENTER);
        add(selectionLabel, BorderLayout.SOUTH);
        render();
    }

    Set<LocalDate> getSelectedDates() {
        return Set.copyOf(selectedDates);
    }

    void setSelectedDates(Set<LocalDate> dates) {
        selectedDates.clear();
        Objects.requireNonNull(dates).stream()
                .filter(date -> !date.isBefore(minimumDate))
                .sorted()
                .forEach(selectedDates::add);
        if (!selectedDates.isEmpty()) {
            displayedMonth = YearMonth.from(selectedDates.iterator().next());
        }
        render();
        notifySelection();
    }

    private void render() {
        monthLabel.setText(displayedMonth.format(MONTH));
        if (selectedDates.isEmpty()) {
            selectionLabel.setText("请选择一个或多个出诊日期");
        } else if (selectedDates.size() <= 3) {
            selectionLabel.setText("已选 " + selectedDates.stream()
                    .sorted().map(SELECTED::format).reduce((left, right) ->
                            left + "、" + right).orElse(""));
        } else {
            selectionLabel.setText("已选 " + selectedDates.size() + " 个日期");
        }
        days.removeAll();
        LocalDate first = displayedMonth.atDay(1);
        int leading = first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int index = 0; index < leading; index++) {
            days.add(emptyDay());
        }
        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            days.add(dayButton(displayedMonth.atDay(day)));
        }
        int used = leading + displayedMonth.lengthOfMonth();
        int trailing = (7 - used % 7) % 7;
        for (int index = 0; index < trailing; index++) {
            days.add(emptyDay());
        }
        days.revalidate();
        days.repaint();
    }

    private JButton dayButton(LocalDate date) {
        JButton button = new JButton(String.valueOf(date.getDayOfMonth()));
        button.setName("hospitalMultiCalendarDay");
        button.setActionCommand(date.toString());
        button.setPreferredSize(new Dimension(38, 32));
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(HospitalTheme.dataFont(Font.BOLD, 12F));
        boolean allowed = !date.isBefore(minimumDate);
        boolean selected = selectedDates.contains(date);
        button.setEnabled(allowed);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        if (selected) {
            button.setBackground(SELECTED_FILL);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SELECTED_BORDER, 2),
                    BorderFactory.createEmptyBorder(5, 3, 5, 3)));
        } else {
            button.setBackground(HospitalTheme.SURFACE);
            button.setForeground(allowed ? HospitalTheme.TEXT : HospitalTheme.MUTED);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(HospitalTheme.BORDER),
                    BorderFactory.createEmptyBorder(6, 4, 6, 4)));
        }
        button.addActionListener(event -> {
            if (!selectedDates.add(date)) {
                selectedDates.remove(date);
            }
            render();
            notifySelection();
        });
        return button;
    }

    private void notifySelection() {
        onSelectionChanged.accept(getSelectedDates());
    }

    private static JButton navigationButton(String text) {
        JButton button = HospitalTheme.quietButton(text);
        button.setPreferredSize(new Dimension(42, 32));
        button.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        button.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
        return button;
    }

    private static JPanel emptyDay() {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setPreferredSize(new Dimension(38, 32));
        return empty;
    }
}
