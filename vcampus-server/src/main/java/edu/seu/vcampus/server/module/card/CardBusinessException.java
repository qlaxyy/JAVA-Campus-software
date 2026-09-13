package edu.seu.vcampus.server.module.card;

import java.util.Objects;

/** Expected campus-card rejection with a stable public error code. */
public final class CardBusinessException extends RuntimeException {

    private final String code;

    /**
     * Creates a business rejection.
     *
     * @param code public error code
     * @param message client-safe message
     */
    public CardBusinessException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public String code() {
        return code;
    }
}
