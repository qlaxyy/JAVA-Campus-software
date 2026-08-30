package edu.seu.vcampus.server.module.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Temporary thread-safe account repository used before the Access DAO is integrated. */
final class InMemoryUserRepository implements UserRepository {

    private final ConcurrentMap<String, UserAccount> accountsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> userIdsByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findById(String userId) {
        return Optional.ofNullable(accountsById.get(userId));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        String userId = userIdsByUsername.get(username);
        return userId == null ? Optional.empty() : findById(userId);
    }

    @Override
    public List<UserAccount> findAll() {
        return new ArrayList<>(accountsById.values());
    }

    @Override
    public void save(UserAccount account) {
        UserAccount previous = accountsById.put(account.userId(), account);
        if (previous != null && !previous.username().equals(account.username())) {
            userIdsByUsername.remove(previous.username(), previous.userId());
        }
        userIdsByUsername.put(account.username(), account.userId());
    }
}
