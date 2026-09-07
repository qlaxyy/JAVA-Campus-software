package edu.seu.vcampus.server.module.library;

/** Transaction boundary used by circulation operations that update multiple repositories. */
@FunctionalInterface
interface LibraryTransactionManager {

    /** Runs one business operation atomically. */
    void execute(Runnable operation);

    /** In-memory repositories already provide the atomicity required by their service lock. */
    static LibraryTransactionManager passthrough() {
        return Runnable::run;
    }
}
