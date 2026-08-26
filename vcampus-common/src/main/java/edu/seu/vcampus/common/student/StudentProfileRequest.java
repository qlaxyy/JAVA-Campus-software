package edu.seu.vcampus.common.student;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学籍查询请求载荷
 */
public class StudentProfileRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String studentId;

    public StudentProfileRequest() {}

    public StudentProfileRequest(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}
