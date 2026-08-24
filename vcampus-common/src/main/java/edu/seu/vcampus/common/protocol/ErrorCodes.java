package edu.seu.vcampus.common.protocol;

/**
 * Error codes that are safe to send to clients.
 */
public final class ErrorCodes {

    public static final String SUCCESS = "SUCCESS";
    public static final String COMMON_INVALID_REQUEST = "COMMON_INVALID_REQUEST";
    public static final String COMMON_UNKNOWN_ACTION = "COMMON_UNKNOWN_ACTION";
    public static final String COMMON_SERVER_ERROR = "COMMON_SERVER_ERROR";

    private ErrorCodes() {
    }
}
