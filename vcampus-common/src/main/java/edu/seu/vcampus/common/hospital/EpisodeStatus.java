package edu.seu.vcampus.common.hospital;

/** Lifecycle of one continuous clinical episode. */
public enum EpisodeStatus {
    IN_PROGRESS,
    WAITING_FOR_RESULTS,
    RESULT_READY,
    COMPLETED,
    CANCELLED
}
