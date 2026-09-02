package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 需要指定选课批次的通用请求。
 */
public final class BatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;

    public BatchRequest(long batchId) {
        if (batchId <= 0) {
            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        this.batchId = batchId;
    }

    public long getBatchId() {
        return batchId;
    }
}
