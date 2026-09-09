package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教师修改教学班成绩比例请求。
 */
public final class TeacherUpdateGradePolicyRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    private final long offeringId;

    private final int usualWeightPercent;

    private final int finalExamWeightPercent;

    private final String reason;

    public TeacherUpdateGradePolicyRequest(
        long offeringId,
        int usualWeightPercent,
        int finalExamWeightPercent,
        String reason) {

        this.offeringId =
            offeringId;

        this.usualWeightPercent =
            usualWeightPercent;

        this.finalExamWeightPercent =
            finalExamWeightPercent;

        this.reason =
            reason == null
                ? ""
                : reason;
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

    public String getReason() {

        return reason;
    }
}
