package edu.seu.vcampus.common.student;

import java.io.Serial;
import java.io.Serializable;

public class StudentProfileResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean found;
    private String message;
    private StudentProfileDto profile;

    public StudentProfileResponse() {}

    public StudentProfileResponse(boolean found, String message, StudentProfileDto profile) {
        this.found = found;
        this.message = message;
        this.profile = profile;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public StudentProfileDto getProfile() {
        return profile;
    }

    public void setProfile(StudentProfileDto profile) {
        this.profile = profile;
    }
}
