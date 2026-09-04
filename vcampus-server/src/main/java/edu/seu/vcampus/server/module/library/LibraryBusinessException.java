package edu.seu.vcampus.server.module.library;

import java.util.Objects;

/** Expected library business rejection with a stable public error code. */
final class LibraryBusinessException extends RuntimeException {

    private final String code;

    LibraryBusinessException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    String code() {
        return code;
    }
}
