package edu.seu.vcampus.common.student;

import java.io.Serial;
import java.io.Serializable;

public class StudentProfileDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 核心只读学籍字段
    private String studentId;
    private String name;
    private String gender;
    private String idCard;
    private String birthDate;
    private String ethnicity;
    private String nativePlace;
    private String department;
    private String major;
    private String className;
    private String enrollmentYear;
    private String educationLevel;
    private String status;

    // 可自主维护的补充信息字段
    private String politicalStatus;
    private String phone;
    private String email;
    private String homeAddress;
    private String emergencyContact;
    private String emergencyPhone;

    public StudentProfileDto() {}

    public StudentProfileDto(String studentId, String name, String gender, String idCard,
                             String birthDate, String ethnicity, String nativePlace,
                             String politicalStatus, String department, String major,
                             String className, String enrollmentYear, String educationLevel,
                             String status, String phone, String email, String homeAddress,
                             String emergencyContact, String emergencyPhone) {
        this.studentId = studentId;
        this.name = name;
        this.gender = gender;
        this.idCard = idCard;
        this.birthDate = birthDate;
        this.ethnicity = ethnicity;
        this.nativePlace = nativePlace;
        this.politicalStatus = politicalStatus;
        this.department = department;
        this.major = major;
        this.className = className;
        this.enrollmentYear = enrollmentYear;
        this.educationLevel = educationLevel;
        this.status = status;
        this.phone = phone;
        this.email = email;
        this.homeAddress = homeAddress;
        this.emergencyContact = emergencyContact;
        this.emergencyPhone = emergencyPhone;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getEthnicity() { return ethnicity; }
    public void setEthnicity(String ethnicity) { this.ethnicity = ethnicity; }

    public String getNativePlace() { return nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }

    public String getPoliticalStatus() { return politicalStatus; }
    public void setPoliticalStatus(String politicalStatus) { this.politicalStatus = politicalStatus; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(String enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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
