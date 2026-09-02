package edu.seu.vcampus.server.module.user;

/** Signals that account data could not be read from or written to Access. */
final class UserPersistenceException extends RuntimeException {

    UserPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
