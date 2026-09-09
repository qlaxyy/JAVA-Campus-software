package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 教师查询指定教学班学生名单的请求。
 */
public final class TeacherListStudentsRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    /**
     * 选课批次 ID。
     */
    private final long batchId;

    /**
     * 教学班 ID。
     */
    private final long offeringId;

    public TeacherListStudentsRequest(
        long batchId,
        long offeringId) {

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
