package edu.seu.vcampus.common.hospital;

/** Lifecycle of one simplified examination order. */
public enum ExaminationStatus {
    ORDERED,
    RESULT_READY,
    REVIEWED,
    CANCELLED
}
