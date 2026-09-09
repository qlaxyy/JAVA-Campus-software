package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教学班成绩计算比例。
 */
public final class CourseGradePolicyInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    private final long offeringId;

    private final int usualWeightPercent;

    private final int finalExamWeightPercent;

    public CourseGradePolicyInfo(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent) {

        this.offeringId =
            offeringId;

        this.usualWeightPercent =
            usualWeightPercent;

        this.finalExamWeightPercent =
            finalExamWeightPercent;
    }

    public long getOfferingId() {

        return offeringId;
    }

    public int getUsualWeightPercent() {

        return usualWeightPercent;
    }

    public int getFinalExamWeightPercent() {

        return finalExamWeightPercent;
    }
}
