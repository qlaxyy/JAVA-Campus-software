package edu.seu.vcampus.server.security;

import java.util.Objects;

/** Minimal identity returned to an approved subsystem onboarding workflow. */
public record ProvisionedAccount(
        String userId,
        String username,
        String displayName,
        boolean enabled) {
    public ProvisionedAccount {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(username);
        Objects.requireNonNull(displayName);
    }
}
