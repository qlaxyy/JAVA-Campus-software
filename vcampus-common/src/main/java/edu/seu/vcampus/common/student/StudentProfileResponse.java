package edu.seu.vcampus.common.student;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学籍查询响应载荷
 */
public class StudentProfileResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean found;
    private StudentProfileDto profile;
    private String message;

    public StudentProfileResponse() {}

    public static StudentProfileResponse success(StudentProfileDto profile) {
        StudentProfileResponse resp = new StudentProfileResponse();
        resp.found = true;
        resp.profile = profile;
        return resp;
    }

    public static StudentProfileResponse notFound(String message) {
        StudentProfileResponse resp = new StudentProfileResponse();
        resp.found = false;
        resp.message = message;
        return resp;
    }

    public boolean isFound() { return found; }
    public StudentProfileDto getProfile() { return profile; }
    public String getMessage() { return message; }
}
