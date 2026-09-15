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
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/** Compact month calendar used by hospital scheduling forms. */
final class HospitalCalendarPicker extends JPanel {

    private static final Color SELECTED_FILL = new Color(198, 88, 24);
    private static final Color SELECTED_BORDER = new Color(151, 61, 13);
    private static final Color SELECTED_TINT = new Color(255, 241, 231);
    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("yyyy年 M月", Locale.CHINA);
    private static final String[] WEEKDAYS = {"一", "二", "三", "四", "五", "六", "日"};

    private final LocalDate minimumDate;
    private final Consumer<LocalDate> onSelectionChanged;
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel selectedLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel days = new JPanel(new GridLayout(0, 7, 4, 4));
    private LocalDate selectedDate;
    private YearMonth displayedMonth;

    HospitalCalendarPicker(
            LocalDate initialDate,
            LocalDate minimumDate,
            Consumer<LocalDate> onSelectionChanged) {
        this.minimumDate = Objects.requireNonNull(minimumDate);
        this.selectedDate = Objects.requireNonNull(initialDate).isBefore(minimumDate)
                ? minimumDate : initialDate;
        this.displayedMonth = YearMonth.from(this.selectedDate);
        this.onSelectionChanged = Objects.requireNonNull(onSelectionChanged);

        setName("hospitalCalendarPicker");
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HospitalTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 10, 12)));

        JPanel navigation = new JPanel(new BorderLayout(8, 0));
        navigation.setOpaque(false);
        JButton previous = calendarNavigationButton("‹");
        previous.setName("hospitalCalendarPreviousMonth");
        previous.addActionListener(event -> {
            displayedMonth = displayedMonth.minusMonths(1);
            render();
        });
        JButton next = calendarNavigationButton("›");
        next.setName("hospitalCalendarNextMonth");
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
        JPanel weekdayHeader = new JPanel(new GridLayout(1, 7, 4, 0));
        weekdayHeader.setOpaque(false);
        for (String weekday : WEEKDAYS) {
            JLabel label = new JLabel(weekday, SwingConstants.CENTER);
            label.setFont(HospitalTheme.uiFont(Font.BOLD, 11F));
            label.setForeground(HospitalTheme.MUTED);
            weekdayHeader.add(label);
        }
        days.setOpaque(false);
        calendar.add(weekdayHeader);
        calendar.add(days);

        selectedLabel.setName("hospitalCalendarSelectedDate");
        selectedLabel.setFont(HospitalTheme.uiFont(Font.BOLD, 12F));
        selectedLabel.setOpaque(true);
        selectedLabel.setForeground(SELECTED_BORDER);
        selectedLabel.setBackground(SELECTED_TINT);
        selectedLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(238, 180, 142)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        add(navigation, BorderLayout.NORTH);
        add(calendar, BorderLayout.CENTER);
        add(selectedLabel, BorderLayout.SOUTH);
        render();
    }

    LocalDate getSelectedDate() {
        return selectedDate;
    }

    void setSelectedDate(LocalDate date) {
        LocalDate allowed = Objects.requireNonNull(date).isBefore(minimumDate)
                ? minimumDate : date;
        selectedDate = allowed;
        displayedMonth = YearMonth.from(allowed);
        render();
        onSelectionChanged.accept(selectedDate);
    }

    private void render() {
        monthLabel.setText(displayedMonth.format(MONTH_FORMAT));
        selectedLabel.setText("已选 " + selectedDate.format(
                DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)));
        days.removeAll();
        LocalDate firstDay = displayedMonth.atDay(1);
        int leading = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int index = 0; index < leading; index++) {
            days.add(emptyDay());
        }
        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            LocalDate date = displayedMonth.atDay(day);
            days.add(dayButton(date));
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
        button.setName("hospitalCalendarDay");
        button.setActionCommand(date.toString());
        button.setPreferredSize(new Dimension(38, 32));
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(HospitalTheme.dataFont(Font.BOLD, 12F));
        button.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        boolean allowed = !date.isBefore(minimumDate);
        button.setEnabled(allowed);
        if (date.equals(selectedDate)) {
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBackground(SELECTED_FILL);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SELECTED_BORDER, 2),
                    BorderFactory.createEmptyBorder(5, 3, 5, 3)));
        } else {
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBackground(HospitalTheme.SURFACE);
            button.setForeground(allowed ? HospitalTheme.TEXT : HospitalTheme.MUTED);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(HospitalTheme.BORDER),
                    BorderFactory.createEmptyBorder(6, 4, 6, 4)));
        }
        button.addActionListener(event -> {
            selectedDate = date;
            render();
            onSelectionChanged.accept(date);
        });
        return button;
    }

    private static JButton calendarNavigationButton(String text) {
        JButton button = HospitalTheme.quietButton(text);
        button.setPreferredSize(new Dimension(42, 32));
        button.setFont(HospitalTheme.uiFont(Font.BOLD, 20F));
        button.setBorder(BorderFactory.createLineBorder(HospitalTheme.BORDER));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JPanel emptyDay() {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setPreferredSize(new Dimension(38, 32));
        return empty;
    }
}
