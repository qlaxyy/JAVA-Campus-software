package edu.seu.vcampus.server.module.hospital;

/** Recoverable model, network, or response-validation failure. */
final class HospitalAiTriageException extends Exception {

    HospitalAiTriageException(String message) {
        super(message);
    }

    HospitalAiTriageException(String message, Throwable cause) {
        super(message, cause);
    }
}
