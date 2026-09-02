package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.SessionLookup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Authentication service with in-memory sessions and a pluggable account repository.
 */
public final class InMemoryAuthenticationService implements SessionLookup {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository users;
    private final ConcurrentMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /** Creates authentication backed by the public development accounts. */
    public InMemoryAuthenticationService() {
        this(DemoUserAccounts.createRepository());
    }

    InMemoryAuthenticationService(UserRepository users) {
        this.users = Objects.requireNonNull(users, "users must not be null");
    }

    UserRepository users() {
        return users;
    }

    /**
     * Authenticates an enabled account and creates a random session.
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
        UserAccount user = users.findByUsername(request.getUsername()).orElse(null);
        if (user == null
                || !user.enabled()
                || !proofMatches(user.passwordProof(), request.getPasswordProof())) {
            return Optional.empty();
        }

        SessionInfo session = new SessionInfo(
                createToken(),
                user.userId(),
                user.username(),
                user.displayName(),
                user.role(),
                user.adminScopes());
        sessions.put(session.getToken(), session);
        return Optional.of(session);
    }

    /**
     * Invalidates one session token.
     *
     * @param token token to remove
     * @return whether an active session was removed
     */
    public boolean logout(String token) {
        return token != null && sessions.remove(token) != null;
    }

    /** Invalidates every active session belonging to an account. */
    void invalidateUserSessions(String userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
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
}
