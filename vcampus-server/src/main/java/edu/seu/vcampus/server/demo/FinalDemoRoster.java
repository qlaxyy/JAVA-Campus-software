package edu.seu.vcampus.server.demo;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;

import java.util.List;
import java.util.Set;

/**
 * Single source of truth for the final multi-client demonstration identities.
 *
 * <p>The campus-card number is the login name shown to users. The user id is an
 * internal immutable key shared by server-side modules.
 */
public final class FinalDemoRoster {

    public static final String INITIAL_PASSWORD = "123456";
    public static final String SUPER_ADMIN_USER_ID = "U-ADMIN-001";

    private static final List<AccountSeed> ACCOUNTS = List.of(
            account("U-ADMIN-001", "20260000", "超级管理员", Role.SUPER_ADMIN,
                    Set.of(AdminScope.STUDENT, AdminScope.COURSE, AdminScope.LIBRARY,
                            AdminScope.SHOP, AdminScope.HOSPITAL)),
            account("U-STUDENT-ADMIN-001", "20260001", "学籍管理员", Role.USER,
                    Set.of(AdminScope.STUDENT)),
            account("U-COURSE-ADMIN-001", "20260002", "选课管理员", Role.USER,
                    Set.of(AdminScope.COURSE)),
            account("U-LIBRARY-ADMIN-001", "20260003", "图书馆管理员", Role.USER,
                    Set.of(AdminScope.LIBRARY)),
            account("U-SHOP-ADMIN-001", "20260004", "商店管理员", Role.USER,
                    Set.of(AdminScope.SHOP)),
            account("U-HOSPITAL-ADMIN-001", "20260005", "医院管理员", Role.USER,
                    Set.of(AdminScope.HOSPITAL)),
            account("U-STUDENT-001", "20260006", "吴尚扬", Role.USER, Set.of()),
            account("U-STUDENT-002", "20260007", "施天琦", Role.USER, Set.of()),
            account("U-STUDENT-003", "20260008", "杨凯涵", Role.USER, Set.of()),
            account("U-STUDENT-004", "20260009", "吴昊哲", Role.USER, Set.of()),
            account("U-STUDENT-005", "20260010", "葛丰玮", Role.USER, Set.of()),
            account("U-STUDENT-006", "20260011", "廖俊杰", Role.USER, Set.of()),
            account("U-STUDENT-007", "20260012", "周一", Role.USER, Set.of()),
            account("U-STUDENT-008", "20260013", "周二", Role.USER, Set.of()),
            account("U-STUDENT-009", "20260014", "周三", Role.USER, Set.of()),
            account("U-STUDENT-010", "20260015", "周四", Role.USER, Set.of()),
            account("U-STUDENT-011", "20260016", "周五", Role.USER, Set.of()),
            account("U-STUDENT-012", "20260017", "周六", Role.USER, Set.of()),
            account("U-STUDENT-013", "20260018", "周七", Role.USER, Set.of()),
            account("U-STUDENT-014", "20260019", "周八", Role.USER, Set.of()),
            account("U-STUDENT-015", "20260020", "周九", Role.USER, Set.of()),
            account("U-TEACHER-001", "20260021", "王建国", Role.USER, Set.of()),
            account("U-TEACHER-002", "20260022", "李静", Role.USER, Set.of()),
            account("U-TEACHER-003", "20260023", "陈立", Role.USER, Set.of()),
            account("U-TEACHER-004", "20260024", "赵敏", Role.USER, Set.of()),
            account("U-TEACHER-005", "20260025", "周航", Role.USER, Set.of()),
            account("U-TEACHER-006", "20260026", "孙晓", Role.USER, Set.of()),
            account("U-TEACHER-007", "20260027", "郑文杰", Role.USER, Set.of()),
            account("U-TEACHER-008", "20260028", "许清", Role.USER, Set.of()),
            account("U-DOCTOR-001", "20260029", "陈安", Role.USER, Set.of()),
            account("U-DOCTOR-002", "20260030", "刘宁", Role.USER, Set.of()),
            account("U-DOCTOR-003", "20260031", "周岚", Role.USER, Set.of()),
            account("U-DOCTOR-004", "20260032", "何远", Role.USER, Set.of()),
            account("U-DOCTOR-005", "20260033", "王清", Role.USER, Set.of()),
            account("U-DOCTOR-006", "20260034", "赵健", Role.USER, Set.of()),
            account("U-DOCTOR-007", "20260035", "孙悦", Role.USER, Set.of()),
            account("U-DOCTOR-008", "20260036", "林川", Role.USER, Set.of()),
            account("U-DOCTOR-009", "20260037", "钱宁", Role.USER, Set.of()),
            account("U-DOCTOR-010", "20260038", "吴凡", Role.USER, Set.of()));

