package edu.seu.vcampus.common.student;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学生学籍档案传输对象
 */
public class StudentProfileDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String name;
    private String gender;
    private String idCard;
    private String birthDate;
    private String ethnicity;
    private String nativePlace;
    private String politicalStatus;
    private String department;
    private String major;
    private String className;
    private String enrollmentYear;
    private String educationLevel;
    private String status;

    public StudentProfileDto() {}

    public StudentProfileDto(String studentId, String name, String gender, String idCard,
                             String birthDate, String ethnicity, String nativePlace,
                             String politicalStatus, String department, String major,
                             String className, String enrollmentYear, String educationLevel,
                             String status) {
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
    }

    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getIdCard() { return idCard; }
    public String getBirthDate() { return birthDate; }
    public String getEthnicity() { return ethnicity; }
    public String getNativePlace() { return nativePlace; }
    public String getPoliticalStatus() { return politicalStatus; }
    public String getDepartment() { return department; }
    public String getMajor() { return major; }
    public String getClassName() { return className; }
    public String getEnrollmentYear() { return enrollmentYear; }
    public String getEducationLevel() { return educationLevel; }
    public String getStatus() { return status; }
}
