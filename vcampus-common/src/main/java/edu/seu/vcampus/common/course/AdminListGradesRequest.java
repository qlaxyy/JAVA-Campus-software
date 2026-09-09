package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教务查询指定学生成绩请求。
 */
public final class AdminListGradesRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String studentId;

    public AdminListGradesRequest(
        String studentId) {

        this.studentId =
            Objects.requireNonNull(
                studentId);
    }

    public String getStudentId() {

        return studentId;
    }
}
