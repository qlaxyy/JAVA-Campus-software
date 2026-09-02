package edu.seu.vcampus.common.protocol;

/**
 * Error codes that are safe to send to clients.
 */
public final class ErrorCodes {

    public static final String SUCCESS = "SUCCESS";
    public static final String COMMON_INVALID_REQUEST = "COMMON_INVALID_REQUEST";
    public static final String COMMON_UNKNOWN_ACTION = "COMMON_UNKNOWN_ACTION";
    public static final String COMMON_SERVER_ERROR = "COMMON_SERVER_ERROR";
    public static final String COMMON_INVALID_ARGUMENT = "COMMON_INVALID_ARGUMENT";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";
    public static final String USER_ACCOUNT_NOT_FOUND = "USER_ACCOUNT_NOT_FOUND";
    public static final String USER_USERNAME_EXISTS = "USER_USERNAME_EXISTS";
    public static final String USER_SELF_DISABLE_FORBIDDEN = "USER_SELF_DISABLE_FORBIDDEN";
    public static final String LIBRARY_BOOK_NOT_FOUND = "LIBRARY_BOOK_NOT_FOUND";
    public static final String LIBRARY_NO_AVAILABLE_COPY = "LIBRARY_NO_AVAILABLE_COPY";
    public static final String LIBRARY_BORROW_LIMIT_REACHED = "LIBRARY_BORROW_LIMIT_REACHED";
    public static final String LIBRARY_ALREADY_BORROWED = "LIBRARY_ALREADY_BORROWED";
    public static final String LIBRARY_OVERDUE_BORROW_EXISTS =
            "LIBRARY_OVERDUE_BORROW_EXISTS";

    private ErrorCodes() {
    }
}
