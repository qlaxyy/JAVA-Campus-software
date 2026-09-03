package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教学班的一条上课时间记录。
 */
public final class ScheduleInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int dayOfWeek;
    private final int startPeriod;
    private final int endPeriod;
    private final int startWeek;
    private final int endWeek;
    private final String weekPattern;

    public ScheduleInfo(
        int dayOfWeek,
        int startPeriod,
        int endPeriod,
        int startWeek,
        int endWeek,
        String weekPattern) {

        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.weekPattern = weekPattern;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public int getStartWeek() {
        return startWeek;
    }

    public int getEndWeek() {
        return endWeek;
    }

    public String getWeekPattern() {
        return weekPattern;
    }
}
