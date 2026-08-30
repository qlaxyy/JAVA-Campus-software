package edu.seu.vcampus.server.module.user;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for user accounts. */
interface UserRepository {
    Optional<UserAccount> findById(String userId);
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findAll();
    void save(UserAccount account);
}
