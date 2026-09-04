package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 管理员查询指定学生已选课程的请求。
 */
public final class AdminListStudentEnrollmentsRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    /**
     * 学号。
     *
     * 当前演示数据示例：student001。
     */
    private final String studentId;

    public AdminListStudentEnrollmentsRequest(
        String studentId) {

        this.studentId =
            Objects.requireNonNull(
                studentId);
    }

    public String getStudentId() {

        return studentId;
    }
}
