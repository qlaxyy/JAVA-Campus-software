package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.SessionLookup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Temporary in-memory demo authentication used before the Access DAO is integrated.
 */
public final class InMemoryAuthenticationService implements SessionLookup {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, DemoUser> users = createDemoUsers();
    private final ConcurrentMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * Authenticates a demo account and creates a random session.
     *
     * @param request validated login request
     * @return new session when credentials match
     */
    public Optional<SessionInfo> login(LoginRequest request) {
        if (request == null
                || request.getUsername() == null
                || request.getPasswordProof() == null
                || !request.getPasswordProof().matches("[0-9a-f]{64}")) {
            return Optional.empty();
        }
        DemoUser user = users.get(request.getUsername());
        if (user == null || !proofMatches(user.passwordProof(), request.getPasswordProof())) {
            return Optional.empty();
        }

        SessionInfo session = new SessionInfo(
                createToken(),
                user.userId(),
                request.getUsername(),
                user.displayName(),
                user.role(),
                user.adminScopes());
        sessions.put(session.getToken(), session);
        return Optional.of(session);
    }

    /**
     * Invalidates a session token.
     *
     * @param token token to remove
     * @return {@code true} when an active session was removed
     */
    public boolean logout(String token) {
        return token != null && sessions.remove(token) != null;
    }

    @Override
    public Optional<SessionInfo> findSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    private String createToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private static boolean proofMatches(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private static Map<String, DemoUser> createDemoUsers() {
        return Map.ofEntries(
                Map.entry("student001", demoUser(
                        "U-STUDENT-001", "student001", "演示学生",
                        Role.STUDENT, Set.of(), "123456")),
                Map.entry("teacher001", demoUser(
                        "U-TEACHER-001", "teacher001", "演示教师",
                        Role.TEACHER, Set.of(), "123456")),
                Map.entry("admin", demoUser(
                        "U-ADMIN-001", "admin", "演示超级管理员",
                        Role.SUPER_ADMIN, Set.of(AdminScope.values()), "123456")),
                Map.entry("studentadmin", demoUser(
                        "U-STUDENT-ADMIN-001", "studentadmin", "演示学籍管理员",
                        Role.STUDENT, Set.of(AdminScope.STUDENT), "123456")),
                Map.entry("courseadmin", demoUser(
                        "U-COURSE-ADMIN-001", "courseadmin", "演示选课管理员",
                        Role.STUDENT, Set.of(AdminScope.COURSE), "123456")),
                Map.entry("libraryadmin", demoUser(
                        "U-LIBRARY-ADMIN-001", "libraryadmin", "演示图书馆管理员",
                        Role.STUDENT, Set.of(AdminScope.LIBRARY), "123456")),
                Map.entry("shopadmin", demoUser(
                        "U-SHOP-ADMIN-001", "shopadmin", "演示商店管理员",
                        Role.STUDENT, Set.of(AdminScope.SHOP), "123456")),
                Map.entry("hospitaladmin", demoUser(
                        "U-HOSPITAL-ADMIN-001", "hospitaladmin", "演示医院管理员",
                        Role.STUDENT, Set.of(AdminScope.HOSPITAL), "123456")));
    }

    private static DemoUser demoUser(
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> adminScopes,
            String passwordText) {
        char[] password = passwordText.toCharArray();
        try {
            return new DemoUser(
                    userId,
                    displayName,
                    role,
                    adminScopes,
                    PasswordProof.create(username, password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private record DemoUser(
            String userId,
            String displayName,
            Role role,
            Set<AdminScope> adminScopes,
            String passwordProof) {
    }
}
