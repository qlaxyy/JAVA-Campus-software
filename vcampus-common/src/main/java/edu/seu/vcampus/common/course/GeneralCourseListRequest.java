package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通选课程列表请求。
 */
public final class GeneralCourseListRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final String generalCategory;

    public GeneralCourseListRequest(
        long batchId,
        String generalCategory) {

        if (batchId <= 0) {
            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        this.batchId = batchId;
        this.generalCategory = normalize(generalCategory);
    }

    public long getBatchId() {
        return batchId;
    }

    /**
     * @return 通选课类别；null 表示全部
     */
    public String getGeneralCategory() {
        return generalCategory;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
