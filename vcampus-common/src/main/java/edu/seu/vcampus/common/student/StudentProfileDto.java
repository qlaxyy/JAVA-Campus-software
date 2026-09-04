package edu.seu.vcampus.common.student;

import java.io.Serializable;

public final class StudentProfileDto implements Serializable {
    private static final long serialVersionUID = 1L;

    // 核心身份与学籍字段（只读）
    private Long id;                  // 物理主键，对应选课模块 tblEnrollment.studentId
    private String studentId;         // 业务学号，如 student001
    private String name;              // 姓名
    private String gender;            // 性别 (男/女 或 MALE/FEMALE)
    private String ethnicity;         // 民族
    private String nativePlace;       // 籍贯
    private String idCardNumber;      // 身份证号
    private String birthDate;         // 出生日期
    private String enrollmentDate;    // 入学日期
    private Integer enrollmentYear;   // 入学年份
    private String department;        // 院系
    private String major;             // 专业
    private String className;         // 班级
    private Integer schoolingLength;  // 学制年限
    private String academicStatus;    // 学籍状态

    // 选课模块联调新字段
    private Long planId;              // 培养方案标识
    private Integer currentTerm;      // 当前修读建议学期 (1-8)
    private Long campusId;            // 校区标识 (1: 九龙湖, 2: 四牌楼, 3: 丁家桥)

    // 学生自维联络与补充档案（支持编辑）
    private String politicalStatus;   // 政治面貌
    private String phone;             // 联系电话
    private String email;             // 电子邮箱
    private String homeAddress;       // 家庭住址
    private String emergencyContact;  // 紧急联系人
    private String emergencyPhone;    // 紧急联系人电话

    public StudentProfileDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEthnicity() { return ethnicity; }
    public void setEthnicity(String ethnicity) { this.ethnicity = ethnicity; }

    public String getNativePlace() { return nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }

    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(String enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public Integer getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(Integer enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Integer getSchoolingLength() { return schoolingLength; }
    public void setSchoolingLength(Integer schoolingLength) { this.schoolingLength = schoolingLength; }

    public String getAcademicStatus() { return academicStatus; }
    public void setAcademicStatus(String academicStatus) { this.academicStatus = academicStatus; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Integer getCurrentTerm() { return currentTerm; }
    public void setCurrentTerm(Integer currentTerm) { this.currentTerm = currentTerm; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

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
