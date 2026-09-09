package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Course-only request that simulates an external examination system reporting. */
public final class PublishDemoExaminationReportRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String orderId;

    public PublishDemoExaminationReportRequest(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        this.orderId = orderId.trim();
    }

    public String getOrderId() {
        return orderId;
    }
}
