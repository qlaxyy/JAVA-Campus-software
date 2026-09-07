package edu.seu.vcampus.server.module.library;

/** Unchecked boundary exception for Access/JDBC failures in the library module. */
final class LibraryPersistenceException extends RuntimeException {

    LibraryPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
