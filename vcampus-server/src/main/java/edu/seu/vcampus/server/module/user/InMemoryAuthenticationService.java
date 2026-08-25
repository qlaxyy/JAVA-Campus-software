package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
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
                user.role());
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
        return Map.of(
                "student001", demoUser("U-STUDENT-001", "演示学生", Role.STUDENT, "Student@123"),
                "teacher001", demoUser("U-TEACHER-001", "演示教师", Role.TEACHER, "Teacher@123"),
                "admin", demoUser("U-ADMIN-001", "演示管理员", Role.ADMIN, "Admin@123"));
    }

    private static DemoUser demoUser(
            String userId,
            String displayName,
            Role role,
            String passwordText) {
        char[] password = passwordText.toCharArray();
        try {
            String username = switch (role) {
                case STUDENT -> "student001";
                case TEACHER -> "teacher001";
                case ADMIN -> "admin";
            };
            return new DemoUser(
                    userId,
                    displayName,
                    role,
                    PasswordProof.create(username, password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private record DemoUser(String userId, String displayName, Role role, String passwordProof) {
    }
}
