package edu.seu.vcampus.server.security;

import java.util.Objects;

/** Minimal read-only identity shared with server-side business modules. */
public record UserIdentity(
        String userId,
        String campusCardNumber,
        String displayName,
        boolean enabled) {
    public UserIdentity {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(campusCardNumber, "campusCardNumber must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
    }
}
