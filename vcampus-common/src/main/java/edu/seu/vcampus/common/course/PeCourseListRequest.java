package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 体育课程列表请求。
 */
public final class PeCourseListRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final String sportProject;

    public PeCourseListRequest(
        long batchId,
        String sportProject) {

        if (batchId <= 0) {
            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        this.batchId = batchId;
        this.sportProject = normalize(sportProject);
    }

    public long getBatchId() {
        return batchId;
    }

    /**
     * @return 体育项目；null 表示全部
     */
    public String getSportProject() {
        return sportProject;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
