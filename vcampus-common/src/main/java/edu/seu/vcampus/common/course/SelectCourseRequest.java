package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学生选课请求。
 */
public final class SelectCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final long offeringId;

    public SelectCourseRequest(
        long batchId,
        long offeringId) {

        if (batchId <= 0) {
            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        if (offeringId <= 0) {
            throw new IllegalArgumentException(
                "offeringId must be positive");
        }

        this.batchId = batchId;
        this.offeringId = offeringId;
    }

    public long getBatchId() {
        return batchId;
    }

    public long getOfferingId() {
        return offeringId;
    }
}
