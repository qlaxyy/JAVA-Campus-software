package edu.seu.vcampus.server.module.hospital;

/** Expected business failure in doctor onboarding. */
final class HospitalWorkflowException extends RuntimeException {
    private final String code;

    HospitalWorkflowException(String code, String message) {
        super(message);
        this.code = code;
    }

    String code() { return code; }
}
