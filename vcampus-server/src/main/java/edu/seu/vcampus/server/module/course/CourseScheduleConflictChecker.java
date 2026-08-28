package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.OfferingInfo;
import edu.seu.vcampus.common.course.ScheduleInfo;

import java.util.List;

/**
 * 课程时间冲突判断工具。
 */
final class CourseScheduleConflictChecker {

    /**
     * 判断目标教学班是否与任意已选教学班冲突。
     */
    boolean hasConflict(
        OfferingInfo targetOffering,
        List<OfferingInfo> selectedOfferings) {

        for (OfferingInfo selectedOffering
            : selectedOfferings) {

            /*
             * 自己和自己不比较。
             */
            if (targetOffering.getOfferingId()
                == selectedOffering.getOfferingId()) {

                continue;
            }

            if (offeringsConflict(
                targetOffering,
                selectedOffering)) {

                return true;
            }
        }

        return false;
    }

    /**
     * 两个教学班是否冲突。
     */
    private boolean offeringsConflict(
        OfferingInfo first,
        OfferingInfo second) {

        for (ScheduleInfo firstSchedule
            : first.getSchedules()) {

            for (ScheduleInfo secondSchedule
                : second.getSchedules()) {

                if (schedulesConflict(
                    firstSchedule,
                    secondSchedule)) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 两条上课时间是否冲突。
     */
    private boolean schedulesConflict(
        ScheduleInfo first,
        ScheduleInfo second) {

        /*
         * 星期不同。
         */
        if (first.getDayOfWeek()
            != second.getDayOfWeek()) {

            return false;
        }

        /*
         * 节次不重叠。
         */
        if (!periodsOverlap(
            first,
            second)) {

            return false;
        }

        /*
         * 判断实际教学周是否重叠。
         */
        return teachingWeeksOverlap(
            first,
            second);
    }

    /**
     * 节次是否存在交集。
     */
    private boolean periodsOverlap(
        ScheduleInfo first,
        ScheduleInfo second) {

        return first.getStartPeriod()
            <= second.getEndPeriod()
            &&
            second.getStartPeriod()
                <= first.getEndPeriod();
    }

    /**
     * 实际教学周是否存在交集。
     */
    private boolean teachingWeeksOverlap(
        ScheduleInfo first,
        ScheduleInfo second) {

        int startWeek =
            Math.max(
                first.getStartWeek(),
                second.getStartWeek());

        int endWeek =
            Math.min(
                first.getEndWeek(),
                second.getEndWeek());

        if (startWeek > endWeek) {
            return false;
        }

        for (int week = startWeek;
             week <= endWeek;
             week++) {

            if (isTeachingWeek(
                first,
                week)
                &&
                isTeachingWeek(
                    second,
                    week)) {

                return true;
            }
        }

        return false;
    }

    /**
     * 指定周是否实际有课。
     */
    private boolean isTeachingWeek(
        ScheduleInfo schedule,
        int week) {

        return switch (
            schedule.getWeekPattern()) {

            case "ODD" ->
                week % 2 == 1;

            case "EVEN" ->
                week % 2 == 0;

            default ->
                true;
        };
    }
}
