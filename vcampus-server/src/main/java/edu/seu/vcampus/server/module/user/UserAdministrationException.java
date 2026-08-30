package edu.seu.vcampus.server.module.user;

/** Expected account-management failure mapped to a public error code. */
final class UserAdministrationException extends RuntimeException {
    private final String code;

    UserAdministrationException(String code, String message) {
        super(message);
        this.code = code;
    }

    String code() {
        return code;
    }
}
