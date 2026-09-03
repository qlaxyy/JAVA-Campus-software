package edu.seu.vcampus.common.student;

import java.io.Serial;
import java.io.Serializable;

public class StudentUpdateProfileRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String politicalStatus;
    private String phone;
    private String email;
    private String homeAddress;
    private String emergencyContact;
    private String emergencyPhone;

    public StudentUpdateProfileRequest() {}

    public StudentUpdateProfileRequest(String studentId, String politicalStatus, String phone,
                                       String email, String homeAddress, String emergencyContact,
                                       String emergencyPhone) {
        this.studentId = studentId;
        this.politicalStatus = politicalStatus;
        this.phone = phone;
        this.email = email;
        this.homeAddress = homeAddress;
        this.emergencyContact = emergencyContact;
        this.emergencyPhone = emergencyPhone;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPoliticalStatus() { return politicalStatus; }
    public void setPoliticalStatus(String politicalStatus) { this.politicalStatus = politicalStatus; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHomeAddress() { return homeAddress; }
    public void setHomeAddress(String homeAddress) { this.homeAddress = homeAddress; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
}
