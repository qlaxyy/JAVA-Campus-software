package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Read-only description of one account-management operation. */
public final class UserAuditLogEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String auditId;
    private final long occurredAtEpochMillis;
    private final String actorUserId;
    private final String actorUsername;
    private final String actorDisplayName;
    private final String actionCode;
    private final String target;
    private final boolean successful;
    private final String detail;

    public UserAuditLogEntry(
            String auditId,
            long occurredAtEpochMillis,
            String actorUserId,
            String actorUsername,
            String actorDisplayName,
            String actionCode,
            String target,
            boolean successful,
            String detail) {
        this.auditId = requireText(auditId, "auditId");
        if (occurredAtEpochMillis <= 0) {
            throw new IllegalArgumentException("occurredAtEpochMillis must be positive");
        }
        this.occurredAtEpochMillis = occurredAtEpochMillis;
        this.actorUserId = requireText(actorUserId, "actorUserId");
        this.actorUsername = requireText(actorUsername, "actorUsername");
        this.actorDisplayName = requireText(actorDisplayName, "actorDisplayName");
        this.actionCode = requireText(actionCode, "actionCode");
        this.target = requireText(target, "target");
        this.successful = successful;
        this.detail = requireText(detail, "detail");
    }

    public String getAuditId() { return auditId; }
    public long getOccurredAtEpochMillis() { return occurredAtEpochMillis; }
    public String getActorUserId() { return actorUserId; }
    public String getActorUsername() { return actorUsername; }
    public String getActorDisplayName() { return actorDisplayName; }
    public String getActionCode() { return actionCode; }
    public String getTarget() { return target; }
    public boolean isSuccessful() { return successful; }
    public String getDetail() { return detail; }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
