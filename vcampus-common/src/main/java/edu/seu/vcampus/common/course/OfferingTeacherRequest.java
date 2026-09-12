package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询教学班任课教师的请求。
 */
public final class OfferingTeacherRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long offeringId;

    public OfferingTeacherRequest(
        long offeringId) {

        if (offeringId <= 0) {

            throw new IllegalArgumentException(
                "offeringId must be positive");
        }

        this.offeringId =
            offeringId;
    }

    public long getOfferingId() {

        return offeringId;
    }
}
