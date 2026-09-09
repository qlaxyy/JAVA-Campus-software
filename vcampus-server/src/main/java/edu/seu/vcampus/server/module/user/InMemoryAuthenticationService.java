package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.CampusCardNumber;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.SessionLookup;
import edu.seu.vcampus.server.security.AccountProvisioning;
import edu.seu.vcampus.server.security.ProvisionedAccount;
import edu.seu.vcampus.server.security.UserDirectory;
import edu.seu.vcampus.server.security.UserIdentity;
import edu.seu.vcampus.server.security.TeacherDirectory;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Year;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Authentication service with in-memory sessions and a pluggable account repository.
 */
public final class InMemoryAuthenticationService
        implements SessionLookup, AccountProvisioning, UserDirectory {

    private static final int TOKEN_BYTES = 32;
    static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(30);
    static final Duration DEFAULT_ABSOLUTE_TIMEOUT = Duration.ofHours(8);
    static final int DEFAULT_MAXIMUM_LOGIN_FAILURES = 5;
    static final Duration DEFAULT_LOGIN_FAILURE_WINDOW = Duration.ofMinutes(10);
    static final Duration DEFAULT_LOGIN_LOCK_DURATION = Duration.ofMinutes(5);

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository users;
    private final Clock clock;
    private final Duration idleTimeout;
    private final Duration absoluteTimeout;
    private final UserAuditRepository auditLogs;
    private final TeacherRegistryService teacherRegistry;
    private final LoginAttemptLimiter loginAttempts;
    private final ConcurrentMap<String, StoredSession> sessions = new ConcurrentHashMap<>();

    /** Creates authentication backed by the public development accounts. */
    public InMemoryAuthenticationService() {
        this(DemoUserAccounts.createRepository());
    }

    InMemoryAuthenticationService(UserRepository users) {
        this(users, Clock.systemUTC(), DEFAULT_IDLE_TIMEOUT, DEFAULT_ABSOLUTE_TIMEOUT);
    }

    InMemoryAuthenticationService(
            UserRepository users,
            Clock clock,
            Duration idleTimeout,
            Duration absoluteTimeout) {
        this.users = Objects.requireNonNull(users, "users must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idleTimeout = requirePositive(idleTimeout, "idleTimeout");
        this.absoluteTimeout = requirePositive(absoluteTimeout, "absoluteTimeout");
        this.loginAttempts = new LoginAttemptLimiter(
                DEFAULT_MAXIMUM_LOGIN_FAILURES,
                DEFAULT_LOGIN_FAILURE_WINDOW,
                DEFAULT_LOGIN_LOCK_DURATION);
        this.auditLogs = users instanceof AccessUserRepository accessRepository
                ? new AccessUserAuditRepository(accessRepository.database())
                : new InMemoryUserAuditRepository();
        this.teacherRegistry = new TeacherRegistryService(
                users,
                users instanceof AccessUserRepository accessRepository
                        ? new AccessTeacherRepository(accessRepository.database())
                        : new InMemoryTeacherRepository(),
                clock);
    }

    UserRepository users() {
        return users;
    }

    UserAuditRepository auditLogs() {
        return auditLogs;
    }

    TeacherRegistryService teachers() {
        return teacherRegistry;
    }

    /** Returns the read-only teacher directory exposed to other server modules. */
    public TeacherDirectory teacherDirectory() {
        return teacherRegistry;
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
                || !CampusCardNumber.isValid(request.getUsername())
                || request.getPasswordProof() == null
                || !request.getPasswordProof().matches("[0-9a-f]{64}")) {
            return Optional.empty();
        }
        String username = request.getUsername();
        Instant now = clock.instant();
        if (loginAttempts.isBlocked(username, now)) {
            return Optional.empty();
        }
        UserAccount user = users.findByUsername(username).orElse(null);
        if (user == null
                || !user.enabled()
                || !proofMatches(user.passwordProof(), request.getPasswordProof())) {
            loginAttempts.recordFailure(username, now);
            return Optional.empty();
        }

        loginAttempts.recordSuccess(username);

        SessionInfo session = new SessionInfo(
                createToken(),
                user.userId(),
                user.username(),
                user.displayName(),
                user.role(),
                user.adminScopes());
        purgeExpiredSessions(now);
        sessions.put(session.getToken(), new StoredSession(session, now, now));
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

    /** Replaces an account password after verifying its current password. */
    synchronized boolean changePassword(
            String userId,
            String currentPasswordProof,
            String newPasswordProof) {
        if (!isPasswordProof(currentPasswordProof) || !isPasswordProof(newPasswordProof)) {
            return false;
        }
        UserAccount user = users.findById(userId).orElse(null);
        if (user == null
                || !proofMatches(user.passwordProof(), currentPasswordProof)) {
            return false;
        }
        users.save(user.withPasswordProof(newPasswordProof));
        invalidateUserSessions(userId);
        return true;
    }

    /** Invalidates every active session belonging to an account. */
    void invalidateUserSessions(String userId) {
        sessions.entrySet().removeIf(
                entry -> entry.getValue().session().getUserId().equals(userId));
    }

    @Override
    public Optional<SessionInfo> findSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        StoredSession active = sessions.computeIfPresent(token, (ignored, stored) ->
                stored.isExpired(now, idleTimeout, absoluteTimeout)
                        ? null
                        : stored.accessed(now));
        return active == null ? Optional.empty() : Optional.of(active.session());
    }

    @Override
    public synchronized Optional<ProvisionedAccount> findAccountByUsername(String username) {
        if (!CampusCardNumber.isValid(username)) {
            return Optional.empty();
        }
        String normalized = CampusCardNumber.normalize(username);
        return users.findByUsername(normalized).map(InMemoryAuthenticationService::toProvisioned);
    }

    @Override
    public synchronized Optional<UserIdentity> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return users.findById(userId).map(InMemoryAuthenticationService::toIdentity);
    }

    @Override
    public synchronized Optional<UserIdentity> findByCampusCardNumber(
            String campusCardNumber) {
        if (!CampusCardNumber.isValid(campusCardNumber)) {
            return Optional.empty();
        }
        String normalized = CampusCardNumber.normalize(campusCardNumber);
        return users.findByUsername(normalized).map(InMemoryAuthenticationService::toIdentity);
    }

    @Override
    public synchronized ProvisionedAccount createGeneratedRegularAccount(String displayName) {
        return toProvisioned(createGeneratedRegularAccount(displayName, Set.of()));
    }

    synchronized String nextGeneratedUsername() {
        int year = Year.now().getValue();
        int maximumSequence = users.findAll().stream()
                .map(UserAccount::username)
                .filter(CampusCardNumber::isValid)
                .filter(value -> value.startsWith(Integer.toString(year)))
                .mapToInt(CampusCardNumber::sequence)
                .max()
                .orElse(0);
        if (maximumSequence >= CampusCardNumber.MAX_SEQUENCE) {
            throw new IllegalStateException(
                    "The campus-card sequence for " + year + " is exhausted.");
        }
        return CampusCardNumber.format(year, maximumSequence + 1);
    }

    synchronized UserAccount createGeneratedRegularAccount(
            String displayName,
            Set<edu.seu.vcampus.common.user.AdminScope> adminScopes) {
        int year = Year.now().getValue();
        int nextSequence = CampusCardNumber.sequence(nextGeneratedUsername());
        char[] password = "123456".toCharArray();
        try {
            for (int sequence = nextSequence;
                 sequence <= CampusCardNumber.MAX_SEQUENCE;
                 sequence++) {
                String username = CampusCardNumber.format(year, sequence);
                CreateAccountInput input = new CreateAccountInput(
                        username,
                        displayName,
                        PasswordProof.create(username, password),
                        adminScopes);
                if (users.findByUsername(input.username()).isPresent()) {
                    continue;
                }
                UserAccount created = new UserAccount(
                        "U-" + UUID.randomUUID().toString().replace("-", ""),
                        input.username(),
                        input.displayName(),
                        Role.USER,
                        input.adminScopes(),
                        input.passwordProof(),
                        true);
                users.save(created);
                return created;
            }
            throw new IllegalStateException(
                    "The campus-card sequence for " + year + " is exhausted.");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static ProvisionedAccount toProvisioned(UserAccount account) {
        return new ProvisionedAccount(
                account.userId(), account.username(), account.displayName(), account.enabled());
    }

    private static UserIdentity toIdentity(UserAccount account) {
        return new UserIdentity(
                account.userId(), account.username(), account.displayName(), account.enabled());
    }

    private record CreateAccountInput(
            String username,
            String displayName,
            String passwordProof,
            Set<edu.seu.vcampus.common.user.AdminScope> adminScopes) {
        private CreateAccountInput {
            edu.seu.vcampus.common.user.CreateUserAccountRequest validated =
                    new edu.seu.vcampus.common.user.CreateUserAccountRequest(
                            username, displayName, passwordProof, adminScopes);
            username = validated.getUsername();
            displayName = validated.getDisplayName();
            passwordProof = validated.getPasswordProof();
            adminScopes = validated.getAdminScopes();
        }
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

    private static boolean isPasswordProof(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private void purgeExpiredSessions(Instant now) {
        sessions.entrySet().removeIf(entry ->
                entry.getValue().isExpired(now, idleTimeout, absoluteTimeout));
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private record StoredSession(
            SessionInfo session,
            Instant createdAt,
            Instant lastAccessAt) {

        private StoredSession accessed(Instant now) {
            return new StoredSession(session, createdAt, now);
        }

        private boolean isExpired(
                Instant now,
                Duration idleTimeout,
                Duration absoluteTimeout) {
            return !now.isBefore(lastAccessAt.plus(idleTimeout))
                    || !now.isBefore(createdAt.plus(absoluteTimeout));
        }
    }
}
