package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.CampusCardNumber;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.SessionLookup;
import edu.seu.vcampus.server.security.AccountProvisioning;
import edu.seu.vcampus.server.security.ProvisionedAccount;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Authentication service with in-memory sessions and a pluggable account repository.
 */
public final class InMemoryAuthenticationService
        implements SessionLookup, AccountProvisioning {

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

    @Override
    public synchronized Optional<ProvisionedAccount> findAccountByUsername(String username) {
        if (!CampusCardNumber.isValid(username)) {
            return Optional.empty();
        }
        String normalized = CampusCardNumber.normalize(username);
        return users.findByUsername(normalized).map(InMemoryAuthenticationService::toProvisioned);
    }

    @Override
    public synchronized ProvisionedAccount createGeneratedRegularAccount(String displayName) {
        int year = Year.now().getValue();
        int maximumSequence = users.findAll().stream()
                .map(UserAccount::username)
                .filter(CampusCardNumber::isValid)
                .filter(value -> value.startsWith(Integer.toString(year)))
                .mapToInt(CampusCardNumber::sequence)
                .max()
                .orElse(0);
        char[] password = "123456".toCharArray();
        try {
            for (int sequence = maximumSequence + 1;
                 sequence <= CampusCardNumber.MAX_SEQUENCE;
                 sequence++) {
                String username = CampusCardNumber.format(year, sequence);
                CreateAccountInput input = new CreateAccountInput(
                        username,
                        displayName,
                        PasswordProof.create(username, password));
                if (users.findByUsername(input.username()).isPresent()) {
                    continue;
                }
                UserAccount created = new UserAccount(
                        "U-" + UUID.randomUUID().toString().replace("-", ""),
                        input.username(),
                        input.displayName(),
                        Role.USER,
                        Set.of(),
                        input.passwordProof(),
                        true);
                users.save(created);
                return toProvisioned(created);
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

    private record CreateAccountInput(
            String username,
            String displayName,
            String passwordProof) {
        private CreateAccountInput {
            edu.seu.vcampus.common.user.CreateUserAccountRequest validated =
                    new edu.seu.vcampus.common.user.CreateUserAccountRequest(
                            username, displayName, passwordProof, Set.of());
            username = validated.getUsername();
            displayName = validated.getDisplayName();
            passwordProof = validated.getPasswordProof();
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
}
