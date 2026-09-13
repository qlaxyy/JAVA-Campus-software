package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.demo.FinalDemoRoster;

import java.util.Arrays;
import java.util.Set;

/** Creates the public development accounts used by local development and tests. */
final class DemoUserAccounts {

    private DemoUserAccounts() {
    }

    static InMemoryUserRepository createRepository() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        seedIfEmpty(repository);
        return repository;
    }

    /** Seeds a newly created repository without overwriting persisted changes. */
    static void seedIfEmpty(UserRepository repository) {
        if (!repository.findAll().isEmpty()) {
            return;
        }
        repository.saveAll(FinalDemoRoster.accounts().stream()
                .map(seed -> account(
                        seed.userId(), seed.campusCardNumber(), seed.displayName(),
                        seed.role(), seed.adminScopes()))
                .toList());
    }

    private static UserAccount account(
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> scopes) {
        char[] password = FinalDemoRoster.INITIAL_PASSWORD.toCharArray();
        try {
            return new UserAccount(
                    userId, username, displayName, role, scopes,
                    PasswordProof.create(username, password), true);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