    private static final List<TeacherSeed> TEACHERS = List.of(
            teacher("U-TEACHER-001", "计算机学院", "教授"),
            teacher("U-TEACHER-002", "计算机学院", "副教授"),
            teacher("U-TEACHER-003", "数学学院", "讲师"),
            teacher("U-TEACHER-004", "外国语学院", "副教授"),
            teacher("U-TEACHER-005", "体育系", "讲师"),
            teacher("U-TEACHER-006", "人文学院", "讲师"),
            teacher("U-TEACHER-007", "物理学院", "副教授"),
            teacher("U-TEACHER-008", "电子学院", "讲师"));

    private static final List<DoctorSeed> DOCTORS = List.of(
            doctor("doctor-chen", "U-DOCTOR-001", "dept-general", "主治医师"),
            doctor("doctor-liu", "U-DOCTOR-002", "dept-respiratory", "副主任医师"),
            doctor("doctor-zhou", "U-DOCTOR-003", "dept-gastroenterology", "主治医师"),
            doctor("doctor-qian", "U-DOCTOR-004", "dept-joint-surgery", "副主任医师"),
            doctor("doctor-lin", "U-DOCTOR-005", "dept-sports-medicine", "主治医师"),
            doctor("doctor-he", "U-DOCTOR-006", "dept-psychology", "医师"),
            doctor("doctor-wu", "U-DOCTOR-007", "dept-dental", "主治医师"),
            doctor("doctor-zhang", "U-DOCTOR-008", "dept-eye", "主治医师"),
            doctor("doctor-wang", "U-DOCTOR-009", "dept-general", "医师"),
            doctor("doctor-zhao", "U-DOCTOR-010", "dept-respiratory", "医师"));

    private static final List<TeachingAssignmentSeed> TEACHING_ASSIGNMENTS = List.of(
            teachingAssignment(1001L, "U-TEACHER-003"),
            teachingAssignment(2001L, "U-TEACHER-007"),
            teachingAssignment(3001L, "U-TEACHER-001"),
            teachingAssignment(4001L, "U-TEACHER-002"),
            teachingAssignment(6001L, "U-TEACHER-008"),
            teachingAssignment(14001L, "U-TEACHER-005"),
            teachingAssignment(15001L, "U-TEACHER-006"),
            teachingAssignment(15002L, "U-TEACHER-004"));

    private FinalDemoRoster() {
    }

    public static List<AccountSeed> accounts() {
        return ACCOUNTS;
    }

    public static List<TeacherSeed> teachers() {
        return TEACHERS;
    }

    public static List<DoctorSeed> doctors() {
        return DOCTORS;
    }

    public static List<TeachingAssignmentSeed> teachingAssignments() {
        return TEACHING_ASSIGNMENTS;
    }

    public static List<AccountSeed> students() {
        return ACCOUNTS.stream()
                .filter(account -> account.userId().startsWith("U-STUDENT-")
                        && !account.userId().contains("ADMIN"))
                .toList();
    }

    public static String displayName(String userId) {
        return ACCOUNTS.stream()
                .filter(account -> account.userId().equals(userId))
                .map(AccountSeed::displayName)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown demo user: " + userId));
    }

    private static AccountSeed account(
            String userId, String cardNumber, String displayName,
            Role role, Set<AdminScope> scopes) {
        return new AccountSeed(userId, cardNumber, displayName, role, Set.copyOf(scopes));
    }

    private static TeacherSeed teacher(String userId, String department, String title) {
        return new TeacherSeed(userId, department, title);
    }

    private static DoctorSeed doctor(
            String doctorId, String userId, String departmentId, String title) {
        return new DoctorSeed(doctorId, userId, departmentId, title);
    }

    private static TeachingAssignmentSeed teachingAssignment(long offeringId, String teacherUserId) {
        return new TeachingAssignmentSeed(offeringId, teacherUserId);
    }

    public record AccountSeed(
            String userId,
            String campusCardNumber,
            String displayName,
            Role role,
            Set<AdminScope> adminScopes) {
    }

    public record TeacherSeed(String userId, String department, String title) {
    }

    public record DoctorSeed(
            String doctorId, String userId, String departmentId, String title) {
    }

    public record TeachingAssignmentSeed(long offeringId, String teacherUserId) {
    }
}
