package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.nio.file.Path;

/** Creates production authentication components without exposing repository internals. */
public final class UserAuthenticationBootstrap {

    private UserAuthenticationBootstrap() {
    }

    /** Creates authentication backed by the given Access database file. */
    public static InMemoryAuthenticationService createAccessBacked(Path databasePath) {
        AccessUserRepository repository = new AccessUserRepository(
                new AccessDatabase(databasePath));
        DemoUserAccounts.seedIfEmpty(repository);
        return new InMemoryAuthenticationService(repository);
    }
}
