package edu.seu.vcampus.server.module;

import edu.seu.vcampus.server.security.SessionLookup;
import edu.seu.vcampus.server.security.AccountProvisioning;
import edu.seu.vcampus.server.security.UserDirectory;
import edu.seu.vcampus.server.security.TeacherDirectory;

import java.util.Objects;
import edu.seu.vcampus.server.module.course.TeacherStudentAccess;
/**
 * Shared server services exposed to business modules without coupling their DAOs.
 */
public final class ServerContext {

    private final SessionLookup sessions;
    private final UserDirectory users;
    private final AccountProvisioning accounts;
    private final TeacherDirectory teachers;
    private final TeacherStudentAccess
        teacherStudentAccess;
    private static final UserDirectory EMPTY_USER_DIRECTORY = new UserDirectory() {
        @Override
        public java.util.Optional<edu.seu.vcampus.server.security.UserIdentity>
                findByUserId(String userId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<edu.seu.vcampus.server.security.UserIdentity>
                findByCampusCardNumber(String campusCardNumber) {
            return java.util.Optional.empty();
        }
    };
    private static final TeacherDirectory EMPTY_TEACHER_DIRECTORY = new TeacherDirectory() {
        @Override
        public java.util.Optional<edu.seu.vcampus.server.security.TeacherIdentity>
                findByUserId(String userId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<edu.seu.vcampus.server.security.TeacherIdentity>
                findActiveTeachers() {
            return java.util.List.of();
        }
    };
    private static final TeacherStudentAccess
        EMPTY_TEACHER_STUDENT_ACCESS =
        (teacherUserId, studentId) -> false;
    /**
     * Creates a server module context.
     *
     * @param sessions read-only session lookup
     */
    public ServerContext(SessionLookup sessions) {
        this(sessions, EMPTY_USER_DIRECTORY, new AccountProvisioning() {
            @Override
            public java.util.Optional<edu.seu.vcampus.server.security.ProvisionedAccount>
                    findAccountByUsername(String username) {
                return java.util.Optional.empty();
            }

            @Override
            public edu.seu.vcampus.server.security.ProvisionedAccount
                    createGeneratedRegularAccount(String displayName) {
                throw new IllegalStateException("Account provisioning is unavailable.");
            }
        }, EMPTY_TEACHER_DIRECTORY);
    }

    /** Creates a context with session lookup and approved account provisioning. */
    public ServerContext(SessionLookup sessions, AccountProvisioning accounts) {
        this(sessions,
                accounts instanceof UserDirectory directory
                        ? directory : EMPTY_USER_DIRECTORY,
                accounts,
                EMPTY_TEACHER_DIRECTORY);
    }

    /** Creates a context with session, read-only identity, and provisioning services. */
    public ServerContext(
            SessionLookup sessions,
            UserDirectory users,
            AccountProvisioning accounts) {
        this(sessions, users, accounts, EMPTY_TEACHER_DIRECTORY);
    }

    /** Creates a context with all shared account and teacher services. */
    public ServerContext(
        SessionLookup sessions,
        UserDirectory users,
        AccountProvisioning accounts,
        TeacherDirectory teachers) {

        this(
            sessions,
            users,
            accounts,
            teachers,
            EMPTY_TEACHER_STUDENT_ACCESS);
    }

    /**
     * 创建包含教师学籍查看范围的服务器上下文。
     */
    public ServerContext(
        SessionLookup sessions,
        UserDirectory users,
        AccountProvisioning accounts,
        TeacherDirectory teachers,
        TeacherStudentAccess teacherStudentAccess) {

        this.sessions =
            Objects.requireNonNull(
                sessions,
                "sessions must not be null");

        this.users =
            Objects.requireNonNull(
                users,
                "users must not be null");

        this.accounts =
            Objects.requireNonNull(
                accounts,
                "accounts must not be null");

        this.teachers =
            Objects.requireNonNull(
                teachers,
                "teachers must not be null");

        this.teacherStudentAccess =
            Objects.requireNonNull(
                teacherStudentAccess,
                "teacherStudentAccess must not be null");
    }

    /** @return read-only session lookup shared by all modules */
    public SessionLookup sessions() {
        return sessions;
    }

    /** @return read-only basic identity directory shared by server modules */
    public UserDirectory users() {
        return users;
    }

    /** @return restricted account provisioning used after super-admin approval */
    public AccountProvisioning accounts() {
        return accounts;
    }

    /** @return read-only active teacher directory shared by server modules */
    public TeacherDirectory teachers() {
        return teachers;
    }
    /**
     * 返回教师可以查看的学生范围。
     */
    public TeacherStudentAccess teacherStudentAccess() {

        return teacherStudentAccess;
    }
}
