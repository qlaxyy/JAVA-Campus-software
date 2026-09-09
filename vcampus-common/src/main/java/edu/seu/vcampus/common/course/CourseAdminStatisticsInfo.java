package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教务端选课数据统计。
 */
public final class CourseAdminStatisticsInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final int courseCount;
    private final int offeringCount;
    private final int selectedCount;
    private final int totalCapacity;
    private final int remainingCount;
    private final int fullOfferingCount;
    private final int closedOfferingCount;
    private final double selectionRate;

    public CourseAdminStatisticsInfo(
        long batchId,
        int courseCount,
        int offeringCount,
        int selectedCount,
        int totalCapacity,
        int remainingCount,
        int fullOfferingCount,
        int closedOfferingCount,
        double selectionRate) {

        this.batchId =
            batchId;

        this.courseCount =
            courseCount;

        this.offeringCount =
            offeringCount;

        this.selectedCount =
            selectedCount;

        this.totalCapacity =
            totalCapacity;

        this.remainingCount =
            remainingCount;

        this.fullOfferingCount =
            fullOfferingCount;

        this.closedOfferingCount =
            closedOfferingCount;

        this.selectionRate =
            selectionRate;
    }

    public long getBatchId() {

        return batchId;
    }

    public int getCourseCount() {

        return courseCount;
    }

    public int getOfferingCount() {

        return offeringCount;
    }

    public int getSelectedCount() {

        return selectedCount;
    }

    public int getTotalCapacity() {

        return totalCapacity;
    }

    public int getRemainingCount() {

        return remainingCount;
    }

    public int getFullOfferingCount() {

        return fullOfferingCount;
    }

    public int getClosedOfferingCount() {

        return closedOfferingCount;
    }

    public double getSelectionRate() {

        return selectionRate;
    }
}
