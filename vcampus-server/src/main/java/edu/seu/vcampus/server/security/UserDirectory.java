package edu.seu.vcampus.server.security;

import java.util.Optional;

/** Read-only account identity lookup available to server-side business modules. */
public interface UserDirectory {

    /** Finds an existing account by its stable internal user identifier. */
    Optional<UserIdentity> findByUserId(String userId);

    /** Finds an existing account by its unique campus-card number. */
    Optional<UserIdentity> findByCampusCardNumber(String campusCardNumber);
}
