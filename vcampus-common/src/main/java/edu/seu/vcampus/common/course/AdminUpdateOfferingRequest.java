package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教务修改教学班设置请求。
 */
public final class AdminUpdateOfferingRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    private final long batchId;
    private final long offeringId;
    private final int capacity;
    private final boolean open;
    private final String reason;

    public AdminUpdateOfferingRequest(
        long batchId,
        long offeringId,
        int capacity,
        boolean open,
        String reason) {

        if (batchId <= 0) {

            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        if (offeringId <= 0) {

            throw new IllegalArgumentException(
                "offeringId must be positive");
        }

        if (capacity < 0) {

            throw new IllegalArgumentException(
                "capacity must not be negative");
        }

        this.batchId =
            batchId;

        this.offeringId =
            offeringId;

        this.capacity =
            capacity;

        this.open =
            open;

        this.reason =
            requireText(
                reason,
                "reason");
    }

    public long getBatchId() {

        return batchId;
    }

    public long getOfferingId() {

        return offeringId;
    }

    public int getCapacity() {

        return capacity;
    }

    public boolean isOpen() {

        return open;
    }

    public String getReason() {

        return reason;
    }

    private static String requireText(
        String value,
        String fieldName) {

        Objects.requireNonNull(
            value,
            fieldName + " must not be null");

        String cleaned =
            value.trim();

        if (cleaned.isBlank()) {

            throw new IllegalArgumentException(
                fieldName + " must not be blank");
        }

        return cleaned;
    }
}
