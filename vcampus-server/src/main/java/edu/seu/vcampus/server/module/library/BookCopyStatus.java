package edu.seu.vcampus.server.module.library;

/** Lifecycle states of one physical library copy. */
enum BookCopyStatus {
    AVAILABLE,
    LOANED,
    WAITING_SHELVING,
    WITHDRAWN
}
