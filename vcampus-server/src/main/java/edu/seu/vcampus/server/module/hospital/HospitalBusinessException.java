package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** Expected hospital rule failure that can be safely mapped to a client error code. */
final class HospitalBusinessException extends RuntimeException {

    private final String errorCode;

    HospitalBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    String errorCode() {
        return errorCode;
    }
}
